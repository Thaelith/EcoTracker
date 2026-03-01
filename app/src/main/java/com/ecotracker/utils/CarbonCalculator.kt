package com.ecotracker.utils

import com.ecotracker.data.remote.ProductDto

object CarbonCalculator {

    /**
     * Calculate a mock carbon footprint (kg CO₂e) based on available product data.
     * In a real app this would use Agribalyse or similar LCA databases.
     */
    fun calculateCarbonFootprint(product: ProductDto): Double {
        // Try real data first
        product.ecoScoreData?.agribalyse?.co2Total?.let { return it }
        product.nutriments?.carbonFootprintPer100g?.let { return it * 1.5 }

        // Fall back to eco-score based estimate
        return when (product.ecoScoreGrade?.uppercase()) {
            "A" -> (0.2..0.8).random()
            "B" -> (0.8..2.0).random()
            "C" -> (2.0..4.0).random()
            "D" -> (4.0..7.0).random()
            "E" -> (7.0..15.0).random()
            else -> (1.0..5.0).random()
        }
    }

    private fun ClosedRange<Double>.random(): Double =
        start + Math.random() * (endInclusive - start)

    /** Format kg CO₂e for display */
    fun format(value: Double): String = String.format("%.2f kg CO₂e", value)
}
