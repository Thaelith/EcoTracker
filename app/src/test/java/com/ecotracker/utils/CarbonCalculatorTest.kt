package com.ecotracker.utils

import com.ecotracker.data.remote.AgribalyseDto
import com.ecotracker.data.remote.EcoScoreDataDto
import com.ecotracker.data.remote.NutrimentsDto
import com.ecotracker.data.remote.ProductDto
import org.junit.Assert.assertEquals
import org.junit.Test

class CarbonCalculatorTest {

    private fun createProduct(
        ecoScoreGrade: String? = null,
        ecoScoreData: EcoScoreDataDto? = null,
        nutriments: NutrimentsDto? = null,
        productQuantity: Double? = null
    ): ProductDto {
        return ProductDto(
            barcode = null,
            productName = null,
            productNameEn = null,
            brands = null,
            categories = null,
            imageUrl = null,
            imageFrontUrl = null,
            ecoScoreGrade = ecoScoreGrade,
            ecoScoreScore = null,
            ecoScoreData = ecoScoreData,
            nutriments = nutriments,
            productQuantity = productQuantity,
            packaging = null,
            origins = null,
            manufacturingPlaces = null
        )
    }

    @Test
    fun `calculateCarbonFootprint should return exactly 0_5 for grade A with default 1kg`() {
        val product = createProduct(ecoScoreGrade = "a")
        val result = CarbonCalculator.calculateCarbonFootprint(product)
        assertEquals(0.5, result, 0.001)
    }

    @Test
    fun `calculateCarbonFootprint should return exactly 1_4 for grade B with default 1kg`() {
        val product = createProduct(ecoScoreGrade = "B")
        val result = CarbonCalculator.calculateCarbonFootprint(product)
        assertEquals(1.4, result, 0.001)
    }

    @Test
    fun `calculateCarbonFootprint should scale based on product quantity`() {
        // Product is 500g (0.5kg) and grade is E (11.0 kg CO2e / kg)
        // Total should be 5.5 kg CO2e
        val product = createProduct(ecoScoreGrade = "e", productQuantity = 500.0)
        val result = CarbonCalculator.calculateCarbonFootprint(product)
        assertEquals(5.5, result, 0.001)
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
        assertEquals(0.84, result, 0.001)
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
        assertEquals(0.14, result, 0.001)
    }
}
