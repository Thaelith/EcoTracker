package com.ecotracker.utils

import com.ecotracker.data.remote.ProductDto

object CarbonCalculator {

    /**
     * Calculate a mock carbon footprint (kg CO₂e) based on available product data.
     * In a real app this would use Agribalyse or similar LCA databases.
     */
    fun calculateCarbonFootprint(product: ProductDto): Double {
        // Find the quantity multiplier. If we know the product is e.g. 500g, multiplier is 0.5 (kg)
        // If productQuantity is missing, we assume a standard 1kg (multiplier = 1.0)
        val quantityInKg = (product.productQuantity ?: 1000.0) / 1000.0

        // 1. Try exact product carbon footprint from unknown/known ingredients if provided in kg
        product.ecoScoreData?.agribalyse?.co2Total?.let { 
            // agribalyse.co2Total is usually per 1kg of product, so we scale it to the actual quantity
            return it * quantityInKg 
        }

        // 2. Try nutriments per 100g 
        product.nutriments?.carbonFootprintPer100g?.let { 
            // carbonFootprintPer100g is often in grams of CO2. 
            // Convert to kg of CO2e per 100g (it / 1000)
            // Multiply by (productQuantity / 100) to get total
            val quantityIn100g = (product.productQuantity ?: 1000.0) / 100.0
            val co2InKgPer100g = it / 1000.0
            return co2InKgPer100g * quantityIn100g
        }

        // 3. Fall back to eco-score based estimate (values represent kg CO2e per 1kg of product)
        val baseScore = when (product.ecoScoreGrade?.uppercase()) {
            "A" -> 0.5
            "B" -> 1.4
            "C" -> 3.0
            "D" -> 5.5
            "E" -> 11.0
            else -> 3.0
        }
        
        return baseScore * quantityInKg
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
    fun format(value: Double): String = String.format("%.2f kg CO₂e", value)
}
