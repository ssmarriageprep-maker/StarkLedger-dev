package com.starklabs.moneytracker.domain

import com.starklabs.moneytracker.data.Transaction
import java.util.Calendar

object InsightsEngine {
    fun generateInsights(transactions: List<Transaction>): List<String> {
        val insights = mutableListOf<String>()
        val debits = transactions.filter { it.type == "DEBIT" }
        
        if (debits.isEmpty()) return listOf("Log some expenses to see insights.")

        val totalSpent = debits.sumOf { it.amount }
        if (totalSpent <= 0) return listOf("Log some expenses to see insights.")
        
        // Insight 1: Highest spend category
        val categorySpends = debits
            .groupBy { CategoryNormalizer.inferCategory(it.merchant) }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            
        val maxCat = categorySpends.maxByOrNull { it.value }
        if (maxCat != null && maxCat.value > 0) {
            val percentage = (maxCat.value / totalSpent * 100).toInt()
            insights.add("${maxCat.key} is your highest expense (${percentage}%)")
        }
        
        // Insight 2: Weekend spending
        val weekendSpend = debits.filter { isWeekend(it.date) }.sumOf { it.amount }
        if ((weekendSpend / totalSpent) > 0.4) {
            insights.add("You overspend on weekends.")
        }
        
        // Insight 3: Largest single transaction
        val maxTxn = debits.maxByOrNull { it.amount }
        if (maxTxn != null && (maxTxn.amount / totalSpent) > 0.5) {
            insights.add("A massive 50%+ chunk went to ${CategoryNormalizer.normalizeMerchant(maxTxn.merchant)}.")
        }
        
        if (insights.isEmpty() || insights.size == 1) {
            insights.add("Your spending patterns look stable.")
        }
        
        return insights
    }
    
    private fun isWeekend(timestamp: Long): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val day = cal.get(Calendar.DAY_OF_WEEK)
        return day == Calendar.SATURDAY || day == Calendar.SUNDAY
    }
}
