package com.moneymanager.shared.data

import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient

expect fun platformSettings(): Settings
expect fun platformHttpClient(): HttpClient
