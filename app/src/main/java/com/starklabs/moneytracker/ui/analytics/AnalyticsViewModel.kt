package com.starklabs.moneytracker.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.starklabs.moneytracker.data.MoneyRepository
import com.starklabs.moneytracker.data.Transaction
import com.starklabs.moneytracker.ui.theme.*
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
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

    val uiState = repository.allTransactions.map { transactions ->
        // 1. Process Pie Chart (By Category)
        // Simplified mapping for demo. Real app would join with Category table.
        val categoryMap = transactions.filter { it.type == "DEBIT" }.groupBy { it.merchant } // approximating category by merchant for now
        val totalDebit = transactions.filter { it.type == "DEBIT" }.sumOf { it.amount }
        
        val slices = categoryMap.map { (cat, list) ->
            val sum = list.sumOf { it.amount }
            val portion = if (totalDebit > 0.0) (sum / totalDebit).toFloat() else 0f
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
        val trend = if (transactions.isEmpty()) emptyList() 
                   else transactions.take(7).map { (it.amount / 5000.0).toFloat().coerceIn(0f, 1f) }

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
