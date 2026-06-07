package com.starklabs.moneytracker.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory holder for the active [TransactionFilter], shared by the History
 * and Analytics ViewModels so filters persist while navigating between them
 * within a session (instantiated once in MainActivity, mirroring how
 * [com.starklabs.moneytracker.data.MoneyRepository] is shared as a singleton).
 *
 * Deliberately not persisted to disk — filters are a working-session concern,
 * not user data, so this avoids any DataStore/Room schema additions.
 */
class TransactionFilterStore {
    private val _filter = MutableStateFlow(TransactionFilter())
    val filter: StateFlow<TransactionFilter> = _filter.asStateFlow()

    fun update(filter: TransactionFilter) {
        _filter.value = filter
    }

    fun clear() {
        _filter.value = TransactionFilter()
    }

    fun clearDimension(dimension: FilterDimension) {
        _filter.value = TransactionFilterEngine.clearing(_filter.value, dimension)
    }
}
