package com.starklabs.moneytracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.starklabs.moneytracker.data.MoneyRepository
import com.starklabs.moneytracker.data.Transaction
import com.starklabs.moneytracker.data.Category
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class HistoryState(
    val groupedTransactions: Map<String, List<Transaction>> = emptyMap(),
    val searchQuery: String = ""
)

class HistoryViewModel(
    private val repository: MoneyRepository,
    private val appSettingsRepository: com.starklabs.moneytracker.data.AppSettingsRepository
) : ViewModel() {

    val selectedAccountId: StateFlow<Int> = appSettingsRepository.selectedAccountId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1)

    fun setSelectedAccount(id: Int) {
        viewModelScope.launch {
            appSettingsRepository.setSelectedAccountId(id)
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val uiState: StateFlow<HistoryState> = combine(
        repository.allTransactions,
        _searchQuery,
        appSettingsRepository.selectedAccountId
    ) { allTransactions, query, selectedId ->

        val transactionsByAccount = if (selectedId == -1) allTransactions else allTransactions.filter { it.accountId == selectedId }

        val filtered = if (query.isBlank()) {
            transactionsByAccount
        } else {
            transactionsByAccount.filter {
                it.merchant.contains(query, ignoreCase = true) ||
                (it.smsBody?.contains(query, ignoreCase = true) ?: false) ||
                it.amount.toString().contains(query)
            }
        }

        val grouped = filtered.groupBy {
            val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            sdf.format(Date(it.date))
        }

        HistoryState(groupedTransactions = grouped, searchQuery = query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryState())

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }
    
    val categories: StateFlow<List<Category>> = repository.allCategories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val accounts: StateFlow<List<com.starklabs.moneytracker.data.Account>> = repository.allAccounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateTransactionCategory(transactionId: Int, newCategoryId: Int, merchant: String) {
        viewModelScope.launch {
            repository.updateTransactionCategory(transactionId, newCategoryId, merchant)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }
}

class HistoryViewModelFactory(
    private val repository: MoneyRepository,
    private val appSettingsRepository: com.starklabs.moneytracker.data.AppSettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(repository, appSettingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
