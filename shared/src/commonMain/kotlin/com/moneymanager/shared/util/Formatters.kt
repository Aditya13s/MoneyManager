package com.moneymanager.shared.util

import kotlin.math.abs
import kotlin.math.round
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun formatCurrency(amount: Double): String {
    val rounded = round(amount * 100.0) / 100.0
    val absolute = abs(rounded)
    val parts = absolute.toString().split('.')
    val whole = parts.first().reversed().chunked(3).joinToString(",").reversed()
    val decimal = parts.getOrNull(1)?.padEnd(2, '0')?.take(2) ?: "00"
    return "₹$whole.$decimal"
}

fun currentEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()

fun formatDate(epochMillis: Long): String {
    val local = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    val day = local.dayOfMonth.toString().padStart(2, '0')
    val month = local.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    return "$day $month ${local.year}"
}

fun formatMonthYearNow(): String {
    val local = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val month = local.month.name.lowercase().replaceFirstChar { it.uppercase() }
    return "$month ${local.year}"
}
