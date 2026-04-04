package com.starklabs.moneytracker.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appDataStore by preferencesDataStore("app_settings")

class AppSettingsRepository(private val context: Context) {

    companion object {
        private val FIRST_LAUNCH_KEY = booleanPreferencesKey("is_first_launch")
        private val DASHBOARD_LOG_COUNT_KEY = intPreferencesKey("dashboard_log_count")
        private val SELECTED_ACCOUNT_ID_KEY = intPreferencesKey("selected_account_id")
    }

    val isFirstLaunch: Flow<Boolean> = context.appDataStore.data.map { preferences ->
        preferences[FIRST_LAUNCH_KEY] ?: true
    }

    val dashboardLogCount: Flow<Int> = context.appDataStore.data.map { preferences ->
        preferences[DASHBOARD_LOG_COUNT_KEY] ?: 10
    }

    val selectedAccountId: Flow<Int> = context.appDataStore.data.map { preferences ->
        preferences[SELECTED_ACCOUNT_ID_KEY] ?: -1
    }

    suspend fun setFirstLaunch(isFirst: Boolean) {
        context.appDataStore.edit { preferences ->
            preferences[FIRST_LAUNCH_KEY] = isFirst
        }
    }

    suspend fun setDashboardLogCount(count: Int) {
        context.appDataStore.edit { preferences ->
            preferences[DASHBOARD_LOG_COUNT_KEY] = count
        }
    }

    suspend fun setSelectedAccountId(id: Int) {
        context.appDataStore.edit { preferences ->
            preferences[SELECTED_ACCOUNT_ID_KEY] = id
        }
    }
}
