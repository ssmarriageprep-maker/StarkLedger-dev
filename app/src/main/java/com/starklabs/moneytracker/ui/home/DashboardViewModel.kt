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
    init {
        // No-op or remove completely. 
    }

    val uiState: StateFlow<DashboardState> = combine(
        repository.totalSpent,
        repository.totalIncome,
        repository.allTransactions,
        repository.allCategories,
        appSettingsRepository.dashboardLogCount
    ) { spent: Double?, income: Double?, transactions: List<Transaction>, categories: List<com.starklabs.moneytracker.data.Category>, logCount: Int ->
        val s = spent ?: 0.0
        val i = income ?: 0.0
        val b = i - s
        
        // Calculate Total Budget dynamically from Categories
        // Default to a reasonable fallback if no categories set, though we seed defaults.
        val totalBudget = categories.sumOf { it.budgetLimit }.takeIf { it > 0 } ?: 25000.0
        
        val progress = (s / totalBudget).coerceIn(0.0, 1.0).toFloat()
        
        DashboardState(
            totalSpent = s,
            totalIncome = i,
            balance = b,
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
