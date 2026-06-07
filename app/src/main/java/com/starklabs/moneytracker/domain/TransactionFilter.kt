package com.starklabs.moneytracker.domain

import com.starklabs.moneytracker.data.Account
import com.starklabs.moneytracker.data.Category
import com.starklabs.moneytracker.data.Transaction
import java.util.Calendar

/**
 * A closed date range used for filtering, paired with a display label
 * so the UI never has to re-derive "Last 30 Days" style copy.
 */
data class DateRange(
    val startMillis: Long,
    val endMillis: Long,
    val label: String
)

enum class TransactionTypeFilter {
    ALL, INCOME, EXPENSE
}

/**
 * Immutable, serialization-free filter spec shared by History and Analytics.
 * A field set to null/blank/ALL means "no constraint on this dimension".
 */
data class TransactionFilter(
    val dateRange: DateRange? = null,
    val transactionType: TransactionTypeFilter = TransactionTypeFilter.ALL,
    val categoryId: Int? = null,
    val accountId: Int? = null,
    val merchantQuery: String = "",
    val minAmount: Double? = null,
    val maxAmount: Double? = null
) {
    val isActive: Boolean
        get() = dateRange != null ||
            transactionType != TransactionTypeFilter.ALL ||
            categoryId != null ||
            accountId != null ||
            merchantQuery.isNotBlank() ||
            minAmount != null ||
            maxAmount != null

    val activeCount: Int
        get() = listOf(
            dateRange != null,
            transactionType != TransactionTypeFilter.ALL,
            categoryId != null,
            accountId != null,
            merchantQuery.isNotBlank(),
            minAmount != null || maxAmount != null
        ).count { it }
}

/** Identifies which slice of a [TransactionFilter] an active chip represents, so the UI can clear it precisely. */
enum class FilterDimension {
    DATE_RANGE, TYPE, CATEGORY, ACCOUNT, MERCHANT, AMOUNT_RANGE
}

data class ActiveFilterChip(val dimension: FilterDimension, val label: String)

/**
 * Pure, deterministic filtering used by both the History and Analytics screens.
 * Kept side-effect free so it can be unit tested without Room/Compose.
 */
object TransactionFilterEngine {

    fun apply(transactions: List<Transaction>, filter: TransactionFilter): List<Transaction> {
        if (!filter.isActive) return transactions
        return transactions.filter { matches(it, filter) }
    }

    fun matches(transaction: Transaction, filter: TransactionFilter): Boolean {
        val range = filter.dateRange
        if (range != null && transaction.date !in range.startMillis..range.endMillis) return false

        when (filter.transactionType) {
            TransactionTypeFilter.INCOME -> if (transaction.type != "CREDIT") return false
            TransactionTypeFilter.EXPENSE -> if (transaction.type != "DEBIT") return false
            TransactionTypeFilter.ALL -> { /* no constraint */ }
        }

        if (filter.categoryId != null && transaction.categoryId != filter.categoryId) return false
        if (filter.accountId != null && transaction.accountId != filter.accountId) return false

        if (filter.merchantQuery.isNotBlank() &&
            !transaction.merchant.contains(filter.merchantQuery, ignoreCase = true)
        ) return false

        if (filter.minAmount != null && transaction.amount < filter.minAmount) return false
        if (filter.maxAmount != null && transaction.amount > filter.maxAmount) return false

        return true
    }

    /**
     * Builds the chip labels shown above the list, e.g. [ HDFC ] [ Food ] [ Last 30 Days ].
     * Pure so the exact copy is unit-testable independent of Compose.
     */
    fun activeChips(filter: TransactionFilter, categories: List<Category>, accounts: List<Account>): List<ActiveFilterChip> {
        val chips = mutableListOf<ActiveFilterChip>()

        filter.dateRange?.let { chips += ActiveFilterChip(FilterDimension.DATE_RANGE, it.label) }

        when (filter.transactionType) {
            TransactionTypeFilter.INCOME -> chips += ActiveFilterChip(FilterDimension.TYPE, "Income")
            TransactionTypeFilter.EXPENSE -> chips += ActiveFilterChip(FilterDimension.TYPE, "Expense")
            TransactionTypeFilter.ALL -> {}
        }

        filter.categoryId?.let { id ->
            val name = categories.find { it.id == id }?.name ?: "Category"
            chips += ActiveFilterChip(FilterDimension.CATEGORY, name)
        }

        filter.accountId?.let { id ->
            val name = accounts.find { it.id == id }?.name ?: "Account"
            chips += ActiveFilterChip(FilterDimension.ACCOUNT, name)
        }

        if (filter.merchantQuery.isNotBlank()) {
            chips += ActiveFilterChip(FilterDimension.MERCHANT, "\"${filter.merchantQuery}\"")
        }

        if (filter.minAmount != null || filter.maxAmount != null) {
            val label = when {
                filter.minAmount != null && filter.maxAmount != null ->
                    "₹${formatAmount(filter.minAmount)}–₹${formatAmount(filter.maxAmount)}"
                filter.minAmount != null -> "Above ₹${formatAmount(filter.minAmount)}"
                else -> "Below ₹${formatAmount(filter.maxAmount!!)}"
            }
            chips += ActiveFilterChip(FilterDimension.AMOUNT_RANGE, label)
        }

        return chips
    }

    private fun formatAmount(amount: Double): String =
        if (amount == amount.toLong().toDouble()) amount.toLong().toString() else String.format("%.2f", amount)

    /** Returns [filter] with the given dimension reset to "no constraint", used by chip "x" buttons. */
    fun clearing(filter: TransactionFilter, dimension: FilterDimension): TransactionFilter = when (dimension) {
        FilterDimension.DATE_RANGE -> filter.copy(dateRange = null)
        FilterDimension.TYPE -> filter.copy(transactionType = TransactionTypeFilter.ALL)
        FilterDimension.CATEGORY -> filter.copy(categoryId = null)
        FilterDimension.ACCOUNT -> filter.copy(accountId = null)
        FilterDimension.MERCHANT -> filter.copy(merchantQuery = "")
        FilterDimension.AMOUNT_RANGE -> filter.copy(minAmount = null, maxAmount = null)
    }
}

/**
 * Common date-range presets for the filter sheet. All take an explicit `now`
 * so tests stay deterministic instead of depending on the system clock.
 */
object DateRangePresets {

    fun last7Days(now: Long = System.currentTimeMillis()): DateRange =
        rollingDays(now, 7, "Last 7 Days")

    fun last30Days(now: Long = System.currentTimeMillis()): DateRange =
        rollingDays(now, 30, "Last 30 Days")

    fun thisMonth(now: Long = System.currentTimeMillis()): DateRange {
        val start = startOfDay(startOfMonth(now))
        return DateRange(start, now, "This Month")
    }

    fun thisYear(now: Long = System.currentTimeMillis()): DateRange {
        val cal = calendarAt(now).apply {
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return DateRange(startOfDay(cal.timeInMillis), now, "This Year")
    }

    private fun rollingDays(now: Long, days: Int, label: String): DateRange {
        val start = now - days.toLong() * 24 * 60 * 60 * 1000L
        return DateRange(start, now, label)
    }

    private fun startOfMonth(timestamp: Long): Long =
        calendarAt(timestamp).apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis

    private fun startOfDay(timestamp: Long): Long =
        calendarAt(timestamp).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun calendarAt(timestamp: Long): Calendar =
        Calendar.getInstance().apply { timeInMillis = timestamp }
}
