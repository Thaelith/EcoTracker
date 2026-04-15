package com.ecotracker.utils

import android.util.Log
import com.ecotracker.BuildConfig

/**
 * Thin logging wrapper.
 * - Suppresses debug logs in release builds.
 * - Provides a single place to enforce masking / filtering.
 */
object Logger {

    private const val DEFAULT_TAG = "EcoTracker"

    fun debug(tag: String = DEFAULT_TAG, message: String) {
        if (BuildConfig.DEBUG) {
            runCatching { Log.d(tag, message) }
        }
    }

    fun info(tag: String = DEFAULT_TAG, message: String) {
        runCatching { Log.i(tag, message) }
    }

    fun warn(tag: String = DEFAULT_TAG, message: String) {
        runCatching { Log.w(tag, message) }
    }

    fun error(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        runCatching {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        }
    }

    /** Masks a barcode for safe logging. */
    fun maskBarcode(barcode: String): String {
        return if (barcode.length > AppConfig.BARCODE_VISIBLE_PREFIX) {
            barcode.take(AppConfig.BARCODE_VISIBLE_PREFIX) + "***"
        } else {
            "***"
        }
    }
}
