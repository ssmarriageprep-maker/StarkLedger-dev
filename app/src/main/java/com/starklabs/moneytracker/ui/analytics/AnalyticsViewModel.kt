package com.starklabs.moneytracker.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.starklabs.moneytracker.data.MoneyRepository
import com.starklabs.moneytracker.data.Transaction
import com.starklabs.moneytracker.ui.theme.NeonCyan
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class Slice(
    val value: Float,
    val color: Color,
    val label: String
)

data class CategoryPerformance(
    val name: String,
    val spent: Double,
    val budget: Double,
    val color: Color,
    val percentage: Float
)

data class AnalyticsState(
    val pieSlices: List<Slice> = emptyList(),
    val weeklySpending: List<Float> = emptyList(), // Last 7 days
    val topCategory: String = "N/A",
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val categoryPerformance: List<CategoryPerformance> = emptyList()
)

class AnalyticsViewModel(private val repository: MoneyRepository) : ViewModel() {

    val uiState = combine(
        repository.allTransactions,
        repository.allCategories
    ) { transactions, categories ->
        
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis
        
        val currentMonthTransactions = transactions.filter { it.date >= startOfMonth }

        // 1. Process Pie Chart (By Category)
        val categoryMap = currentMonthTransactions.filter { it.type == "DEBIT" }.groupBy { it.categoryId }
        val totalDebit = currentMonthTransactions.filter { it.type == "DEBIT" }.sumOf { it.amount }
        val totalCredit = currentMonthTransactions.filter { it.type == "CREDIT" }.sumOf { it.amount }
        
        val slices = categoryMap.mapNotNull { (catId, list) ->
            val category = categories.find { it.id == catId }
            val name = category?.name ?: "Uncategorized"
            val colorHex = category?.colorHex ?: "#808080"
            
            val sum = list.sumOf { it.amount }
            if (sum == 0.0) return@mapNotNull null
            
            val portion = if (totalDebit > 0.0) (sum / totalDebit).toFloat() else 0f
            
            Slice(
                value = portion,
                color = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { NeonCyan },
                label = name
            )
        }.sortedByDescending { it.value }.take(5) // Top 5
        
        // 2. Process Weekly Spending (Last 7 days approx)
        val trend = if (transactions.isEmpty()) emptyList() 
                   else transactions.take(7).map { (it.amount / 5000.0).toFloat().coerceIn(0f, 1f) }

        // 3. Process Category Performance
        val performance = categoryMap.mapNotNull { (catId, list) ->
            val category = categories.find { it.id == catId } ?: return@mapNotNull null
            val sum = list.sumOf { it.amount }
            if (sum == 0.0) return@mapNotNull null
            
            CategoryPerformance(
                name = category.name,
                spent = sum,
                budget = category.budgetLimit,
                color = try { Color(android.graphics.Color.parseColor(category.colorHex)) } catch (e: Exception) { NeonCyan },
                percentage = (sum / category.budgetLimit).toFloat().coerceIn(0f, 1f)
            )
        }.sortedByDescending { it.percentage }

        AnalyticsState(
            pieSlices = slices,
            weeklySpending = trend,
            topCategory = slices.firstOrNull()?.label ?: "N/A",
            totalIncome = totalCredit,
            totalExpense = totalDebit,
            categoryPerformance = performance
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsState())
}

class AnalyticsViewModelFactory(private val repository: MoneyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AnalyticsViewModel(repository) as T
    }
}
