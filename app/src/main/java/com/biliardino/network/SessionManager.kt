package com.biliardino.network

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "session")

class SessionManager(private val context: Context) {
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private const val SECURE_PREFS_NAME = "secure_session"
        private const val EMAIL_KEY = "user_email"
        private const val PASSWORD_KEY = "user_password"
        private val BIO_ENABLED_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("biometric_enabled")
        private val THEME_KEY = stringPreferencesKey("theme_preference")
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs = EncryptedSharedPreferences.create(
        context,
        SECURE_PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    val authToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    fun saveCredentials(email: String, password: String) {
        securePrefs.edit()
            .putString(EMAIL_KEY, email)
            .putString(PASSWORD_KEY, password)
            .apply()
    }

    fun getCredentials(): Pair<String?, String?> {
        return securePrefs.getString(EMAIL_KEY, null) to securePrefs.getString(PASSWORD_KEY, null)
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
        }
        // Non cancelliamo le credenziali qui per permettere il login biometrico successivo.
        // Se l'utente vuole cambiare account, le nuove credenziali sovrascriveranno le vecchie al login.
    }
}
