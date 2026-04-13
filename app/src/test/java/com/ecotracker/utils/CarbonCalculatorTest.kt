package com.ecotracker.utils

import com.ecotracker.data.remote.AgribalyseDto
import com.ecotracker.data.remote.EcoScoreDataDto
import com.ecotracker.data.remote.NutrimentsDto
import com.ecotracker.data.remote.ProductDto
import org.junit.Assert.*
import org.junit.Test

class CarbonCalculatorTest {

    private fun createProduct(
        productName: String? = null,
        categories: String? = null,
        ecoScoreGrade: String? = null,
        ecoScoreData: EcoScoreDataDto? = null,
        nutriments: NutrimentsDto? = null,
        productQuantity: Double? = null
    ): ProductDto {
        return ProductDto(
            barcode = null,
            productName = productName,
            productNameEn = null,
            brands = null,
            categories = categories,
            imageUrl = null,
            imageFrontUrl = null,
            ecoScoreGrade = ecoScoreGrade,
            ecoScoreScore = null,
            ecoScoreData = ecoScoreData,
            nutriments = nutriments,
            productQuantity = productQuantity,
            packaging = null,
            origins = null,
            manufacturingPlaces = null,
            quantity = null
        )
    }

    @Test
    fun `calculateCarbonFootprint should return exactly 0_5 for grade A with default 1kg`() {
        val product = createProduct(ecoScoreGrade = "a")
        val result = CarbonCalculator.calculateCarbonFootprint(product)
        assertEquals(0.5, result.value!!, 0.001)
        assertEquals(com.ecotracker.data.local.EstimationStatus.CATEGORY_AVERAGE, result.status)
    }

    @Test
    fun `calculateCarbonFootprint should return exactly 1_4 for grade B with default 1kg`() {
        val product = createProduct(ecoScoreGrade = "B")
        val result = CarbonCalculator.calculateCarbonFootprint(product)
        assertEquals(1.4, result.value!!, 0.001)
        assertEquals(com.ecotracker.data.local.EstimationStatus.CATEGORY_AVERAGE, result.status)
    }

    @Test
    fun `calculateCarbonFootprint should scale based on product quantity`() {
        // Product is 500g (0.5kg) and grade is E (11.0 kg CO2e / kg)
        // Total should be 5.5 kg CO2e
        val product = createProduct(ecoScoreGrade = "e", productQuantity = 500.0)
        val result = CarbonCalculator.calculateCarbonFootprint(product)
        assertEquals(5.5, result.value!!, 0.001)
    }

    @Test
    fun `calculateCarbonFootprint should prioritize real agribalyse data and scale by quantity`() {
        // Agribalyse has a value of 4.2 kg CO2e per kg
        val agribalyse = AgribalyseDto(co2Total = 4.2, co2Agriculture = null, co2Packaging = null)
        val ecoScoreData = EcoScoreDataDto(score = null, grade = null, agribalyse = agribalyse)
        
        // Product is 200g (0.2kg). 4.2 * 0.2 = 0.84 kg CO2e
        val product = createProduct(
            ecoScoreGrade = "a",
            ecoScoreData = ecoScoreData,
            productQuantity = 200.0
        )
        val result = CarbonCalculator.calculateCarbonFootprint(product)
        assertEquals(0.84, result.value!!, 0.001)
        assertEquals(com.ecotracker.data.local.EstimationStatus.VERIFIED, result.status)
    }

    @Test
    fun `calculateCarbonFootprint should prioritize nutriments data over fallbacks`() {
        // Nutriments has carbonFootprintPer100g of 140g CO2e 
        val nutriments = NutrimentsDto(carbonFootprint = null, carbonFootprintPer100g = 140.0)
        
        // Product is 100g exactly (multiplier 1.0 vs 100g). 
        // 140g CO2e / 1000 = 0.14 kg CO2e 
        val product = createProduct(
            ecoScoreGrade = "e", // E fallback is 11.0
            nutriments = nutriments,
            productQuantity = 100.0
        )
        val result = CarbonCalculator.calculateCarbonFootprint(product)
        assertEquals(0.14, result.value!!, 0.001)
        assertEquals(com.ecotracker.data.local.EstimationStatus.VERIFIED, result.status)
    }

    @Test
    fun `calculateCarbonFootprint should use category map if grade is missing`() {
        // "Beef steak" contains "beef" which is 27.0 kg CO2e / kg
        val product = createProduct(
            productName = "Beef steak",
            productQuantity = 1000.0
        )
        val result = CarbonCalculator.calculateCarbonFootprint(product)
        assertEquals(27.0, result.value!!, 0.001)
        assertEquals(com.ecotracker.data.local.EstimationStatus.CATEGORY_AVERAGE, result.status)
    }

    @Test
    fun `calculateCarbonFootprint should return null for unknown items with no data`() {
        val product = createProduct(
            productName = "Mysterious Alien Gadget",
            productQuantity = 1000.0
        )
        val result = CarbonCalculator.calculateCarbonFootprint(product)
        assertEquals(null, result.value)
        assertEquals(com.ecotracker.data.local.EstimationStatus.NEEDS_ESTIMATION, result.status)
    }

    @Test
    fun `format should return dash for null value`() {
        assertEquals("— kg CO₂e", CarbonCalculator.format(null))
    }

    @Test
    fun `format should return formatted string for non-null value`() {
        val result = CarbonCalculator.format(3.14159)
        assertEquals("3.14 kg CO₂e", result)
    }

    @Test
    fun `hasRealCarbonData returns true when agribalyse data present`() {
        val agribalyse = AgribalyseDto(co2Total = 2.0, co2Agriculture = null, co2Packaging = null)
        val ecoScoreData = EcoScoreDataDto(score = null, grade = null, agribalyse = agribalyse)
        val product = createProduct(ecoScoreData = ecoScoreData)
        assertTrue(CarbonCalculator.hasRealCarbonData(product))
    }

    @Test
    fun `hasRealCarbonData returns true when nutriments data present`() {
        val nutriments = NutrimentsDto(carbonFootprint = null, carbonFootprintPer100g = 50.0)
        val product = createProduct(nutriments = nutriments)
        assertTrue(CarbonCalculator.hasRealCarbonData(product))
    }

    @Test
    fun `hasRealCarbonData returns false when neither source present`() {
        val product = createProduct(ecoScoreGrade = "B")
        assertFalse(CarbonCalculator.hasRealCarbonData(product))
    }

    @Test
    fun `calculateCarbonFootprint matches category from categories field`() {
        val product = createProduct(
            productName = "Generic Item",
            categories = "Fresh Fish",
            productQuantity = 500.0
        )
        val result = CarbonCalculator.calculateCarbonFootprint(product)
        // fish = 5.0 * 0.5kg = 2.5
        assertEquals(2.5, result.value!!, 0.001)
    }

    @Test
    fun `calculateCarbonFootprint uses default 1000g when quantity null`() {
        val product = createProduct(ecoScoreGrade = "C", productQuantity = null)
        val result = CarbonCalculator.calculateCarbonFootprint(product)
        // C = 3.0 * 1.0kg = 3.0
        assertEquals(3.0, result.value!!, 0.001)
    }
}
