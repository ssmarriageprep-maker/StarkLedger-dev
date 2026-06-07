package com.starklabs.moneytracker.domain

import com.starklabs.moneytracker.data.Category
import com.starklabs.moneytracker.data.Transaction
import java.util.Calendar

/** A category's aggregate spend, ready for display (name/color resolved against the DB). */
data class CategoryAmount(
    val categoryId: Int?,
    val name: String,
    val colorHex: String,
    val amount: Double
)

/** One bar/point in a 12-month trend — index 0 = January. */
data class MonthlyTrendPoint(
    val monthIndex: Int,
    val label: String,
    val income: Double,
    val expense: Double
)

data class YearlySummary(
    val year: Int,
    val totalIncome: Double,
    val totalExpense: Double,
    val savings: Double,
    val topCategories: List<CategoryAmount>,
    val monthlyTrend: List<MonthlyTrendPoint>
)

/**
 * Aggregates a flat transaction list into a yearly view: totals, top spending
 * categories, and a month-by-month income/expense trend. Pure and deterministic
 * (calendar is injectable) so it mirrors [InsightsEngine]'s testable style and
 * needs no Room access — it operates on data the ViewModel already streams.
 */
object YearlyAnalyticsEngine {

    private val MONTH_LABELS = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    fun compute(
        transactions: List<Transaction>,
        categories: List<Category>,
        year: Int,
        calendar: Calendar = Calendar.getInstance()
    ): YearlySummary {
        val yearTransactions = transactions.filter { yearOf(it.date, calendar) == year }

        val expenses = yearTransactions.filter { it.type == "DEBIT" }
        val income = yearTransactions.filter { it.type == "CREDIT" }

        val totalExpense = expenses.sumOf { it.amount }
        val totalIncome = income.sumOf { it.amount }

        val topCategories = expenses
            .groupBy { it.categoryId }
            .map { (catId, txns) ->
                val category = categories.find { it.id == catId }
                CategoryAmount(
                    categoryId = catId,
                    name = category?.name ?: "Uncategorized",
                    colorHex = category?.colorHex ?: "#808080",
                    amount = txns.sumOf { it.amount }
                )
            }
            .filter { it.amount > 0 }
            .sortedByDescending { it.amount }

        val trend = (0..11).map { monthIndex ->
            val monthTxns = yearTransactions.filter { monthOf(it.date, calendar) == monthIndex }
            MonthlyTrendPoint(
                monthIndex = monthIndex,
                label = MONTH_LABELS[monthIndex],
                income = monthTxns.filter { it.type == "CREDIT" }.sumOf { it.amount },
                expense = monthTxns.filter { it.type == "DEBIT" }.sumOf { it.amount }
            )
        }

        return YearlySummary(
            year = year,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            savings = totalIncome - totalExpense,
            topCategories = topCategories,
            monthlyTrend = trend
        )
    }

    /** Distinct years present in the data, most recent first — drives the year selector. */
    fun availableYears(transactions: List<Transaction>, calendar: Calendar = Calendar.getInstance()): List<Int> =
        transactions.map { yearOf(it.date, calendar) }.distinct().sortedDescending()

    private fun yearOf(timestamp: Long, calendar: Calendar): Int =
        (calendar.clone() as Calendar).apply { timeInMillis = timestamp }.get(Calendar.YEAR)

    private fun monthOf(timestamp: Long, calendar: Calendar): Int =
        (calendar.clone() as Calendar).apply { timeInMillis = timestamp }.get(Calendar.MONTH)
}
