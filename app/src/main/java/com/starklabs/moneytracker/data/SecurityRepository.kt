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

        /**
         * One-way SHA-256 hash so the raw PIN is never stored.
         */
        private fun hashPin(pin: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    val pinFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PIN_KEY]
    }

    /**
     * Stores the SHA-256 hash of the PIN, never the raw value.
     */
    suspend fun savePin(pin: String) {
        context.dataStore.edit { preferences ->
            preferences[PIN_KEY] = hashPin(pin)
        }
    }

    /**
     * Verifies an input PIN against the stored hash.
     * Returns true if the PIN matches, false otherwise (including when no PIN is set).
     */
    suspend fun verifyPin(inputPin: String): Boolean {
        val storedHash = context.dataStore.data
            .map { it[PIN_KEY] }
            .first()
        return storedHash != null && storedHash == hashPin(inputPin)
    }
}
