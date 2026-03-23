package com.starklabs.moneytracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.starklabs.moneytracker.data.MoneyRepository
import com.starklabs.moneytracker.data.Transaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.starklabs.moneytracker.data.AppSettingsRepository
import com.starklabs.moneytracker.data.Category

data class DashboardState(
    val totalSpent: Double = 0.0,
    val totalIncome: Double = 0.0,
    val balance: Double = 0.0,
    val recentTransactions: List<Transaction> = emptyList(),
    val budgetProgress: Float = 0.0f
)

class DashboardViewModel(
    private val repository: MoneyRepository,
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    // Seed defaults on init (simplified for this demo)
    // Init block removed: Seeding is handled safely in Repository or should be triggered once by MainActivity if needed.
    // For now, we rely on Repository's safe check if called elsewhere, or just don't call it here to avoid startup lag.
    // No-op or remove completely. 

    val categories: StateFlow<List<Category>> = repository.allCategories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateTransactionCategory(transactionId: Int, newCategoryId: Int, merchant: String) {
        viewModelScope.launch {
            repository.updateTransactionCategory(transactionId, newCategoryId, merchant)
        }
    }

    val uiState: StateFlow<DashboardState> = combine(
        repository.allAccounts,
        repository.allTransactions,
        repository.allCategories,
        appSettingsRepository.dashboardLogCount
    ) { accounts, transactions, categories, logCount ->
        
        // Total All-Time Balance
        val totalBalance = accounts.sumOf { it.balance }
        
        // Monthly Calculations
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis
        
        val currentMonthTransactions = transactions.filter { it.date >= startOfMonth }
        val s = currentMonthTransactions.filter { it.type == "DEBIT" }.sumOf { it.amount }
        val i = currentMonthTransactions.filter { it.type == "CREDIT" }.sumOf { it.amount }
        
        // Calculate Total Budget dynamically from Categories
        // Default to a reasonable fallback if no categories set
        val totalBudget = categories.sumOf { it.budgetLimit }.takeIf { it > 0 } ?: 25000.0
        
        val progress = (s / totalBudget).coerceIn(0.0, 1.0).toFloat()
        
        DashboardState(
            totalSpent = s,
            totalIncome = i,
            balance = totalBalance,
            recentTransactions = transactions.take(logCount),
            budgetProgress = progress
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())

}

class DashboardViewModelFactory(
    private val repository: MoneyRepository,
    private val appSettingsRepository: AppSettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository, appSettingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
