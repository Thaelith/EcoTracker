package com.ecotracker.data.local

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "scanned_products")
data class ScannedProduct(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val barcode: String,
    val productName: String,
    val brand: String,
    val categories: String,
    val imageUrl: String,
    val ecoScore: String,
    val ecoScoreValue: Int,
    val carbonFootprint: Double,
    val aiReasoning: String? = null,
    val aiConfidence: String? = null,
    val aiDataQuality: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable
