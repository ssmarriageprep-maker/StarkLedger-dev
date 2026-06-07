package com.starklabs.moneytracker

import com.starklabs.moneytracker.data.Account
import com.starklabs.moneytracker.data.Category
import com.starklabs.moneytracker.data.Transaction
import com.starklabs.moneytracker.domain.DateRange
import com.starklabs.moneytracker.domain.DateRangePresets
import com.starklabs.moneytracker.domain.FilterDimension
import com.starklabs.moneytracker.domain.TransactionFilter
import com.starklabs.moneytracker.domain.TransactionFilterEngine
import com.starklabs.moneytracker.domain.TransactionTypeFilter
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the Phase 2 advanced filter system: pure matching/filtering logic,
 * date-range presets, active-chip generation, and dimension clearing. Mirrors the
 * fixed-timestamp style used in [InsightsEngineTest] for determinism.
 */
class TransactionFilterTest {

    // Wed 2024-01-10 12:00 UTC — used as a stable "now" for date-range tests.
    private val NOW = 1_704_888_000_000L
    private val DAY = 24 * 60 * 60 * 1000L

    private fun tx(
        amount: Double,
        merchant: String = "Merchant",
        date: Long = NOW,
        type: String = "DEBIT",
        accountId: Int = 1,
        categoryId: Int? = 1
    ) = Transaction(amount = amount, merchant = merchant, date = date, type = type, accountId = accountId, categoryId = categoryId)

    private val foodCategory = Category(id = 1, name = "Food", iconName = "restaurant")
    private val travelCategory = Category(id = 2, name = "Travel", iconName = "car")
    private val categories = listOf(foodCategory, travelCategory)

    private val hdfcAccount = Account(id = 1, name = "HDFC", type = "BANK", balance = 0.0, last4Digits = "3263")
    private val sbiAccount = Account(id = 2, name = "SBI", type = "BANK", balance = 0.0, last4Digits = "1234")
    private val accounts = listOf(hdfcAccount, sbiAccount)

    // ---- Date range logic ----

    @Test
    fun `date range filter keeps only transactions inside the closed interval`() {
        val range = DateRange(startMillis = NOW - DAY, endMillis = NOW + DAY, label = "Window")
        val txns = listOf(
            tx(100.0, date = NOW - 2 * DAY), // before
            tx(200.0, date = NOW),           // inside
            tx(300.0, date = NOW + DAY),     // boundary, inclusive
            tx(400.0, date = NOW + 2 * DAY)  // after
        )
        val result = TransactionFilterEngine.apply(txns, TransactionFilter(dateRange = range))
        assertEquals(listOf(200.0, 300.0), result.map { it.amount })
    }

    @Test
    fun `last 7 days preset spans exactly now minus seven days`() {
        val range = DateRangePresets.last7Days(now = NOW)
        assertEquals(NOW - 7 * DAY, range.startMillis)
        assertEquals(NOW, range.endMillis)
        assertEquals("Last 7 Days", range.label)
    }

    @Test
    fun `last 30 days preset spans exactly now minus thirty days`() {
        val range = DateRangePresets.last30Days(now = NOW)
        assertEquals(NOW - 30 * DAY, range.startMillis)
        assertEquals(NOW, range.endMillis)
    }

    @Test
    fun `this month preset starts at midnight on the first of the month`() {
        val range = DateRangePresets.thisMonth(now = NOW)
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = range.startMillis }
        assertEquals(1, cal.get(java.util.Calendar.DAY_OF_MONTH))
        assertEquals(0, cal.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(java.util.Calendar.MINUTE))
        assertEquals(NOW, range.endMillis)
    }

    @Test
    fun `this year preset starts on January 1st`() {
        val range = DateRangePresets.thisYear(now = NOW)
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = range.startMillis }
        assertEquals(java.util.Calendar.JANUARY, cal.get(java.util.Calendar.MONTH))
        assertEquals(1, cal.get(java.util.Calendar.DAY_OF_MONTH))
    }

    // ---- Amount range logic ----

    @Test
    fun `amount range filter applies inclusive min and max bounds`() {
        val txns = listOf(tx(100.0), tx(500.0), tx(1000.0))
        val result = TransactionFilterEngine.apply(txns, TransactionFilter(minAmount = 100.0, maxAmount = 1000.0))
        assertEquals(3, result.size)

        val narrowed = TransactionFilterEngine.apply(txns, TransactionFilter(minAmount = 200.0, maxAmount = 900.0))
        assertEquals(listOf(500.0), narrowed.map { it.amount })
    }

    @Test
    fun `min amount only excludes everything below the floor`() {
        val txns = listOf(tx(50.0), tx(150.0), tx(300.0))
        val result = TransactionFilterEngine.apply(txns, TransactionFilter(minAmount = 100.0))
        assertEquals(listOf(150.0, 300.0), result.map { it.amount })
    }

    @Test
    fun `max amount only excludes everything above the ceiling`() {
        val txns = listOf(tx(50.0), tx(150.0), tx(300.0))
        val result = TransactionFilterEngine.apply(txns, TransactionFilter(maxAmount = 200.0))
        assertEquals(listOf(50.0, 150.0), result.map { it.amount })
    }

    // ---- Category / Account / Merchant filters ----

    @Test
    fun `category filter keeps only matching category id`() {
        val txns = listOf(
            tx(100.0, categoryId = 1),
            tx(200.0, categoryId = 2),
            tx(300.0, categoryId = null)
        )
        val result = TransactionFilterEngine.apply(txns, TransactionFilter(categoryId = 1))
        assertEquals(listOf(100.0), result.map { it.amount })
    }

    @Test
    fun `account filter keeps only matching account id`() {
        val txns = listOf(
            tx(100.0, accountId = 1),
            tx(200.0, accountId = 2)
        )
        val result = TransactionFilterEngine.apply(txns, TransactionFilter(accountId = 2))
        assertEquals(listOf(200.0), result.map { it.amount })
    }

    @Test
    fun `merchant filter matches case-insensitive substrings`() {
        val txns = listOf(
            tx(100.0, merchant = "Amazon Pay India"),
            tx(200.0, merchant = "SWIGGY"),
            tx(300.0, merchant = "Local Store")
        )
        val result = TransactionFilterEngine.apply(txns, TransactionFilter(merchantQuery = "amazon"))
        assertEquals(listOf("Amazon Pay India"), result.map { it.merchant })

        val swiggyResult = TransactionFilterEngine.apply(txns, TransactionFilter(merchantQuery = "swiggy"))
        assertEquals(listOf("SWIGGY"), swiggyResult.map { it.merchant })
    }

    @Test
    fun `transaction type filter distinguishes income from expense`() {
        val txns = listOf(
            tx(100.0, type = "DEBIT"),
            tx(200.0, type = "CREDIT")
        )
        assertEquals(listOf(100.0), TransactionFilterEngine.apply(txns, TransactionFilter(transactionType = TransactionTypeFilter.EXPENSE)).map { it.amount })
        assertEquals(listOf(200.0), TransactionFilterEngine.apply(txns, TransactionFilter(transactionType = TransactionTypeFilter.INCOME)).map { it.amount })
        assertEquals(2, TransactionFilterEngine.apply(txns, TransactionFilter(transactionType = TransactionTypeFilter.ALL)).size)
    }

    @Test
    fun `combined filters apply every active dimension together`() {
        val txns = listOf(
            tx(500.0, merchant = "Amazon", type = "DEBIT", accountId = 1, categoryId = 1),
            tx(500.0, merchant = "Amazon", type = "CREDIT", accountId = 1, categoryId = 1), // wrong type
            tx(500.0, merchant = "Amazon", type = "DEBIT", accountId = 2, categoryId = 1),  // wrong account
            tx(50.0, merchant = "Amazon", type = "DEBIT", accountId = 1, categoryId = 1)    // below min amount
        )
        val filter = TransactionFilter(
            transactionType = TransactionTypeFilter.EXPENSE,
            accountId = 1,
            categoryId = 1,
            merchantQuery = "amazon",
            minAmount = 100.0
        )
        val result = TransactionFilterEngine.apply(txns, filter)
        assertEquals(1, result.size)
        assertEquals(500.0, result.first().amount, 0.0)
    }

    // ---- isActive / activeCount ----

    @Test
    fun `default filter is inactive and apply returns the input untouched`() {
        val txns = listOf(tx(100.0), tx(200.0))
        assertFalse(TransactionFilter().isActive)
        assertEquals(0, TransactionFilter().activeCount)
        assertSame(txns, TransactionFilterEngine.apply(txns, TransactionFilter()))
    }

    @Test
    fun `active count reflects number of distinct constrained dimensions`() {
        val filter = TransactionFilter(categoryId = 1, accountId = 2, minAmount = 100.0, maxAmount = 500.0)
        assertTrue(filter.isActive)
        // category + account + amount-range(combined) = 3
        assertEquals(3, filter.activeCount)
    }

    // ---- Active filter chips ----

    @Test
    fun `active chips render human-readable labels for each dimension`() {
        val filter = TransactionFilter(
            dateRange = DateRangePresets.last30Days(now = NOW),
            transactionType = TransactionTypeFilter.EXPENSE,
            categoryId = foodCategory.id,
            accountId = hdfcAccount.id,
            merchantQuery = "swiggy",
            minAmount = 100.0,
            maxAmount = 1000.0
        )
        val chips = TransactionFilterEngine.activeChips(filter, categories, accounts)
        val labels = chips.map { it.label }

        assertTrue(labels.contains("Last 30 Days"))
        assertTrue(labels.contains("Expense"))
        assertTrue(labels.contains("Food"))
        assertTrue(labels.contains("HDFC"))
        assertTrue(labels.contains("\"swiggy\""))
        assertTrue(labels.any { it.contains("100") && it.contains("1000") })
    }

    @Test
    fun `chips fall back to generic labels when ids are unknown`() {
        val filter = TransactionFilter(categoryId = 999, accountId = 888)
        val chips = TransactionFilterEngine.activeChips(filter, categories, accounts)
        val labels = chips.map { it.label }
        assertTrue(labels.contains("Category"))
        assertTrue(labels.contains("Account"))
    }

    @Test
    fun `inactive filter produces no chips`() {
        assertTrue(TransactionFilterEngine.activeChips(TransactionFilter(), categories, accounts).isEmpty())
    }

    // ---- Clearing dimensions ----

    @Test
    fun `clearing a dimension resets only that dimension`() {
        val filter = TransactionFilter(
            dateRange = DateRangePresets.last7Days(now = NOW),
            transactionType = TransactionTypeFilter.EXPENSE,
            categoryId = 1,
            accountId = 1,
            merchantQuery = "swiggy",
            minAmount = 10.0,
            maxAmount = 100.0
        )

        val clearedDate = TransactionFilterEngine.clearing(filter, FilterDimension.DATE_RANGE)
        assertNull(clearedDate.dateRange)
        assertEquals(filter.transactionType, clearedDate.transactionType)

        val clearedAmount = TransactionFilterEngine.clearing(filter, FilterDimension.AMOUNT_RANGE)
        assertNull(clearedAmount.minAmount)
        assertNull(clearedAmount.maxAmount)
        assertEquals(filter.merchantQuery, clearedAmount.merchantQuery)

        val clearedType = TransactionFilterEngine.clearing(filter, FilterDimension.TYPE)
        assertEquals(TransactionTypeFilter.ALL, clearedType.transactionType)
    }

    // ---- Edge cases / empty state ----

    @Test
    fun `filtering an empty list returns an empty list`() {
        assertTrue(TransactionFilterEngine.apply(emptyList(), TransactionFilter(categoryId = 1)).isEmpty())
    }

    @Test
    fun `filter matching nothing returns empty result without throwing`() {
        val txns = listOf(tx(100.0, categoryId = 1))
        val result = TransactionFilterEngine.apply(txns, TransactionFilter(categoryId = 999))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `blank merchant query is treated as no constraint`() {
        val txns = listOf(tx(100.0, merchant = "Amazon"), tx(200.0, merchant = "Swiggy"))
        val result = TransactionFilterEngine.apply(txns, TransactionFilter(merchantQuery = "   "))
        assertEquals(2, result.size)
    }
}
