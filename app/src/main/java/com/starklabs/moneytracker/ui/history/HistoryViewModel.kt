package com.starklabs.moneytracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.starklabs.moneytracker.data.MoneyRepository
import com.starklabs.moneytracker.data.Transaction
import com.starklabs.moneytracker.data.Category
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

data class HistoryState(
    val groupedTransactions: Map<String, List<Transaction>> = emptyMap(),
    val searchQuery: String = ""
)

class HistoryViewModel(private val repository: MoneyRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val uiState: StateFlow<HistoryState> = combine(
        repository.allTransactions,
        _searchQuery
    ) { transactions, query ->
        val filtered = if (query.isBlank()) {
            transactions
        } else {
            transactions.filter {
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

    fun updateTransactionCategory(transactionId: Int, newCategoryId: Int, merchant: String) {
        viewModelScope.launch {
            repository.updateTransactionCategory(transactionId, newCategoryId, merchant)
        }
    }
}

class HistoryViewModelFactory(private val repository: MoneyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
