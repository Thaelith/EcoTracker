package com.ecotracker.utils

import com.ecotracker.data.local.EstimationStatus
import com.ecotracker.data.remote.ProductDto

object CarbonCalculator {

    /**
     * Calculate a mock carbon footprint (kg CO₂e) based on available product data.
     * In a real app this would use Agribalyse or similar LCA databases.
     */
    /**
     * Map of common categories to average kg CO2e per 1kg of product.
     * Based on generalized lifecycle assessment (LCA) data.
     */
    private val CATEGORY_CARBON_MAP = mapOf(
        "beef" to 27.0,
        "lamb" to 24.0,
        "cheese" to 11.0,
        "chocolate" to 19.0,
        "coffee" to 17.0,
        "pork" to 7.0,
        "poultry" to 6.0,
        "fish" to 5.0,
        "eggs" to 4.5,
        "rice" to 4.0,
        "milk" to 2.5,
        "tofu" to 2.0,
        "vegetables" to 0.4,
        "fruit" to 0.5,
        "grains" to 1.0,
        "water" to 0.1,
        "electronics" to 15.0,
        "cosmetics" to 3.0,
        "packaging" to 2.0
    )

    /**
     * Represents the result of a carbon footprint calculation.
     */
    data class CalculationResult(
        val value: Double?,
        val status: EstimationStatus
    )

    /**
     * Calculate a carbon footprint (kg CO₂e) based on available product data.
     * Resolution order: Agribalyse -> Nutriments -> Category Map -> EcoScore Grade.
     */
    fun calculateCarbonFootprint(product: ProductDto): CalculationResult {
        val quantityInKg = (product.productQuantity ?: 1000.0) / 1000.0

        // 1. Precise LCA data (Agribalyse)
        product.ecoScoreData?.agribalyse?.co2Total?.let { 
            return CalculationResult(it * quantityInKg, EstimationStatus.VERIFIED)
        }

        // 2. Nutriments (carbon_footprint_100g)
        product.nutriments?.carbonFootprintPer100g?.let { 
            val quantityIn100g = (product.productQuantity ?: 1000.0) / 100.0
            val result = (it / 1000.0) * quantityIn100g
            return CalculationResult(result, EstimationStatus.VERIFIED)
        }

        // 3. Category Safety Net (matches key categories in the name or category string)
        val searchString = (product.categories ?: "") + " " + (product.productName ?: "")
        CATEGORY_CARBON_MAP.forEach { (cat, co2) ->
            if (searchString.contains(cat, ignoreCase = true)) {
                return CalculationResult(co2 * quantityInKg, EstimationStatus.CATEGORY_AVERAGE)
            }
        }

        // 4. Broad Eco-Score Fallback
        val baseScore = when (product.ecoScoreGrade?.uppercase()) {
            "A" -> 0.5
            "B" -> 1.4
            "C" -> 3.0
            "D" -> 5.5
            "E" -> 11.0
            else -> null
        }
        
        return if (baseScore != null) {
            CalculationResult(baseScore * quantityInKg, EstimationStatus.CATEGORY_AVERAGE)
        } else {
            CalculationResult(null, EstimationStatus.NEEDS_ESTIMATION)
        }
    }

    /**
     * Returns true if the product has real carbon data from Agribalyse or nutriments.
     * Returns false if we'd be falling back to a generic eco-score estimate.
     */
    fun hasRealCarbonData(product: ProductDto): Boolean {
        if (product.ecoScoreData?.agribalyse?.co2Total != null) return true
        if (product.nutriments?.carbonFootprintPer100g != null) return true
        return false
    }

    /** Format kg CO₂e for display */
    fun format(value: Double?): String = 
        if (value == null) "— kg CO₂e" else String.format("%.2f kg CO₂e", value)
}
