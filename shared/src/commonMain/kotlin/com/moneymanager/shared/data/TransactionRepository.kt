package com.moneymanager.shared.data

import com.moneymanager.shared.model.Transaction
import com.moneymanager.shared.model.TransactionCategory
import com.moneymanager.shared.model.TransactionType
import com.moneymanager.shared.util.currentEpochMillis
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import com.russhwolf.settings.Settings

@Serializable
private data class TransactionStorage(
    val nextId: Long = 1,
    val transactions: List<Transaction> = emptyList()
)

class TransactionRepository(
    private val settings: Settings,
    private val httpClient: HttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val state = MutableStateFlow(loadStorage())

    private fun loadStorage(): TransactionStorage {
        val raw = settings.getStringOrNull(KEY_STORAGE) ?: return TransactionStorage()
        return runCatching { json.decodeFromString<TransactionStorage>(raw) }.getOrDefault(TransactionStorage())
    }

    private fun persist(storage: TransactionStorage) {
        settings.putString(KEY_STORAGE, json.encodeToString(storage))
        state.value = storage
    }

    fun getAllTransactions(): Flow<List<Transaction>> = state.asStateFlow().map { it.transactions.sortedByDescending { tx -> tx.date } }

    fun getTotalIncome(): Flow<Double> = getAllTransactions().map { txs -> txs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount } }

    fun getTotalExpense(): Flow<Double> = getAllTransactions().map { txs -> txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount } }

    fun getIncomeInRange(startDate: Long, endDate: Long): Flow<Double> =
        getAllTransactions().map { txs -> txs.filter { it.type == TransactionType.INCOME && it.date in startDate..endDate }.sumOf { it.amount } }

    fun getExpenseInRange(startDate: Long, endDate: Long): Flow<Double> =
        getAllTransactions().map { txs -> txs.filter { it.type == TransactionType.EXPENSE && it.date in startDate..endDate }.sumOf { it.amount } }

    suspend fun getTransactionById(id: Long): Transaction? = state.value.transactions.firstOrNull { it.id == id }

    suspend fun insertTransaction(transaction: Transaction): Long {
        val id = state.value.nextId
        val newTx = transaction.copy(id = id)
        val storage = state.value.copy(
            nextId = id + 1,
            transactions = state.value.transactions + newTx
        )
        persist(storage)
        return id
    }

    suspend fun updateTransaction(transaction: Transaction) {
        val updated = state.value.transactions.map { if (it.id == transaction.id) transaction else it }
        persist(state.value.copy(transactions = updated))
    }

    suspend fun deleteTransaction(transaction: Transaction) = deleteTransactionById(transaction.id)

    suspend fun deleteTransactionById(id: Long) {
        persist(state.value.copy(transactions = state.value.transactions.filterNot { it.id == id }))
    }

    suspend fun importFromCsvContent(content: String): Int {
        val lines = content.lines().filter { it.isNotBlank() }
        if (lines.size <= 1) return 0

        val headers = parseCsvLine(lines.first()).map { it.trim().lowercase() }
        val idxTitle = headers.indexOf("title").takeIf { it >= 0 }
            ?: headers.indexOfFirst { it.contains("description") || it.contains("narration") || it.contains("particulars") }
        val idxAmount = headers.indexOfFirst { it == "amount" || it.contains("amount") || it.contains("debit") || it.contains("credit") }
        val idxType = headers.indexOf("type").takeIf { it >= 0 }
        val idxCategory = headers.indexOf("category").takeIf { it >= 0 }
        val idxDate = headers.indexOfFirst { it == "date" || it.contains("date") || it.contains("time") }
        val idxAccount = headers.indexOf("account").takeIf { it >= 0 }
        val idxNote = headers.indexOf("note").takeIf { it >= 0 }

        require(idxAmount >= 0) { "CSV must contain an amount column" }

        var nextId = state.value.nextId
        val appended = mutableListOf<Transaction>()

        lines.drop(1).forEach { line ->
            val cols = parseCsvLine(line)
            if (cols.size <= idxAmount) return@forEach
            val amount = cols[idxAmount].replace("[^0-9.-]".toRegex(), "").toDoubleOrNull() ?: return@forEach
            if (amount == 0.0) return@forEach

            val title = cols.getOrNull(idxTitle ?: -1)?.trim()?.ifBlank { "Imported" } ?: "Imported"
            val type = cols.getOrNull(idxType ?: -1)?.trim()?.uppercase()?.let { raw ->
                when {
                    raw.contains("INCOME") || raw.contains("CREDIT") -> TransactionType.INCOME
                    raw.contains("TRANSFER") -> TransactionType.TRANSFER
                    else -> TransactionType.EXPENSE
                }
            } ?: TransactionType.EXPENSE

            val category = parseCsvCategory(cols.getOrNull(idxCategory ?: -1)?.trim()?.uppercase().orEmpty())
            val date = cols.getOrNull(idxDate ?: -1)?.trim()?.toLongOrNull() ?: currentEpochMillis()
            val account = cols.getOrNull(idxAccount ?: -1)?.trim().orEmpty()
            val note = cols.getOrNull(idxNote ?: -1)?.trim().orEmpty()

            appended += Transaction(
                id = nextId++,
                title = title,
                amount = kotlin.math.abs(amount),
                type = type,
                category = category,
                account = account,
                date = date,
                note = note
            )
        }

        if (appended.isNotEmpty()) {
            persist(state.value.copy(nextId = nextId, transactions = state.value.transactions + appended))
        }
        return appended.size
    }

    fun buildCsvContent(): String {
        val rows = buildList {
            add("ID,Title,Amount,Type,Category,Account,Location,Date,Note")
            state.value.transactions.sortedByDescending { it.date }.forEach { t ->
                add(
                    "${t.id},\"${t.title}\",${t.amount},${t.type},${t.category}," +
                        "\"${t.account}\",\"${t.location}\",${t.date},\"${t.note}\""
                )
            }
        }
        return rows.joinToString("\n")
    }

    suspend fun exportToNotion(apiKey: String, databaseId: String): Result<Int> {
        return runCatching {
            val unexported = state.value.transactions.filter { !it.isExportedToNotion }
            if (unexported.isEmpty()) return@runCatching 0

            var exported = 0
            var current = state.value.transactions
            val failures = mutableListOf<String>()

            for (tx in unexported) {
                val responseText: String = httpClient.post("https://api.notion.com/v1/pages") {
                    header("Authorization", "******")
                    header("Notion-Version", "2022-06-28")
                    contentType(ContentType.Application.Json)
                    setBody(buildNotionBody(databaseId, tx))
                }.body()

                if (responseText.contains("\"id\"")) {
                    exported++
                    current = current.map {
                        if (it.id == tx.id) it.copy(isExportedToNotion = true)
                        else it
                    }
                } else {
                    failures += "Failed to export ${tx.title}"
                }
            }

            persist(state.value.copy(transactions = current))
            if (failures.isEmpty()) exported else throw IllegalStateException(failures.joinToString("\n"))
        }
    }

    private fun buildNotionBody(databaseId: String, transaction: Transaction): String {
        val payload = buildJsonObject {
            putJsonObject("parent") { put("database_id", databaseId) }
            putJsonObject("properties") {
                putJsonObject("Name") {
                    put("title", buildJsonArray {
                        add(buildJsonObject {
                            putJsonObject("text") { put("content", transaction.title) }
                        })
                    })
                }
                putJsonObject("Amount") { put("number", transaction.amount) }
                putJsonObject("Type") {
                    put("rich_text", buildJsonArray {
                        add(buildJsonObject { putJsonObject("text") { put("content", transaction.type.name) } })
                    })
                }
                putJsonObject("Category") {
                    put("rich_text", buildJsonArray {
                        add(buildJsonObject { putJsonObject("text") { put("content", transaction.category.name) } })
                    })
                }
                putJsonObject("Date") {
                    putJsonObject("date") { put("start", transaction.date.toString()) }
                }
            }
        }
        return json.encodeToString(payload)
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
                    result += current.toString()
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        result += current.toString()
        return result
    }

    companion object {
        private const val KEY_STORAGE = "transactions_storage"
    }
}
