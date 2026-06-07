package com.starklabs.moneytracker

import com.starklabs.moneytracker.data.Category
import com.starklabs.moneytracker.data.Transaction
import com.starklabs.moneytracker.domain.AccountAnalyticsEngine
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the account-level "intelligence" summary shown on the account
 * detail screen: income/expense/savings, transaction counts, top categories,
 * and merchant grouping (via [com.starklabs.moneytracker.domain.CategoryNormalizer]).
 */
class AccountAnalyticsEngineTest {

    private val DAY = 24 * 60 * 60 * 1000L
    private val BASE = 1_704_283_200_000L // Wed 2024-01-03 12:00 UTC

    private fun tx(
        amount: Double,
        merchant: String = "Merchant",
        date: Long = BASE,
        type: String = "DEBIT",
        categoryId: Int? = 1
    ) = Transaction(amount = amount, merchant = merchant, date = date, type = type, accountId = 1, categoryId = categoryId)

    private val foodCategory = Category(id = 1, name = "Food", iconName = "restaurant", colorHex = "#FF0000")
    private val travelCategory = Category(id = 2, name = "Travel", iconName = "car", colorHex = "#00FF00")
    private val categories = listOf(foodCategory, travelCategory)

    @Test
    fun `computes income, expense, savings and transaction count`() {
        val txns = listOf(
            tx(5000.0, type = "CREDIT"),
            tx(1000.0, type = "DEBIT"),
            tx(500.0, type = "DEBIT")
        )
        val summary = AccountAnalyticsEngine.compute(txns, categories)

        assertEquals(5000.0, summary.income, 0.0)
        assertEquals(1500.0, summary.expense, 0.0)
        assertEquals(3500.0, summary.savings, 0.0)
        assertEquals(3, summary.transactionCount)
    }

    @Test
    fun `savings can be negative when expenses exceed income`() {
        val txns = listOf(tx(100.0, type = "CREDIT"), tx(900.0, type = "DEBIT"))
        val summary = AccountAnalyticsEngine.compute(txns, categories)
        assertEquals(-800.0, summary.savings, 0.0)
    }

    @Test
    fun `top categories rank debit spend descending and ignore credits`() {
        val txns = listOf(
            tx(300.0, categoryId = 1, type = "DEBIT"),
            tx(700.0, categoryId = 2, type = "DEBIT"),
            tx(10000.0, categoryId = 1, type = "CREDIT") // income — must not count toward category spend
        )
        val summary = AccountAnalyticsEngine.compute(txns, categories)

        assertEquals(listOf("Travel", "Food"), summary.topCategories.map { it.name })
        assertEquals(700.0, summary.topCategories[0].amount, 0.0)
        assertEquals(300.0, summary.topCategories[1].amount, 0.0)
    }

    @Test
    fun `unknown category falls back to Uncategorized`() {
        val txns = listOf(tx(200.0, categoryId = null))
        val summary = AccountAnalyticsEngine.compute(txns, categories)
        assertEquals("Uncategorized", summary.topCategories.single().name)
    }

    @Test
    fun `top merchants are grouped on normalized merchant names`() {
        val txns = listOf(
            tx(100.0, merchant = "AMAZON PAY INDIA", type = "DEBIT"),
            tx(200.0, merchant = "Amazon", type = "DEBIT"),
            tx(50.0, merchant = "Local Cafe", type = "DEBIT")
        )
        val summary = AccountAnalyticsEngine.compute(txns, categories)

        val amazonEntry = summary.topMerchants.find { it.merchant.equals("Amazon", ignoreCase = true) }
        assertNotNull("Normalized Amazon entries should roll up into one merchant", amazonEntry)
        assertEquals(2, amazonEntry!!.count)
        assertEquals(300.0, amazonEntry.amount, 0.0)
    }

    @Test
    fun `top merchants exclude income transactions`() {
        val txns = listOf(
            tx(1000.0, merchant = "Employer", type = "CREDIT"),
            tx(100.0, merchant = "Cafe", type = "DEBIT")
        )
        val summary = AccountAnalyticsEngine.compute(txns, categories)
        assertTrue(summary.topMerchants.none { it.merchant.equals("Employer", ignoreCase = true) })
    }

    @Test
    fun `recent transactions are sorted newest first and capped at the limit`() {
        val txns = (1..15).map { i -> tx(amount = i.toDouble(), date = BASE + i * DAY, merchant = "M$i") }
        // Shuffle to ensure the engine sorts defensively rather than relying on input order.
        val shuffled = txns.shuffled(java.util.Random(42))

        val summary = AccountAnalyticsEngine.compute(shuffled, categories, recentLimit = 10)

        assertEquals(10, summary.recentTransactions.size)
        assertEquals("M15", summary.recentTransactions.first().merchant)
        assertTrue(summary.recentTransactions.zipWithNext().all { (a, b) -> a.date >= b.date })
    }

    @Test
    fun `respects custom limits for top categories and merchants`() {
        val txns = listOf(
            tx(100.0, merchant = "A", categoryId = 1),
            tx(200.0, merchant = "B", categoryId = 2),
            tx(300.0, merchant = "C", categoryId = 1)
        )
        val summary = AccountAnalyticsEngine.compute(txns, categories, topCategoriesLimit = 1, topMerchantsLimit = 1)

        assertEquals(1, summary.topCategories.size)
        assertEquals(1, summary.topMerchants.size)
    }

    @Test
    fun `empty transaction list yields a zeroed summary without throwing`() {
        val summary = AccountAnalyticsEngine.compute(emptyList(), categories)

        assertEquals(0.0, summary.income, 0.0)
        assertEquals(0.0, summary.expense, 0.0)
        assertEquals(0.0, summary.savings, 0.0)
        assertEquals(0, summary.transactionCount)
        assertTrue(summary.topCategories.isEmpty())
        assertTrue(summary.topMerchants.isEmpty())
        assertTrue(summary.recentTransactions.isEmpty())
    }
}
