package com.ecotracker.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScannedProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ScannedProduct): Long

    @Delete
    suspend fun deleteProduct(product: ScannedProduct)

    @Query("DELETE FROM scanned_products WHERE id = :id")
    suspend fun deleteProductById(id: Long)

    @Query("SELECT * FROM scanned_products ORDER BY timestamp DESC")
    fun getAllProducts(): Flow<List<ScannedProduct>>

    @Query("SELECT * FROM scanned_products WHERE id = :id")
    suspend fun getProductById(id: Long): ScannedProduct?

    @Query("SELECT * FROM scanned_products WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getProductsSince(startTime: Long): Flow<List<ScannedProduct>>

    @Query("SELECT SUM(carbonFootprint) FROM scanned_products WHERE timestamp >= :startTime")
    fun getTotalCarbonSince(startTime: Long): Flow<Double?>

    @Query("SELECT COUNT(*) FROM scanned_products")
    fun getTotalScannedCount(): Flow<Int>

    @Query("SELECT * FROM scanned_products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ScannedProduct?

    @Query("DELETE FROM scanned_products")
    suspend fun deleteAll()
}
