package com.moneymanager.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.app.data.db.entities.AccountType
import com.moneymanager.app.data.db.entities.Transaction
import com.moneymanager.app.data.db.entities.TransactionCategory
import com.moneymanager.app.data.db.entities.TransactionType
import com.moneymanager.app.data.repository.TransactionRepository
import com.moneymanager.app.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionListState(
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedType: TransactionType? = null,
    val exportMessage: String = "",
    val isExportError: Boolean = false,
    val amountsHidden: Boolean = false,
    val savedNotionApiKey: String = "",
    val savedNotionDatabaseId: String = "",
    val savedAccounts: List<String> = emptyList()
)

data class TransactionEditState(
    val id: Long = 0,
    val title: String = "",
    val amount: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val category: TransactionCategory = TransactionCategory.OTHER,
    val account: String = "",
    val accountType: AccountType = AccountType.BANK,
    val location: String = "",
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val isNew: Boolean = true
)

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {

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
        viewModelScope.launch {
            prefsRepository.amountsHidden.collect { hidden ->
                _listState.update { it.copy(amountsHidden = hidden) }
            }
        }
        viewModelScope.launch {
            combine(prefsRepository.notionApiKey, prefsRepository.notionDatabaseId) { key, id ->
                Pair(key, id)
            }.collect { (key, id) ->
                _listState.update { it.copy(savedNotionApiKey = key, savedNotionDatabaseId = id) }
            }
        }
        viewModelScope.launch {
            prefsRepository.savedAccounts.collect { accounts ->
                _listState.update { it.copy(savedAccounts = accounts) }
            }
        }
    }

    fun toggleAmountsHidden() {
        viewModelScope.launch {
            prefsRepository.toggleAmountsHidden()
        }
    }

    fun saveNotionCredentials(apiKey: String, databaseId: String) {
        viewModelScope.launch {
            prefsRepository.saveNotionCredentials(apiKey, databaseId)
        }
    }

    fun addSavedAccount(account: String) {
        viewModelScope.launch {
            prefsRepository.addAccount(account)
        }
    }

    fun removeSavedAccount(account: String) {
        viewModelScope.launch {
            prefsRepository.removeAccount(account)
        }
    }

    private fun loadTransactions() {
        viewModelScope.launch {
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
        viewModelScope.launch {
            val transaction = repository.getTransactionById(id)
            transaction?.let { t ->
                _editState.value = TransactionEditState(
                    id = t.id,
                    title = t.title,
                    amount = t.amount.toString(),
                    type = t.type,
                    category = t.category,
                    account = t.account,
                    accountType = t.accountType,
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
                "accountType" -> state.copy(accountType = value as AccountType)
                "location" -> state.copy(location = value as String)
                "note" -> state.copy(note = value as String)
                "date" -> state.copy(date = value as Long)
                else -> state
            }
        }
    }

    fun saveTransaction(): Boolean {
        val state = _editState.value
        val amount = state.amount.toDoubleOrNull() ?: return false
        if (state.title.isBlank()) return false

        val transaction = Transaction(
            id = if (state.isNew) 0 else state.id,
            title = state.title.trim(),
            amount = amount,
            type = state.type,
            category = state.category,
            account = state.account.trim(),
            accountType = state.accountType,
            location = state.location.trim(),
            note = state.note.trim(),
            date = state.date
        )

        viewModelScope.launch {
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
            // Auto-save non-empty account names for future suggestions
            if (state.account.isNotBlank()) {
                prefsRepository.addAccount(state.account.trim())
            }
        }
        return true
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun deleteTransactionById(id: Long) {
        viewModelScope.launch {
            repository.deleteTransactionById(id)
        }
    }

    fun prepareNewTransaction() {
        _editState.value = TransactionEditState()
    }

    fun exportToCsv() {
        viewModelScope.launch {
            try {
                val file = repository.exportToCsv()
                _listState.update { it.copy(exportMessage = "Exported to ${file.absolutePath}") }
            } catch (e: Exception) {
                _listState.update { it.copy(exportMessage = "Export failed: ${e.message}") }
            }
        }
    }

    fun exportToNotion(apiKey: String, databaseId: String) {
        viewModelScope.launch {
            _listState.update { it.copy(exportMessage = "Exporting to Notion...", isExportError = false) }
            val result = repository.exportToNotion(apiKey, databaseId)
            result.fold(
                onSuccess = { count ->
                    val msg = if (count == 0)
                        "No new transactions to export — all transactions are already in Notion"
                    else
                        "Successfully exported $count transaction${if (count == 1) "" else "s"} to Notion"
                    _listState.update { it.copy(exportMessage = msg, isExportError = false) }
                },
                onFailure = { e ->
                    _listState.update { it.copy(exportMessage = "Notion export failed: ${e.message}", isExportError = true) }
                }
            )
        }
    }
}
