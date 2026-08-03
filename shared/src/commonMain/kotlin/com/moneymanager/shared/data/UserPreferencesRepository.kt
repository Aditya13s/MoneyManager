package com.moneymanager.shared.data

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferencesRepository(private val settings: Settings) {
    private val amountsHiddenState = MutableStateFlow(settings.getBoolean(KEY_AMOUNTS_HIDDEN, false))
    private val notionApiKeyState = MutableStateFlow(settings.getString(KEY_NOTION_API_KEY, ""))
    private val notionDatabaseIdState = MutableStateFlow(settings.getString(KEY_NOTION_DATABASE_ID, ""))

    val amountsHidden: Flow<Boolean> = amountsHiddenState.asStateFlow()
    val notionApiKey: Flow<String> = notionApiKeyState.asStateFlow()
    val notionDatabaseId: Flow<String> = notionDatabaseIdState.asStateFlow()

    suspend fun toggleAmountsHidden() {
        val newValue = !amountsHiddenState.value
        settings.putBoolean(KEY_AMOUNTS_HIDDEN, newValue)
        amountsHiddenState.value = newValue
    }

    suspend fun saveNotionCredentials(apiKey: String, databaseId: String) {
        settings.putString(KEY_NOTION_API_KEY, apiKey)
        settings.putString(KEY_NOTION_DATABASE_ID, databaseId)
        notionApiKeyState.value = apiKey
        notionDatabaseIdState.value = databaseId
    }

    companion object {
        private const val KEY_AMOUNTS_HIDDEN = "amounts_hidden"
        private const val KEY_NOTION_API_KEY = "notion_api_key"
        private const val KEY_NOTION_DATABASE_ID = "notion_database_id"
    }
}
