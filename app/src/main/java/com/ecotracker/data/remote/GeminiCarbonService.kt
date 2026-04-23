package com.ecotracker.data.remote

import com.ecotracker.data.local.EstimationStatus
import com.ecotracker.data.local.ScannedProduct
import com.ecotracker.utils.Logger
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

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

@Singleton
class GeminiCarbonService @Inject constructor(
    private val functions: FirebaseFunctions
) {

    companion object {
        private const val TAG = "GeminiService"
        private const val PROXY_FN_ESTIMATE = "estimateCarbonFootprint"
        private const val PROXY_FN_IDENTIFY = "identifyProduct"
    }

    /**
     * Prompts the LLM to estimate a carbon footprint with full metadata.
     * Uses the proxy for all requests to ensure the API key remains secure on the server.
     */
    suspend fun estimateCarbonFootprint(
        productTitle: String,
        category: String?,
        quantity: String? = null
    ): GeminiAnalysis? {
        Logger.debug(TAG, "Estimating footprint for '$productTitle' (proxy=true)")

        return estimateViaProxy(productTitle, category, quantity)
    }

    /**
     * Prompts the LLM to identify a product using its barcode and a helpful hint from the user.
     * Uses the proxy for all requests to ensure the API key remains secure on the server.
     */
    suspend fun identifyProductWithUserHint(barcode: String, userHint: String): ScannedProduct? {
        val masked = Logger.maskBarcode(barcode)
        Logger.debug(TAG, "Identifying $masked with user description (proxy=true)")

        return identifyViaProxy(barcode, userHint)
    }

    // ── Proxy Methods ─────────────────────────────────────────────────────────

    private suspend fun estimateViaProxy(
        productTitle: String,
        category: String?,
        quantity: String?
    ): GeminiAnalysis? = withContext(Dispatchers.IO) {
        try {
            val data = hashMapOf(
                "productTitle" to productTitle,
                "category" to (category ?: ""),
                "quantity" to (quantity ?: "")
            )

            val result = functions
                .getHttpsCallable(PROXY_FN_ESTIMATE)
                .call(data)
                .await()
                .data as? Map<*, *>

            if (result == null) {
                Logger.error(TAG, "Proxy returned null result")
                return@withContext null
            }

            GeminiAnalysis(
                estimatedCategory = result["estimatedCategory"] as? String ?: "Unknown",
                kgCo2e = (result["kgCo2e"] as? Number)?.toDouble(),
                reasoning = result["reasoning"] as? String ?: "No reasoning provided",
                confidence = result["confidence"] as? String ?: "Unknown",
                dataQuality = result["dataQuality"] as? String ?: "Expert Estimate"
            ).also {
                Logger.debug(TAG, "Proxy analysis complete: category=${it.estimatedCategory}, co2e=${it.kgCo2e}")
            }
        } catch (e: Exception) {
            Logger.error(TAG, "Proxy estimation failed: ${e.javaClass.simpleName}", e)
            null
        }
    }

    private suspend fun identifyViaProxy(barcode: String, userHint: String): ScannedProduct? =
        withContext(Dispatchers.IO) {
            try {
                val data = hashMapOf(
                    "barcode" to barcode,
                    "userHint" to userHint
                )

                val result = functions
                    .getHttpsCallable(PROXY_FN_IDENTIFY)
                    .call(data)
                    .await()
                    .data as? Map<*, *>

                if (result == null) {
                    Logger.error(TAG, "Proxy returned null result")
                    return@withContext null
                }

                ScannedProduct(
                    barcode = barcode,
                    productName = result["productName"] as? String ?: "Unknown Product",
                    brand = "",
                    categories = result["estimatedCategory"] as? String ?: "Unknown",
                    imageUrl = "",
                    ecoScore = "not-applicable",
                    ecoScoreValue = -1,
                    carbonFootprint = (result["kgCo2e"] as? Number)?.toDouble(),
                    status = EstimationStatus.AI_ESTIMATED,
                    aiReasoning = result["reasoning"] as? String ?: "No reasoning provided",
                    aiConfidence = result["confidence"] as? String ?: "Medium",
                    aiDataQuality = result["dataQuality"] as? String ?: "User-Assisted Estimate"
                ).also {
                    Logger.debug(TAG, "Proxy identification successful: ${it.productName}")
                }
            } catch (e: Exception) {
                Logger.error(TAG, "Proxy identification failed: ${e.javaClass.simpleName}", e)
                null
            }
        }
}
