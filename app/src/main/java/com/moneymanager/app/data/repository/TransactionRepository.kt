package com.moneymanager.app.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.JsonObject
import com.moneymanager.app.data.db.TransactionDao
import com.moneymanager.app.data.db.entities.Transaction
import com.moneymanager.app.data.db.entities.TransactionCategory
import com.moneymanager.app.data.db.entities.TransactionType
import com.moneymanager.app.data.network.NotionApiService
import com.moneymanager.app.data.sms.SmsReader
import com.moneymanager.app.data.sms.SmsParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val smsReader: SmsReader,
    private val notionApiService: NotionApiService,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "NotionExport"
    }

    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> =
        transactionDao.getTransactionsByType(type)

    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsByDateRange(startDate, endDate)

    fun getTotalIncome(): Flow<Double?> = transactionDao.getTotalIncome()

    fun getTotalExpense(): Flow<Double?> = transactionDao.getTotalExpense()

    fun getIncomeInRange(startDate: Long, endDate: Long): Flow<Double?> =
        transactionDao.getIncomeInRange(startDate, endDate)

    fun getExpenseInRange(startDate: Long, endDate: Long): Flow<Double?> =
        transactionDao.getExpenseInRange(startDate, endDate)

    suspend fun getTransactionById(id: Long): Transaction? = transactionDao.getTransactionById(id)

    suspend fun insertTransaction(transaction: Transaction): Long =
        transactionDao.insertTransaction(transaction)

    suspend fun updateTransaction(transaction: Transaction) =
        transactionDao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: Transaction) =
        transactionDao.deleteTransaction(transaction)

    suspend fun deleteTransactionById(id: Long) =
        transactionDao.deleteTransactionById(id)

    suspend fun syncSmsTransactions(): Int {
        val smsList = smsReader.readFinancialSms()
        var newCount = 0

        for (sms in smsList) {
            val existing = transactionDao.countBySmsSource(sms.body.take(200))
            if (existing == 0) {
                val parsed = SmsParser.parse(sms.body, sms.sender, sms.timestamp)
                if (parsed != null) {
                    val transaction = SmsParser.parsedSmsToTransaction(parsed, sms.body)
                    transactionDao.insertTransaction(transaction)
                    newCount++
                }
            }
        }
        return newCount
    }

    suspend fun importFromCsv(uri: Uri): Int {
        val transactions = mutableListOf<Transaction>()
        val dateFormats = listOf(
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
            SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        )

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                val headerLine = reader.readLine() ?: return 0
                val headers = parseCsvLine(headerLine).map { it.trim().lowercase() }

                val idxTitle    = headers.indexOf("title").takeIf { it >= 0 }
                    ?: headers.indexOfFirst { it.contains("description") || it.contains("narration") || it.contains("particulars") }
                val idxAmount   = headers.indexOfFirst { it == "amount" || it.contains("amount") || it.contains("debit") || it.contains("credit") }
                val idxType     = headers.indexOf("type").takeIf { it >= 0 }
                val idxCategory = headers.indexOf("category").takeIf { it >= 0 }
                val idxDate     = headers.indexOfFirst { it == "date" || it.contains("date") || it.contains("time") }
                val idxAccount  = headers.indexOf("account").takeIf { it >= 0 }
                val idxNote     = headers.indexOf("note").takeIf { it >= 0 }

                if (idxAmount < 0) throw IllegalArgumentException("CSV must contain an amount column")

                reader.lineSequence().forEach { rawLine ->
                    val line = rawLine.trim()
                    if (line.isBlank()) return@forEach
                    val cols = parseCsvLine(line)
                    if (cols.size <= idxAmount) return@forEach

                    val amountStr = cols.getOrNull(idxAmount)?.trim() ?: return@forEach
                    val amount = amountStr.replace("[^0-9.-]".toRegex(), "").toDoubleOrNull() ?: return@forEach
                    if (amount == 0.0) return@forEach

                    val title = cols.getOrNull(idxTitle ?: -1)?.trim()?.ifBlank { "Imported" } ?: "Imported"
                    val typeStr = cols.getOrNull(idxType ?: -1)?.trim()?.uppercase() ?: ""
                    val type = when {
                        typeStr.contains("INCOME") || typeStr.contains("CREDIT") -> TransactionType.INCOME
                        typeStr.contains("TRANSFER") -> TransactionType.TRANSFER
                        else -> TransactionType.EXPENSE
                    }
                    val categoryStr = cols.getOrNull(idxCategory ?: -1)?.trim()?.uppercase() ?: ""
                    val category = parseCsvCategory(categoryStr)

                    val dateStr = cols.getOrNull(idxDate ?: -1)?.trim() ?: ""
                    val date = dateFormats.firstNotNullOfOrNull { fmt ->
                        runCatching { fmt.parse(dateStr)?.time }.getOrNull()
                    } ?: System.currentTimeMillis()

                    val account = cols.getOrNull(idxAccount ?: -1)?.trim() ?: ""
                    val note = cols.getOrNull(idxNote ?: -1)?.trim() ?: ""

                    transactions.add(
                        Transaction(
                            title = title,
                            amount = kotlin.math.abs(amount),
                            type = type,
                            category = category,
                            account = account,
                            date = date,
                            note = note
                        )
                    )
                }
            }
        }

        if (transactions.isNotEmpty()) {
            transactionDao.insertTransactions(transactions)
        }
        return transactions.size
    }

    private fun parseCsvCategory(value: String): TransactionCategory {
        if (value.isBlank()) return TransactionCategory.OTHER
        runCatching { TransactionCategory.valueOf(value) }.getOrNull()?.let { return it }
        return when {
            value.contains("FOOD") || value.contains("DINE") || value.contains("RESTAURANT") || value.contains("GROCERY") -> TransactionCategory.FOOD
            value.contains("TRANSPORT") || value.contains("TRAVEL") || value.contains("CAB") || value.contains("FUEL") -> TransactionCategory.TRANSPORT
            value.contains("SHOP") || value.contains("RETAIL") || value.contains("PURCHASE") -> TransactionCategory.SHOPPING
            value.contains("ENTERTAIN") || value.contains("MOVIE") || value.contains("SUBSCRI") -> TransactionCategory.ENTERTAINMENT
            value.contains("HEALTH") || value.contains("MEDICAL") || value.contains("PHARMA") -> TransactionCategory.HEALTH
            value.contains("UTIL") || value.contains("ELECTRIC") || value.contains("WATER") || value.contains("BILL") -> TransactionCategory.UTILITIES
            value.contains("RENT") || value.contains("HOUSE") -> TransactionCategory.RENT
            value.contains("SALARY") || value.contains("PAYROLL") -> TransactionCategory.SALARY
            value.contains("TRANSFER") -> TransactionCategory.TRANSFER
            else -> TransactionCategory.OTHER
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && !inQuotes -> inQuotes = true
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                c == '"' && inQuotes -> inQuotes = false
                c == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }

    suspend fun exportToCsv(): File {
        val transactions = transactionDao.getAllTransactions().first()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val fileName = "transactions_${System.currentTimeMillis()}.csv"
        val file = File(context.getExternalFilesDir(null), fileName)

        FileWriter(file).use { writer ->
            writer.write("ID,Title,Amount,Type,Category,Account,Location,Date,Note\n")
            transactions.forEach { t ->
                writer.write(
                    "${t.id},\"${t.title}\",${t.amount},${t.type},${t.category}," +
                    "\"${t.account}\",\"${t.location}\",\"${dateFormat.format(Date(t.date))}\",\"${t.note}\"\n"
                )
            }
        }
        return file
    }

    suspend fun exportToNotion(apiKey: String, databaseId: String): Result<Int> {
        return try {
            val unexported = transactionDao.getUnexportedTransactions()
            Log.i(TAG, "Starting Notion export: ${unexported.size} transaction(s) to export")

            if (unexported.isEmpty()) {
                Log.i(TAG, "No unexported transactions found")
                return Result.success(0)
            }

            var exportedCount = 0
            val errors = mutableListOf<String>()

            for (transaction in unexported) {
                Log.d(TAG, "Exporting transaction #${transaction.id}: '${transaction.title}' " +
                    "(${transaction.type}, amount=${transaction.amount})")
                val body = buildNotionPageBody(databaseId, transaction)
                try {
                    val response = notionApiService.createPage(
                        token = "Bearer $apiKey",
                        body = body
                    )

                    if (response.isSuccessful) {
                        val pageId = response.body()?.get("id")?.asString ?: ""
                        Log.d(TAG, "Transaction #${transaction.id} exported successfully, pageId=$pageId")
                        transactionDao.updateTransaction(
                            transaction.copy(notionPageId = pageId, isExportedToNotion = true)
                        )
                        exportedCount++
                    } else {
                        val errorBody = (response.errorBody()?.string() ?: "").take(500)
                        val titlePreview = transaction.title.take(50)
                        Log.e(TAG, "Notion API error for transaction #${transaction.id} " +
                            "'${transaction.title}': HTTP ${response.code()}, body=$errorBody")
                        val message = when (response.code()) {
                            400 -> "Bad request (400) for '$titlePreview': check database ID and column names. Details: $errorBody"
                            401 -> "Unauthorized (401): verify your Notion API key. Details: $errorBody"
                            403 -> "Forbidden (403): make sure your integration is connected to the database. Details: $errorBody"
                            404 -> "Database not found (404): verify the database ID. Details: $errorBody"
                            else -> "Notion API error ${response.code()} for '$titlePreview': $errorBody"
                        }
                        errors.add(message)
                        // Auth errors affect all transactions — stop immediately
                        if (response.code() == 401 || response.code() == 403) {
                            Log.e(TAG, "Stopping export early due to auth error (${response.code()})")
                            break
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Network error exporting transaction #${transaction.id}", e)
                    errors.add("Network error for '${transaction.title}': ${e.message}")
                }
            }

            Log.i(TAG, "Notion export complete: $exportedCount succeeded, ${errors.size} failed")

            when {
                errors.isEmpty() -> Result.success(exportedCount)
                exportedCount > 0 -> {
                    val errorDetail = errors.joinToString("\n• ", prefix = "\n• ")
                    Result.failure(Exception("Exported $exportedCount of ${unexported.size} transaction(s). Failures:$errorDetail"))
                }
                else -> Result.failure(Exception(errors.joinToString("\n")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Notion export", e)
            Result.failure(e)
        }
    }

    private fun buildNotionPageBody(databaseId: String, transaction: Transaction): JsonObject {
        val body = JsonObject()
        val parent = JsonObject().apply { addProperty("database_id", databaseId) }
        body.add("parent", parent)

        val properties = JsonObject()

        val titleContent = JsonObject()
        val titleArray = com.google.gson.JsonArray()
        val titleText = JsonObject().apply {
            add("text", JsonObject().apply { addProperty("content", transaction.title) })
        }
        titleArray.add(titleText)
        titleContent.add("title", titleArray)
        properties.add("Name", titleContent)

        val amountProp = JsonObject().apply { addProperty("number", transaction.amount) }
        properties.add("Amount", amountProp)

        val typeProp = JsonObject()
        typeProp.add("select", JsonObject().apply { addProperty("name", transaction.type.name) })
        properties.add("Type", typeProp)

        val categoryProp = JsonObject()
        categoryProp.add("select", JsonObject().apply { addProperty("name", transaction.category.name) })
        properties.add("Category", categoryProp)

        val dateProp = JsonObject()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        dateProp.add("date", JsonObject().apply { addProperty("start", sdf.format(Date(transaction.date))) })
        properties.add("Date", dateProp)

        body.add("properties", properties)
        return body
    }
}
