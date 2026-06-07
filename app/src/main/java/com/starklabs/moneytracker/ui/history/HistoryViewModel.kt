package com.starklabs.moneytracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.starklabs.moneytracker.data.MoneyRepository
import com.starklabs.moneytracker.data.Transaction
import com.starklabs.moneytracker.data.Category
import com.starklabs.moneytracker.domain.FilterDimension
import com.starklabs.moneytracker.domain.TransactionFilter
import com.starklabs.moneytracker.domain.TransactionFilterEngine
import com.starklabs.moneytracker.domain.TransactionFilterStore
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
    private val appSettingsRepository: com.starklabs.moneytracker.data.AppSettingsRepository,
    private val filterStore: TransactionFilterStore
) : ViewModel() {

    val selectedAccountId: StateFlow<Int> = appSettingsRepository.selectedAccountId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1)

    fun setSelectedAccount(id: Int) {
        viewModelScope.launch {
            appSettingsRepository.setSelectedAccountId(id)
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** Shared advanced-filter state — same instance Analytics reads, so filters persist across screens. */
    val filter: StateFlow<TransactionFilter> = filterStore.filter

    fun applyFilter(newFilter: TransactionFilter) = filterStore.update(newFilter)

    fun clearFilterDimension(dimension: FilterDimension) = filterStore.clearDimension(dimension)

    fun clearAllFilters() = filterStore.clear()

    val uiState: StateFlow<HistoryState> = combine(
        repository.allTransactions,
        _searchQuery,
        appSettingsRepository.selectedAccountId,
        filterStore.filter
    ) { allTransactions, query, selectedId, activeFilter ->

        val transactionsByAccount = if (selectedId == -1) allTransactions else allTransactions.filter { it.accountId == selectedId }

        val advancedFiltered = TransactionFilterEngine.apply(transactionsByAccount, activeFilter)

        val filtered = if (query.isBlank()) {
            advancedFiltered
        } else {
            advancedFiltered.filter {
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
    private val appSettingsRepository: com.starklabs.moneytracker.data.AppSettingsRepository,
    private val filterStore: TransactionFilterStore
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(repository, appSettingsRepository, filterStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
