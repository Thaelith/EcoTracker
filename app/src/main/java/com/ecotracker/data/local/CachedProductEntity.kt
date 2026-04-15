package com.ecotracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_products")
data class CachedProductEntity(
    @PrimaryKey
    val barcode: String,
    val productName: String,
    val brand: String,
    val categories: String,
    val imageUrl: String,
    val ecoScore: String,
    val ecoScoreValue: Int,
    val carbonFootprint: Double?,
    val status: EstimationStatus,
    val aiReasoning: String? = null,
    val aiConfidence: String? = null,
    val aiDataQuality: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
