package com.ecotracker.utils

import com.ecotracker.data.local.EstimationStatus
import com.ecotracker.data.local.ScannedProduct
import org.junit.Assert.*
import org.junit.Test

class GamificationEngineTest {

    // -- Rank calculation -------------------------------------------------------

    @Test
    fun `rank is Seedling when scanCount is 0`() {
        val rank = GamificationEngine.calculateRank(0)
        assertEquals(0, rank.level)
        assertEquals(0, rank.percentage)
    }

    @Test
    fun `rank is Sprout at scanCount 1`() {
        val rank = GamificationEngine.calculateRank(1)
        assertEquals(1, rank.level)
        assertEquals(0, rank.percentage)
    }

    @Test
    fun `rank is Sprout at scanCount 4 with 75 percent progress`() {
        val rank = GamificationEngine.calculateRank(4)
        assertEquals(1, rank.level)
        assertEquals(75, rank.percentage)
    }

    @Test
    fun `rank is Sapling at scanCount 5`() {
        val rank = GamificationEngine.calculateRank(5)
        assertEquals(2, rank.level)
        assertEquals(0, rank.percentage)
    }

    @Test
    fun `rank is Tree at scanCount 15`() {
        val rank = GamificationEngine.calculateRank(15)
        assertEquals(3, rank.level)
        assertEquals(0, rank.percentage)
    }

    @Test
    fun `rank is Forest Guardian at scanCount 30`() {
        val rank = GamificationEngine.calculateRank(30)
        assertEquals(4, rank.level)
        assertEquals(100, rank.percentage)
    }

    @Test
    fun `rank is Forest Guardian at scanCount above 30`() {
        val rank = GamificationEngine.calculateRank(999)
        assertEquals(4, rank.level)
        assertEquals(100, rank.percentage)
    }

    // -- Badge calculations -----------------------------------------------------

    private fun makeProduct(
        barcode: String = "0000",
        status: EstimationStatus = EstimationStatus.CATEGORY_AVERAGE,
        carbonFootprint: Double? = null
    ) = ScannedProduct(
        barcode = barcode,
        productName = "Test",
        brand = "Test",
        categories = "",
        imageUrl = "",
        ecoScore = "N/A",
        ecoScoreValue = 0,
        carbonFootprint = carbonFootprint,
        status = status
    )

    @Test
    fun `no badges unlocked with empty list`() {
        val badges = GamificationEngine.getBadges(emptyList())
        val unlocked = badges.filter { it.isUnlocked }
        assertTrue(unlocked.isEmpty())
    }

    @Test
    fun `first_seed badge unlocked with 1 scan`() {
        val badges = GamificationEngine.getBadges(listOf(makeProduct()))
        val firstSeed = badges.find { it.id == "first_seed" }
        assertNotNull(firstSeed)
        assertTrue(firstSeed!!.isUnlocked)
    }

    @Test
    fun `eco_expert badge unlocked with 5 scans`() {
        val products = (1..5).map { makeProduct(barcode = it.toString()) }
        val badges = GamificationEngine.getBadges(products)
        val ecoExpert = badges.find { it.id == "eco_expert" }
        assertNotNull(ecoExpert)
        assertTrue(ecoExpert!!.isUnlocked)
    }

    @Test
    fun `eco_expert badge locked with 4 scans`() {
        val products = (1..4).map { makeProduct(barcode = it.toString()) }
        val badges = GamificationEngine.getBadges(products)
        val ecoExpert = badges.find { it.id == "eco_expert" }
        assertNotNull(ecoExpert)
        assertFalse(ecoExpert!!.isUnlocked)
    }

    @Test
    fun `verified_scout badge unlocked when a product is VERIFIED`() {
        val products = listOf(makeProduct(status = EstimationStatus.VERIFIED))
        val badges = GamificationEngine.getBadges(products)
        val verifiedScout = badges.find { it.id == "verified_scout" }
        assertTrue(verifiedScout!!.isUnlocked)
    }

    @Test
    fun `carbon_hero badge unlocked when a product has carbon below 1`() {
        val products = listOf(makeProduct(carbonFootprint = 0.5))
        val badges = GamificationEngine.getBadges(products)
        val carbonHero = badges.find { it.id == "carbon_hero" }
        assertTrue(carbonHero!!.isUnlocked)
    }

    @Test
    fun `carbon_hero badge locked when carbon is exactly 1`() {
        val products = listOf(makeProduct(carbonFootprint = 1.0))
        val badges = GamificationEngine.getBadges(products)
        val carbonHero = badges.find { it.id == "carbon_hero" }
        assertFalse(carbonHero!!.isUnlocked)
    }
}
