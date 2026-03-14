package com.ecotracker.utils

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.Toast
import androidx.core.graphics.ColorUtils
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
    return ScannedProduct(
        barcode       = barcode,
        productName   = productName ?: productNameEn ?: "Unknown Product",
        brand         = brands ?: "Unknown Brand",
        ecoScore      = grade,
        ecoScoreValue = score,
        carbonFootprint = CarbonCalculator.calculateCarbonFootprint(this),
        imageUrl      = imageUrl ?: imageFrontUrl ?: "",
        categories    = categories ?: ""
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
fun Double.toColorGradient(): Int {
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
