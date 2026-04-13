package com.ecotracker.data.remote

import com.ecotracker.BuildConfig
import com.ecotracker.data.local.EstimationStatus
import com.ecotracker.data.local.ScannedProduct
import com.ecotracker.utils.Logger
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Structured analysis result from Gemini.
 */
data class GeminiAnalysis(
    val estimatedCategory: String,
    val kgCo2e: Double?,
    val reasoning: String,
    val confidence: String,
    val dataQuality: String
)

object GeminiCarbonService {

    private val apiKey = BuildConfig.GEMINI_API_KEY

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = apiKey
        )
    }

    private const val TAG = "GeminiService"

    /**
     * Prompts the LLM to estimate a carbon footprint with full metadata.
     */
    suspend fun estimateCarbonFootprint(productTitle: String, category: String?, quantity: String? = null): GeminiAnalysis? {
        Logger.debug(TAG, "Estimating footprint for '$productTitle'")

        if (apiKey.isBlank()) {
            Logger.error(TAG, "Gemini API key is not configured")
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

                Logger.debug(TAG, "Fetching estimation...")
                val response = generativeModel.generateContent(prompt)
                val text = response.text?.replace("```json", "")?.replace("```", "")?.trim()

                if (text.isNullOrBlank()) return@withContext null

                val json = com.google.gson.JsonParser.parseString(text).asJsonObject

                fun getString(key: String, default: String) = json.get(key)?.let {
                    if (it.isJsonPrimitive) it.asString else default
                } ?: default

                fun getDouble(key: String): Double? = json.get(key)?.let {
                    if (it.isJsonPrimitive && (it.asJsonPrimitive.isNumber || it.asJsonPrimitive.isString)) {
                        try { it.asDouble } catch (e: Exception) { null }
                    } else null
                }

                GeminiAnalysis(
                    estimatedCategory = getString("estimated_category", "Unknown"),
                    kgCo2e = getDouble("kg_co2e"),
                    reasoning = getString("reasoning", "No reasoning provided"),
                    confidence = getString("confidence", "Unknown"),
                    dataQuality = getString("data_quality_flag", "Expert Estimate")
                ).also {
                    Logger.debug(TAG, "Analysis complete: category=${it.estimatedCategory}, co2e=${it.kgCo2e}")
                }
            } catch (e: Exception) {
                Logger.error(TAG, "Estimation failed: ${e.javaClass.simpleName}", e)
                null
            }
        }
    }

    /**
     * Prompts the LLM to identify a product using its barcode and a helpful hint from the user.
     */
    suspend fun identifyProductWithUserHint(barcode: String, userHint: String): ScannedProduct? {
        val masked = Logger.maskBarcode(barcode)
        Logger.debug(TAG, "Identifying $masked with user description")

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

                Logger.debug(TAG, "Querying for barcode identification...")
                val response = generativeModel.generateContent(prompt)
                val text = response.text?.replace("```json", "")?.replace("```", "")?.trim()

                if (text.isNullOrBlank()) return@withContext null

                val json = com.google.gson.JsonParser.parseString(text).asJsonObject

                fun getString(key: String, default: String) = json.get(key)?.let {
                    if (it.isJsonPrimitive) it.asString else default
                } ?: default

                fun getDouble(key: String): Double? = json.get(key)?.let {
                    if (it.isJsonPrimitive && (it.asJsonPrimitive.isNumber || it.asJsonPrimitive.isString)) {
                        try { it.asDouble } catch (e: Exception) { null }
                    } else null
                }

                ScannedProduct(
                    barcode = barcode,
                    productName = getString("product_name", "Unknown Product"),
                    brand = "",
                    categories = getString("estimated_category", "Unknown"),
                    imageUrl = "",
                    ecoScore = "not-applicable",
                    ecoScoreValue = -1,
                    carbonFootprint = getDouble("kg_co2e"),
                    status = EstimationStatus.AI_ESTIMATED,
                    aiReasoning = getString("reasoning", "No reasoning provided"),
                    aiConfidence = getString("confidence", "Medium"),
                    aiDataQuality = getString("data_quality_flag", "User-Assisted Estimate")
                ).also {
                    Logger.debug(TAG, "Identification successful: ${it.productName}")
                }
            } catch (e: Exception) {
                Logger.error(TAG, "Identification failed: ${e.javaClass.simpleName}", e)
                null
            }
        }
    }
}
