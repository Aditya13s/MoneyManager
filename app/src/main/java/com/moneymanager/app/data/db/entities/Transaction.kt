package com.moneymanager.app.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType { INCOME, EXPENSE, TRANSFER }

enum class TransactionCategory {
    SALARY, FOOD, TRANSPORT, SHOPPING, ENTERTAINMENT,
    HEALTH, UTILITIES, RENT, TRANSFER, OTHER
}

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: TransactionCategory,
    val account: String,
    val location: String = "",
    val date: Long = System.currentTimeMillis(),
    val note: String = "",
    val smsSource: String = "",
    val notionPageId: String = "",
    val isExportedToNotion: Boolean = false
)
