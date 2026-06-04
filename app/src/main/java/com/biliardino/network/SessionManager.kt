package com.biliardino.network

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "session")

class SessionManager(private val context: Context) {
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val BIO_ENABLED_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("biometric_enabled")
        private val THEME_KEY = stringPreferencesKey("theme_preference")
    }

    val authToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }

    val refreshToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[REFRESH_TOKEN_KEY]
    }

    suspend fun saveTokens(jwt: String, refreshToken: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = jwt
            preferences[REFRESH_TOKEN_KEY] = refreshToken
        }
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    val isBiometricEnabled: Flow<Boolean> = context.dataStore.data.map { it[BIO_ENABLED_KEY] ?: false }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[BIO_ENABLED_KEY] = enabled }
    }

    val themePreference: Flow<String> = context.dataStore.data.map { it[THEME_KEY] ?: "SYSTEM" }

    suspend fun setThemePreference(theme: String) {
        context.dataStore.edit { it[THEME_KEY] = theme }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
        }
    }
}
