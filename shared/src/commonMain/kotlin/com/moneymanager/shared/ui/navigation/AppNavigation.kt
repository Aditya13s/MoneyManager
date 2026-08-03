package com.moneymanager.shared.ui.navigation

sealed class Screen {
    data object Dashboard : Screen()
    data object TransactionList : Screen()
    data class TransactionDetail(val id: Long) : Screen()
    data object Export : Screen()
}
