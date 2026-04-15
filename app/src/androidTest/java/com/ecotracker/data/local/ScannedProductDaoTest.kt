package com.ecotracker.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScannedProductDaoTest {

    private lateinit var database: EcoTrackerDatabase
    private lateinit var dao: ScannedProductDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, EcoTrackerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.scannedProductDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getAllProducts_keepsEveryHistoryRowButUsesLatestCachedProductState() = runBlocking {
        dao.upsertCachedProduct(
            cachedProduct(
                barcode = "111",
                productName = "Old Name",
                carbonFootprint = 1.0,
                updatedAt = 100L
            )
        )
        dao.insertScanHistory(ScanHistoryEntity(barcode = "111", scannedAt = 100L))

        dao.upsertCachedProduct(
            cachedProduct(
                barcode = "111",
                productName = "New Name",
                carbonFootprint = 2.5,
                updatedAt = 300L
            )
        )
        dao.insertScanHistory(ScanHistoryEntity(barcode = "111", scannedAt = 300L))

        dao.upsertCachedProduct(
            cachedProduct(
                barcode = "222",
                productName = "Other Product",
                carbonFootprint = 4.0,
                updatedAt = 200L
            )
        )
        dao.insertScanHistory(ScanHistoryEntity(barcode = "222", scannedAt = 200L))

        val products = dao.getAllProducts().first()

        assertEquals(listOf(300L, 200L, 100L), products.map { it.timestamp })
        assertEquals(listOf("111", "222", "111"), products.map { it.barcode })
        assertEquals(listOf("New Name", "Other Product", "New Name"), products.map { it.productName })
        assertEquals(listOf(2.5, 4.0, 2.5), products.map { it.carbonFootprint })
    }

    @Test
    fun statisticsQueries_filterByTimeWindowAndCountRepeatedScans() = runBlocking {
        dao.upsertCachedProduct(
            cachedProduct(
                barcode = "111",
                productName = "Repeat Scan Product",
                carbonFootprint = 1.25
            )
        )
        dao.upsertCachedProduct(
            cachedProduct(
                barcode = "222",
                productName = "Older Product",
                carbonFootprint = 4.0
            )
        )

        dao.insertScanHistory(ScanHistoryEntity(barcode = "222", scannedAt = 50L))
        dao.insertScanHistory(ScanHistoryEntity(barcode = "111", scannedAt = 150L))
        dao.insertScanHistory(ScanHistoryEntity(barcode = "111", scannedAt = 250L))

        val recentProducts = dao.getProductsSince(100L).first()
        val recentCarbon = dao.getTotalCarbonSince(100L).first()
        val totalCount = dao.getTotalScannedCount().first()

        assertEquals(listOf(250L, 150L), recentProducts.map { it.timestamp })
        assertEquals(listOf("111", "111"), recentProducts.map { it.barcode })
        assertEquals(2.5, recentCarbon ?: 0.0, 0.0001)
        assertEquals(3, totalCount)
    }

    @Test
    fun deleteScanHistoryById_removesOnlyThatHistoryEntryAndLeavesCacheIntact() = runBlocking {
        dao.upsertCachedProduct(
            cachedProduct(
                barcode = "111",
                productName = "Reusable Cache",
                carbonFootprint = 3.0
            )
        )

        val firstId = dao.insertScanHistory(ScanHistoryEntity(barcode = "111", scannedAt = 100L))
        val secondId = dao.insertScanHistory(ScanHistoryEntity(barcode = "111", scannedAt = 200L))

        dao.deleteScanHistoryById(firstId)

        val products = dao.getAllProducts().first()
        val cachedProduct = dao.getCachedProductByBarcode("111")

        assertEquals(listOf(secondId), products.map { it.id })
        assertEquals(listOf(200L), products.map { it.timestamp })
        assertNotNull(cachedProduct)
        assertEquals("Reusable Cache", cachedProduct?.productName)
    }

    @Test
    fun deleteAllScanHistory_clearsHistoryWithoutDeletingCachedProducts() = runBlocking {
        dao.upsertCachedProduct(
            cachedProduct(
                barcode = "111",
                productName = "Cached Only",
                carbonFootprint = 1.5
            )
        )
        dao.insertScanHistory(ScanHistoryEntity(barcode = "111", scannedAt = 100L))

        dao.deleteAllScanHistory()

        assertEquals(emptyList<ScannedProduct>(), dao.getAllProducts().first())
        assertEquals(0, dao.getTotalScannedCount().first())
        assertNotNull(dao.getCachedProductByBarcode("111"))

        dao.deleteAllCachedProducts()

        assertNull(dao.getCachedProductByBarcode("111"))
    }

    private fun cachedProduct(
        barcode: String,
        productName: String,
        carbonFootprint: Double,
        updatedAt: Long = 1L
    ) = CachedProductEntity(
        barcode = barcode,
        productName = productName,
        brand = "Brand",
        categories = "Category",
        imageUrl = "",
        ecoScore = "B",
        ecoScoreValue = 55,
        carbonFootprint = carbonFootprint,
        status = EstimationStatus.VERIFIED,
        updatedAt = updatedAt
    )
}
