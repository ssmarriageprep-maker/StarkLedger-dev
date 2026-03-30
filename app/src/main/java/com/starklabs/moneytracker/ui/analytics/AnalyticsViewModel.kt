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
    val categoryPerformance: List<CategoryPerformance> = emptyList(),
    val pulseTitle: String = "Calculating Pulse...",
    val pulseDescription: String = "Analyzing your spending patterns...",
    val pulseColor: Color = Color(0xFFFEB300) // SecondaryContainer
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

        // 4. Calculate Weekly Pulse (Rolling 7 days)
        val now = System.currentTimeMillis()
        val sevenDaysMillis = 7 * 24 * 60 * 60 * 1000L
        val last7DaysTransactions = transactions.filter { it.date in (now - sevenDaysMillis)..now && it.type == "DEBIT" }
        val previous7DaysTransactions = transactions.filter { it.date in (now - 2 * sevenDaysMillis)..(now - sevenDaysMillis) && it.type == "DEBIT" }

        val last7DaysSpend = last7DaysTransactions.sumOf { it.amount }
        val previous7DaysSpend = previous7DaysTransactions.sumOf { it.amount }

        val (pulseTitle, pulseDescription, pulseColor) = if (previous7DaysSpend > 0) {
            val percentChange = ((last7DaysSpend - previous7DaysSpend) / previous7DaysSpend * 100).toInt()
            if (percentChange > 0) {
                // Determine spike category
                val last7DaysByCat = last7DaysTransactions.groupBy { it.categoryId }
                val prev7DaysByCat = previous7DaysTransactions.groupBy { it.categoryId }

                val spikeCategory = last7DaysByCat.mapNotNull { (catId, list) ->
                    val lastSum = list.sumOf { it.amount }
                    val prevSum = prev7DaysByCat[catId]?.sumOf { it.amount } ?: 0.0
                    val increase = lastSum - prevSum
                    if (increase > 0) catId to increase else null
                }.maxByOrNull { it.second }?.first?.let { catId -> categories.find { it.id == catId }?.name } ?: "multiple categories"

                Triple(
                    "You spent $percentChange% more this week",
                    "Unusual spikes detected in the $spikeCategory. Consider reviewing your last three transactions.",
                    Color(0xFFFFB4AB) // Error color for spending increase
                )
            } else {
                Triple(
                    "You saved ${Math.abs(percentChange)}% this week!",
                    "Excellent progress! Your spending velocity has decreased compared to last week. Keep it up.",
                    Color(0xFF5FEC79) // TertiaryContainer for savings
                )
            }
        } else if (last7DaysSpend > 0) {
            Triple(
                "Fresh activity detected",
                "You've started tracking ₹${String.format("%,.0f", last7DaysSpend)} this week. We'll compare this to your next 7 days.",
                Color(0xFF00DAF2) // PrimaryFixedDim for new data
            )
        } else {
            Triple(
                "Pulse check complete",
                "No spending detected in the last 7 days. Your budget is currently under zero pressure.",
                Color(0xFF00DAF2)
            )
        }

        AnalyticsState(
            pieSlices = slices,
            weeklySpending = trend,
            topCategory = slices.firstOrNull()?.label ?: "N/A",
            totalIncome = totalCredit,
            totalExpense = totalDebit,
            categoryPerformance = performance,
            pulseTitle = pulseTitle,
            pulseDescription = pulseDescription,
            pulseColor = pulseColor
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsState())
}

class AnalyticsViewModelFactory(private val repository: MoneyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AnalyticsViewModel(repository) as T
    }
}
