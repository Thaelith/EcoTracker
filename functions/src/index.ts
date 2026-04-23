import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import { GoogleGenerativeAI, HarmCategory, HarmBlockThreshold } from "@google/generative-ai";

admin.initializeApp();

// ── Configuration ───────────────────────────────────────────────────────────

const GEMINI_API_KEY = functions.config().gemini?.key || process.env.GEMINI_API_KEY;
const RATE_LIMIT_WINDOW_MS = 60 * 1000; // 1 minute
const RATE_LIMIT_MAX_REQUESTS = 30;

// ── Rate Limiting ───────────────────────────────────────────────────────────

async function checkRateLimit(clientId: string): Promise<boolean> {
  const now = Date.now();
  const db = admin.firestore();
  const rateLimitRef = db.collection("rate_limits").doc(clientId);

  try {
    return await db.runTransaction(async (transaction) => {
      const doc = await transaction.get(rateLimitRef);
      if (!doc.exists) {
        transaction.set(rateLimitRef, { count: 1, resetAt: now + RATE_LIMIT_WINDOW_MS });
        return true;
      }

      const data = doc.data();
      if (!data) return true;

      if (now > data.resetAt) {
        transaction.set(rateLimitRef, { count: 1, resetAt: now + RATE_LIMIT_WINDOW_MS });
        return true;
      }

      if (data.count >= RATE_LIMIT_MAX_REQUESTS) {
        return false;
      }

      transaction.update(rateLimitRef, { count: data.count + 1 });
      return true;
    });
  } catch (e) {
    console.error("Rate limit check failed", e);
    return true; // fail open if db unavailable
  }
}

// ── Prompt Builders ─────────────────────────────────────────────────────────

function buildEstimationPrompt(
  productTitle: string,
  category?: string,
  quantity?: string
): string {
  const catStr = category ? `It belongs to the category: ${category}.` : "";
  const qtyStr = quantity ? `The product size/quantity is: ${quantity}.` : "";

  return `
You are a strict environmental data scientist. Analyze this product: "${productTitle}".
${catStr}
${qtyStr}
Estimate the lifecycle carbon footprint in kg CO2e for this EXACT product size.
Return ONLY a JSON object with this exact structure:
{
  "estimated_category": "string",
  "kg_co2e": double,
  "reasoning": "A concise 2-3 sentence explanation of the primary carbon drivers (materials, production, and transport).",
  "confidence": "High/Medium/Low",
  "data_quality_flag": "Carbon Expert Estimate"
}
Do not include markdown formatting or any text outside the JSON.
`.trim();
}

function buildIdentificationPrompt(barcode: string, userHint: string): string {
  return `
You are a universal product database. The user scanned the barcode "${barcode}" but we couldn't find it.
The user has provided a helpful description of the product: "${userHint}".
Using the barcode number and the user's description, identify the exact product.
Estimate its lifecycle carbon footprint in kg CO2e.
Return ONLY a JSON object with this exact structure:
{
  "product_name": "string",
  "estimated_category": "string",
  "kg_co2e": double,
  "reasoning": "A concise 2-3 sentence explanation of why the user's description helped identify this specific product and its primary carbon impact.",
  "confidence": "Medium",
  "data_quality_flag": "User-Assisted Estimate"
}
Do not include markdown formatting or any text outside the JSON.
`.trim();
}

// ── Gemini Client ───────────────────────────────────────────────────────────

function getGeminiModel() {
  if (!GEMINI_API_KEY) {
    throw new Error("Gemini API key is not configured");
  }
  const genAI = new GoogleGenerativeAI(GEMINI_API_KEY);
  return genAI.getGenerativeModel({
    model: "gemini-2.5-flash",
    safetySettings: [
      {
        category: HarmCategory.HARM_CATEGORY_HARASSMENT,
        threshold: HarmBlockThreshold.BLOCK_NONE,
      },
      {
        category: HarmCategory.HARM_CATEGORY_HATE_SPEECH,
        threshold: HarmBlockThreshold.BLOCK_NONE,
      },
      {
        category: HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT,
        threshold: HarmBlockThreshold.BLOCK_NONE,
      },
      {
        category: HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT,
        threshold: HarmBlockThreshold.BLOCK_NONE,
      },
    ],
  });
}

async function callGemini(prompt: string): Promise<Record<string, unknown> | null> {
  const model = getGeminiModel();
  const result = await model.generateContent(prompt);
  const response = result.response;
  const text = response.text()
    .replace(/\`\`\`json/g, "")
    .replace(/\`\`\`/g, "")
    .trim();

  if (!text) return null;

  try {
    return JSON.parse(text) as Record<string, unknown>;
  } catch {
    console.error("Failed to parse Gemini response as JSON:", text.substring(0, 200));
    return null;
  }
}

// ── Cloud Functions ─────────────────────────────────────────────────────────

/**
 * Proxy endpoint for carbon footprint estimation.
 * POST /estimate
 * Body: { productTitle: string, category?: string, quantity?: string }
 */
export const estimateCarbonFootprint = functions.https.onCall(async (data, context) => {
  // Authentication check (optional but recommended)
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "User must be authenticated to use this feature."
    );
  }

  // Rate limiting
  const clientId = context.auth.uid;
  if (!(await checkRateLimit(clientId))) {
    throw new functions.https.HttpsError(
      "resource-exhausted",
      "Rate limit exceeded. Please try again later."
    );
  }


  const { productTitle, category, quantity } = data as {
    productTitle?: string;
    category?: string;
    quantity?: string;
  };

  const safeProductTitle = productTitle?.trim().substring(0, 100);
  const safeCategory = category?.trim().substring(0, 50);
  const safeQuantity = quantity?.trim().substring(0, 20);

  if (!safeProductTitle) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "productTitle is required and must be a valid string."
    );
  }

  try {
    const prompt = buildEstimationPrompt(safeProductTitle, safeCategory, safeQuantity);
    const result = await callGemini(prompt);

    if (!result) {
      throw new functions.https.HttpsError(
        "internal",
        "Failed to get a valid response from the AI model."
      );
    }

    return {
      estimatedCategory: result.estimated_category as string || "Unknown",
      kgCo2e: typeof result.kg_co2e === "number" ? result.kg_co2e : null,
      reasoning: result.reasoning as string || "No reasoning provided",
      confidence: result.confidence as string || "Unknown",
      dataQuality: result.data_quality_flag as string || "Expert Estimate",
    };
  } catch (error) {
    console.error("estimateCarbonFootprint error:", error);
    throw new functions.https.HttpsError(
      "internal",
      "An error occurred while processing your request."
    );
  }
});

/**
 * Proxy endpoint for product identification with user hint.
 * POST /identify
 * Body: { barcode: string, userHint: string }
 */
export const identifyProduct = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "User must be authenticated to use this feature."
    );
  }

  const clientId = context.auth.uid;
  if (!(await checkRateLimit(clientId))) {
    throw new functions.https.HttpsError(
      "resource-exhausted",
      "Rate limit exceeded. Please try again later."
    );
  }

  const { barcode, userHint } = data as {
    barcode?: string;
    userHint?: string;
  };

  const safeBarcode = barcode?.trim().substring(0, 30);
  const safeUserHint = userHint?.trim().substring(0, 200);

  if (!safeBarcode) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "barcode is required and must be a string."
    );
  }

  if (!safeUserHint) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "userHint is required and must be a valid string."
    );
  }

  try {
    const prompt = buildIdentificationPrompt(safeBarcode, safeUserHint);
    const result = await callGemini(prompt);

    if (!result) {
      throw new functions.https.HttpsError(
        "internal",
        "Failed to get a valid response from the AI model."
      );
    }

    return {
      productName: result.product_name as string || "Unknown Product",
      estimatedCategory: result.estimated_category as string || "Unknown",
      kgCo2e: typeof result.kg_co2e === "number" ? result.kg_co2e : null,
      reasoning: result.reasoning as string || "No reasoning provided",
      confidence: result.confidence as string || "Medium",
      dataQuality: result.data_quality_flag as string || "User-Assisted Estimate",
    };
  } catch (error) {
    console.error("identifyProduct error:", error);
    throw new functions.https.HttpsError(
      "internal",
      "An error occurred while processing your request."
    );
  }
});
