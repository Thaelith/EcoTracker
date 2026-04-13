package com.ecotracker.utils

/**
 * Centralized configuration constants for the application.
 * Avoids scattering magic numbers across repository, DI, and UI layers.
 */
object AppConfig {

    // Network
    const val NETWORK_CONNECT_TIMEOUT_SECONDS = 60L
    const val NETWORK_READ_TIMEOUT_SECONDS = 60L

    // Global product cache (Firestore)
    const val CACHE_TTL_DAYS = 90L
    val CACHE_TTL_MILLIS: Long get() = CACHE_TTL_DAYS * 24 * 60 * 60 * 1000L

    // Leaderboard
    const val LEADERBOARD_MAX_SIZE = 20L

    // Statistics chart
    const val CHART_HISTORY_DAYS = 7

    // Username
    const val USERNAME_MAX_LENGTH = 24
    val USERNAME_REGEX = Regex("^[a-zA-Z0-9_\\- ]{1,$USERNAME_MAX_LENGTH}$")

    // Barcode masking — never log a full barcode
    const val BARCODE_VISIBLE_PREFIX = 4
}
