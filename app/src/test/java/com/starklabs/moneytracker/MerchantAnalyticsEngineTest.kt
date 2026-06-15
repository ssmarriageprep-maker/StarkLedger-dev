package com.starklabs.moneytracker

import com.starklabs.moneytracker.data.Category
import com.starklabs.moneytracker.data.Transaction
import com.starklabs.moneytracker.domain.MerchantAnalyticsEngine
import com.starklabs.moneytracker.domain.MerchantNormalizationEngine
import com.starklabs.moneytracker.domain.TrendDirection
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [MerchantAnalyticsEngine].
 *
 * Verifies alias-aware grouping, aggregation correctness, trend direction computation,
 * category breakdown, and Sprint 3B invariants (frequencyPerMonth, monthsActive).
 * All tests are pure — no Room, no Android runtime.
 */
class MerchantAnalyticsEngineTest {

    @Before fun clearCache() = MerchantNormalizationEngine.clearCache()

    // Reproducible timestamps (UTC epoch start of each month)
    private val JAN_2024 = 1_704_067_200_000L
    private val FEB_2024 = 1_706_745_600_000L
    private val MAR_2024 = 1_709_251_200_000L

    private val food     = Category(id = 1, name = "Food",     iconName = "restaurant", colorHex = "#FF5733", budgetLimit = 5000.0)
    private val shopping = Category(id = 2, name = "Shopping", iconName = "shopping_bag", colorHex = "#33C5FF", budgetLimit = 3000.0)

    private fun debit(merchant: String, amount: Double, date: Long = JAN_2024, catId: Int? = null) =
        Transaction(amount = amount, merchant = merchant, type = "DEBIT", date = date, accountId = 1, categoryId = catId)

    private fun credit(merchant: String, amount: Double, date: Long = JAN_2024) =
        Transaction(amount = amount, merchant = merchant, type = "CREDIT", date = date, accountId = 1)

    private val engineResolve: (String) -> String = { MerchantNormalizationEngine.normalize(it).canonicalName }

    // ── alias-aware grouping ─────────────────────────────────────────────────────

    @Test fun `alias-aware grouping merges three Amazon variants into one summary`() {
        val transactions = listOf(
            debit("AMAZON PAY INDIA",  500.0, JAN_2024),
            debit("Amazon Marketplace", 300.0, FEB_2024),
            debit("amazon",             200.0, MAR_2024)
        )
        val summaries = MerchantAnalyticsEngine.computeAll(transactions, emptyList(), engineResolve)
        assertEquals(1, summaries.size)
        val amazon = summaries.single()
        assertEquals("Amazon", amazon.canonicalName)
        assertEquals(1000.0, amazon.totalSpent, 0.01)
        assertEquals(3, amazon.transactionCount)
    }

    @Test fun `alias map override groups variants under user-defined canonical`() {
        val aliasMap = mapOf(
            "amazon pay india"   to "Amazon",
            "amazon marketplace" to "Amazon",
            "amazon"             to "Amazon"
        )
        val resolve = { raw: String -> aliasMap[raw.trim().lowercase()] ?: engineResolve(raw) }
        val transactions = listOf(
            debit("AMAZON PAY INDIA",   500.0),
            debit("Amazon Marketplace", 300.0),
            debit("amazon",             200.0)
        )
        val summaries = MerchantAnalyticsEngine.computeAll(transactions, emptyList(), resolve)
        assertEquals(1, summaries.size)
        assertEquals("Amazon", summaries.single().canonicalName)
        assertEquals(1000.0, summaries.single().totalSpent, 0.01)
    }

    @Test fun `distinct merchants produce distinct summaries`() {
        val transactions = listOf(
            debit("Swiggy Food",  300.0),
            debit("Zomato Order", 200.0)
        )
        val summaries = MerchantAnalyticsEngine.computeAll(transactions, emptyList(), engineResolve)
        assertEquals(2, summaries.size)
        val names = summaries.map { it.canonicalName }.toSet()
        assertTrue("Swiggy" in names)
        assertTrue("Zomato" in names)
    }

    // ── credit filtering ─────────────────────────────────────────────────────────

    @Test fun `credit transactions are excluded from merchant summaries`() {
        val transactions = listOf(
            debit("Amazon",  500.0),
            credit("Amazon", 1000.0)
        )
        val summaries = MerchantAnalyticsEngine.computeAll(transactions, emptyList(), engineResolve)
        assertEquals(1, summaries.size)
        assertEquals(500.0, summaries.single().totalSpent, 0.01)
        assertEquals(1, summaries.single().transactionCount)
    }

    @Test fun `credits-only returns empty list`() {
        val transactions = listOf(credit("Amazon", 500.0), credit("Swiggy", 200.0))
        assertTrue(MerchantAnalyticsEngine.computeAll(transactions, emptyList(), engineResolve).isEmpty())
    }

    // ── aggregation correctness ──────────────────────────────────────────────────

    @Test fun `totalSpent is sum of all debit amounts`() {
        val transactions = listOf(debit("Swiggy", 150.0), debit("Swiggy", 250.0), debit("Swiggy", 100.0))
        val summary = MerchantAnalyticsEngine.computeAll(transactions, emptyList(), engineResolve).single()
        assertEquals(500.0, summary.totalSpent, 0.01)
    }

    @Test fun `averageTransaction equals totalSpent divided by count`() {
        val transactions = listOf(debit("Swiggy", 200.0), debit("Swiggy", 400.0))
        val summary = MerchantAnalyticsEngine.computeAll(transactions, emptyList(), engineResolve).single()
        assertEquals(300.0, summary.averageTransaction, 0.01)
    }

    @Test fun `largestTransaction is max single amount`() {
        val transactions = listOf(debit("Swiggy", 100.0), debit("Swiggy", 999.0), debit("Swiggy", 200.0))
        val summary = MerchantAnalyticsEngine.computeAll(transactions, emptyList(), engineResolve).single()
        assertEquals(999.0, summary.largestTransaction, 0.01)
    }

    @Test fun `firstSeen and lastSeen reflect min and max dates`() {
        val transactions = listOf(
            debit("Swiggy", 100.0, JAN_2024),
            debit("Swiggy", 200.0, MAR_2024),
            debit("Swiggy", 150.0, FEB_2024)
        )
        val summary = MerchantAnalyticsEngine.computeAll(transactions, emptyList(), engineResolve).single()
        assertEquals(JAN_2024, summary.firstSeen)
        assertEquals(MAR_2024, summary.lastSeen)
    }

    // ── monthsActive + frequencyPerMonth ────────────────────────────────────────

    @Test fun `monthsActive counts distinct calendar months`() {
        val transactions = listOf(
            debit("Swiggy", 100.0, JAN_2024),
            debit("Swiggy", 200.0, JAN_2024 + 86_400_000L),  // still January
            debit("Swiggy", 150.0, FEB_2024),
            debit("Swiggy", 120.0, MAR_2024)
        )
        val summary = MerchantAnalyticsEngine.computeAll(transactions, emptyList(), engineResolve).single()
        assertEquals(3, summary.monthsActive)
    }

    @Test fun `frequencyPerMonth equals transactionCount divided by monthsActive`() {
        val transactions = listOf(
            debit("Swiggy", 100.0, JAN_2024),
            debit("Swiggy", 200.0, FEB_2024),
            debit("Swiggy", 150.0, FEB_2024)
        )
        val summary = MerchantAnalyticsEngine.computeAll(transactions, emptyList(), engineResolve).single()
        assertEquals(1.5, summary.frequencyPerMonth, 0.01)
    }

    @Test fun `single transaction gives monthsActive 1 and frequency 1_0`() {
        val summary = MerchantAnalyticsEngine.computeAll(
            listOf(debit("Swiggy", 100.0, JAN_2024)), emptyList(), engineResolve
        ).single()
        assertEquals(1, summary.monthsActive)
        assertEquals(1.0, summary.frequencyPerMonth, 0.01)
    }

    // ── trend direction ──────────────────────────────────────────────────────────

    @Test fun `INSUFFICIENT_DATA when only one month of activity`() {
        val transactions = listOf(
            debit("Swiggy", 100.0, JAN_2024),
            debit("Swiggy", 200.0, JAN_2024 + 86_400_000L)
        )
        val summary = MerchantAnalyticsEngine.computeAll(transactions, emptyList(), engineResolve).single()
        assertEquals(TrendDirection.INSUFFICIENT_DATA, summary.trendDirection)
    }

    @Test fun `INCREASING when last month spend exceeds 1_2x monthly average`() {
        // Jan 100, Feb 100, Mar 500 → avg 233, last 500 > 280 → INCREASING
        val transactions = listOf(
            debit("Swiggy", 100.0, JAN_2024),
            debit("Swiggy", 100.0, FEB_2024),
            debit("Swiggy", 500.0, MAR_2024)
        )
        val summary = MerchantAnalyticsEngine.computeAll(transactions, emptyList(), engineResolve).single()
        assertEquals(TrendDirection.INCREASING, summary.trendDirection)
    }

    @Test fun `DECREASING when last month spend is below 0_8x monthly average`() {
        // Jan 500, Feb 500, Mar 50 → avg 350, last 50 < 280 → DECREASING
        val transactions = listOf(
            debit("Swiggy", 500.0, JAN_2024),
            debit("Swiggy", 500.0, FEB_2024),
            debit("Swiggy", 50.0,  MAR_2024)
        )
        val summary = MerchantAnalyticsEngine.computeAll(transactions, emptyList(), engineResolve).single()
        assertEquals(TrendDirection.DECREASING, summary.trendDirection)
    }

    @Test fun `STABLE when last month spend is within 0_8x-1_2x of average`() {
        val transactions = listOf(
            debit("Swiggy", 300.0, JAN_2024),
            debit("Swiggy", 300.0, FEB_2024),
            debit("Swiggy", 300.0, MAR_2024)
        )
        val summary = MerchantAnalyticsEngine.computeAll(transactions, emptyList(), engineResolve).single()
        assertEquals(TrendDirection.STABLE, summary.trendDirection)
    }

    // ── top categories ───────────────────────────────────────────────────────────

    @Test fun `topCategories sorted by amount descending`() {
        val transactions = listOf(
            debit("Amazon", 100.0, catId = food.id),
            debit("Amazon", 300.0, catId = shopping.id),
            debit("Amazon",  50.0, catId = food.id)
        )
        val summary = MerchantAnalyticsEngine.computeAll(transactions, listOf(food, shopping), engineResolve).single()
        assertEquals("Shopping", summary.topCategories[0].name)  // 300 > 150
        assertEquals("Food",     summary.topCategories[1].name)
    }

    @Test fun `topCategories respects the limit parameter`() {
        val cats = (1..5).map { Category(id = it, name = "Cat$it", iconName = "receipt", colorHex = "#FFF", budgetLimit = 1000.0) }
        val txns = cats.map { cat -> debit("Amazon", cat.id * 100.0, catId = cat.id) }
        val summary = MerchantAnalyticsEngine.computeAll(txns, cats, engineResolve, topCategoriesLimit = 3).single()
        assertEquals(3, summary.topCategories.size)
    }

    // ── computeFor ───────────────────────────────────────────────────────────────

    @Test fun `computeFor returns null for unknown canonical name`() {
        val result = MerchantAnalyticsEngine.computeFor("NonExistent", listOf(debit("Swiggy", 100.0)), emptyList(), engineResolve)
        assertNull(result)
    }

    @Test fun `computeFor returns correct summary for known merchant`() {
        val transactions = listOf(debit("Swiggy", 150.0), debit("Zomato Order", 200.0))
        val result = MerchantAnalyticsEngine.computeFor("Swiggy", transactions, emptyList(), engineResolve)
        assertNotNull(result)
        assertEquals("Swiggy", result!!.canonicalName)
        assertEquals(150.0, result.totalSpent, 0.01)
        assertEquals(1, result.transactionCount)
    }

    // ── recentTransactions order ─────────────────────────────────────────────────

    @Test fun `recentTransactions are ordered newest-first`() {
        val transactions = listOf(
            debit("Swiggy", 100.0, JAN_2024),
            debit("Swiggy", 200.0, MAR_2024),
            debit("Swiggy", 150.0, FEB_2024)
        )
        val summary = MerchantAnalyticsEngine.computeAll(transactions, emptyList(), engineResolve).single()
        assertEquals(MAR_2024, summary.recentTransactions.first().date)
    }

    // ── edge cases ───────────────────────────────────────────────────────────────

    @Test fun `empty transaction list returns empty list`() {
        assertTrue(MerchantAnalyticsEngine.computeAll(emptyList(), emptyList(), engineResolve).isEmpty())
    }
}
