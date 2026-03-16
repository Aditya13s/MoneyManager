package com.moneymanager.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.moneymanager.app.data.db.entities.Transaction
import com.moneymanager.app.data.db.entities.TransactionType
import com.moneymanager.app.ui.navigation.Screen
import com.moneymanager.app.ui.theme.ExpenseColor
import com.moneymanager.app.ui.theme.IncomeColor
import com.moneymanager.app.viewmodel.DashboardViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    val csvImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.importCsvTransactions(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Money Manager", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { csvImportLauncher.launch("*/*") }) {
                        Icon(Icons.Default.Upload, contentDescription = "Import CSV")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                navController.navigate(Screen.TransactionDetail.createRoute(-1L))
            }) {
                Icon(Icons.Default.Add, "Add Transaction")
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

            item { SummaryCard(state.totalIncome, state.totalExpense, state.remaining, currencyFormat) }

            item { MonthlyCard(state.monthlyIncome, state.monthlyExpense, currencyFormat) }

            item {
                if (state.categoryBreakdown.isNotEmpty()) {
                    CategoryBreakdownCard(state.categoryBreakdown, currencyFormat)
                }
            }

            item {
                Text("Recent Transactions", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            items(state.recentTransactions) { transaction ->
                TransactionCard(
                    transaction = transaction,
                    currencyFormat = currencyFormat,
                    onClick = { navController.navigate(Screen.TransactionDetail.createRoute(transaction.id)) }
                )
            }
        }
    }
}

@Composable
fun SummaryCard(income: Double, expense: Double, remaining: Double, format: NumberFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Overall Balance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                text = format.format(remaining),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = if (remaining >= 0) IncomeColor else ExpenseColor
            )
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Income", style = MaterialTheme.typography.labelMedium)
                    Text(format.format(income), color = IncomeColor, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Expenses", style = MaterialTheme.typography.labelMedium)
                    Text(format.format(expense), color = ExpenseColor, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun MonthlyCard(monthlyIncome: Double, monthlyExpense: Double, format: NumberFormat) {
    val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("This Month: $monthName", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryItem("Income", format.format(monthlyIncome), IncomeColor)
                SummaryItem("Expenses", format.format(monthlyExpense), ExpenseColor)
                SummaryItem("Saved", format.format(monthlyIncome - monthlyExpense),
                    if (monthlyIncome >= monthlyExpense) IncomeColor else ExpenseColor)
            }
            Spacer(Modifier.height(12.dp))
            if (monthlyIncome > 0) {
                val progress = (monthlyExpense / monthlyIncome).toFloat().coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = if (progress < 0.75f) IncomeColor else ExpenseColor
                )
                Spacer(Modifier.height(4.dp))
                Text("${(progress * 100).toInt()}% of income spent", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun SummaryItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun CategoryBreakdownCard(breakdown: Map<String, Double>, format: NumberFormat) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Spending by Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            val total = breakdown.values.sum()
            breakdown.entries.sortedByDescending { it.value }.take(5).forEach { (category, amount) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(category, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text(format.format(amount), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
                if (total > 0) {
                    val pct = (amount / total).toFloat().coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { pct },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionCard(transaction: Transaction, currencyFormat: NumberFormat, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.title, fontWeight = FontWeight.Medium)
                Text(
                    "${transaction.category.name} • ${dateFormat.format(Date(transaction.date))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${if (transaction.type == TransactionType.EXPENSE) "-" else "+"}${currencyFormat.format(transaction.amount)}",
                color = if (transaction.type == TransactionType.EXPENSE) ExpenseColor else IncomeColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
