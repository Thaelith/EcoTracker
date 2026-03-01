package com.ecotracker.utils

import android.content.Context
import android.view.View
import android.widget.Toast
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
    "A"  -> android.graphics.Color.parseColor("#1a9850")
    "B"  -> android.graphics.Color.parseColor("#91cf60")
    "C"  -> android.graphics.Color.parseColor("#fee08b")
    "D"  -> android.graphics.Color.parseColor("#fc8d59")
    "E"  -> android.graphics.Color.parseColor("#d73027")
    else -> android.graphics.Color.parseColor("#9e9e9e")
}
