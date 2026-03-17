package com.moneymanager.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.moneymanager.app.data.db.entities.Transaction
import com.moneymanager.app.data.db.entities.TransactionCategory
import com.moneymanager.app.data.db.entities.TransactionType
import com.moneymanager.app.ui.navigation.Screen
import com.moneymanager.app.ui.theme.*
import com.moneymanager.app.ui.util.badgeColor
import com.moneymanager.app.ui.util.emoji
import com.moneymanager.app.ui.util.toCategoryTitle
import com.moneymanager.app.viewmodel.DashboardViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private const val HIDDEN_AMOUNT = "••••••"

private fun formatAmount(amount: Double, hidden: Boolean, format: NumberFormat): String =
    if (hidden) HIDDEN_AMOUNT else format.format(amount)

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
                    IconButton(
                        onClick = { viewModel.toggleAmountsHidden() },
                        modifier = Modifier.semantics {
                            contentDescription = if (state.amountsHidden) "Show amounts" else "Hide amounts"
                        }
                    ) {
                        Icon(
                            imageVector = if (state.amountsHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                    IconButton(onClick = { csvImportLauncher.launch("*/*") }) {
                        Icon(Icons.Default.Upload, contentDescription = "Import CSV")
                    }
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.TransactionDetail.createRoute(-1L)) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
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
                    Text(state.syncMessage, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.height(8.dp))
                }
            }

            item { SummaryCard(state.totalIncome, state.totalExpense, state.remaining, currencyFormat, state.amountsHidden) }

            item { MonthlyCard(state.monthlyIncome, state.monthlyExpense, currencyFormat, state.amountsHidden) }

            item {
                if (state.categoryBreakdown.isNotEmpty()) {
                    CategoryBreakdownCard(state.categoryBreakdown, currencyFormat, state.amountsHidden)
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
                        TextButton(onClick = { navController.navigate(Screen.TransactionList.route) }) {
                            Text("See all")
                        }
                    }
                }
            }

            if (state.recentTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💸", fontSize = 48.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No transactions yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Tap + to add your first transaction",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(state.recentTransactions) { transaction ->
                    TransactionCard(
                        transaction = transaction,
                        currencyFormat = currencyFormat,
                        amountsHidden = state.amountsHidden,
                        onClick = { navController.navigate(Screen.TransactionDetail.createRoute(transaction.id)) }
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryCard(income: Double, expense: Double, remaining: Double, format: NumberFormat, amountsHidden: Boolean = false) {
    val isPositive = remaining >= 0
    val gradientColors = if (isPositive)
        listOf(Color(0xFF5C35CC), Color(0xFF7C4DFF))
    else
        listOf(Color(0xFFB71C1C), Color(0xFFE53935))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradientColors))
                .padding(20.dp)
        ) {
            Column {
                Text(
                    "Total Balance",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatAmount(remaining, amountsHidden, format),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    // Income pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.TrendingUp, null, tint = Color(0xFF69FF47), modifier = Modifier.size(18.dp))
                        Column {
                            Text("Income", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                            Text(formatAmount(income, amountsHidden, format), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    // Expense pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.TrendingDown, null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                        Column {
                            Text("Expenses", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                            Text(formatAmount(expense, amountsHidden, format), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyCard(monthlyIncome: Double, monthlyExpense: Double, format: NumberFormat, amountsHidden: Boolean = false) {
    val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📅  $monthName", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryItem("Income", formatAmount(monthlyIncome, amountsHidden, format), IncomeColor)
                SummaryItem("Expenses", formatAmount(monthlyExpense, amountsHidden, format), ExpenseColor)
                SummaryItem(
                    "Saved",
                    formatAmount(monthlyIncome - monthlyExpense, amountsHidden, format),
                    if (amountsHidden || monthlyIncome >= monthlyExpense) IncomeColor else ExpenseColor
                )
            }
            Spacer(Modifier.height(12.dp))
            if (monthlyIncome > 0) {
                val progress = (monthlyExpense / monthlyIncome).toFloat().coerceIn(0f, 1f)
                val barColor = when {
                    progress < 0.5f  -> IncomeColor
                    progress < 0.75f -> Color(0xFFFFAB00)
                    else             -> ExpenseColor
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = barColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (amountsHidden) "•••% of income spent"
                    else "${(progress * 100).toInt()}% of income spent",
                    style = MaterialTheme.typography.bodySmall,
                    color = barColor
                )
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
fun CategoryBreakdownCard(breakdown: Map<String, Double>, format: NumberFormat, amountsHidden: Boolean = false) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📊  Spending by Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            val total = breakdown.values.sum()
            breakdown.entries.sortedByDescending { it.value }.take(5).forEach { (categoryName, amount) ->
                val category = runCatching { TransactionCategory.valueOf(categoryName) }.getOrDefault(TransactionCategory.OTHER)
                val pct = if (total > 0) (amount / total).toFloat().coerceIn(0f, 1f) else 0f
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(category.badgeColor().copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(category.emoji(), fontSize = 14.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            categoryName.toCategoryTitle(),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        formatAmount(amount, amountsHidden, format),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                LinearProgressIndicator(
                    progress = { pct },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = category.badgeColor(),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionCard(transaction: Transaction, currencyFormat: NumberFormat, amountsHidden: Boolean = false, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val amountColor = if (transaction.type == TransactionType.EXPENSE) ExpenseColor else IncomeColor
    val sign = if (transaction.type == TransactionType.EXPENSE) "-" else "+"

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category badge circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(transaction.category.badgeColor().copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(transaction.category.emoji(), fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    "${transaction.category.name.toCategoryTitle()} • ${dateFormat.format(Date(transaction.date))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = if (amountsHidden) "$sign $HIDDEN_AMOUNT"
                       else "$sign${currencyFormat.format(transaction.amount)}",
                color = amountColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

