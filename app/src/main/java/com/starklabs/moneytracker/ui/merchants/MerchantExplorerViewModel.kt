package com.starklabs.moneytracker.ui.merchants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.starklabs.moneytracker.data.MoneyRepository
import com.starklabs.moneytracker.domain.MerchantAnalyticsEngine
import com.starklabs.moneytracker.domain.MerchantNormalizationEngine
import com.starklabs.moneytracker.domain.MerchantSortOrder
import com.starklabs.moneytracker.domain.MerchantSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class MerchantExplorerViewModel(repository: MoneyRepository) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val sortOrder = MutableStateFlow(MerchantSortOrder.HIGHEST_SPEND)

    private val allMerchantSummaries: StateFlow<List<MerchantSummary>> = combine(
        repository.allTransactions,
        repository.allCategories,
        repository.allMerchantAliases
    ) { transactions, categories, aliases ->
        val aliasMap = aliases.associateBy({ it.aliasKey }, { it.canonicalMerchant })
        val resolve = { raw: String ->
            val key = raw.trim().lowercase()
            aliasMap[key] ?: MerchantNormalizationEngine.normalize(raw).canonicalName
        }
        MerchantAnalyticsEngine.computeAll(transactions, categories, resolve)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredMerchants: StateFlow<List<MerchantSummary>> = combine(
        allMerchantSummaries,
        searchQuery,
        sortOrder
    ) { summaries, query, order ->
        val searched = MerchantAnalyticsEngine.search(summaries, query)
        MerchantAnalyticsEngine.sort(searched, order)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearch(query: String) { searchQuery.value = query }
    fun updateSort(order: MerchantSortOrder) { sortOrder.value = order }
}

class MerchantExplorerViewModelFactory(
    private val repository: MoneyRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MerchantExplorerViewModel(repository) as T
    }
}
