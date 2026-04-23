package com.ecotracker.utils

import com.ecotracker.R
import com.ecotracker.data.local.EstimationStatus
import com.ecotracker.data.local.ScannedProduct
import org.junit.Assert.*
import org.junit.Test

class GamificationEngineTest {

    private fun buildProduct(
        status: EstimationStatus = EstimationStatus.VERIFIED,
        carbonFootprint: Double? = 2.0
    ): ScannedProduct {
        return ScannedProduct(
            barcode = "123",
            productName = "Test",
            brand = "Brand",
            categories = "",
            imageUrl = "",
            ecoScore = "B",
            ecoScoreValue = 50,
            carbonFootprint = carbonFootprint,
            status = status
        )
    }

    // ── Rank Tests ────────────────────────────────────────────────────────────

    @Test
    fun `calculateRank returns seedling for 0 scans`() {
        val rank = GamificationEngine.calculateRank(0)
        assertEquals(R.string.rank_seedling, rank.nameResId)
        assertEquals(0, rank.level)
        assertEquals(0, rank.percentage)
    }

    @Test
    fun `calculateRank returns sprout for 1 scan`() {
        val rank = GamificationEngine.calculateRank(1)
        assertEquals(R.string.rank_sprout, rank.nameResId)
        assertEquals(1, rank.level)
        assertEquals(0, rank.percentage)
    }

    @Test
    fun `calculateRank returns sprout for 4 scans`() {
        val rank = GamificationEngine.calculateRank(4)
        assertEquals(R.string.rank_sprout, rank.nameResId)
        assertEquals(1, rank.level)
        assertEquals(75, rank.percentage)
    }

    @Test
    fun `calculateRank returns sapling for 5 scans`() {
        val rank = GamificationEngine.calculateRank(5)
        assertEquals(R.string.rank_sapling, rank.nameResId)
        assertEquals(2, rank.level)
        assertEquals(0, rank.percentage)
    }

    @Test
    fun `calculateRank returns sapling for 14 scans`() {
        val rank = GamificationEngine.calculateRank(14)
        assertEquals(R.string.rank_sapling, rank.nameResId)
        assertEquals(2, rank.level)
        assertEquals(90, rank.percentage)
    }

    @Test
    fun `calculateRank returns tree for 15 scans`() {
        val rank = GamificationEngine.calculateRank(15)
        assertEquals(R.string.rank_tree, rank.nameResId)
        assertEquals(3, rank.level)
        assertEquals(0, rank.percentage)
    }

    @Test
    fun `calculateRank returns tree for 29 scans`() {
        val rank = GamificationEngine.calculateRank(29)
        assertEquals(R.string.rank_tree, rank.nameResId)
        assertEquals(3, rank.level)
        assertEquals(93, rank.percentage)
    }

    @Test
    fun `calculateRank returns forest guardian for 30 scans`() {
        val rank = GamificationEngine.calculateRank(30)
        assertEquals(R.string.rank_forest_guardian, rank.nameResId)
        assertEquals(4, rank.level)
        assertEquals(100, rank.percentage)
    }

    @Test
    fun `calculateRank returns forest guardian for 100 scans`() {
        val rank = GamificationEngine.calculateRank(100)
        assertEquals(R.string.rank_forest_guardian, rank.nameResId)
        assertEquals(4, rank.level)
        assertEquals(100, rank.percentage)
    }

    // ── Badge Tests ───────────────────────────────────────────────────────────

    @Test
    fun `getBadges returns all locked for empty list`() {
        val badges = GamificationEngine.getBadges(emptyList())

        assertEquals(4, badges.size)
        assertFalse(badges[0].isUnlocked) // first_seed
        assertFalse(badges[1].isUnlocked) // eco_expert
        assertFalse(badges[2].isUnlocked) // verified_scout
        assertFalse(badges[3].isUnlocked) // carbon_hero
    }

    @Test
    fun `getBadges unlocks first_seed with one product`() {
        val badges = GamificationEngine.getBadges(listOf(buildProduct()))

        assertTrue(badges[0].isUnlocked) // first_seed
        assertFalse(badges[1].isUnlocked) // eco_expert (needs 5)
    }

    @Test
    fun `getBadges unlocks eco_expert with 5 products`() {
        val products = List(5) { buildProduct() }
        val badges = GamificationEngine.getBadges(products)

        assertTrue(badges[1].isUnlocked) // eco_expert
    }

    @Test
    fun `getBadges unlocks verified_scout with verified product`() {
        val products = listOf(buildProduct(status = EstimationStatus.VERIFIED))
        val badges = GamificationEngine.getBadges(products)

        assertTrue(badges[2].isUnlocked) // verified_scout
    }

    @Test
    fun `getBadges does not unlock verified_scout with AI estimated`() {
        val products = listOf(buildProduct(status = EstimationStatus.AI_ESTIMATED))
        val badges = GamificationEngine.getBadges(products)

        assertFalse(badges[2].isUnlocked) // verified_scout
    }

    @Test
    fun `getBadges unlocks carbon_hero with low carbon product`() {
        val products = listOf(buildProduct(carbonFootprint = 0.5))
        val badges = GamificationEngine.getBadges(products)

        assertTrue(badges[3].isUnlocked) // carbon_hero
    }

    @Test
    fun `getBadges does not unlock carbon_hero with high carbon product`() {
        val products = listOf(buildProduct(carbonFootprint = 2.0))
        val badges = GamificationEngine.getBadges(products)

        assertFalse(badges[3].isUnlocked) // carbon_hero
    }

    @Test
    fun `getBadges does not unlock carbon_hero with null carbon`() {
        val products = listOf(buildProduct(carbonFootprint = null))
        val badges = GamificationEngine.getBadges(products)

        assertFalse(badges[3].isUnlocked) // carbon_hero
    }

    @Test
    fun `getBadges with explicit params unlocks all`() {
        val badges = GamificationEngine.getBadges(
            scanCount = 10,
            hasVerifiedProduct = true,
            hasLowCarbonProduct = true
        )

        assertTrue(badges[0].isUnlocked) // first_seed
        assertTrue(badges[1].isUnlocked) // eco_expert
        assertTrue(badges[2].isUnlocked) // verified_scout
        assertTrue(badges[3].isUnlocked) // carbon_hero
    }
}
