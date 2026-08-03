package com.moneymanager.shared.data

class AppContainer(
    val transactionRepository: TransactionRepository,
    val preferencesRepository: UserPreferencesRepository
)

fun createAppContainer(): AppContainer {
    val settings = platformSettings()
    val httpClient = platformHttpClient()
    return AppContainer(
        transactionRepository = TransactionRepository(settings, httpClient),
        preferencesRepository = UserPreferencesRepository(settings)
    )
}
