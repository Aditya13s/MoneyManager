package com.moneymanager.shared.model

import kotlinx.serialization.Serializable

@Serializable
enum class TransactionType { INCOME, EXPENSE, TRANSFER }

@Serializable
enum class TransactionCategory {
    SALARY, FOOD, TRANSPORT, SHOPPING, ENTERTAINMENT,
    HEALTH, UTILITIES, RENT, TRANSFER, OTHER
}

@Serializable
data class Transaction(
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: TransactionCategory,
    val account: String,
    val location: String = "",
    val date: Long,
    val note: String = "",
    val smsSource: String = "",
    val notionPageId: String = "",
    val isExportedToNotion: Boolean = false
)
