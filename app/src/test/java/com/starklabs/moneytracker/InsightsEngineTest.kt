package com.starklabs.moneytracker

import com.starklabs.moneytracker.data.Transaction
import com.starklabs.moneytracker.domain.InsightsEngine
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for spending-insight generation.
 *
 * Timestamps are fixed at 12:00 UTC on known days so weekend detection is
 * deterministic on UTC CI runners:
 *   WEDNESDAY = 2024-01-03, SATURDAY = 2024-01-06.
 */
class InsightsEngineTest {

    private val WEEKDAY = 1_704_283_200_000L // Wed 2024-01-03 12:00 UTC
    private val SATURDAY = 1_704_542_400_000L // Sat 2024-01-06 12:00 UTC

    private fun tx(
        amount: Double,
        merchant: String = "M",
        date: Long = WEEKDAY,
        type: String = "DEBIT",
        categoryId: Int? = null
    ) = Transaction(amount = amount, merchant = merchant, date = date, type = type, accountId = 1, categoryId = categoryId)

    @Test
    fun `no debits yields the empty-state insight`() {
        assertEquals(listOf("Log some expenses to see insights."), InsightsEngine.generateInsights(emptyList(), emptyMap()))
    }

    @Test
    fun `credits only are treated as no spend`() {
        val credits = listOf(tx(1000.0, type = "CREDIT", categoryId = 1))
        assertEquals(listOf("Log some expenses to see insights."), InsightsEngine.generateInsights(credits, mapOf(1 to "Salary")))
    }

    @Test
    fun `highest category insight uses db-backed name and percentage`() {
        val txns = listOf(tx(800.0, categoryId = 1), tx(200.0, categoryId = 2))
        val out = InsightsEngine.generateInsights(txns, mapOf(1 to "Food", 2 to "Travel"))
        assertTrue(out.any { it == "Food is your highest expense (80%)" })
    }

    @Test
    fun `null category falls back to Uncategorized`() {
        val out = InsightsEngine.generateInsights(listOf(tx(500.0, merchant = "Shop", categoryId = null)), emptyMap())
        assertTrue(out.any { it == "Uncategorized is your highest expense (100%)" })
    }

    @Test
    fun `credits are excluded from totals`() {
        val txns = listOf(tx(800.0, categoryId = 1), tx(1000.0, type = "CREDIT", categoryId = 2))
        val out = InsightsEngine.generateInsights(txns, mapOf(1 to "Food", 2 to "Salary"))
        // 800 of 800 spent -> 100%, credit ignored.
        assertTrue(out.any { it == "Food is your highest expense (100%)" })
    }

    @Test
    fun `weekend overspending is detected`() {
        val txns = listOf(tx(600.0, date = SATURDAY, categoryId = 1), tx(400.0, date = WEEKDAY, categoryId = 1))
        val out = InsightsEngine.generateInsights(txns, mapOf(1 to "Food"))
        assertTrue(out.any { it == "You overspend on weekends." })
    }

    @Test
    fun `no weekend insight when all spend is on weekdays`() {
        val txns = listOf(tx(600.0, date = WEEKDAY, categoryId = 1), tx(400.0, date = WEEKDAY, categoryId = 1))
        val out = InsightsEngine.generateInsights(txns, mapOf(1 to "Food"))
        assertFalse(out.any { it == "You overspend on weekends." })
    }

    @Test
    fun `dominant single transaction triggers the massive-chunk insight`() {
        val txns = listOf(tx(600.0, merchant = "BigBuy", categoryId = 1), tx(200.0, categoryId = 2), tx(200.0, categoryId = 3))
        val out = InsightsEngine.generateInsights(txns, mapOf(1 to "Food", 2 to "Rent", 3 to "Bills"))
        // Merchant name is normalised (title-cased) by MerchantNormalizationEngine; use ignoreCase
        assertTrue(out.any { it.contains("massive 50%+") && it.contains("BigBuy", ignoreCase = true) })
    }

    @Test
    fun `balanced spending reports a stable pattern`() {
        val txns = listOf(tx(400.0, categoryId = 1), tx(300.0, categoryId = 2), tx(300.0, categoryId = 3))
        val out = InsightsEngine.generateInsights(txns, mapOf(1 to "Food", 2 to "Rent", 3 to "Bills"))
        assertTrue(out.any { it == "Your spending patterns look stable." })
    }
}
