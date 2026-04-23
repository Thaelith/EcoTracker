package com.ecotracker.data.repository

import com.ecotracker.data.local.EstimationStatus
import com.ecotracker.data.local.ScanHistoryEntity
import com.ecotracker.data.local.ScannedProduct
import com.ecotracker.data.local.ScannedProductDao
import com.ecotracker.data.remote.OpenBeautyFactsApiService
import com.ecotracker.data.remote.OpenFoodFactsApiService
import com.ecotracker.data.remote.UPCItemDbApiService
import com.ecotracker.utils.AppConfig
import com.ecotracker.utils.BarcodeValidator
import com.ecotracker.utils.CarbonCalculator
import com.ecotracker.utils.Logger
import com.ecotracker.utils.Resource
import com.ecotracker.utils.toCachedProductEntity
import com.ecotracker.utils.toScannedProduct
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val foodApi: OpenFoodFactsApiService,
    private val beautyApi: OpenBeautyFactsApiService,
    private val upcApi: UPCItemDbApiService,
    private val dao: ScannedProductDao,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userRepository: UserRepository,
    private val geminiService: com.ecotracker.data.remote.GeminiCarbonService
) {
    companion object {
        private const val TAG = "ProductRepo"
        private const val GUEST_USER_ID = "__guest__"
    }

    // ── Product Lookup ─────────────────────────────────────────────────────────

    suspend fun fetchProductByBarcode(barcode: String): Resource<ScannedProduct> {
        val validationError = BarcodeValidator.getValidationError(barcode)
        if (validationError != null) {
            Logger.debug(TAG, "Invalid barcode rejected: $validationError")
            return Resource.Error(validationError)
        }

        return coroutineScope {
            val masked = Logger.maskBarcode(barcode)
            Logger.debug(TAG, "Starting parallel lookup for $masked")

            val offDeferred = async { tryOpenFoodFacts(barcode) }
            val obfDeferred = async { tryOpenBeautyFacts(barcode) }
            val cacheDeferred = async { checkGlobalCache(barcode) }

            val offResult = offDeferred.await()
            if (offResult != null && !offResult.isWeak()) {
                Logger.debug(TAG, "Found strong result via OpenFoodFacts")
                return@coroutineScope Resource.Success(offResult)
            }

            val obfResult = obfDeferred.await()
            if (obfResult != null && !obfResult.isWeak()) {
                Logger.debug(TAG, "Found strong result via OpenBeautyFacts")
                return@coroutineScope Resource.Success(obfResult)
            }

            val cacheResult = cacheDeferred.await()
            if (cacheResult != null && !cacheResult.isWeak()) {
                Logger.debug(TAG, "Found strong result in global cache")
                return@coroutineScope Resource.Success(cacheResult)
            }

            val upcResult = tryUPCItemDbAndGemini(barcode)
            if (upcResult != null && !upcResult.isWeak()) {
                Logger.debug(TAG, "Found strong result via UPCitemdb + Gemini")
                return@coroutineScope Resource.Success(upcResult)
            }

            val bestCandidate = listOf(offResult, obfResult, cacheResult, upcResult)
                .filterNotNull()
                .firstOrNull { it.productName != "Unknown Product" && it.productName.isNotBlank() }
                ?: offResult ?: obfResult ?: cacheResult ?: upcResult

            if (bestCandidate != null) {
                Logger.debug(
                    TAG,
                    "No strong result found for $masked, returning best candidate: ${bestCandidate.productName}"
                )
                return@coroutineScope Resource.Success(bestCandidate)
            }

            Logger.debug(TAG, "All sources exhausted for $masked, requesting user input")
            Resource.NeedsInput(barcode)
        }
    }

    suspend fun estimateWithUserPrompt(barcode: String, userHint: String): Resource<ScannedProduct> {
        val masked = Logger.maskBarcode(barcode)
        Logger.debug(TAG, "User provided description for $masked")
        return try {
            val aiGuessResult =
                geminiService.identifyProductWithUserHint(
                    barcode,
                    userHint
                )

            if (aiGuessResult != null) {
                Logger.debug(TAG, "User-assisted identification succeeded")
                val stampedResult = aiGuessResult.copy(status = EstimationStatus.AI_ESTIMATED)
                cacheProductGlobally(stampedResult)
                Resource.Success(stampedResult)
            } else {
                Resource.Error("Could not identify the product from your description. Try adding more detail.")
            }
        } catch (e: UnknownHostException) {
            Resource.Error("No internet connection. Check your network and try again.")
        } catch (e: SocketTimeoutException) {
            Resource.Error("Request timed out. Please try again.")
        } catch (e: Exception) {
            Logger.error(TAG, "User-assisted estimation failed", e)
            Resource.Error("Something went wrong. Please try again.")
        }
    }

    // ── Local History CRUD ────────────────────────────────────────────────────

    suspend fun saveProduct(product: ScannedProduct, remoteId: String): Long {
        val id = dao.insertProductAndHistory(
            product = product.toCachedProductEntity(updatedAt = product.timestamp),
            ScanHistoryEntity(
                userId = currentUserKey(),
                barcode = product.barcode,
                scannedAt = product.timestamp
            )
        )

        val firebaseUser = try {
            auth.currentUser
        } catch (e: Exception) {
            Logger.error(TAG, "FirebaseAuth unavailable during save", e)
            null
        }

        if (firebaseUser != null) {
            try {
                val userRef = firestore.collection("users").document(firebaseUser.uid)
                val scanRef = userRef.collection("scans").document(remoteId)

                val scanData = hashMapOf(
                    "barcode" to product.barcode,
                    "productName" to product.productName,
                    "carbonFootprint" to product.carbonFootprint,
                    "status" to product.status.name,
                    "timestamp" to product.timestamp
                )
                scanRef.set(scanData, SetOptions.merge()).await()
                userRepository.syncCurrentUserStats(userRef, firebaseUser.uid)
            } catch (e: Exception) {
                Logger.error(TAG, "Firestore sync failed during save", e)
            }
        }

        return id
    }

    suspend fun saveProduct(product: ScannedProduct): Long {
        val remoteId = "${product.barcode}_${product.timestamp}"
        return saveProduct(product, remoteId)
    }

    suspend fun deleteProduct(product: ScannedProduct) {
        dao.deleteScanHistoryById(currentUserKey(), product.id)
        deleteRemoteScanAndSync(product)
    }

    suspend fun deleteProductById(id: Long) {
        val userId = currentUserKey()
        val product = dao.getProductByHistoryId(userId, id)
        dao.deleteScanHistoryById(userId, id)
        if (product != null) {
            deleteRemoteScanAndSync(product)
        }
    }

    fun getAllProducts(): Flow<List<ScannedProduct>> = dao.getAllProducts(currentUserKey())

    fun getProductsSince(startTime: Long): Flow<List<ScannedProduct>> =
        dao.getProductsSince(currentUserKey(), startTime)

    suspend fun getProductByBarcode(barcode: String): ScannedProduct? =
        dao.getCachedProductByBarcode(barcode)?.toScannedProduct()

    suspend fun deleteAllProducts() {
        dao.deleteAllScanHistory(currentUserKey())
        dao.deleteAllCachedProducts()
    }

    // ── Private Lookup Methods ──────────────────────────────────────────────

    private suspend fun tryOpenFoodFacts(barcode: String): ScannedProduct? {
        return try {
            val response = foodApi.getProductByBarcode(barcode)
            if (response.isSuccessful && response.body()?.status == 1 && response.body()?.product != null) {
                val productDto = response.body()!!.product!!
                val baseProduct = productDto.toScannedProduct(barcode)

                if (CarbonCalculator.hasRealCarbonData(productDto)) {
                    return baseProduct
                }

                val quantity = productDto.quantity?.takeIf { it.isNotBlank() }
                    ?: productDto.productQuantity?.let { "${it.toInt()}ml" }
                val analysis = geminiService
                    .estimateCarbonFootprint(baseProduct.productName, baseProduct.categories, quantity)

                if (analysis != null) {
                    val enhanced = baseProduct.copy(
                        carbonFootprint = analysis.kgCo2e,
                        status = EstimationStatus.AI_ESTIMATED,
                        ecoScore = "AI Enhanced",
                        aiReasoning = analysis.reasoning,
                        aiConfidence = analysis.confidence,
                        aiDataQuality = analysis.dataQuality
                    )
                    cacheProductGlobally(enhanced)
                    return enhanced
                }

                baseProduct
            } else {
                null
            }
        } catch (e: Exception) {
            Logger.debug(TAG, "OpenFoodFacts lookup failed: ${e.javaClass.simpleName}")
            null
        }
    }

    private suspend fun tryOpenBeautyFacts(barcode: String): ScannedProduct? {
        return try {
            val response = beautyApi.getProductByBarcode(barcode)
            if (response.isSuccessful && response.body()?.status == 1 && response.body()?.product != null) {
                response.body()!!.product!!.toScannedProduct(barcode)
            } else {
                null
            }
        } catch (e: Exception) {
            Logger.debug(TAG, "OpenBeautyFacts lookup failed: ${e.javaClass.simpleName}")
            null
        }
    }

    private suspend fun checkGlobalCache(barcode: String): ScannedProduct? {
        return try {
            val doc = firestore.collection("global_products").document(barcode).get().await()
            if (doc.exists()) {
                val cachedAt = doc.getLong("cachedAt") ?: 0L
                val age = System.currentTimeMillis() - cachedAt

                if (age > AppConfig.CACHE_TTL_MILLIS) {
                    Logger.debug(
                        TAG,
                        "Global cache hit is stale (>${AppConfig.CACHE_TTL_DAYS} days), ignoring"
                    )
                    return null
                }

                ScannedProduct(
                    barcode = barcode,
                    productName = doc.getString("productName") ?: "Unknown",
                    brand = doc.getString("brand") ?: "Unknown",
                    ecoScore = "AI Forecast",
                    ecoScoreValue = 0,
                    carbonFootprint = doc.getDouble("carbonFootprint"),
                    status = try {
                        EstimationStatus.valueOf(doc.getString("status") ?: "CATEGORY_AVERAGE")
                    } catch (_: Exception) {
                        EstimationStatus.CATEGORY_AVERAGE
                    },
                    imageUrl = doc.getString("imageUrl") ?: "",
                    categories = doc.getString("category") ?: "",
                    aiReasoning = doc.getString("aiReasoning"),
                    aiConfidence = doc.getString("aiConfidence"),
                    aiDataQuality = doc.getString("aiDataQuality")
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Logger.debug(TAG, "Global cache check failed: ${e.javaClass.simpleName}")
            null
        }
    }

    private suspend fun tryUPCItemDbAndGemini(barcode: String): ScannedProduct? {
        return try {
            val response = upcApi.lookupBarcode(barcode)
            if (response.isSuccessful) {
                val item = response.body()?.items?.firstOrNull() ?: return null
                val title = item.title ?: return null

                val analysis =
                    geminiService.estimateCarbonFootprint(
                        title,
                        item.category
                    )
                val generatedProduct = ScannedProduct(
                    barcode = barcode,
                    productName = title,
                    brand = item.brand ?: "Unknown",
                    ecoScore = "AI Forecast",
                    ecoScoreValue = 0,
                    carbonFootprint = analysis?.kgCo2e,
                    status = if (analysis != null) {
                        EstimationStatus.AI_ESTIMATED
                    } else {
                        EstimationStatus.NEEDS_ESTIMATION
                    },
                    imageUrl = item.images?.firstOrNull() ?: "",
                    categories = item.category ?: "",
                    aiReasoning = analysis?.reasoning,
                    aiConfidence = analysis?.confidence,
                    aiDataQuality = analysis?.dataQuality
                )

                cacheProductGlobally(generatedProduct)
                generatedProduct
            } else {
                Logger.debug(TAG, "UPC request failed with code ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Logger.error(TAG, "UPC lookup exception: ${e.javaClass.simpleName}")
            null
        }
    }

    private fun cacheProductGlobally(product: ScannedProduct) {
        try {
            val data = hashMapOf(
                "barcode" to product.barcode,
                "productName" to product.productName,
                "brand" to product.brand,
                "category" to product.categories,
                "imageUrl" to product.imageUrl,
                "carbonFootprint" to product.carbonFootprint,
                "status" to product.status.name,
                "aiReasoning" to product.aiReasoning,
                "aiConfidence" to product.aiConfidence,
                "aiDataQuality" to product.aiDataQuality,
                "cachedAt" to System.currentTimeMillis()
            )
            firestore.collection("global_products").document(product.barcode).set(data)
        } catch (e: Exception) {
            Logger.error(TAG, "Failed to cache product globally", e)
        }
    }

    private suspend fun deleteRemoteScanAndSync(product: ScannedProduct) {
        val firebaseUser = try {
            auth.currentUser
        } catch (e: Exception) {
            Logger.error(TAG, "FirebaseAuth unavailable during delete", e)
            null
        } ?: return

        try {
            val userRef = firestore.collection("users").document(firebaseUser.uid)
            val remoteId = "${product.barcode}_${product.timestamp}"
            userRef.collection("scans").document(remoteId).delete().await()
            userRepository.syncCurrentUserStats(userRef, firebaseUser.uid)
        } catch (e: Exception) {
            Logger.error(TAG, "Firestore sync failed during delete", e)
        }
    }

    private fun currentUserKey(): String {
        return auth.currentUser?.uid ?: GUEST_USER_ID
    }
}
