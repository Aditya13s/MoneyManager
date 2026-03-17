package com.moneymanager.app.ui.util

import androidx.compose.ui.graphics.Color
import com.moneymanager.app.data.db.entities.TransactionCategory
import com.moneymanager.app.ui.theme.*

/** Returns the emoji icon for a [TransactionCategory] for use in the UI. */
fun TransactionCategory.emoji(): String = when (this) {
    TransactionCategory.SALARY        -> "💼"
    TransactionCategory.FOOD          -> "🍔"
    TransactionCategory.TRANSPORT     -> "🚌"
    TransactionCategory.SHOPPING      -> "🛍️"
    TransactionCategory.ENTERTAINMENT -> "🎬"
    TransactionCategory.HEALTH        -> "💊"
    TransactionCategory.UTILITIES     -> "💡"
    TransactionCategory.RENT          -> "🏠"
    TransactionCategory.TRANSFER      -> "🔄"
    TransactionCategory.OTHER         -> "📦"
}

/** Returns the accent [Color] for a [TransactionCategory] badge. */
fun TransactionCategory.badgeColor(): Color = when (this) {
    TransactionCategory.SALARY        -> CategorySalaryColor
    TransactionCategory.FOOD          -> CategoryFoodColor
    TransactionCategory.TRANSPORT     -> CategoryTransportColor
    TransactionCategory.SHOPPING      -> CategoryShoppingColor
    TransactionCategory.ENTERTAINMENT -> CategoryEntertainmentColor
    TransactionCategory.HEALTH        -> CategoryHealthColor
    TransactionCategory.UTILITIES     -> CategoryUtilitiesColor
    TransactionCategory.RENT          -> CategoryRentColor
    TransactionCategory.TRANSFER      -> CategoryTransferColor
    TransactionCategory.OTHER         -> CategoryOtherColor
}

/** Converts a category name (e.g. "FOOD") to a user-friendly title (e.g. "Food"). */
fun String.toCategoryTitle(): String = lowercase().replaceFirstChar { it.uppercase() }
