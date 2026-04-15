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
    val carbonFootprint: Double?,
    val status: EstimationStatus = EstimationStatus.UNIDENTIFIED,
    val aiReasoning: String? = null,
    val aiConfidence: String? = null,
    val aiDataQuality: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {
    fun isWeak(): Boolean {
        val isUnknown = productName.equals("Unknown Product", ignoreCase = true) || 
                        productName.isBlank()
        val hasNoCarbon = carbonFootprint == null || carbonFootprint == 0.0
        
        return isUnknown || (hasNoCarbon && status == EstimationStatus.NEEDS_ESTIMATION)
    }
}
