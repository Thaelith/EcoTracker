package com.ecotracker.data.remote

import com.ecotracker.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service responsible for asking Gemini to estimate the carbon footprint of an unknown product.
 */
/**
 * Structured analysis result from Gemini.
 */
data class GeminiAnalysis(
    val estimatedCategory: String,
    val kgCo2e: Double,
    val reasoning: String,
    val confidence: String,
    val dataQuality: String
)

object GeminiCarbonService {

    // Make sure you provide GEMINI_API_KEY in local.properties
    private val apiKey = BuildConfig.GEMINI_API_KEY

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = apiKey
        )
    }

    private const val LOG_TAG = "CarbonService"

    /**
     * Prompts the LLM to estimate a carbon footprint with full metadata.
     */
    suspend fun estimateCarbonFootprint(productTitle: String, category: String?, quantity: String? = null): GeminiAnalysis? {
        android.util.Log.d(LOG_TAG, "Called with title='$productTitle', category='$category', quantity='$quantity'")
        
        if (apiKey.isBlank()) {
            android.util.Log.e(LOG_TAG, "API Key is BLANK! Check local.properties.")
            return null
        }
        
        return withContext(Dispatchers.IO) {
            try {
                val catStr = if (!category.isNullOrBlank()) "It belongs to the category: $category." else ""
                val qtyStr = if (!quantity.isNullOrBlank()) "The product size/quantity is: $quantity." else ""
                val prompt = """
                    You are a strict environmental data scientist. Analyze this product: "$productTitle".
                    $catStr
                    $qtyStr
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
                """.trimIndent()

                android.util.Log.d(LOG_TAG, "Fetching estimation...")
                val response = generativeModel.generateContent(prompt)
                val text = response.text?.replace("```json", "")?.replace("```", "")?.trim()
                
                android.util.Log.d(LOG_TAG, "Raw Response received")

                if (text.isNullOrBlank()) {
                    android.util.Log.e(LOG_TAG, "Response text is empty")
                    return@withContext null
                }

                val json = com.google.gson.JsonParser.parseString(text).asJsonObject
                GeminiAnalysis(
                    estimatedCategory = json.get("estimated_category")?.asString ?: "Unknown",
                    kgCo2e = json.get("kg_co2e")?.asDouble ?: 0.0,
                    reasoning = json.get("reasoning")?.asString ?: "No reasoning provided",
                    confidence = json.get("confidence")?.asString ?: "Unknown",
                    dataQuality = json.get("data_quality_flag")?.asString ?: "Expert Estimate"
                ).also {
                    android.util.Log.d(LOG_TAG, "Analysis Complete: $it")
                }
            } catch (e: Exception) {
                android.util.Log.e(LOG_TAG, "Error during analysis: ${e.message}")
                null
            }
        }
    }

    /**
     * Prompts the LLM to identify a product using its barcode and a helpful hint from the user.
     */
    suspend fun identifyProductWithUserHint(barcode: String, userHint: String): com.ecotracker.data.local.ScannedProduct? {
        android.util.Log.d(LOG_TAG, "Identifying barcode='$barcode' with user description")
        
        if (apiKey.isBlank()) return null
        
        return withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    You are a universal product database. The user scanned the barcode "$barcode" but we couldn't find it.
                    The user has provided a helpful description of the product: "$userHint".
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
                """.trimIndent()

                android.util.Log.d(LOG_TAG, "Querying service for barcode identification...")
                val response = generativeModel.generateContent(prompt)
                val text = response.text?.replace("```json", "")?.replace("```", "")?.trim()
                
                android.util.Log.d(LOG_TAG, "Response received for barcode")

                if (text.isNullOrBlank()) return@withContext null

                val json = com.google.gson.JsonParser.parseString(text).asJsonObject
                
                val productName = json.get("product_name")?.asString ?: "Unknown Product"
                val category = json.get("estimated_category")?.asString ?: "Unknown"
                
                com.ecotracker.data.local.ScannedProduct(
                    barcode = barcode,
                    productName = productName,
                    brand = "",
                    categories = category,
                    imageUrl = "", // No image
                    ecoScore = "not-applicable",
                    ecoScoreValue = -1,
                    carbonFootprint = json.get("kg_co2e")?.asDouble ?: 0.0,
                    aiReasoning = json.get("reasoning")?.asString ?: "No reasoning provided",
                    aiConfidence = json.get("confidence")?.asString ?: "Medium",
                    aiDataQuality = json.get("data_quality_flag")?.asString ?: "User-Assisted Estimate"
                ).also {
                    android.util.Log.d(LOG_TAG, "Identification Successful: ${it.productName}")
                }
            } catch (e: Exception) {
                android.util.Log.e(LOG_TAG, "Identification failed: ${e.message}")
                null
            }
        }
    }
}
