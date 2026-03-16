package com.moneymanager.app.data.sms

import android.content.Context
import android.database.Cursor
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class SmsMessage(
    val id: String,
    val sender: String,
    val body: String,
    val timestamp: Long
)

@Singleton
class SmsReader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun readFinancialSms(limit: Int = 500): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("_id", "address", "body", "date")

        val bankSenders = listOf(
            "HDFCBK", "ICICIBK", "SBIINB", "AXISBK", "KOTAKBK",
            "INDBNK", "PAYTM", "GPAY", "PHONEPE", "AMAZONPAY",
            "YESBNK", "PNBSMS", "BOBIBNK", "UNIONBK", "CANBNK",
            "SCBL", "CITIBNK", "HSBC", "CREDCLUB", "IDFCFB"
        )

        try {
            val cursor: Cursor? = context.contentResolver.query(
                uri, projection, null, null, "date DESC LIMIT $limit"
            )
            cursor?.use { c ->
                val idIdx = c.getColumnIndexOrThrow("_id")
                val addressIdx = c.getColumnIndexOrThrow("address")
                val bodyIdx = c.getColumnIndexOrThrow("body")
                val dateIdx = c.getColumnIndexOrThrow("date")

                while (c.moveToNext()) {
                    val sender = c.getString(addressIdx) ?: continue
                    val body = c.getString(bodyIdx) ?: continue

                    val isFinancial = bankSenders.any {
                        sender.uppercase().contains(it)
                    } || isFinancialMessage(body)

                    if (isFinancial) {
                        messages.add(
                            SmsMessage(
                                id = c.getString(idIdx),
                                sender = sender,
                                body = body,
                                timestamp = c.getLong(dateIdx)
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // SMS content provider not available or permission not granted
        }

        return messages
    }

    private fun isFinancialMessage(body: String): Boolean {
        val lower = body.lowercase()
        return (lower.contains("debited") || lower.contains("credited") ||
                lower.contains("transaction") || lower.contains("payment") ||
                lower.contains("salary") || lower.contains("withdrawn")) &&
               (lower.contains("rs.") || lower.contains("inr") || lower.contains("₹"))
    }
}
