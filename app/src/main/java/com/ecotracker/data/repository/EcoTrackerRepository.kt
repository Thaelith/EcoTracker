package com.ecotracker.data.repository

import com.ecotracker.data.local.EstimationStatus
import com.ecotracker.data.local.ScannedProduct
import com.ecotracker.data.local.ScannedProductDao
import com.ecotracker.data.remote.OpenBeautyFactsApiService
import com.ecotracker.data.remote.OpenFoodFactsApiService
import com.ecotracker.data.remote.UPCItemDbApiService
import com.ecotracker.utils.Resource
import com.ecotracker.utils.CarbonCalculator
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

@Singleton
class EcoTrackerRepository @Inject constructor(
    private val foodApi: OpenFoodFactsApiService,
    private val beautyApi: OpenBeautyFactsApiService,
    private val upcApi: UPCItemDbApiService,
    private val dao: ScannedProductDao
) {
    suspend fun fetchProductByBarcode(barcode: String): Resource<ScannedProduct> = coroutineScope {
        android.util.Log.d("EcoRepo", "=== Starting parallel lookup for barcode: $barcode ===")
        
        val offDeferred = async { tryOpenFoodFacts(barcode) }
        val obfDeferred = async { tryOpenBeautyFacts(barcode) }
        val cacheDeferred = async { checkGlobalCache(barcode) }

        val offResult = offDeferred.await()
        if (offResult != null) {
            android.util.Log.d("EcoRepo", "Found via OpenFoodFacts")
            return@coroutineScope Resource.Success(offResult)
        }

        val obfResult = obfDeferred.await()
        if (obfResult != null) {
            android.util.Log.d("EcoRepo", "Found via OpenBeautyFacts")
            return@coroutineScope Resource.Success(obfResult)
        }
        
        val cacheResult = cacheDeferred.await()
        if (cacheResult != null) {
            android.util.Log.d("EcoRepo", "Found in Global Cache")
            return@coroutineScope Resource.Success(cacheResult)
        }

        val upcResult = tryUPCItemDbAndGemini(barcode)
        if (upcResult != null) {
            android.util.Log.d("EcoRepo", "Found via UPCitemdb and Estimation Service")
            return@coroutineScope Resource.Success(upcResult)
        }

        android.util.Log.d("EcoRepo", "[Final] All databases/AI failed. Requesting user input for barcode $barcode...")
        Resource.NeedsInput(barcode)
    }

    suspend fun estimateWithUserPrompt(barcode: String, userHint: String): Resource<ScannedProduct> {
        android.util.Log.d("EcoRepo", "User provided description for barcode $barcode")
        val aiGuessResult = com.ecotracker.data.remote.GeminiCarbonService.identifyProductWithUserHint(barcode, userHint)
        
        if (aiGuessResult != null) {
            android.util.Log.d("EcoRepo", "Found via User-Assisted Fallback")
            val stampedResult = aiGuessResult.copy(status = EstimationStatus.AI_ESTIMATED)
            cacheProductGlobally(stampedResult)
            return Resource.Success(stampedResult)
        }
        
        return Resource.Error("AI couldn't estimate the product from the hint.", barcode)
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
        } catch (e: Exception) { null }
    }

    private suspend fun tryOpenBeautyFacts(barcode: String): ScannedProduct? {
        return try {
            val r = beautyApi.getProductByBarcode(barcode)
            if (r.isSuccessful && r.body()?.status == 1 && r.body()?.product != null)
                r.body()!!.product!!.toScannedProduct(barcode)
            else null
        } catch (e: Exception) { null }
    }
    
    private suspend fun checkGlobalCache(barcode: String): ScannedProduct? {
        return try {
            val db = FirebaseFirestore.getInstance()
            val doc = db.collection("global_products").document(barcode).get().await()
            if (doc.exists()) {
                ScannedProduct(
                    barcode        = barcode,
                    productName    = doc.getString("productName") ?: "Unknown",
                    brand          = doc.getString("brand") ?: "Unknown",
                    ecoScore       = "AI Forecast",
                    ecoScoreValue  = 0,
                    carbonFootprint = doc.getDouble("carbonFootprint"),
                    status         = try { EstimationStatus.valueOf(doc.getString("status") ?: "CATEGORY_AVERAGE") } catch(e: Exception) { EstimationStatus.CATEGORY_AVERAGE },
                    imageUrl       = doc.getString("imageUrl") ?: "",
                    categories     = doc.getString("category") ?: "",
                    aiReasoning    = doc.getString("aiReasoning"),
                    aiConfidence   = doc.getString("aiConfidence"),
                    aiDataQuality  = doc.getString("aiDataQuality")
                )
            } else null
        } catch (e: Exception) { null }
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
                android.util.Log.d("EcoRepo", "[4/4] UPC: request failed with code ${r.code()}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("EcoRepo", "[4/4] UPC EXCEPTION: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }
    
    private fun cacheProductGlobally(product: ScannedProduct) {
        try {
            val db = FirebaseFirestore.getInstance()
            val data = hashMapOf(
                "barcode" to product.barcode,
                "productName" to product.productName,
                "brand" to product.brand,
                "category" to product.categories,
                "imageUrl" to product.imageUrl,
                "carbonFootprint" to product.carbonFootprint,
                "status" to product.status.name, // Added status
                "aiReasoning" to product.aiReasoning,
                "aiConfidence" to product.aiConfidence,
                "aiDataQuality" to product.aiDataQuality,
                "cachedAt" to System.currentTimeMillis()
            )
            db.collection("global_products").document(product.barcode).set(data)
        } catch (e: Exception) { 
            /* Silently drop cache failures */ 
        }
    }

    // ── Local ─────────────────────────────────────────────────────────────────

    suspend fun saveProduct(product: ScannedProduct): Long {
        val id = dao.insertProduct(product)
        
        // Sync with Firestore if logged in
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            
            val scanData = hashMapOf(
                "barcode" to product.barcode,
                "productName" to product.productName,
                "carbonFootprint" to product.carbonFootprint,
                "status" to product.status.name, // Added status
                "timestamp" to product.timestamp
            )
            
            db.collection("users").document(user.uid)
                .collection("scans").add(scanData)
                
            // Safe null check for increment
            product.carbonFootprint?.let { carbon ->
                if (carbon > 0.0) {
                    db.collection("users").document(user.uid)
                        .update("co2e", FieldValue.increment(carbon))
                }
            }
        }
        return id
    }

    suspend fun deleteProduct(product: ScannedProduct) = dao.deleteProduct(product)

    suspend fun deleteProductById(id: Long) = dao.deleteProductById(id)

    fun getAllProducts(): Flow<List<ScannedProduct>> = dao.getAllProducts()

    fun getProductsSince(startTime: Long): Flow<List<ScannedProduct>> =
        dao.getProductsSince(startTime)

    fun getTotalCarbonSince(startTime: Long): Flow<Double?> =
        dao.getTotalCarbonSince(startTime)

    fun getTotalScannedCount(): Flow<Int> = dao.getTotalScannedCount()

    suspend fun getProductByBarcode(barcode: String): ScannedProduct? =
        dao.getProductByBarcode(barcode)

    suspend fun deleteAllProducts() = dao.deleteAll()
}
