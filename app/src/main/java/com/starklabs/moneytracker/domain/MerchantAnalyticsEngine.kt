package com.starklabs.moneytracker.domain

import com.starklabs.moneytracker.data.Category
import com.starklabs.moneytracker.data.Transaction
import java.util.Calendar

enum class TrendDirection { INCREASING, DECREASING, STABLE, INSUFFICIENT_DATA }

enum class MerchantSortOrder { HIGHEST_SPEND, MOST_TRANSACTIONS, RECENTLY_ACTIVE, ALPHABETICAL }

data class MerchantSummary(
    val canonicalName: String,
    val totalSpent: Double,
    val transactionCount: Int,
    val averageTransaction: Double,
    val largestTransaction: Double,
    val firstSeen: Long,
    val lastSeen: Long,
    val monthsActive: Int,
    val frequencyPerMonth: Double,
    val trendDirection: TrendDirection,
    val topCategories: List<CategoryAmount>,
    val recentTransactions: List<Transaction>
)

/**
 * Pure, deterministic merchant analytics engine — no Room, no Compose.
 *
 * Resolution is caller-supplied so this engine works in any context (ViewModel with alias DB,
 * unit tests with a plain map, etc.) without coupling to MoneyRepository.
 *
 * Performance: builds rawToCanonical map once per call (O(unique merchants)), then groups
 * all transactions in O(n) — avoids repeated alias lookups per transaction.
 */
object MerchantAnalyticsEngine {

    /**
     * Aggregates all expense transactions into per-merchant summaries.
     *
     * @param resolve maps a raw merchant string to its canonical name (alias-aware)
     */
    fun computeAll(
        transactions: List<Transaction>,
        categories: List<Category>,
        resolve: (String) -> String,
        topCategoriesLimit: Int = 3,
        recentLimit: Int = 5
    ): List<MerchantSummary> {
        val expenses = transactions.filter { it.type == "DEBIT" }
        if (expenses.isEmpty()) return emptyList()

        val rawToCanonical = expenses.map { it.merchant }.distinct().associateWith { resolve(it) }

        return expenses
            .groupBy { rawToCanonical[it.merchant] ?: it.merchant }
            .map { (canonical, group) ->
                buildSummary(canonical, group, categories, topCategoriesLimit, recentLimit)
            }
    }

    /**
     * Builds a single merchant summary — useful for the detail screen.
     * Returns null if no expense transactions match the canonical name.
     */
    fun computeFor(
        canonicalName: String,
        transactions: List<Transaction>,
        categories: List<Category>,
        resolve: (String) -> String,
        topCategoriesLimit: Int = 5,
        recentLimit: Int = 10
    ): MerchantSummary? {
        val expenses = transactions.filter { it.type == "DEBIT" }
        val rawToCanonical = expenses.map { it.merchant }.distinct().associateWith { resolve(it) }
        val group = expenses.filter { (rawToCanonical[it.merchant] ?: it.merchant) == canonicalName }
        if (group.isEmpty()) return null
        return buildSummary(canonicalName, group, categories, topCategoriesLimit, recentLimit)
    }

    fun sort(summaries: List<MerchantSummary>, order: MerchantSortOrder): List<MerchantSummary> =
        when (order) {
            MerchantSortOrder.HIGHEST_SPEND -> summaries.sortedByDescending { it.totalSpent }
            MerchantSortOrder.MOST_TRANSACTIONS -> summaries.sortedByDescending { it.transactionCount }
            MerchantSortOrder.RECENTLY_ACTIVE -> summaries.sortedByDescending { it.lastSeen }
            MerchantSortOrder.ALPHABETICAL -> summaries.sortedBy { it.canonicalName.lowercase() }
        }

    fun search(summaries: List<MerchantSummary>, query: String): List<MerchantSummary> {
        if (query.isBlank()) return summaries
        return summaries.filter { it.canonicalName.contains(query.trim(), ignoreCase = true) }
    }

    private fun buildSummary(
        canonicalName: String,
        group: List<Transaction>,
        categories: List<Category>,
        topCategoriesLimit: Int,
        recentLimit: Int
    ): MerchantSummary {
        val totalSpent = group.sumOf { it.amount }
        val count = group.size
        val avg = if (count > 0) totalSpent / count else 0.0
        val largest = group.maxOfOrNull { it.amount } ?: 0.0
        val firstSeen = group.minOfOrNull { it.date } ?: 0L
        val lastSeen = group.maxOfOrNull { it.date } ?: 0L

        val monthsActive = distinctMonths(group)
        val frequencyPerMonth = if (monthsActive > 0) count.toDouble() / monthsActive else 0.0

        val topCategories = group
            .groupBy { it.categoryId }
            .map { (catId, catGroup) ->
                val cat = categories.find { it.id == catId }
                CategoryAmount(
                    categoryId = catId,
                    name = cat?.name ?: "Uncategorized",
                    colorHex = cat?.colorHex ?: "#808080",
                    amount = catGroup.sumOf { it.amount }
                )
            }
            .filter { it.amount > 0 }
            .sortedByDescending { it.amount }
            .take(topCategoriesLimit)

        val recentTransactions = group.sortedByDescending { it.date }.take(recentLimit)

        return MerchantSummary(
            canonicalName = canonicalName,
            totalSpent = totalSpent,
            transactionCount = count,
            averageTransaction = avg,
            largestTransaction = largest,
            firstSeen = firstSeen,
            lastSeen = lastSeen,
            monthsActive = monthsActive,
            frequencyPerMonth = frequencyPerMonth,
            trendDirection = computeTrend(group),
            topCategories = topCategories,
            recentTransactions = recentTransactions
        )
    }

    private fun distinctMonths(txns: List<Transaction>): Int {
        val cal = Calendar.getInstance()
        return txns.map { tx ->
            cal.timeInMillis = tx.date
            cal.get(Calendar.YEAR) * 12 + cal.get(Calendar.MONTH)
        }.distinct().size
    }

    private fun computeTrend(txns: List<Transaction>): TrendDirection {
        val buckets = monthlyTotals(txns)
        if (buckets.size < 2) return TrendDirection.INSUFFICIENT_DATA
        val avg = buckets.values.average()
        val lastMonth = buckets.values.last()
        return when {
            lastMonth > avg * 1.2 -> TrendDirection.INCREASING
            lastMonth < avg * 0.8 -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
    }

    // Sorted by date → LinkedHashMap preserves insertion order → .values.last() == most recent month.
    private fun monthlyTotals(txns: List<Transaction>): Map<Int, Double> {
        val cal = Calendar.getInstance()
        return txns.sortedBy { it.date }
            .groupBy { tx ->
                cal.timeInMillis = tx.date
                cal.get(Calendar.YEAR) * 12 + cal.get(Calendar.MONTH)
            }
            .mapValues { (_, group) -> group.sumOf { it.amount } }
    }
}
