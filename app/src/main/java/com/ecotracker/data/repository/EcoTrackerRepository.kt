package com.ecotracker.data.repository

import com.ecotracker.data.local.EstimationStatus
import com.ecotracker.data.local.ScannedProduct
import com.ecotracker.data.local.ScannedProductDao
import com.ecotracker.data.local.ScanHistoryEntity
import com.ecotracker.data.model.UserProfile
import com.ecotracker.data.remote.OpenBeautyFactsApiService
import com.ecotracker.data.remote.OpenFoodFactsApiService
import com.ecotracker.data.remote.UPCItemDbApiService
import com.ecotracker.utils.AppConfig
import com.ecotracker.utils.Logger
import com.ecotracker.utils.Resource
import com.ecotracker.utils.CarbonCalculator
import com.ecotracker.utils.toCachedProductEntity
import com.ecotracker.utils.toScannedProduct
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import java.net.UnknownHostException
import java.net.SocketTimeoutException

@Singleton
class EcoTrackerRepository @Inject constructor(
    private val foodApi: OpenFoodFactsApiService,
    private val beautyApi: OpenBeautyFactsApiService,
    private val upcApi: UPCItemDbApiService,
    private val dao: ScannedProductDao,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "EcoRepo"
    }

    suspend fun fetchProductByBarcode(barcode: String): Resource<ScannedProduct> = coroutineScope {
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

        // If we reached here, we have no "strong" result. 
        // We pick the "least weak" one we found, prioritizing sources that at least found a name.
        val bestCandidate = listOf(offResult, obfResult, cacheResult, upcResult)
            .filterNotNull()
            .firstOrNull { it.productName != "Unknown Product" && it.productName.isNotBlank() }
            ?: offResult ?: obfResult ?: cacheResult ?: upcResult

        if (bestCandidate != null) {
            Logger.debug(TAG, "No strong result found for $masked, returning best candidate: ${bestCandidate.productName}")
            return@coroutineScope Resource.Success(bestCandidate)
        }

        Logger.debug(TAG, "All sources exhausted for $masked, requesting user input")
        Resource.NeedsInput(barcode)
    }

    suspend fun estimateWithUserPrompt(barcode: String, userHint: String): Resource<ScannedProduct> {
        val masked = Logger.maskBarcode(barcode)
        Logger.debug(TAG, "User provided description for $masked")
        return try {
            val aiGuessResult = com.ecotracker.data.remote.GeminiCarbonService.identifyProductWithUserHint(barcode, userHint)

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

    private suspend fun tryOpenFoodFacts(barcode: String): ScannedProduct? {
        return try {
            val r = foodApi.getProductByBarcode(barcode)
            if (r.isSuccessful && r.body()?.status == 1 && r.body()?.product != null) {
                val productDto = r.body()!!.product!!
                val baseProduct = productDto.toScannedProduct(barcode)

                if (CarbonCalculator.hasRealCarbonData(productDto)) {
                    return baseProduct
                }

                val quantity = productDto.quantity?.takeIf { it.isNotBlank() }
                    ?: productDto.productQuantity?.let { "${it.toInt()}ml" }
                val analysis = com.ecotracker.data.remote.GeminiCarbonService.estimateCarbonFootprint(
                    baseProduct.productName, baseProduct.categories, quantity
                )

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
            } else null
        } catch (e: Exception) {
            Logger.debug(TAG, "OpenFoodFacts lookup failed: ${e.javaClass.simpleName}")
            null
        }
    }

    private suspend fun tryOpenBeautyFacts(barcode: String): ScannedProduct? {
        return try {
            val r = beautyApi.getProductByBarcode(barcode)
            if (r.isSuccessful && r.body()?.status == 1 && r.body()?.product != null)
                r.body()!!.product!!.toScannedProduct(barcode)
            else null
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
                    Logger.debug(TAG, "Global cache hit is stale (>${AppConfig.CACHE_TTL_DAYS} days), ignoring")
                    return null
                }

                ScannedProduct(
                    barcode        = barcode,
                    productName    = doc.getString("productName") ?: "Unknown",
                    brand          = doc.getString("brand") ?: "Unknown",
                    ecoScore       = "AI Forecast",
                    ecoScoreValue  = 0,
                    carbonFootprint = doc.getDouble("carbonFootprint"),
                    status         = try { EstimationStatus.valueOf(doc.getString("status") ?: "CATEGORY_AVERAGE") } catch (e: Exception) { EstimationStatus.CATEGORY_AVERAGE },
                    imageUrl       = doc.getString("imageUrl") ?: "",
                    categories     = doc.getString("category") ?: "",
                    aiReasoning    = doc.getString("aiReasoning"),
                    aiConfidence   = doc.getString("aiConfidence"),
                    aiDataQuality  = doc.getString("aiDataQuality")
                )
            } else null
        } catch (e: Exception) {
            Logger.debug(TAG, "Global cache check failed: ${e.javaClass.simpleName}")
            null
        }
    }

    private suspend fun tryUPCItemDbAndGemini(barcode: String): ScannedProduct? {
        return try {
            val r = upcApi.lookupBarcode(barcode)
            if (r.isSuccessful) {
                val item = r.body()?.items?.firstOrNull() ?: return null
                val title = item.title ?: return null

                val analysis = com.ecotracker.data.remote.GeminiCarbonService.estimateCarbonFootprint(title, item.category)
                val generatedProduct = ScannedProduct(
                    barcode        = barcode,
                    productName    = title,
                    brand          = item.brand ?: "Unknown",
                    ecoScore       = "AI Forecast",
                    ecoScoreValue  = 0,
                    carbonFootprint = analysis?.kgCo2e,
                    status         = if (analysis != null) EstimationStatus.AI_ESTIMATED else EstimationStatus.NEEDS_ESTIMATION,
                    imageUrl       = item.images?.firstOrNull() ?: "",
                    categories     = item.category ?: "",
                    aiReasoning    = analysis?.reasoning,
                    aiConfidence   = analysis?.confidence,
                    aiDataQuality  = analysis?.dataQuality
                )

                cacheProductGlobally(generatedProduct)
                generatedProduct
            } else {
                Logger.debug(TAG, "UPC request failed with code ${r.code()}")
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

    // -- Local -------------------------------------------------------------------

    suspend fun saveProduct(product: ScannedProduct, remoteId: String): Long {
        dao.upsertCachedProduct(product.toCachedProductEntity(updatedAt = product.timestamp))
        val id = dao.insertScanHistory(
            ScanHistoryEntity(
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

                firestore.runTransaction { transaction ->
                    val scanDoc = transaction.get(scanRef)
                    if (!scanDoc.exists()) {
                        val scanData = hashMapOf(
                            "barcode" to product.barcode,
                            "productName" to product.productName,
                            "carbonFootprint" to product.carbonFootprint,
                            "status" to product.status.name,
                            "timestamp" to product.timestamp
                        )
                        transaction.set(scanRef, scanData)

                        val updates = mutableMapOf<String, Any>(
                            "scanCount" to FieldValue.increment(1)
                        )

                        product.carbonFootprint?.let { carbon ->
                            if (carbon > 0.0) {
                                updates["co2e"] = FieldValue.increment(carbon)
                            }
                        }

                        transaction.update(userRef, updates)
                    }
                    null
                }.await()
            } catch (e: Exception) {
                Logger.error(TAG, "Firestore sync failed during save", e)
                // Local save still succeeded — don't throw
            }
        }
        return id
    }

    /** Convenience overload used by ManualEntryViewModel (no remote sync ID). */
    suspend fun saveProduct(product: ScannedProduct): Long {
        val remoteId = "${product.barcode}_${product.timestamp}"
        return saveProduct(product, remoteId)
    }

    suspend fun deleteProduct(product: ScannedProduct) = dao.deleteScanHistoryById(product.id)

    suspend fun deleteProductById(id: Long) = dao.deleteScanHistoryById(id)

    fun getAllProducts(): Flow<List<ScannedProduct>> = dao.getAllProducts()

    fun getProductsSince(startTime: Long): Flow<List<ScannedProduct>> =
        dao.getProductsSince(startTime)

    fun getTotalCarbonSince(startTime: Long): Flow<Double?> =
        dao.getTotalCarbonSince(startTime)

    fun getTotalScannedCount(): Flow<Int> = dao.getTotalScannedCount()

    suspend fun getProductByBarcode(barcode: String): ScannedProduct? =
        dao.getCachedProductByBarcode(barcode)?.toScannedProduct()

    suspend fun getCurrentUserProfile(): UserProfile {
        val currentUser = auth.currentUser ?: return UserProfile(
            email = "Not signed in",
            username = "—"
        )

        val username = try {
            val document = firestore.collection("users").document(currentUser.uid).get().await()
            sanitizeUsername(document.getString("username").orEmpty()).ifBlank { "—" }
        } catch (e: Exception) {
            Logger.debug(TAG, "Failed to load current user profile: ${e.javaClass.simpleName}")
            "—"
        }

        return UserProfile(
            email = currentUser.email ?: "—",
            username = username
        )
    }

    suspend fun deleteAllProducts() {
        dao.deleteAllScanHistory()
        dao.deleteAllCachedProducts()
    }

    fun getLeaderboardUsers(): Flow<Resource<List<com.ecotracker.data.model.LeaderboardUser>>> = callbackFlow {
        val listener = firestore.collection("users")
            .orderBy("scanCount", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(AppConfig.LEADERBOARD_MAX_SIZE)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    val message = when (error.code) {
                        com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED -> 
                            "Permission denied. Please ensure you are logged in."
                        com.google.firebase.firestore.FirebaseFirestoreException.Code.FAILED_PRECONDITION -> 
                            "Leaderboard is being initialized. Please try again in 1-2 minutes." // Usually missing index
                        else -> error.message ?: "Failed to load leaderboard"
                    }
                    trySend(Resource.Error(message))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val users = snapshot.documents.mapIndexed { index, doc ->
                        val rawUsername = doc.getString("username") ?: "Anonymous"
                        com.ecotracker.data.model.LeaderboardUser(
                            uid = doc.id,
                            rank = index + 1,
                            username = sanitizeUsername(rawUsername),
                            scanCount = doc.getLong("scanCount")?.toInt() ?: 0,
                            co2e = doc.getDouble("co2e") ?: 0.0
                        )
                    }
                    trySend(Resource.Success(users))
                }
            }
        awaitClose { listener.remove() }
    }

    /** Strip anything that isn't alphanumeric, space, underscore, or hyphen. */
    private fun sanitizeUsername(raw: String): String {
        return raw.replace(Regex("[^a-zA-Z0-9_ \\-]"), "").take(AppConfig.USERNAME_MAX_LENGTH).ifBlank { "Anonymous" }
    }
}
