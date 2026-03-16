package com.moneymanager.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.app.data.db.entities.Transaction
import com.moneymanager.app.data.db.entities.TransactionCategory
import com.moneymanager.app.data.db.entities.TransactionType
import com.moneymanager.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionListState(
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val selectedType: TransactionType? = null,
    val exportMessage: String = ""
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
    val date: Long = System.currentTimeMillis(),
    val isNew: Boolean = true
)

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _listState = MutableStateFlow(TransactionListState())
    val listState: StateFlow<TransactionListState> = _listState.asStateFlow()

    private val _editState = MutableStateFlow(TransactionEditState())
    val editState: StateFlow<TransactionEditState> = _editState.asStateFlow()

    private val _allTransactions = MutableStateFlow<List<Transaction>>(emptyList())

    init {
        loadTransactions()
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
        }
        return true
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
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
            _listState.update { it.copy(exportMessage = "Exporting to Notion...") }
            val result = repository.exportToNotion(apiKey, databaseId)
            result.fold(
                onSuccess = { count -> _listState.update { it.copy(exportMessage = "Exported $count transactions to Notion") } },
                onFailure = { e -> _listState.update { it.copy(exportMessage = "Notion export failed: ${e.message}") } }
            )
        }
    }
}
