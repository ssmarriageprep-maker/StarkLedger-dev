package com.starklabs.moneytracker

import com.starklabs.moneytracker.domain.MerchantAnalyticsEngine
import com.starklabs.moneytracker.domain.MerchantSummary
import com.starklabs.moneytracker.domain.TrendDirection
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [MerchantAnalyticsEngine.search].
 * Verifies case-insensitive matching, partial matching, blank/empty query passthrough,
 * and that non-matching queries return an empty list.
 */
class MerchantSearchTest {

    private fun summary(name: String) = MerchantSummary(
        canonicalName = name,
        totalSpent = 100.0,
        transactionCount = 1,
        averageTransaction = 100.0,
        largestTransaction = 100.0,
        firstSeen = 0L,
        lastSeen = 0L,
        monthsActive = 1,
        frequencyPerMonth = 1.0,
        trendDirection = TrendDirection.INSUFFICIENT_DATA,
        topCategories = emptyList(),
        recentTransactions = emptyList()
    )

    private val amazon  = summary("Amazon")
    private val swiggy  = summary("Swiggy")
    private val zomato  = summary("Zomato")
    private val tata    = summary("Tata Neu")

    private val all = listOf(amazon, swiggy, zomato, tata)

    // ── passthrough for blank/empty ──────────────────────────────────────────────

    @Test fun `empty query returns all summaries`() {
        val result = MerchantAnalyticsEngine.search(all, "")
        assertEquals(all, result)
    }

    @Test fun `whitespace-only query returns all summaries`() {
        val result = MerchantAnalyticsEngine.search(all, "   ")
        assertEquals(all, result)
    }

    // ── case-insensitive matching ────────────────────────────────────────────────

    @Test fun `exact lowercase query matches`() {
        val result = MerchantAnalyticsEngine.search(all, "amazon")
        assertEquals(1, result.size)
        assertEquals("Amazon", result.single().canonicalName)
    }

    @Test fun `exact uppercase query matches`() {
        val result = MerchantAnalyticsEngine.search(all, "SWIGGY")
        assertEquals(1, result.size)
        assertEquals("Swiggy", result.single().canonicalName)
    }

    @Test fun `mixed-case query matches`() {
        val result = MerchantAnalyticsEngine.search(all, "ZoMaTo")
        assertEquals(1, result.size)
        assertEquals("Zomato", result.single().canonicalName)
    }

    // ── partial matching ─────────────────────────────────────────────────────────

    @Test fun `partial prefix match finds merchant`() {
        val result = MerchantAnalyticsEngine.search(all, "Sw")
        assertEquals(1, result.size)
        assertEquals("Swiggy", result.single().canonicalName)
    }

    @Test fun `partial suffix match finds merchant`() {
        val result = MerchantAnalyticsEngine.search(all, "ato")
        assertEquals(1, result.size)
        assertEquals("Zomato", result.single().canonicalName)
    }

    @Test fun `partial middle match finds merchant`() {
        val result = MerchantAnalyticsEngine.search(all, "ata")  // matches "Tata Neu"
        assertEquals(1, result.size)
        assertEquals("Tata Neu", result.single().canonicalName)
    }

    @Test fun `query matching multiple merchants returns all matches`() {
        val extras = all + summary("Amazon Fresh") + summary("Amazon Pay")
        val result = MerchantAnalyticsEngine.search(extras, "Amazon")
        assertEquals(3, result.size)
        assertTrue(result.all { it.canonicalName.contains("Amazon", ignoreCase = true) })
    }

    // ── no-match ─────────────────────────────────────────────────────────────────

    @Test fun `non-matching query returns empty list`() {
        val result = MerchantAnalyticsEngine.search(all, "Netflix")
        assertTrue(result.isEmpty())
    }

    @Test fun `single character non-match returns empty list`() {
        val result = MerchantAnalyticsEngine.search(all, "X")
        assertTrue(result.isEmpty())
    }

    // ── edge: empty input list ────────────────────────────────────────────────────

    @Test fun `search on empty list always returns empty list`() {
        assertTrue(MerchantAnalyticsEngine.search(emptyList(), "Amazon").isEmpty())
        assertTrue(MerchantAnalyticsEngine.search(emptyList(), "").isEmpty())
    }

    // ── leading/trailing whitespace trimmed from query ────────────────────────────

    @Test fun `query with leading and trailing whitespace still matches`() {
        val result = MerchantAnalyticsEngine.search(all, "  Amazon  ")
        assertEquals(1, result.size)
        assertEquals("Amazon", result.single().canonicalName)
    }
}
