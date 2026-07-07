package com.moneymanager.shared.viewmodel

import com.moneymanager.shared.data.TransactionRepository
import com.moneymanager.shared.data.UserPreferencesRepository
import com.moneymanager.shared.model.Transaction
import com.moneymanager.shared.model.TransactionCategory
import com.moneymanager.shared.model.TransactionType
import com.moneymanager.shared.util.currentEpochMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransactionListState(
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedType: TransactionType? = null,
    val exportMessage: String = "",
    val isExportError: Boolean = false,
    val amountsHidden: Boolean = false,
    val savedNotionApiKey: String = "",
    val savedNotionDatabaseId: String = ""
)

data class TransactionEditState(
    val id: Long = 0,
    val title: String = "",
    val amount: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val category: TransactionCategory = TransactionCategory.OTHER,
    val account: String = "",
    val location: String = "",
    val note: String = "",
    val date: Long = currentEpochMillis(),
    val isNew: Boolean = true
)

class TransactionViewModel(
    private val repository: TransactionRepository,
    private val prefsRepository: UserPreferencesRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _listState = MutableStateFlow(TransactionListState())
    val listState: StateFlow<TransactionListState> = _listState.asStateFlow()

    private val _editState = MutableStateFlow(TransactionEditState())
    val editState: StateFlow<TransactionEditState> = _editState.asStateFlow()

    private val _allTransactions = MutableStateFlow<List<Transaction>>(emptyList())

    init {
        loadTransactions()
        observePreferences()
    }

    private fun observePreferences() {
        scope.launch {
            prefsRepository.amountsHidden.collect { hidden ->
                _listState.update { it.copy(amountsHidden = hidden) }
            }
        }
        scope.launch {
            combine(prefsRepository.notionApiKey, prefsRepository.notionDatabaseId) { key, id -> key to id }
                .collect { (key, id) ->
                    _listState.update { it.copy(savedNotionApiKey = key, savedNotionDatabaseId = id) }
                }
        }
    }

    fun toggleAmountsHidden() {
        scope.launch { prefsRepository.toggleAmountsHidden() }
    }

    fun saveNotionCredentials(apiKey: String, databaseId: String) {
        scope.launch { prefsRepository.saveNotionCredentials(apiKey, databaseId) }
    }

    private fun loadTransactions() {
        scope.launch {
            _listState.update { it.copy(isLoading = true) }
            repository.getAllTransactions().collect { transactions ->
                _allTransactions.value = transactions
                applyFilters()
                _listState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _listState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun setTypeFilter(type: TransactionType?) {
        _listState.update { it.copy(selectedType = type) }
        applyFilters()
    }

    private fun applyFilters() {
        val query = _listState.value.searchQuery.lowercase()
        val type = _listState.value.selectedType
        val filtered = _allTransactions.value.filter { transaction ->
            val matchesQuery = query.isEmpty() ||
                transaction.title.lowercase().contains(query) ||
                transaction.account.lowercase().contains(query) ||
                transaction.category.name.lowercase().contains(query)
            val matchesType = type == null || transaction.type == type
            matchesQuery && matchesType
        }
        _listState.update { it.copy(transactions = filtered) }
    }

    fun loadTransactionForEdit(id: Long) {
        scope.launch {
            repository.getTransactionById(id)?.let { t ->
                _editState.value = TransactionEditState(
                    id = t.id,
                    title = t.title,
                    amount = t.amount.toString(),
                    type = t.type,
                    category = t.category,
                    account = t.account,
                    location = t.location,
                    note = t.note,
                    date = t.date,
                    isNew = false
                )
            }
        }
    }

    fun updateEditField(field: String, value: Any) {
        _editState.update { state ->
            when (field) {
                "title" -> state.copy(title = value as String)
                "amount" -> state.copy(amount = value as String)
                "type" -> state.copy(type = value as TransactionType)
                "category" -> state.copy(category = value as TransactionCategory)
                "account" -> state.copy(account = value as String)
                "location" -> state.copy(location = value as String)
                "note" -> state.copy(note = value as String)
                "date" -> state.copy(date = value as Long)
                else -> state
            }
        }
    }

    fun saveTransaction(onSuccess: () -> Unit, onError: () -> Unit) {
        val state = _editState.value
        val amount = state.amount.toDoubleOrNull()
        if (state.title.isBlank() || amount == null) {
            onError()
            return
        }

        val transaction = Transaction(
            id = if (state.isNew) 0 else state.id,
            title = state.title.trim(),
            amount = amount,
            type = state.type,
            category = state.category,
            account = state.account.trim(),
            location = state.location.trim(),
            note = state.note.trim(),
            date = state.date
        )

        scope.launch {
            if (state.isNew) {
                repository.insertTransaction(transaction)
            } else {
                val existing = repository.getTransactionById(state.id)
                if (existing != null) {
                    repository.updateTransaction(
                        transaction.copy(
                            notionPageId = existing.notionPageId,
                            isExportedToNotion = existing.isExportedToNotion
                        )
                    )
                } else {
                    repository.updateTransaction(transaction)
                }
            }
            onSuccess()
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        scope.launch { repository.deleteTransaction(transaction) }
    }

    fun deleteTransactionById(id: Long) {
        scope.launch { repository.deleteTransactionById(id) }
    }

    fun prepareNewTransaction() {
        _editState.value = TransactionEditState()
    }

    fun buildCsvContent(): String = repository.buildCsvContent()

    fun onCsvExported(path: String) {
        _listState.update { it.copy(exportMessage = "Exported to $path", isExportError = false) }
    }

    fun onCsvExportFailed(error: String) {
        _listState.update { it.copy(exportMessage = "Export failed: $error", isExportError = true) }
    }

    fun exportToNotion(apiKey: String, databaseId: String) {
        scope.launch {
            _listState.update { it.copy(exportMessage = "Exporting to Notion...", isExportError = false) }
            repository.exportToNotion(apiKey, databaseId)
                .onSuccess { count ->
                    val msg = if (count == 0)
                        "No new transactions to export — all transactions are already in Notion"
                    else
                        "Successfully exported $count transaction${if (count == 1) "" else "s"} to Notion"
                    _listState.update { it.copy(exportMessage = msg, isExportError = false) }
                }
                .onFailure { e ->
                    _listState.update { it.copy(exportMessage = "Notion export failed: ${e.message}", isExportError = true) }
                }
        }
    }
}
