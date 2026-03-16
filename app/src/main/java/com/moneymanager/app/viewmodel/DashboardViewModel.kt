package com.moneymanager.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.app.data.db.entities.Transaction
import com.moneymanager.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class DashboardState(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val remaining: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val recentTransactions: List<Transaction> = emptyList(),
    val categoryBreakdown: Map<String, Double> = emptyMap(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val syncMessage: String = ""
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val startOfMonth = getStartOfMonth()
            val endOfMonth = getEndOfMonth()

            combine(
                repository.getTotalIncome(),
                repository.getTotalExpense(),
                repository.getIncomeInRange(startOfMonth, endOfMonth),
                repository.getExpenseInRange(startOfMonth, endOfMonth),
                repository.getAllTransactions()
            ) { totalIncome, totalExpense, monthlyIncome, monthlyExpense, allTransactions ->
                val income = totalIncome ?: 0.0
                val expense = totalExpense ?: 0.0
                val mIncome = monthlyIncome ?: 0.0
                val mExpense = monthlyExpense ?: 0.0

                val categoryBreakdown = allTransactions
                    .filter { it.type.name == "EXPENSE" }
                    .groupBy { it.category.name }
                    .mapValues { (_, txns) -> txns.sumOf { it.amount } }

                DashboardState(
                    totalIncome = income,
                    totalExpense = expense,
                    remaining = income - expense,
                    monthlyIncome = mIncome,
                    monthlyExpense = mExpense,
                    recentTransactions = allTransactions.take(10),
                    categoryBreakdown = categoryBreakdown,
                    isLoading = false
                )
            }.collect { newState ->
                _state.update { current ->
                    newState.copy(
                        isSyncing = current.isSyncing,
                        syncMessage = current.syncMessage
                    )
                }
            }
        }
    }

    fun syncSmsTransactions() {
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true, syncMessage = "Syncing SMS transactions...") }
            try {
                val count = repository.syncSmsTransactions()
                _state.update { it.copy(isSyncing = false, syncMessage = "Synced $count new transactions") }
            } catch (e: Exception) {
                _state.update { it.copy(isSyncing = false, syncMessage = "Sync failed: ${e.message}") }
            }
        }
    }

    private fun getStartOfMonth(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getEndOfMonth(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }
}
