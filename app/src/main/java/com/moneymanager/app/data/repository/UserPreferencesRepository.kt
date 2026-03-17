package com.moneymanager.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
}
