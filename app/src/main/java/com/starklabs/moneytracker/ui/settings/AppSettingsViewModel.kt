package com.starklabs.moneytracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.starklabs.moneytracker.data.AppSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppSettingsViewModel(private val repository: AppSettingsRepository) : ViewModel() {

    val isFirstLaunch: StateFlow<Boolean> = repository.isFirstLaunch
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dashboardLogCount: StateFlow<Int> = repository.dashboardLogCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 10)

    fun setFirstLaunch(isFirst: Boolean) {
        viewModelScope.launch {
            repository.setFirstLaunch(isFirst)
        }
    }

    fun setDashboardLogCount(count: Int) {
        viewModelScope.launch {
            repository.setDashboardLogCount(count)
        }
    }
}

class AppSettingsViewModelFactory(private val repository: AppSettingsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppSettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppSettingsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
