package com.moneymanager.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val AMOUNTS_HIDDEN = booleanPreferencesKey("amounts_hidden")
        private val NOTION_API_KEY = stringPreferencesKey("notion_api_key")
        private val NOTION_DATABASE_ID = stringPreferencesKey("notion_database_id")
        private val SAVED_ACCOUNTS = stringSetPreferencesKey("saved_accounts")
        private val ONBOARDING_SHOWN = booleanPreferencesKey("onboarding_shown")
    }

    val amountsHidden: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AMOUNTS_HIDDEN] ?: false
    }

    val notionApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[NOTION_API_KEY] ?: ""
    }

    val notionDatabaseId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[NOTION_DATABASE_ID] ?: ""
    }

    /** Saved account names, sorted alphabetically. Backed up via Android Auto Backup. */
    val savedAccounts: Flow<List<String>> = context.dataStore.data.map { prefs ->
        (prefs[SAVED_ACCOUNTS] ?: emptySet()).sorted()
    }

    /** Whether the onboarding screen has already been shown. */
    val isOnboardingShown: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ONBOARDING_SHOWN] ?: false
    }

    suspend fun toggleAmountsHidden() {
        context.dataStore.edit { prefs ->
            prefs[AMOUNTS_HIDDEN] = !(prefs[AMOUNTS_HIDDEN] ?: false)
        }
    }

    suspend fun saveNotionCredentials(apiKey: String, databaseId: String) {
        context.dataStore.edit { prefs ->
            prefs[NOTION_API_KEY] = apiKey
            prefs[NOTION_DATABASE_ID] = databaseId
        }
    }

    suspend fun addAccount(account: String) {
        val trimmed = account.trim()
        if (trimmed.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = prefs[SAVED_ACCOUNTS] ?: emptySet()
            prefs[SAVED_ACCOUNTS] = current + trimmed
        }
    }

    suspend fun removeAccount(account: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[SAVED_ACCOUNTS] ?: emptySet()
            prefs[SAVED_ACCOUNTS] = current - account
        }
    }

    suspend fun setOnboardingShown() {
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_SHOWN] = true
        }
    }
}
