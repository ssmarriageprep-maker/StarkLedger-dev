package com.starklabs.moneytracker.ui.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.starklabs.moneytracker.data.SecurityRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SecurityViewModel(private val repository: SecurityRepository) : ViewModel() {

    val isPinSet: StateFlow<Boolean?> = repository.pinFlow
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun savePin(pin: String) {
        viewModelScope.launch {
            repository.savePin(pin)
        }
    }

    suspend fun verifyPin(inputPin: String): Boolean {
        return repository.verifyPin(inputPin)
    }
}

class SecurityViewModelFactory(private val repository: SecurityRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SecurityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SecurityViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
