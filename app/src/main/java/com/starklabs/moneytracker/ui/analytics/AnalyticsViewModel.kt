package com.starklabs.moneytracker.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.starklabs.moneytracker.data.MoneyRepository
import com.starklabs.moneytracker.data.Transaction
import com.starklabs.moneytracker.ui.theme.*
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class Slice(
    val value: Float,
    val color: Color,
    val label: String
)

data class AnalyticsState(
    val pieSlices: List<Slice> = emptyList(),
    val weeklySpending: List<Float> = emptyList(), // Last 7 days
    val topCategory: String = "N/A"
)

class AnalyticsViewModel(private val repository: MoneyRepository) : ViewModel() {

    val uiState = combine(repository.allTransactions) { transactions ->
        // 1. Process Pie Chart (By Category)
        // Simplified mapping for demo. Real app would join with Category table.
        val categoryMap = transactions.filter { it.type == "DEBIT" }.groupBy { it.merchant } // approximating category by merchant for now
        val totalDebit = transactions.filter { it.type == "DEBIT" }.sumOf { it.amount }
        
        val slices = categoryMap.map { (cat, list) ->
            val sum = list.sumOf { it.amount }
            val portion = (sum / totalDebit).toFloat()
            Slice(
                value = portion,
                color = when(cat.hashCode() % 5) {
                    0 -> NeonCyan
                    1 -> JarvisGold
                    2 -> JarvisOrange
                    3 -> MetallicRed
                    else -> ElectricBlue
                },
                label = cat
            )
        }.sortedByDescending { it.value }.take(5) // Top 5
        
        // 2. Process Weekly Spending (Last 7 days approx)
        // Mocking trend for visualization if empty
        val trend = if (transactions.isEmpty()) listOf(0.2f, 0.4f, 0.3f, 0.8f, 0.5f, 0.9f, 0.6f) 
                   else transactions.take(7).map { (it.amount / 5000).toFloat().coerceIn(0f, 1f) }

        AnalyticsState(
            pieSlices = slices,
            weeklySpending = trend,
            topCategory = slices.firstOrNull()?.label ?: "N/A"
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsState())
}

class AnalyticsViewModelFactory(private val repository: MoneyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AnalyticsViewModel(repository) as T
    }
}
