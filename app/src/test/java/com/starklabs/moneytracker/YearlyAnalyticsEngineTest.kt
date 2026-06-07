package com.starklabs.moneytracker

import com.starklabs.moneytracker.data.Category
import com.starklabs.moneytracker.data.Transaction
import com.starklabs.moneytracker.domain.YearlyAnalyticsEngine
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * Unit tests for yearly aggregation: totals, savings, top categories, and the
 * 12-month trend used by the Year toggle on the Analytics screen. A fixed
 * UTC calendar is injected so month/year bucketing is deterministic on any CI runner.
 */
class YearlyAnalyticsEngineTest {

    private val utcCalendar: Calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

    private fun millisFor(year: Int, month: Int, day: Int): Long {
        val cal = utcCalendar.clone() as Calendar
        cal.clear()
        cal.set(year, month, day, 12, 0, 0)
        return cal.timeInMillis
    }

    private fun tx(
        amount: Double,
        date: Long,
        type: String = "DEBIT",
        categoryId: Int? = 1,
        merchant: String = "Merchant"
    ) = Transaction(amount = amount, merchant = merchant, date = date, type = type, accountId = 1, categoryId = categoryId)

    private val foodCategory = Category(id = 1, name = "Food", iconName = "restaurant", colorHex = "#FF0000")
    private val travelCategory = Category(id = 2, name = "Travel", iconName = "car", colorHex = "#00FF00")
    private val categories = listOf(foodCategory, travelCategory)

    @Test
    fun `compute aggregates totals and savings for the requested year only`() {
        val txns = listOf(
            tx(1000.0, date = millisFor(2024, Calendar.JANUARY, 5), type = "CREDIT"),
            tx(400.0, date = millisFor(2024, Calendar.FEBRUARY, 10), type = "DEBIT"),
            tx(99999.0, date = millisFor(2023, Calendar.DECEMBER, 31), type = "DEBIT") // different year, excluded
        )
        val summary = YearlyAnalyticsEngine.compute(txns, categories, year = 2024, calendar = utcCalendar)

        assertEquals(2024, summary.year)
        assertEquals(1000.0, summary.totalIncome, 0.0)
        assertEquals(400.0, summary.totalExpense, 0.0)
        assertEquals(600.0, summary.savings, 0.0)
    }

    @Test
    fun `top categories are sorted by spend descending and exclude zero amounts`() {
        val txns = listOf(
            tx(300.0, date = millisFor(2024, Calendar.MARCH, 1), categoryId = 1),
            tx(700.0, date = millisFor(2024, Calendar.APRIL, 1), categoryId = 2),
            tx(0.0, date = millisFor(2024, Calendar.MAY, 1), categoryId = 1)
        )
        val summary = YearlyAnalyticsEngine.compute(txns, categories, year = 2024, calendar = utcCalendar)

        assertEquals(listOf("Travel", "Food"), summary.topCategories.map { it.name })
        assertEquals(700.0, summary.topCategories.first().amount, 0.0)
    }

    @Test
    fun `unknown category id falls back to Uncategorized with default color`() {
        val txns = listOf(tx(150.0, date = millisFor(2024, Calendar.JUNE, 1), categoryId = 999))
        val summary = YearlyAnalyticsEngine.compute(txns, categories, year = 2024, calendar = utcCalendar)

        val entry = summary.topCategories.single()
        assertEquals("Uncategorized", entry.name)
        assertEquals("#808080", entry.colorHex)
    }

    @Test
    fun `monthly trend has exactly twelve points labeled Jan through Dec`() {
        val summary = YearlyAnalyticsEngine.compute(emptyList(), categories, year = 2024, calendar = utcCalendar)

        assertEquals(12, summary.monthlyTrend.size)
        assertEquals("Jan", summary.monthlyTrend.first().label)
        assertEquals("Dec", summary.monthlyTrend.last().label)
        assertEquals((0..11).toList(), summary.monthlyTrend.map { it.monthIndex })
    }

    @Test
    fun `monthly trend buckets income and expense into the correct month`() {
        val txns = listOf(
            tx(1000.0, date = millisFor(2024, Calendar.MARCH, 15), type = "CREDIT"),
            tx(300.0, date = millisFor(2024, Calendar.MARCH, 20), type = "DEBIT"),
            tx(500.0, date = millisFor(2024, Calendar.JULY, 4), type = "DEBIT")
        )
        val summary = YearlyAnalyticsEngine.compute(txns, categories, year = 2024, calendar = utcCalendar)

        val march = summary.monthlyTrend[Calendar.MARCH]
        val july = summary.monthlyTrend[Calendar.JULY]
        val january = summary.monthlyTrend[Calendar.JANUARY]

        assertEquals(1000.0, march.income, 0.0)
        assertEquals(300.0, march.expense, 0.0)
        assertEquals(500.0, july.expense, 0.0)
        assertEquals(0.0, january.income, 0.0)
        assertEquals(0.0, january.expense, 0.0)
    }

    @Test
    fun `compute on empty transactions yields zeroed summary without throwing`() {
        val summary = YearlyAnalyticsEngine.compute(emptyList(), categories, year = 2024, calendar = utcCalendar)

        assertEquals(0.0, summary.totalIncome, 0.0)
        assertEquals(0.0, summary.totalExpense, 0.0)
        assertEquals(0.0, summary.savings, 0.0)
        assertTrue(summary.topCategories.isEmpty())
        assertTrue(summary.monthlyTrend.all { it.income == 0.0 && it.expense == 0.0 })
    }

    @Test
    fun `available years are distinct and ordered most recent first`() {
        val txns = listOf(
            tx(100.0, date = millisFor(2022, Calendar.JANUARY, 1)),
            tx(100.0, date = millisFor(2024, Calendar.JANUARY, 1)),
            tx(100.0, date = millisFor(2023, Calendar.JANUARY, 1)),
            tx(100.0, date = millisFor(2024, Calendar.JUNE, 1))
        )
        assertEquals(listOf(2024, 2023, 2022), YearlyAnalyticsEngine.availableYears(txns, utcCalendar))
    }

    @Test
    fun `available years on empty data is empty`() {
        assertTrue(YearlyAnalyticsEngine.availableYears(emptyList(), utcCalendar).isEmpty())
    }
}
