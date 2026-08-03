package com.moneymanager.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.shared.ui.theme.ExpenseColor
import com.moneymanager.shared.ui.theme.IncomeColor
import com.moneymanager.shared.util.formatCurrency
import com.moneymanager.shared.util.formatMonthYearNow
import com.moneymanager.shared.viewmodel.DashboardViewModel

private const val HIDDEN_AMOUNT = "••••••"

private fun formatAmount(amount: Double, hidden: Boolean): String = if (hidden) HIDDEN_AMOUNT else formatCurrency(amount)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onAddTransaction: () -> Unit,
    onOpenTransactionList: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    onImportCsv: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Money Manager", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.toggleAmountsHidden() }) {
                        Icon(
                            imageVector = if (state.amountsHidden) Icons.Default.List else Icons.Default.Share,
                            contentDescription = if (state.amountsHidden) "Show amounts" else "Hide amounts"
                        )
                    }
                    IconButton(onClick = onImportCsv) {
                        Icon(Icons.Default.Share, contentDescription = "Import CSV")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransaction, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, "Add Transaction", tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                if (state.isSyncing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                }
                if (state.syncMessage.isNotEmpty()) {
                    Text(state.syncMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(8.dp))
                }
            }

            item { SummaryCard(state.totalIncome, state.totalExpense, state.remaining, state.amountsHidden) }
            item { MonthlyCard(state.monthlyIncome, state.monthlyExpense, state.amountsHidden) }

            item {
                if (state.categoryBreakdown.isNotEmpty()) {
                    CategoryBreakdownCard(state.categoryBreakdown, state.amountsHidden)
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Recent Transactions", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    if (state.recentTransactions.isNotEmpty()) {
                        TextButton(onClick = onOpenTransactionList) { Text("See all") }
                    }
                }
            }

            if (state.recentTransactions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💸", fontSize = 48.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("No transactions yet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Tap + to add your first transaction", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(state.recentTransactions) { transaction ->
                    TransactionCard(
                        transaction = transaction,
                        amountsHidden = state.amountsHidden,
                        onClick = { onOpenTransaction(transaction.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(income: Double, expense: Double, remaining: Double, amountsHidden: Boolean) {
    val isPositive = remaining >= 0
    val gradientColors = if (isPositive) listOf(Color(0xFF5C35CC), Color(0xFF7C4DFF)) else listOf(Color(0xFFB71C1C), Color(0xFFE53935))

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(gradientColors)).padding(20.dp)) {
            Column {
                Text("Total Balance", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
                Spacer(Modifier.height(4.dp))
                Text(text = formatAmount(remaining, amountsHidden), fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SummaryItem("Income", formatAmount(income, amountsHidden), IncomeColor)
                    SummaryItem("Expenses", formatAmount(expense, amountsHidden), ExpenseColor)
                }
            }
        }
    }
}

@Composable
private fun MonthlyCard(monthlyIncome: Double, monthlyExpense: Double, amountsHidden: Boolean) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📅  ${formatMonthYearNow()}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryItem("Income", formatAmount(monthlyIncome, amountsHidden), IncomeColor)
                SummaryItem("Expenses", formatAmount(monthlyExpense, amountsHidden), ExpenseColor)
                SummaryItem("Saved", formatAmount(monthlyIncome - monthlyExpense, amountsHidden), if (amountsHidden || monthlyIncome >= monthlyExpense) IncomeColor else ExpenseColor)
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun CategoryBreakdownCard(breakdown: Map<String, Double>, amountsHidden: Boolean) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📊  Spending by Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            breakdown.entries.sortedByDescending { it.value }.take(5).forEach { (categoryName, amount) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(categoryName)
                    Text(formatAmount(amount, amountsHidden), fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}
