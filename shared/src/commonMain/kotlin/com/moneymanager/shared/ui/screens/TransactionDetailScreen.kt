package com.moneymanager.shared.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePicker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.shared.model.TransactionCategory
import com.moneymanager.shared.model.TransactionType
import com.moneymanager.shared.ui.theme.CategoryTransferColor
import com.moneymanager.shared.ui.theme.ExpenseColor
import com.moneymanager.shared.ui.theme.IncomeColor
import com.moneymanager.shared.util.badgeColor
import com.moneymanager.shared.util.emoji
import com.moneymanager.shared.util.formatDate
import com.moneymanager.shared.util.toCategoryTitle
import com.moneymanager.shared.viewmodel.TransactionViewModel

private val quickAmounts = listOf("100", "500", "1000", "2000", "5000", "10000")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val editState by viewModel.editState.collectAsState()
    var showError by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(transactionId) {
        if (transactionId > 0) viewModel.loadTransactionForEdit(transactionId) else viewModel.prepareNewTransaction()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete \"${editState.title}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTransactionById(editState.id)
                    showDeleteDialog = false
                    onBack()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = editState.date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.updateEditField("date", it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editState.isNew) "Add Transaction" else "Edit Transaction", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    if (!editState.isNew) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete transaction", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    IconButton(onClick = {
                        viewModel.saveTransaction(onSuccess = onBack, onError = { showError = true })
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
            if (showError) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        "Please fill in all required fields (Title & Amount)",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Text("Type", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TransactionType.entries.forEach { type ->
                    val selected = editState.type == type
                    val accent = when (type) {
                        TransactionType.INCOME -> IncomeColor
                        TransactionType.EXPENSE -> ExpenseColor
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
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, selectedBorderColor = accent)
                    )
                }
            }

            OutlinedTextField(
                value = editState.title,
                onValueChange = { viewModel.updateEditField("title", it) },
                label = { Text("Title *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = showError && editState.title.isBlank()
            )

            OutlinedTextField(
                value = editState.amount,
                onValueChange = { viewModel.updateEditField("amount", it) },
                label = { Text("Amount (₹) *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = showError && editState.amount.toDoubleOrNull() == null
            )

            Text("Quick amounts", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                quickAmounts.forEach { preset ->
                    SuggestionChip(onClick = { viewModel.updateEditField("amount", preset) }, label = { Text("₹$preset") })
                }
            }

            Text("Category", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

            OutlinedTextField(value = editState.account, onValueChange = { viewModel.updateEditField("account", it) }, label = { Text("Account") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            OutlinedTextField(
                value = formatDate(editState.date),
                onValueChange = {},
                readOnly = true,
                label = { Text("Date") },
                trailingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
            )

            OutlinedTextField(value = editState.note, onValueChange = { viewModel.updateEditField("note", it) }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.saveTransaction(onSuccess = onBack, onError = { showError = true }) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = if (editState.isNew) "Add Transaction" else "Save Changes", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            if (!editState.isNew) {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
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
