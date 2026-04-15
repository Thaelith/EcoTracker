package com.ecotracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScannedProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCachedProduct(product: CachedProductEntity)

    @Insert
    suspend fun insertScanHistory(scanHistory: ScanHistoryEntity): Long

    @Query("DELETE FROM scan_history WHERE id = :id")
    suspend fun deleteScanHistoryById(id: Long)

    @Query(
        """
        SELECT
            sh.id AS id,
            cp.barcode AS barcode,
            cp.productName AS productName,
            cp.brand AS brand,
            cp.categories AS categories,
            cp.imageUrl AS imageUrl,
            cp.ecoScore AS ecoScore,
            cp.ecoScoreValue AS ecoScoreValue,
            cp.carbonFootprint AS carbonFootprint,
            cp.status AS status,
            cp.aiReasoning AS aiReasoning,
            cp.aiConfidence AS aiConfidence,
            cp.aiDataQuality AS aiDataQuality,
            sh.scannedAt AS timestamp
        FROM scan_history sh
        INNER JOIN cached_products cp ON cp.barcode = sh.barcode
        ORDER BY sh.scannedAt DESC, sh.id DESC
        """
    )
    fun getAllProducts(): Flow<List<ScannedProduct>>

    @Query(
        """
        SELECT
            sh.id AS id,
            cp.barcode AS barcode,
            cp.productName AS productName,
            cp.brand AS brand,
            cp.categories AS categories,
            cp.imageUrl AS imageUrl,
            cp.ecoScore AS ecoScore,
            cp.ecoScoreValue AS ecoScoreValue,
            cp.carbonFootprint AS carbonFootprint,
            cp.status AS status,
            cp.aiReasoning AS aiReasoning,
            cp.aiConfidence AS aiConfidence,
            cp.aiDataQuality AS aiDataQuality,
            sh.scannedAt AS timestamp
        FROM scan_history sh
        INNER JOIN cached_products cp ON cp.barcode = sh.barcode
        WHERE sh.scannedAt >= :startTime
        ORDER BY sh.scannedAt DESC, sh.id DESC
        """
    )
    fun getProductsSince(startTime: Long): Flow<List<ScannedProduct>>

    @Query(
        """
        SELECT SUM(cp.carbonFootprint)
        FROM scan_history sh
        INNER JOIN cached_products cp ON cp.barcode = sh.barcode
        WHERE sh.scannedAt >= :startTime
        """
    )
    fun getTotalCarbonSince(startTime: Long): Flow<Double?>

    @Query("SELECT COUNT(*) FROM scan_history")
    fun getTotalScannedCount(): Flow<Int>

    @Query(
        """
        SELECT SUM(cp.carbonFootprint)
        FROM scan_history sh
        INNER JOIN cached_products cp ON cp.barcode = sh.barcode
        """
    )
    fun getTotalCarbon(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM scan_history")
    suspend fun getTotalScannedCountValue(): Int

    @Query(
        """
        SELECT SUM(cp.carbonFootprint)
        FROM scan_history sh
        INNER JOIN cached_products cp ON cp.barcode = sh.barcode
        """
    )
    suspend fun getTotalCarbonValue(): Double?

    @Query("SELECT * FROM cached_products WHERE barcode = :barcode LIMIT 1")
    suspend fun getCachedProductByBarcode(barcode: String): CachedProductEntity?

    @Query("DELETE FROM scan_history")
    suspend fun deleteAllScanHistory()

    @Query("DELETE FROM cached_products")
    suspend fun deleteAllCachedProducts()
}
