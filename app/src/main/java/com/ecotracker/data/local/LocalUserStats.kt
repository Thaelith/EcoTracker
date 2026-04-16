package com.ecotracker.data.local

data class LocalUserStats(
    val scanCount: Int,
    val totalCarbon: Double,
    val hasVerified: Int,
    val hasLowCarbon: Int
) {
    val verifiedBadgeUnlocked: Boolean
        get() = hasVerified != 0

    val lowCarbonBadgeUnlocked: Boolean
        get() = hasLowCarbon != 0
}
