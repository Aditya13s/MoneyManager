package com.moneymanager.shared.data

import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

actual fun platformSettings(): Settings = Settings()

actual fun platformHttpClient(): HttpClient = HttpClient(OkHttp)
