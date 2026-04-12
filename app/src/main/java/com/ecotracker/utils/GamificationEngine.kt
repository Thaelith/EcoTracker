package com.ecotracker.utils

import com.ecotracker.R
import com.ecotracker.data.local.EstimationStatus
import com.ecotracker.data.local.ScannedProduct

data class Badge(
    val id: String,
    val nameResId: Int,
    val descResId: Int,
    val isUnlocked: Boolean,
    val iconResId: Int? = null
)

data class RankInfo(
    val nameResId: Int,
    val level: Int,
    val currentProgress: Int,
    val maxProgress: Int,
    val percentage: Int
)

object GamificationEngine {

    fun calculateRank(scanCount: Int): RankInfo {
        return when {
            scanCount < 1 -> RankInfo(R.string.rank_seedling, 0, scanCount, 1, 0)
            scanCount < 5 -> RankInfo(R.string.rank_sprout, 1, scanCount - 1, 4, ((scanCount - 1) / 4f * 100).toInt())
            scanCount < 15 -> RankInfo(R.string.rank_sapling, 2, scanCount - 5, 10, ((scanCount - 5) / 10f * 100).toInt())
            scanCount < 30 -> RankInfo(R.string.rank_tree, 3, scanCount - 15, 15, ((scanCount - 15) / 15f * 100).toInt())
            else -> RankInfo(R.string.rank_forest_guardian, 4, 30, 30, 100)
        }
    }

    fun getBadges(products: List<ScannedProduct>): List<Badge> {
        val scanCount = products.size
        
        return listOf(
            Badge(
                "first_seed",
                R.string.badge_first_seed_name,
                R.string.badge_first_seed_desc,
                scanCount >= 1
            ),
            Badge(
                "eco_expert",
                R.string.badge_eco_expert_name,
                R.string.badge_eco_expert_desc,
                scanCount >= 5
            ),
            Badge(
                "verified_scout",
                R.string.badge_verified_scout_name,
                R.string.badge_verified_scout_desc,
                products.any { it.status == EstimationStatus.VERIFIED }
            ),
            Badge(
                "carbon_hero",
                R.string.badge_carbon_hero_name,
                R.string.badge_carbon_hero_desc,
                products.any { it.carbonFootprint != null && it.carbonFootprint < 1.0 }
            )
        )
    }
}
