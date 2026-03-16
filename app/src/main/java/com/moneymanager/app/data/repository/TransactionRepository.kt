package com.moneymanager.app.data.repository

import android.content.Context
import com.google.gson.JsonObject
import com.moneymanager.app.data.db.TransactionDao
import com.moneymanager.app.data.db.entities.Transaction
import com.moneymanager.app.data.db.entities.TransactionType
import com.moneymanager.app.data.network.NotionApiService
import com.moneymanager.app.data.sms.SmsReader
import com.moneymanager.app.data.sms.SmsParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileWriter
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
            var exportedCount = 0

            for (transaction in unexported) {
                val body = buildNotionPageBody(databaseId, transaction)
                val response = notionApiService.createPage(
                    token = "Bearer $apiKey",
                    body = body
                )

                if (response.isSuccessful) {
                    val pageId = response.body()?.get("id")?.asString ?: ""
                    transactionDao.updateTransaction(
                        transaction.copy(notionPageId = pageId, isExportedToNotion = true)
                    )
                    exportedCount++
                }
            }
            Result.success(exportedCount)
        } catch (e: Exception) {
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
