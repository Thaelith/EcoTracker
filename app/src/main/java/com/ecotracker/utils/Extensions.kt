package com.ecotracker.utils

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import com.ecotracker.data.local.CachedProductEntity
import com.ecotracker.data.local.EstimationStatus
import com.ecotracker.data.local.ScannedProduct
import com.ecotracker.data.remote.ProductDto
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

// ── View extensions ───────────────────────────────────────────────────────────

fun View.visible() { visibility = View.VISIBLE }
fun View.gone()    { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }

fun View.showSnackbar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
    Snackbar.make(this, message, duration).show()
}

// ── Context extensions ────────────────────────────────────────────────────────

fun Context.toast(message: String) =
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

// ── Date extensions ───────────────────────────────────────────────────────────

fun Long.toFormattedDate(): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toShortDate(): String {
    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
    return sdf.format(Date(this))
}

fun startOfDay(offsetDays: Int = 0): Long {
    val cal = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -offsetDays)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

// ── ProductDto → ScannedProduct mapper ───────────────────────────────────────

fun ProductDto.toScannedProduct(barcode: String): ScannedProduct {
    val grade = ecoScoreGrade?.uppercase() ?: "N/A"
    val score = ecoScoreScore ?: ecoScoreData?.score ?: 0
    val calc = CarbonCalculator.calculateCarbonFootprint(this)
    
    return ScannedProduct(
        barcode       = barcode,
        productName   = productName ?: productNameEn ?: "Unknown Product",
        brand         = brands ?: "Unknown Brand",
        ecoScore      = grade,
        ecoScoreValue = score,
        carbonFootprint = calc.value,
        status        = calc.status,
        imageUrl      = imageUrl ?: imageFrontUrl ?: "",
        categories    = categories ?: ""
    )
}

fun ScannedProduct.toCachedProductEntity(updatedAt: Long = System.currentTimeMillis()): CachedProductEntity {
    return CachedProductEntity(
        barcode = barcode,
        productName = productName,
        brand = brand,
        categories = categories,
        imageUrl = imageUrl,
        ecoScore = ecoScore,
        ecoScoreValue = ecoScoreValue,
        carbonFootprint = carbonFootprint,
        status = status,
        aiReasoning = aiReasoning,
        aiConfidence = aiConfidence,
        aiDataQuality = aiDataQuality,
        updatedAt = updatedAt
    )
}

fun CachedProductEntity.toScannedProduct(
    id: Long = 0,
    timestamp: Long = updatedAt
): ScannedProduct {
    return ScannedProduct(
        id = id,
        barcode = barcode,
        productName = productName,
        brand = brand,
        categories = categories,
        imageUrl = imageUrl,
        ecoScore = ecoScore,
        ecoScoreValue = ecoScoreValue,
        carbonFootprint = carbonFootprint,
        status = status,
        aiReasoning = aiReasoning,
        aiConfidence = aiConfidence,
        aiDataQuality = aiDataQuality,
        timestamp = timestamp
    )
}

// ── EcoScore color helper ─────────────────────────────────────────────────────

fun String.ecoScoreColor(): Int = when (this.uppercase()) {
    "A"  -> Color.parseColor("#1a9850") // Green
    "B"  -> Color.parseColor("#91cf60") // Light green
    "C"  -> Color.parseColor("#fee08b") // Yellow
    "D"  -> Color.parseColor("#fc8d59") // Orange
    "E"  -> Color.parseColor("#d73027") // Red
    else -> Color.parseColor("#9e9e9e") // Grey
}

// ── Carbon Footprint gradient helper ──────────────────────────────────────────

/**
 * Maps a CO2e value (kg) to a color gradient interpolating from Green (0) to Yellow (3.0) to Red (11.0+).
 */
fun Double?.toColorGradient(): Int {
    if (this == null) return Color.parseColor("#9e9e9e") // Grey for unknown

    val green = Color.parseColor("#1a9850")  // 0.0
    val yellow = Color.parseColor("#ffd54f") // ~3.0
    val red = Color.parseColor("#d73027")    // >= 11.0

    return when {
        this <= 0.0 -> green
        this < 3.0 -> ColorUtils.blendARGB(green, yellow, (this / 3.0).toFloat())
        this < 11.0 -> ColorUtils.blendARGB(yellow, red, ((this - 3.0) / 8.0).toFloat())
        else -> red
    }
}

// ── Status Color Helper ───────────────────────────────────────────────────────

fun EstimationStatus.toColor(): Int = when (this) {
    EstimationStatus.VERIFIED         -> Color.parseColor("#1b5e20") // Deep Green
    EstimationStatus.AI_ESTIMATED     -> Color.parseColor("#1565c0") // Deep Blue
    EstimationStatus.CATEGORY_AVERAGE -> Color.parseColor("#f9a825") // Deep Yellow
    EstimationStatus.NEEDS_ESTIMATION -> Color.parseColor("#ef6c00") // Orange
    EstimationStatus.UNIDENTIFIED     -> Color.parseColor("#9e9e9e") // Grey
}
