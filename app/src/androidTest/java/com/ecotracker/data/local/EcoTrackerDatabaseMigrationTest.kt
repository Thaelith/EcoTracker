package com.ecotracker.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class EcoTrackerDatabaseMigrationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "migration-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrationFrom4To6_preservesHistoryAndPromotesLatestProductPerBarcode() = runBlocking {
        createVersion4Database().use { database: SQLiteDatabase ->
            database.execSQL(
                """
                INSERT INTO scanned_products (
                    id, barcode, productName, brand, categories, imageUrl,
                    ecoScore, ecoScoreValue, carbonFootprint, status,
                    aiReasoning, aiConfidence, aiDataQuality, timestamp
                ) VALUES
                    (1, '111', 'Old Coffee', 'Brand A', 'drinks', '', 'C', 30, 1.2, 'VERIFIED', NULL, NULL, NULL, 100),
                    (2, '111', 'Fresh Coffee', 'Brand A', 'drinks', '', 'B', 60, 2.4, 'AI_ESTIMATED', 'reason', 'high', 'good', 300),
                    (3, '222', 'Soap', 'Brand B', 'beauty', '', 'A', 90, 0.7, 'CATEGORY_AVERAGE', NULL, NULL, NULL, 200)
                """.trimIndent()
            )
        }

        val migratedDb = Room.databaseBuilder(context, EcoTrackerDatabase::class.java, databaseName)
            .addMigrations(EcoTrackerDatabase.MIGRATION_4_5, EcoTrackerDatabase.MIGRATION_5_6)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = migratedDb.scannedProductDao()

            val legacyProducts = dao.getAllProducts(EcoTrackerDatabase.LEGACY_USER_ID).first()
            val cached111 = dao.getCachedProductByBarcode("111")
            val cached222 = dao.getCachedProductByBarcode("222")
            val rawDb = SQLiteDatabase.openDatabase(
                context.getDatabasePath(databaseName).path,
                null,
                SQLiteDatabase.OPEN_READONLY
            )

            rawDb.use { database: SQLiteDatabase ->
                assertEquals(
                    listOf(300L, 200L, 100L),
                    legacyProducts.map { product -> product.timestamp }
                )
                assertEquals(
                    listOf("Fresh Coffee", "Soap", "Fresh Coffee"),
                    legacyProducts.map { product -> product.productName }
                )
                assertEquals("Fresh Coffee", cached111?.productName)
                assertEquals(300L, cached111?.updatedAt)
                assertEquals("Soap", cached222?.productName)

                assertTrue(tableExists(database, "cached_products"))
                assertTrue(tableExists(database, "scan_history"))
                assertTrue(!tableExists(database, "scanned_products"))
                assertTrue(hasIndex(database, "scan_history", "index_scan_history_barcode"))
                assertTrue(hasIndex(database, "scan_history", "index_scan_history_userId_scannedAt"))
                assertEquals(
                    listOf(EcoTrackerDatabase.LEGACY_USER_ID, EcoTrackerDatabase.LEGACY_USER_ID, EcoTrackerDatabase.LEGACY_USER_ID),
                    queryScanHistoryUserIds(database)
                )
            }
        } finally {
            migratedDb.close()
        }
    }

    private fun createVersion4Database(): SQLiteDatabase {
        context.deleteDatabase(databaseName)
        val databaseFile: File = context.getDatabasePath(databaseName)
        databaseFile.parentFile?.mkdirs()

        return SQLiteDatabase.openOrCreateDatabase(databaseFile, null).apply {
            execSQL(
                """
                CREATE TABLE IF NOT EXISTS scanned_products (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    barcode TEXT NOT NULL,
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
                    timestamp INTEGER NOT NULL
                )
                """.trimIndent()
            )
            setVersion(4)
        }
    }

    private fun tableExists(database: SQLiteDatabase, tableName: String): Boolean {
        database.rawQuery(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(tableName)
        ).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private fun hasIndex(database: SQLiteDatabase, tableName: String, indexName: String): Boolean {
        database.rawQuery("PRAGMA index_list($tableName)", null).use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.getString(cursor.getColumnIndexOrThrow("name")) == indexName) {
                    return true
                }
            }
        }
        return false
    }

    private fun queryScanHistoryUserIds(database: SQLiteDatabase): List<String> {
        database.rawQuery("SELECT userId FROM scan_history ORDER BY id ASC", null).use { cursor ->
            val values = mutableListOf<String>()
            while (cursor.moveToNext()) {
                values += cursor.getString(cursor.getColumnIndexOrThrow("userId"))
            }
            return values
        }
    }
}
