package com.ecotracker.data.repository

import com.ecotracker.data.local.ScannedProduct
import com.ecotracker.data.local.ScannedProductDao
import com.ecotracker.data.remote.OpenBeautyFactsApiService
import com.ecotracker.data.remote.OpenFoodFactsApiService
import com.ecotracker.data.remote.UPCItemDbApiService
import com.ecotracker.utils.Resource
import com.ecotracker.utils.toScannedProduct
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EcoTrackerRepository @Inject constructor(
    private val foodApi: OpenFoodFactsApiService,
    private val beautyApi: OpenBeautyFactsApiService,
    private val upcApi: UPCItemDbApiService,
    private val dao: ScannedProductDao
) {
    suspend fun fetchProductByBarcode(barcode: String): Resource<ScannedProduct> {
        // 1️⃣ Try Open Food Facts
        tryOpenFoodFacts(barcode)?.let { return Resource.Success(it) }

        // 2️⃣ Try Open Beauty Facts
        tryOpenBeautyFacts(barcode)?.let { return Resource.Success(it) }

        // 3️⃣ Try UPCitemdb
        tryUPCItemDb(barcode)?.let { return Resource.Success(it) }

        // ❌ All APIs failed
        return Resource.Error("Product not found in any database. Try adding it manually.", barcode)
    }

    private suspend fun tryOpenFoodFacts(barcode: String): ScannedProduct? {
        return try {
            val r = foodApi.getProductByBarcode(barcode)
            if (r.isSuccessful && r.body()?.status == 1 && r.body()?.product != null)
                r.body()!!.product!!.toScannedProduct(barcode)
            else null
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

    private suspend fun tryUPCItemDb(barcode: String): ScannedProduct? {
        return try {
            val r = upcApi.lookupBarcode(barcode)
            if (r.isSuccessful) {
                val item = r.body()?.items?.firstOrNull() ?: return null
                ScannedProduct(
                    barcode        = barcode,
                    productName    = item.title ?: return null,
                    brand          = item.brand ?: "Unknown",
                    ecoScore       = "N/A",
                    ecoScoreValue  = 0,
                    carbonFootprint = 2.0, // mock value since UPCitemdb has no eco data
                    imageUrl       = item.images?.firstOrNull() ?: "",
                    categories     = item.category ?: ""
                )
            } else null
        } catch (e: Exception) { null }
    }

    // ── Local ─────────────────────────────────────────────────────────────────

    suspend fun saveProduct(product: ScannedProduct): Long = dao.insertProduct(product)

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
}
