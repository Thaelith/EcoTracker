package com.ecotracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CachedProductEntity::class, ScanHistoryEntity::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(EcoTrackerConverters::class)
abstract class EcoTrackerDatabase : RoomDatabase() {

    abstract fun scannedProductDao(): ScannedProductDao

    companion object {
        const val DATABASE_NAME = "eco_tracker_db"

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS cached_products (
                        barcode TEXT NOT NULL PRIMARY KEY,
                        productName TEXT NOT NULL,
                        brand TEXT NOT NULL,
                        categories TEXT NOT NULL,
                        imageUrl TEXT NOT NULL,
                        ecoScore TEXT NOT NULL,
                        ecoScoreValue INTEGER NOT NULL,
                        carbonFootprint REAL,
                        status TEXT NOT NULL,
                        aiReasoning TEXT,
                        aiConfidence TEXT,
                        aiDataQuality TEXT,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS scan_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        barcode TEXT NOT NULL,
                        scannedAt INTEGER NOT NULL,
                        FOREIGN KEY(barcode) REFERENCES cached_products(barcode) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_scan_history_barcode
                    ON scan_history(barcode)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR REPLACE INTO cached_products (
                        barcode,
                        productName,
                        brand,
                        categories,
                        imageUrl,
                        ecoScore,
                        ecoScoreValue,
                        carbonFootprint,
                        status,
                        aiReasoning,
                        aiConfidence,
                        aiDataQuality,
                        updatedAt
                    )
                    SELECT
                        sp.barcode,
                        sp.productName,
                        sp.brand,
                        sp.categories,
                        sp.imageUrl,
                        sp.ecoScore,
                        sp.ecoScoreValue,
                        sp.carbonFootprint,
                        sp.status,
                        sp.aiReasoning,
                        sp.aiConfidence,
                        sp.aiDataQuality,
                        sp.timestamp
                    FROM scanned_products sp
                    INNER JOIN (
                        SELECT barcode, MAX(timestamp) AS maxTimestamp
                        FROM scanned_products
                        GROUP BY barcode
                    ) latest
                        ON latest.barcode = sp.barcode
                       AND latest.maxTimestamp = sp.timestamp
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO scan_history (barcode, scannedAt)
                    SELECT barcode, timestamp
                    FROM scanned_products
                    ORDER BY timestamp ASC, id ASC
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE scanned_products")
            }
        }
    }
}

class EcoTrackerConverters {
    @TypeConverter
    fun fromStatus(status: EstimationStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): EstimationStatus = try {
        EstimationStatus.valueOf(value)
    } catch (e: Exception) {
        EstimationStatus.UNIDENTIFIED
    }
}
