package com.moneymanager.app.data.sms

import com.moneymanager.app.data.db.entities.Transaction
import com.moneymanager.app.data.db.entities.TransactionCategory
import com.moneymanager.app.data.db.entities.TransactionType
import java.util.regex.Pattern

data class ParsedSms(
    val amount: Double,
    val type: TransactionType,
    val account: String,
    val merchant: String,
    val date: Long
)

object SmsParser {

    private val debitPatterns = listOf(
        Pattern.compile("(?i)debited.*?(?:Rs\\.?|INR|\\$)\\s*([\\d,]+\\.?\\d*)"),
        Pattern.compile("(?i)(?:Rs\\.?|INR|\\$)\\s*([\\d,]+\\.?\\d*).*?(?:debited|deducted|spent|paid)"),
        Pattern.compile("(?i)spent\\s+(?:Rs\\.?|INR|\\$)\\s*([\\d,]+\\.?\\d*)"),
        Pattern.compile("(?i)payment.*?(?:Rs\\.?|INR|\\$)\\s*([\\d,]+\\.?\\d*)"),
        Pattern.compile("(?i)withdrawn.*?(?:Rs\\.?|INR|\\$)\\s*([\\d,]+\\.?\\d*)")
    )

    private val creditPatterns = listOf(
        Pattern.compile("(?i)credited.*?(?:Rs\\.?|INR|\\$)\\s*([\\d,]+\\.?\\d*)"),
        Pattern.compile("(?i)(?:Rs\\.?|INR|\\$)\\s*([\\d,]+\\.?\\d*).*?credited"),
        Pattern.compile("(?i)received.*?(?:Rs\\.?|INR|\\$)\\s*([\\d,]+\\.?\\d*)"),
        Pattern.compile("(?i)salary.*?(?:Rs\\.?|INR|\\$)\\s*([\\d,]+\\.?\\d*)"),
        Pattern.compile("(?i)(?:Rs\\.?|INR|\\$)\\s*([\\d,]+\\.?\\d*).*?received")
    )

    private val accountPattern = Pattern.compile("(?i)(?:a/c|account|acct)\\s*(?:no\\.?|number)?\\s*[xX*]+([\\dA-Za-z]{4})")
    private val merchantPattern = Pattern.compile("(?i)(?:at|to|from|merchant|vendor)\\s+([A-Za-z][A-Za-z0-9\\s]{1,30}?)(?:\\s+on|\\s+for|\\.|,|$)")

    fun parse(smsBody: String, sender: String, timestamp: Long): ParsedSms? {
        val lowerBody = smsBody.lowercase()

        if (lowerBody.contains("otp") || lowerBody.contains("password") ||
            lowerBody.contains("verification") || lowerBody.contains("alert: your")) {
            return null
        }

        var amount: Double? = null
        var type: TransactionType? = null

        for (pattern in debitPatterns) {
            val matcher = pattern.matcher(smsBody)
            if (matcher.find()) {
                amount = matcher.group(1)?.replace(",", "")?.toDoubleOrNull()
                type = TransactionType.EXPENSE
                break
            }
        }

        if (type == null) {
            for (pattern in creditPatterns) {
                val matcher = pattern.matcher(smsBody)
                if (matcher.find()) {
                    amount = matcher.group(1)?.replace(",", "")?.toDoubleOrNull()
                    type = TransactionType.INCOME
                    break
                }
            }
        }

        if (amount == null || type == null || amount <= 0) return null

        val account = accountPattern.matcher(smsBody).let {
            if (it.find()) it.group(1) ?: "Unknown" else extractAccount(sender)
        }

        val merchant = merchantPattern.matcher(smsBody).let {
            if (it.find()) it.group(1)?.trim() ?: sender else sender
        }

        return ParsedSms(
            amount = amount,
            type = type,
            account = account,
            merchant = merchant,
            date = timestamp
        )
    }

    fun parsedSmsToTransaction(parsed: ParsedSms, smsBody: String): Transaction {
        val category = inferCategory(smsBody, parsed.type)
        return Transaction(
            title = parsed.merchant,
            amount = parsed.amount,
            type = parsed.type,
            category = category,
            account = parsed.account,
            date = parsed.date,
            smsSource = smsBody.take(200)
        )
    }

    private fun inferCategory(smsBody: String, type: TransactionType): TransactionCategory {
        if (type == TransactionType.INCOME) {
            return if (smsBody.lowercase().contains("salary")) TransactionCategory.SALARY
            else TransactionCategory.OTHER
        }
        val lower = smsBody.lowercase()
        return when {
            lower.containsAny("zomato", "swiggy", "restaurant", "food", "cafe", "hotel", "dining") -> TransactionCategory.FOOD
            lower.containsAny("uber", "ola", "metro", "bus", "fuel", "petrol", "diesel", "cab") -> TransactionCategory.TRANSPORT
            lower.containsAny("amazon", "flipkart", "myntra", "shopping", "mall", "store") -> TransactionCategory.SHOPPING
            lower.containsAny("netflix", "spotify", "prime", "movie", "cinema", "entertainment") -> TransactionCategory.ENTERTAINMENT
            lower.containsAny("hospital", "pharmacy", "doctor", "medical", "health", "clinic") -> TransactionCategory.HEALTH
            lower.containsAny("electricity", "water", "gas", "internet", "mobile", "recharge", "bill") -> TransactionCategory.UTILITIES
            lower.containsAny("rent", "landlord", "housing") -> TransactionCategory.RENT
            lower.containsAny("transfer", "neft", "imps", "upi") -> TransactionCategory.TRANSFER
            else -> TransactionCategory.OTHER
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it) }

    private fun extractAccount(sender: String): String {
        return sender.replace(Regex("[^A-Za-z0-9]"), "").take(10)
    }
}
