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

    @Query("DELETE FROM scan_history WHERE userId = :userId AND id = :id")
    suspend fun deleteScanHistoryById(userId: String, id: Long)

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
        WHERE sh.userId = :userId
        ORDER BY sh.scannedAt DESC, sh.id DESC
        """
    )
    fun getAllProducts(userId: String): Flow<List<ScannedProduct>>

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
        WHERE sh.userId = :userId
          AND sh.scannedAt >= :startTime
        ORDER BY sh.scannedAt DESC, sh.id DESC
        """
    )
    fun getProductsSince(userId: String, startTime: Long): Flow<List<ScannedProduct>>

    @Query(
        """
        SELECT SUM(cp.carbonFootprint)
        FROM scan_history sh
        INNER JOIN cached_products cp ON cp.barcode = sh.barcode
        WHERE sh.userId = :userId
          AND sh.scannedAt >= :startTime
        """
    )
    fun getTotalCarbonSince(userId: String, startTime: Long): Flow<Double?>

    @Query("SELECT COUNT(*) FROM scan_history WHERE userId = :userId")
    fun getTotalScannedCount(userId: String): Flow<Int>

    @Query(
        """
        SELECT SUM(cp.carbonFootprint)
        FROM scan_history sh
        INNER JOIN cached_products cp ON cp.barcode = sh.barcode
        WHERE sh.userId = :userId
        """
    )
    fun getTotalCarbon(userId: String): Flow<Double?>

    @Query("SELECT COUNT(*) FROM scan_history WHERE userId = :userId")
    suspend fun getTotalScannedCountValue(userId: String): Int

    @Query("UPDATE scan_history SET userId = :toUserId WHERE userId = :fromUserId")
    suspend fun reassignScanHistoryUser(fromUserId: String, toUserId: String): Int

    @Query(
        """
        SELECT SUM(cp.carbonFootprint)
        FROM scan_history sh
        INNER JOIN cached_products cp ON cp.barcode = sh.barcode
        WHERE sh.userId = :userId
        """
    )
    suspend fun getTotalCarbonValue(userId: String): Double?

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
        WHERE sh.userId = :userId
          AND sh.id = :id
        LIMIT 1
        """
    )
    suspend fun getProductByHistoryId(userId: String, id: Long): ScannedProduct?

    @Query("SELECT * FROM cached_products WHERE barcode = :barcode LIMIT 1")
    suspend fun getCachedProductByBarcode(barcode: String): CachedProductEntity?

    @Query("DELETE FROM scan_history WHERE userId = :userId")
    suspend fun deleteAllScanHistory(userId: String)

    @Query("DELETE FROM cached_products")
    suspend fun deleteAllCachedProducts()
}
