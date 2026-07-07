package com.moneymanager.shared.util

import androidx.compose.ui.graphics.Color
import com.moneymanager.shared.model.TransactionCategory
import com.moneymanager.shared.ui.theme.*

fun TransactionCategory.emoji(): String = when (this) {
    TransactionCategory.SALARY -> "💼"
    TransactionCategory.FOOD -> "🍔"
    TransactionCategory.TRANSPORT -> "🚌"
    TransactionCategory.SHOPPING -> "🛍️"
    TransactionCategory.ENTERTAINMENT -> "🎬"
    TransactionCategory.HEALTH -> "💊"
    TransactionCategory.UTILITIES -> "💡"
    TransactionCategory.RENT -> "🏠"
    TransactionCategory.TRANSFER -> "🔄"
    TransactionCategory.OTHER -> "📦"
}

fun TransactionCategory.badgeColor(): Color = when (this) {
    TransactionCategory.SALARY -> CategorySalaryColor
    TransactionCategory.FOOD -> CategoryFoodColor
    TransactionCategory.TRANSPORT -> CategoryTransportColor
    TransactionCategory.SHOPPING -> CategoryShoppingColor
    TransactionCategory.ENTERTAINMENT -> CategoryEntertainmentColor
    TransactionCategory.HEALTH -> CategoryHealthColor
    TransactionCategory.UTILITIES -> CategoryUtilitiesColor
    TransactionCategory.RENT -> CategoryRentColor
    TransactionCategory.TRANSFER -> CategoryTransferColor
    TransactionCategory.OTHER -> CategoryOtherColor
}

fun String.toCategoryTitle(): String = lowercase().replaceFirstChar { it.uppercase() }
