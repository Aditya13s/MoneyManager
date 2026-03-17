package com.moneymanager.app.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.moneymanager.app.data.db.entities.TransactionCategory
import com.moneymanager.app.data.db.entities.TransactionType
import com.moneymanager.app.ui.theme.*
import com.moneymanager.app.ui.util.badgeColor
import com.moneymanager.app.ui.util.emoji
import com.moneymanager.app.ui.util.toCategoryTitle
import com.moneymanager.app.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

private val quickAmounts = listOf("100", "500", "1000", "2000", "5000", "10000")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    navController: NavController,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val editState by viewModel.editState.collectAsState()
    val listState by viewModel.listState.collectAsState()
    val savedAccounts = listState.savedAccounts

    var showError by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())

    LaunchedEffect(transactionId) {
        if (transactionId > 0) {
            viewModel.loadTransactionForEdit(transactionId)
        } else {
            viewModel.prepareNewTransaction()
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete \"${editState.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransactionById(editState.id)
                        showDeleteDialog = false
                        navController.popBackStack()
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (editState.isNew) "Add Transaction" else "Edit Transaction",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (!editState.isNew) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete transaction",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(onClick = {
                        if (viewModel.saveTransaction()) navController.popBackStack()
                        else showError = true
                    }) {
                        Icon(Icons.Default.Check, "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Error banner ────────────────────────────────────────────────
            if (showError) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Please fill in all required fields (Title & Amount)",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // ── Transaction Type selector ────────────────────────────────────
            Text("Type", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TransactionType.entries.forEach { type ->
                    val selected = editState.type == type
                    val accent = when (type) {
                        TransactionType.INCOME   -> IncomeColor
                        TransactionType.EXPENSE  -> ExpenseColor
                        TransactionType.TRANSFER -> CategoryTransferColor
                    }
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.updateEditField("type", type) },
                        label = { Text(type.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent.copy(alpha = 0.15f),
                            selectedLabelColor = accent
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            selectedBorderColor = accent
                        )
                    )
                }
            }

            // ── Title ────────────────────────────────────────────────────────
            OutlinedTextField(
                value = editState.title,
                onValueChange = { viewModel.updateEditField("title", it) },
                label = { Text("Title *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = showError && editState.title.isBlank()
            )

            // ── Amount ───────────────────────────────────────────────────────
            OutlinedTextField(
                value = editState.amount,
                onValueChange = { viewModel.updateEditField("amount", it) },
                label = { Text("Amount (₹) *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = showError && editState.amount.toDoubleOrNull() == null
            )

            // Quick-amount chips
            Text("Quick amounts", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                quickAmounts.forEach { preset ->
                    SuggestionChip(
                        onClick = { viewModel.updateEditField("amount", preset) },
                        label = { Text("₹$preset") }
                    )
                }
            }

            // ── Category grid ────────────────────────────────────────────────
            Text("Category", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TransactionCategory.entries.forEach { category ->
                    val selected = editState.category == category
                    val accent = category.badgeColor()
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) accent.copy(alpha = 0.18f) else Color.Transparent)
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.updateEditField("category", category) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(category.emoji(), fontSize = 20.sp)
                            Text(
                                text = category.name.toCategoryTitle(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // ── Account (with dropdown suggestions) ──────────────────────────
            Text("Account", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            ExposedDropdownMenuBox(
                expanded = accountDropdownExpanded && savedAccounts.isNotEmpty(),
                onExpandedChange = { accountDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = editState.account,
                    onValueChange = {
                        viewModel.updateEditField("account", it)
                        accountDropdownExpanded = true
                    },
                    label = { Text("Account") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    singleLine = true,
                    trailingIcon = {
                        if (savedAccounts.isNotEmpty()) {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded)
                        }
                    }
                )
                // Show matching saved accounts as dropdown options
                val filtered = if (editState.account.isBlank()) {
                    savedAccounts
                } else {
                    savedAccounts.filter { it.contains(editState.account, ignoreCase = true) }
                }
                if (filtered.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = accountDropdownExpanded,
                        onDismissRequest = { accountDropdownExpanded = false }
                    ) {
                        filtered.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account) },
                                onClick = {
                                    viewModel.updateEditField("account", account)
                                    accountDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // ── Date picker ──────────────────────────────────────────────────
            val calendar = remember(editState.date) {
                Calendar.getInstance().apply { timeInMillis = editState.date }
            }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = dateFormat.format(Date(editState.date)),
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Date") },
                    trailingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable {
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val cal = Calendar.getInstance().apply { set(year, month, day) }
                                    viewModel.updateEditField("date", cal.timeInMillis)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                )
            }

            // ── Note ─────────────────────────────────────────────────────────
            OutlinedTextField(
                value = editState.note,
                onValueChange = { viewModel.updateEditField("note", it) },
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Spacer(Modifier.height(8.dp))

            // ── Save button ───────────────────────────────────────────────────
            Button(
                onClick = {
                    if (viewModel.saveTransaction()) navController.popBackStack()
                    else showError = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (editState.isNew) "Add Transaction" else "Save Changes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // ── Delete button (edit mode only) ────────────────────────────────
            if (!editState.isNew) {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Delete Transaction", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

