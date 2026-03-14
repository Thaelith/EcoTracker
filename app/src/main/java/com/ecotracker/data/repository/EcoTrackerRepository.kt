package com.ecotracker.data.repository

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

@Singleton
class EcoTrackerRepository @Inject constructor(
    private val foodApi: OpenFoodFactsApiService,
    private val beautyApi: OpenBeautyFactsApiService,
    private val upcApi: UPCItemDbApiService,
    private val dao: ScannedProductDao
) {
    suspend fun fetchProductByBarcode(barcode: String): Resource<ScannedProduct> {
        android.util.Log.d("EcoRepo", "=== Starting lookup for barcode: $barcode ===")
        
        // 1. Try Open Food Facts
        val offResult = tryOpenFoodFacts(barcode)
        if (offResult != null) {
            android.util.Log.d("EcoRepo", "Found via OpenFoodFacts")
            return Resource.Success(offResult)
        }

        // 2. Try Open Beauty Facts
        val obfResult = tryOpenBeautyFacts(barcode)
        if (obfResult != null) {
            android.util.Log.d("EcoRepo", "Found via OpenBeautyFacts")
            return Resource.Success(obfResult)
        }
        
        // 3. Check Global Community Cache (Firestore)
        val cacheResult = checkGlobalCache(barcode)
        if (cacheResult != null) {
            android.util.Log.d("EcoRepo", "Found in Global Cache")
            return Resource.Success(cacheResult)
        }

        // 4. Try UPCitemdb + Estimation Service
        val upcResult = tryUPCItemDbAndGemini(barcode)
        if (upcResult != null) {
            android.util.Log.d("EcoRepo", "Found via UPCitemdb and Estimation Service")
            return Resource.Success(upcResult)
        }

        // 5. Ultimate Fallback: Request user description
        android.util.Log.d("EcoRepo", "[5/5] All databases failed. Requesting user input for barcode $barcode...")
        return Resource.NeedsInput(barcode)
    }

    suspend fun estimateWithUserPrompt(barcode: String, userHint: String): Resource<ScannedProduct> {
        android.util.Log.d("EcoRepo", "User provided description for barcode $barcode")
        val aiGuessResult = com.ecotracker.data.remote.GeminiCarbonService.identifyProductWithUserHint(barcode, userHint)
        
        if (aiGuessResult != null) {
            android.util.Log.d("EcoRepo", "Found via User-Assisted Fallback")
            cacheProductGlobally(aiGuessResult)
            return Resource.Success(aiGuessResult)
        }
        
        return Resource.Error("AI couldn't estimate the product from the hint.", barcode)
    }

    private suspend fun tryOpenFoodFacts(barcode: String): ScannedProduct? {
        return try {
            android.util.Log.d("EcoRepo", "[1/4] Trying OpenFoodFacts for $barcode...")
            val r = foodApi.getProductByBarcode(barcode)
            android.util.Log.d("EcoRepo", "[1/4] OFF response: code=${r.code()}, isSuccessful=${r.isSuccessful}, status=${r.body()?.status}")
            if (r.isSuccessful && r.body()?.status == 1 && r.body()?.product != null) {
                val productDto = r.body()!!.product!!
                val baseProduct = productDto.toScannedProduct(barcode)
                android.util.Log.d("EcoRepo", "[1/4] OFF product found: ${baseProduct.productName}")
                
                // If OFF has real carbon data (Agribalyse/nutriments), use it directly
                if (CarbonCalculator.hasRealCarbonData(productDto)) {
                    android.util.Log.d("EcoRepo", "[1/4] OFF has REAL carbon data, using directly")
                    return baseProduct
                }
                
                // OFF found the product but lacks carbon data → enhance with Estimation Service
                // Get the quantity string (e.g. "33cl", "500ml") from the raw DTO
                val quantity = productDto.quantity?.takeIf { it.isNotBlank() } 
                    ?: productDto.productQuantity?.let { "${it.toInt()}ml" }
                android.util.Log.d("EcoRepo", "[1/4] OFF lacks carbon data, enhancing with Estimation Service...")
                val analysis = com.ecotracker.data.remote.GeminiCarbonService.estimateCarbonFootprint(
                    baseProduct.productName, baseProduct.categories, quantity
                )
                
                if (analysis != null) {
                    android.util.Log.d("EcoRepo", "[1/4] Estimation success: ${analysis.kgCo2e} kg CO2e")
                    val enhanced = baseProduct.copy(
                        carbonFootprint = analysis.kgCo2e,
                        ecoScore = "AI Enhanced",
                        aiReasoning = analysis.reasoning,
                        aiConfidence = analysis.confidence,
                        aiDataQuality = analysis.dataQuality
                    )
                    cacheProductGlobally(enhanced)
                    return enhanced
                }
                
                android.util.Log.d("EcoRepo", "[1/4] Estimation Service failed, returning OFF product with generic estimate")
                baseProduct
            }
            else {
                android.util.Log.d("EcoRepo", "[1/4] OFF: product not in database")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("EcoRepo", "[1/4] OFF EXCEPTION: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }

    private suspend fun tryOpenBeautyFacts(barcode: String): ScannedProduct? {
        return try {
            android.util.Log.d("EcoRepo", "[2/4] Trying OpenBeautyFacts for $barcode...")
            val r = beautyApi.getProductByBarcode(barcode)
            android.util.Log.d("EcoRepo", "[2/4] OBF response: code=${r.code()}, isSuccessful=${r.isSuccessful}, status=${r.body()?.status}")
            if (r.isSuccessful && r.body()?.status == 1 && r.body()?.product != null)
                r.body()!!.product!!.toScannedProduct(barcode)
            else {
                android.util.Log.d("EcoRepo", "[2/4] OBF: product not in database")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("EcoRepo", "[2/4] OBF EXCEPTION: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }
    
    private suspend fun checkGlobalCache(barcode: String): ScannedProduct? {
        return try {
            android.util.Log.d("EcoRepo", "[3/4] Checking Global Firestore Cache for $barcode...")
            val db = FirebaseFirestore.getInstance()
            val doc = db.collection("global_products").document(barcode).get().await()
            if (doc.exists()) {
                android.util.Log.d("EcoRepo", "[3/4] Found in global cache")
                ScannedProduct(
                    barcode        = barcode,
                    productName    = doc.getString("productName") ?: "Unknown",
                    brand          = doc.getString("brand") ?: "Unknown",
                    ecoScore       = "AI Forecast",
                    ecoScoreValue  = 0,
                    carbonFootprint = doc.getDouble("carbonFootprint") ?: 0.0,
                    imageUrl       = doc.getString("imageUrl") ?: "",
                    categories     = doc.getString("category") ?: "",
                    aiReasoning    = doc.getString("aiReasoning"),
                    aiConfidence   = doc.getString("aiConfidence"),
                    aiDataQuality  = doc.getString("aiDataQuality")
                )
            } else {
                android.util.Log.d("EcoRepo", "[3/4] Not in global cache")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("EcoRepo", "[3/4] Cache EXCEPTION: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }

    private suspend fun tryUPCItemDbAndGemini(barcode: String): ScannedProduct? {
        return try {
            android.util.Log.d("EcoRepo", "[4/4] Trying UPCitemdb + Gemini for $barcode...")
            val r = upcApi.lookupBarcode(barcode)
            android.util.Log.d("EcoRepo", "[4/4] UPC response: code=${r.code()}, isSuccessful=${r.isSuccessful}")
            if (r.isSuccessful) {
                val item = r.body()?.items?.firstOrNull()
                if (item == null) {
                    android.util.Log.d("EcoRepo", "[4/4] UPC: no items in response")
                    return null
                }
                val title = item.title
                if (title == null) {
                    android.util.Log.d("EcoRepo", "[4/4] UPC: item has no title")
                    return null
                }
                
                android.util.Log.d("EcoRepo", "[4/4] UPC found: $title, invoking Estimation Service...")
                val analysis = com.ecotracker.data.remote.GeminiCarbonService.estimateCarbonFootprint(title, item.category)
                
                val estimatedFootprint = analysis?.kgCo2e ?: 2.0
                android.util.Log.d("EcoRepo", "[4/4] Analysis result: ${analysis?.kgCo2e ?: "FAILED, using fallback 2.0"}")

                val generatedProduct = ScannedProduct(
                    barcode        = barcode,
                    productName    = title,
                    brand          = item.brand ?: "Unknown",
                    ecoScore       = "AI Forecast",
                    ecoScoreValue  = 0,
                    carbonFootprint = estimatedFootprint,
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
                "timestamp" to product.timestamp
            )
            
            db.collection("users").document(user.uid)
                .collection("scans").add(scanData)
                
            if (product.carbonFootprint > 0.0) {
                db.collection("users").document(user.uid)
                    .update("co2e", FieldValue.increment(product.carbonFootprint))
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
