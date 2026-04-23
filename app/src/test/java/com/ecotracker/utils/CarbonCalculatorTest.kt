package com.ecotracker.utils

import com.ecotracker.data.local.EstimationStatus
import com.ecotracker.data.remote.AgribalyseDto
import com.ecotracker.data.remote.EcoScoreDataDto
import com.ecotracker.data.remote.NutrimentsDto
import com.ecotracker.data.remote.ProductDto
import org.junit.Assert.*
import org.junit.Test

class CarbonCalculatorTest {

    private fun buildProductDto(
        agribalyseCo2: Double? = null,
        carbonPer100g: Double? = null,
        categories: String? = null,
        productName: String? = null,
        ecoScoreGrade: String? = null,
        productQuantity: Double? = 1000.0
    ): ProductDto {
        return ProductDto(
            barcode = "123",
            productName = productName,
            productNameEn = null,
            brands = null,
            categories = categories,
            imageUrl = null,
            imageFrontUrl = null,
            ecoScoreGrade = ecoScoreGrade,
            ecoScoreScore = null,
            ecoScoreData = if (agribalyseCo2 != null) {
                EcoScoreDataDto(
                    score = null,
                    grade = ecoScoreGrade,
                    agribalyse = AgribalyseDto(
                        co2Total = agribalyseCo2,
                        co2Agriculture = null,
                        co2Packaging = null
                    )
                )
            } else {
                null
            },
            nutriments = if (carbonPer100g != null) {
                NutrimentsDto(
                    carbonFootprint = null,
                    carbonFootprintPer100g = carbonPer100g
                )
            } else {
                null
            },
            productQuantity = productQuantity,
            packaging = null,
            origins = null,
            manufacturingPlaces = null,
            quantity = null
        )
    }

    @Test
    fun `calculateCarbonFootprint uses Agribalyse data when available`() {
        val product = buildProductDto(agribalyseCo2 = 2.5, productQuantity = 1000.0)
        val result = CarbonCalculator.calculateCarbonFootprint(product)

        assertTrue(result.status == EstimationStatus.VERIFIED)
        assertEquals(2.5, result.value!!, 0.001)
    }

    @Test
    fun `calculateCarbonFootprint scales Agribalyse by quantity`() {
        val product = buildProductDto(agribalyseCo2 = 2.5, productQuantity = 500.0)
        val result = CarbonCalculator.calculateCarbonFootprint(product)

        assertTrue(result.status == EstimationStatus.VERIFIED)
        assertEquals(1.25, result.value!!, 0.001)
    }

    @Test
    fun `calculateCarbonFootprint falls back to nutriments when no Agribalyse`() {
        val product = buildProductDto(carbonPer100g = 500.0, productQuantity = 1000.0)
        val result = CarbonCalculator.calculateCarbonFootprint(product)

        assertTrue(result.status == EstimationStatus.VERIFIED)
        // (500 / 1000) * (1000 / 100) = 0.5 * 10 = 5.0
        assertEquals(5.0, result.value!!, 0.001)
    }

    @Test
    fun `calculateCarbonFootprint uses category map for beef`() {
        val product = buildProductDto(categories = "food beef steak", productName = "Beef Steak")
        val result = CarbonCalculator.calculateCarbonFootprint(product)

        assertTrue(result.status == EstimationStatus.CATEGORY_AVERAGE)
        assertEquals(27.0, result.value!!, 0.001)
    }

    @Test
    fun `calculateCarbonFootprint uses category map for vegetables`() {
        val product = buildProductDto(categories = "fresh vegetables", productName = "Carrots")
        val result = CarbonCalculator.calculateCarbonFootprint(product)

        assertTrue(result.status == EstimationStatus.CATEGORY_AVERAGE)
        assertEquals(0.4, result.value!!, 0.001)
    }

    @Test
    fun `calculateCarbonFootprint uses ecoScore grade fallback`() {
        val product = buildProductDto(ecoScoreGrade = "A")
        val result = CarbonCalculator.calculateCarbonFootprint(product)

        assertTrue(result.status == EstimationStatus.CATEGORY_AVERAGE)
        assertEquals(0.5, result.value!!, 0.001)
    }

    @Test
    fun `calculateCarbonFootprint returns needs estimation when no data`() {
        val product = buildProductDto()
        val result = CarbonCalculator.calculateCarbonFootprint(product)

        assertTrue(result.status == EstimationStatus.NEEDS_ESTIMATION)
        assertNull(result.value)
    }

    @Test
    fun `hasRealCarbonData returns true for Agribalyse`() {
        val product = buildProductDto(agribalyseCo2 = 2.5)
        assertTrue(CarbonCalculator.hasRealCarbonData(product))
    }

    @Test
    fun `hasRealCarbonData returns true for nutriments`() {
        val product = buildProductDto(carbonPer100g = 500.0)
        assertTrue(CarbonCalculator.hasRealCarbonData(product))
    }

    @Test
    fun `hasRealCarbonData returns false for category fallback`() {
        val product = buildProductDto(categories = "beef")
        assertFalse(CarbonCalculator.hasRealCarbonData(product))
    }

    @Test
    fun `format returns dash for null`() {
        assertEquals("— kg CO₂e", CarbonCalculator.format(null))
    }

    @Test
    fun `format returns formatted value`() {
        assertEquals("2.50 kg CO₂e", CarbonCalculator.format(2.5))
    }

    @Test
    fun `format handles zero`() {
        assertEquals("0.00 kg CO₂e", CarbonCalculator.format(0.0))
    }
}
