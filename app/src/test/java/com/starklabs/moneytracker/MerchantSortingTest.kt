package com.starklabs.moneytracker

import com.starklabs.moneytracker.data.Transaction
import com.starklabs.moneytracker.domain.MerchantAnalyticsEngine
import com.starklabs.moneytracker.domain.MerchantSortOrder
import com.starklabs.moneytracker.domain.MerchantSummary
import com.starklabs.moneytracker.domain.TrendDirection
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [MerchantAnalyticsEngine.sort] — all four [MerchantSortOrder] modes.
 * Uses hand-built [MerchantSummary] instances to test sort independently of compute logic.
 */
class MerchantSortingTest {

    private fun summary(
        name: String,
        totalSpent: Double = 0.0,
        txCount: Int = 1,
        lastSeen: Long = 0L
    ) = MerchantSummary(
        canonicalName = name,
        totalSpent = totalSpent,
        transactionCount = txCount,
        averageTransaction = if (txCount > 0) totalSpent / txCount else 0.0,
        largestTransaction = totalSpent,
        firstSeen = 0L,
        lastSeen = lastSeen,
        monthsActive = 1,
        frequencyPerMonth = txCount.toDouble(),
        trendDirection = TrendDirection.INSUFFICIENT_DATA,
        topCategories = emptyList(),
        recentTransactions = emptyList()
    )

    private val amazon  = summary("Amazon",  totalSpent = 5000.0, txCount = 10, lastSeen = 3000L)
    private val swiggy  = summary("Swiggy",  totalSpent = 2000.0, txCount = 25, lastSeen = 1000L)
    private val zomato  = summary("Zomato",  totalSpent = 3000.0, txCount = 5,  lastSeen = 5000L)
    private val blinkit = summary("Blinkit", totalSpent = 1000.0, txCount = 15, lastSeen = 2000L)

    private val all = listOf(amazon, swiggy, zomato, blinkit)

    @Test fun `HIGHEST_SPEND sorts by totalSpent descending`() {
        val sorted = MerchantAnalyticsEngine.sort(all, MerchantSortOrder.HIGHEST_SPEND)
        assertEquals(listOf("Amazon", "Zomato", "Swiggy", "Blinkit"), sorted.map { it.canonicalName })
    }

    @Test fun `MOST_TRANSACTIONS sorts by transactionCount descending`() {
        val sorted = MerchantAnalyticsEngine.sort(all, MerchantSortOrder.MOST_TRANSACTIONS)
        assertEquals(listOf("Swiggy", "Blinkit", "Amazon", "Zomato"), sorted.map { it.canonicalName })
    }

    @Test fun `RECENTLY_ACTIVE sorts by lastSeen descending`() {
        val sorted = MerchantAnalyticsEngine.sort(all, MerchantSortOrder.RECENTLY_ACTIVE)
        assertEquals(listOf("Zomato", "Amazon", "Blinkit", "Swiggy"), sorted.map { it.canonicalName })
    }

    @Test fun `ALPHABETICAL sorts by canonical name case-insensitively ascending`() {
        val mixed = listOf(
            summary("zomato"),
            summary("Amazon"),
            summary("BLINKIT"),
            summary("swiggy")
        )
        val sorted = MerchantAnalyticsEngine.sort(mixed, MerchantSortOrder.ALPHABETICAL)
        assertEquals(listOf("Amazon", "BLINKIT", "swiggy", "zomato"), sorted.map { it.canonicalName })
    }

    @Test fun `ALPHABETICAL is case-insensitive — lowercase before uppercase with same letter loses to original`() {
        val items = listOf(summary("Banana"), summary("apple"), summary("Cherry"))
        val sorted = MerchantAnalyticsEngine.sort(items, MerchantSortOrder.ALPHABETICAL)
        // a < b < c regardless of case
        assertEquals("apple", sorted[0].canonicalName)
        assertEquals("Banana", sorted[1].canonicalName)
        assertEquals("Cherry", sorted[2].canonicalName)
    }

    @Test fun `sort of empty list returns empty list`() {
        for (order in MerchantSortOrder.entries) {
            assertTrue(MerchantAnalyticsEngine.sort(emptyList(), order).isEmpty())
        }
    }

    @Test fun `sort of single-element list returns same element`() {
        val single = listOf(summary("Solo", totalSpent = 100.0))
        for (order in MerchantSortOrder.entries) {
            val result = MerchantAnalyticsEngine.sort(single, order)
            assertEquals(1, result.size)
            assertEquals("Solo", result.single().canonicalName)
        }
    }

    @Test fun `sort does not mutate the original list`() {
        val original = all.toList()
        MerchantAnalyticsEngine.sort(all, MerchantSortOrder.ALPHABETICAL)
        assertEquals(original, all)  // original order unchanged
    }
}
