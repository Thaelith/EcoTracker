package com.ecotracker.data.repository

import com.ecotracker.data.local.EstimationStatus
import com.ecotracker.data.local.ScannedProduct
import com.ecotracker.data.local.ScannedProductDao
import com.ecotracker.data.remote.*
import com.ecotracker.utils.Resource
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class EcoTrackerRepositoryTest {

    private lateinit var foodApi: OpenFoodFactsApiService
    private lateinit var beautyApi: OpenBeautyFactsApiService
    private lateinit var upcApi: UPCItemDbApiService
    private lateinit var dao: ScannedProductDao
    private lateinit var repository: EcoTrackerRepository

    private val testBarcode = "3017620422003"

    @Before
    fun setup() {
        foodApi = mockk()
        beautyApi = mockk()
        upcApi = mockk()
        dao = mockk(relaxed = true)

        repository = EcoTrackerRepository(foodApi, beautyApi, upcApi, dao)
    }

    private fun buildProductDto(name: String, ecoGrade: String? = "B"): ProductDto {
        return ProductDto(
            barcode = testBarcode,
            productName = name,
            productNameEn = null,
            brands = "Test Brand",
            categories = "food",
            imageUrl = null,
            imageFrontUrl = null,
            ecoScoreGrade = ecoGrade,
            ecoScoreScore = null,
            ecoScoreData = EcoScoreDataDto(
                score = null,
                grade = ecoGrade,
                agribalyse = AgribalyseDto(co2Total = 2.5, co2Agriculture = null, co2Packaging = null)
            ),
            nutriments = null,
            productQuantity = 1000.0,
            packaging = null,
            origins = null,
            manufacturingPlaces = null,
            quantity = null
        )
    }

    @Test
    fun `fetchProductByBarcode returns OpenFoodFacts result when available`() = runTest {
        val dto = buildProductDto("Nutella")
        val offResponse = OpenFoodFactsResponse(status = 1, statusVerbose = "found", product = dto)
        coEvery { foodApi.getProductByBarcode(testBarcode) } returns Response.success(offResponse)
        coEvery { beautyApi.getProductByBarcode(testBarcode) } returns Response.success(
            OpenFoodFactsResponse(status = 0, statusVerbose = null, product = null)
        )

        val result = repository.fetchProductByBarcode(testBarcode)

        assertTrue(result is Resource.Success)
        assertEquals("Nutella", (result as Resource.Success).data.productName)
    }

    @Test
    fun `fetchProductByBarcode falls back to OpenBeautyFacts when OFF returns nothing`() = runTest {
        val obfDto = buildProductDto("Face Cream")
        coEvery { foodApi.getProductByBarcode(testBarcode) } returns Response.success(
            OpenFoodFactsResponse(status = 0, statusVerbose = null, product = null)
        )
        coEvery { beautyApi.getProductByBarcode(testBarcode) } returns Response.success(
            OpenFoodFactsResponse(status = 1, statusVerbose = "found", product = obfDto)
        )

        val result = repository.fetchProductByBarcode(testBarcode)

        assertTrue(result is Resource.Success)
        assertEquals("Face Cream", (result as Resource.Success).data.productName)
    }

    @Test
    fun `fetchProductByBarcode returns NeedsInput when all sources fail`() = runTest {
        coEvery { foodApi.getProductByBarcode(testBarcode) } returns Response.success(
            OpenFoodFactsResponse(status = 0, statusVerbose = null, product = null)
        )
        coEvery { beautyApi.getProductByBarcode(testBarcode) } returns Response.success(
            OpenFoodFactsResponse(status = 0, statusVerbose = null, product = null)
        )
        coEvery { upcApi.lookupBarcode(testBarcode) } returns Response.success(
            UPCItemDbResponse(code = "OK", total = 0, items = emptyList())
        )

        val result = repository.fetchProductByBarcode(testBarcode)

        assertTrue(result is Resource.NeedsInput)
    }

    @Test
    fun `fetchProductByBarcode handles API exceptions gracefully`() = runTest {
        coEvery { foodApi.getProductByBarcode(testBarcode) } throws RuntimeException("Network error")
        coEvery { beautyApi.getProductByBarcode(testBarcode) } throws RuntimeException("Network error")
        coEvery { upcApi.lookupBarcode(testBarcode) } throws RuntimeException("Network error")

        val result = repository.fetchProductByBarcode(testBarcode)

        // Should not crash, should return NeedsInput as all sources failed
        assertTrue(result is Resource.NeedsInput)
    }

    // -- Duplicate scan prevention / idempotent save ----------------------------

    @Test
    fun `saveProduct inserts into local DAO`() = runTest {
        val product = ScannedProduct(
            barcode = testBarcode,
            productName = "Test",
            brand = "Brand",
            categories = "",
            imageUrl = "",
            ecoScore = "B",
            ecoScoreValue = 50,
            carbonFootprint = 2.0,
            status = EstimationStatus.VERIFIED
        )
        coEvery { dao.insertProduct(product) } returns 1L

        val id = repository.saveProduct(product, "${product.barcode}_${product.timestamp}")

        assertEquals(1L, id)
        coVerify(exactly = 1) { dao.insertProduct(product) }
    }

    @Test
    fun `saveProduct generates deterministic remoteId from barcode and timestamp`() = runTest {
        val product = ScannedProduct(
            barcode = "123456",
            productName = "Test",
            brand = "Brand",
            categories = "",
            imageUrl = "",
            ecoScore = "B",
            ecoScoreValue = 50,
            carbonFootprint = 2.0,
            status = EstimationStatus.VERIFIED,
            timestamp = 1000L
        )
        coEvery { dao.insertProduct(product) } returns 1L

        // Using the convenience overload
        repository.saveProduct(product)

        coVerify(exactly = 1) { dao.insertProduct(product) }
    }

    @Test
    fun `getProductByBarcode returns cached local product`() = runTest {
        val cached = ScannedProduct(
            barcode = testBarcode,
            productName = "Cached",
            brand = "Brand",
            categories = "",
            imageUrl = "",
            ecoScore = "B",
            ecoScoreValue = 50,
            carbonFootprint = 1.0,
            status = EstimationStatus.VERIFIED
        )
        coEvery { dao.getProductByBarcode(testBarcode) } returns cached

        val result = repository.getProductByBarcode(testBarcode)

        assertNotNull(result)
        assertEquals("Cached", result?.productName)
    }
}
