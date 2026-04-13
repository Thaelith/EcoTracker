package com.ecotracker.data.model

data class LeaderboardUser(
    val uid: String,
    val rank: Int,
    val username: String,
    val scanCount: Int,
    val co2e: Double
)
