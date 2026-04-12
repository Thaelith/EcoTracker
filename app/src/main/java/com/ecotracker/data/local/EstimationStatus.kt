package com.ecotracker.data.local

/**
 * Defines the estimation quality of a product's carbon footprint.
 */
enum class EstimationStatus {
    /** High confidence data from Agribalyse or official manufacturer nutriments. */
    VERIFIED,
    
    /** Forecasted data using Gemini AI analysis. */
    AI_ESTIMATED,
    
    /** Estimated using localized category-based averages. */
    CATEGORY_AVERAGE,
    
    /** Core identification succeeded but estimation was not possible. */
    NEEDS_ESTIMATION,
    
    /** Product remains fully unidentified after all lookup tiers. */
    UNIDENTIFIED
}
