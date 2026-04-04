package com.starklabs.moneytracker.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore by preferencesDataStore("security_settings")

class SecurityRepository(private val context: Context) {

    companion object {
        private val PIN_KEY = stringPreferencesKey("user_pin")
    }

    val pinFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PIN_KEY]
    }

    suspend fun savePin(pin: String) {
        val hashedPin = hashPin(pin)
        context.dataStore.edit { preferences ->
            preferences[PIN_KEY] = hashedPin
        }
    }

    suspend fun verifyPin(inputPin: String): Boolean {
        val preferences = context.dataStore.data.first()
        val storedPin = preferences[PIN_KEY] ?: return false

        // 1. Check if it matches as a salted hash (new format)
        val hashedInput = hashPin(inputPin)
        if (storedPin == hashedInput) return true

        // 2. Migration: Check if it matches as plaintext (old format - 4 digits)
        if (storedPin.length == 4 && storedPin == inputPin) {
            // Automatically migrate to salted hash
            savePin(inputPin)
            return true
        }

        return false
    }

    private fun hashPin(pin: String): String {
        val salt = "STARK_LEDGER_2024_SALT" // Static salt to prevent simple rainbow tables
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((salt + pin).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
