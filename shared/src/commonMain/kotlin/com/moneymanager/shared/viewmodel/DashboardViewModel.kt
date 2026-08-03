package com.moneymanager.shared.viewmodel

import com.moneymanager.shared.data.TransactionRepository
import com.moneymanager.shared.data.UserPreferencesRepository
import com.moneymanager.shared.model.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

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
    val syncMessage: String = "",
    val amountsHidden: Boolean = false
)

class DashboardViewModel(
    private val repository: TransactionRepository,
    private val prefsRepository: UserPreferencesRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadDashboardData()
        observePreferences()
    }

    private fun observePreferences() {
        scope.launch {
            prefsRepository.amountsHidden.collect { hidden ->
                _state.update { it.copy(amountsHidden = hidden) }
            }
        }
    }

    fun toggleAmountsHidden() {
        scope.launch { prefsRepository.toggleAmountsHidden() }
    }

    private fun loadDashboardData() {
        scope.launch {
            _state.update { it.copy(isLoading = true) }
            val (startOfMonth, endOfMonth) = monthRangeNow()

            combine(
                repository.getTotalIncome(),
                repository.getTotalExpense(),
                repository.getIncomeInRange(startOfMonth, endOfMonth),
                repository.getExpenseInRange(startOfMonth, endOfMonth),
                repository.getAllTransactions()
            ) { totalIncome, totalExpense, monthlyIncome, monthlyExpense, allTransactions ->
                val categoryBreakdown = allTransactions
                    .filter { it.type.name == "EXPENSE" }
                    .groupBy { it.category.name }
                    .mapValues { (_, txns) -> txns.sumOf { it.amount } }

                DashboardState(
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    remaining = totalIncome - totalExpense,
                    monthlyIncome = monthlyIncome,
                    monthlyExpense = monthlyExpense,
                    recentTransactions = allTransactions.take(10),
                    categoryBreakdown = categoryBreakdown,
                    isLoading = false,
                    amountsHidden = _state.value.amountsHidden,
                    syncMessage = _state.value.syncMessage,
                    isSyncing = _state.value.isSyncing
                )
            }.collect { _state.value = it }
        }
    }

    fun importCsvContent(content: String) {
        scope.launch {
            _state.update { it.copy(isSyncing = true, syncMessage = "Importing transactions…") }
            runCatching { repository.importFromCsvContent(content) }
                .onSuccess { count ->
                    val message = if (count > 0) "Imported $count transaction(s)" else "No transactions found in file"
                    _state.update { it.copy(isSyncing = false, syncMessage = message) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isSyncing = false, syncMessage = "Import failed: ${e.message}") }
                }
        }
    }

    fun setImportError(message: String) {
        _state.update { it.copy(isSyncing = false, syncMessage = "Import failed: $message") }
    }

    private fun monthRangeNow(): Pair<Long, Long> {
        val timeZone = TimeZone.currentSystemDefault()
        val nowDate = Clock.System.now().toLocalDateTime(timeZone).date
        val startOfMonth = LocalDate(nowDate.year, nowDate.monthNumber, 1)
        val nextMonthStart = if (nowDate.monthNumber == 12) {
            LocalDate(nowDate.year + 1, 1, 1)
        } else {
            LocalDate(nowDate.year, nowDate.monthNumber + 1, 1)
        }
        val startMillis = startOfMonth.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val endMillis = nextMonthStart.atStartOfDayIn(timeZone).toEpochMilliseconds() - 1L
        return startMillis to endMillis
    }
}
