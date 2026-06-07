package com.starklabs.moneytracker.domain

import com.starklabs.moneytracker.data.Category
import com.starklabs.moneytracker.data.Transaction

/** A merchant's aggregate spend, grouped on its normalized name to avoid raw-string fragmentation. */
data class MerchantAmount(
    val merchant: String,
    val amount: Double,
    val count: Int
)

data class AccountAnalyticsSummary(
    val income: Double,
    val expense: Double,
    val savings: Double,
    val transactionCount: Int,
    val topCategories: List<CategoryAmount>,
    val topMerchants: List<MerchantAmount>,
    val recentTransactions: List<Transaction>
)

/**
 * Builds the "account intelligence" summary shown on the account detail screen:
 * income/expense/savings, top categories, top merchants, and recent activity —
 * all derived from the account's own transactions. Pure and deterministic,
 * mirroring [InsightsEngine] / [YearlyAnalyticsEngine] so it stays unit-testable
 * without Room.
 *
 * Merchant names are normalized via [CategoryNormalizer] before grouping so
 * "AMAZON PAY INDIA" and "Amazon" roll up into a single "Amazon" entry —
 * reusing the existing normalization rather than duplicating cleanup logic.
 */
object AccountAnalyticsEngine {

    fun compute(
        transactions: List<Transaction>,
        categories: List<Category>,
        topCategoriesLimit: Int = 5,
        topMerchantsLimit: Int = 5,
        recentLimit: Int = 10
    ): AccountAnalyticsSummary {
        val expenses = transactions.filter { it.type == "DEBIT" }
        val income = transactions.filter { it.type == "CREDIT" }

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
            .take(topCategoriesLimit)

        val topMerchants = expenses
            .groupBy { CategoryNormalizer.normalizeMerchant(it.merchant) }
            .map { (name, txns) -> MerchantAmount(merchant = name, amount = txns.sumOf { it.amount }, count = txns.size) }
            .filter { it.amount > 0 }
            .sortedByDescending { it.amount }
            .take(topMerchantsLimit)

        // Transactions already arrive newest-first from the repository (ORDER BY date DESC),
        // but sort defensively so this engine's output doesn't depend on caller ordering.
        val recentTransactions = transactions.sortedByDescending { it.date }.take(recentLimit)

        return AccountAnalyticsSummary(
            income = totalIncome,
            expense = totalExpense,
            savings = totalIncome - totalExpense,
            transactionCount = transactions.size,
            topCategories = topCategories,
            topMerchants = topMerchants,
            recentTransactions = recentTransactions
        )
    }
}
