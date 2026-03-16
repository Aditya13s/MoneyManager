package com.moneymanager.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.moneymanager.app.data.db.entities.TransactionCategory
import com.moneymanager.app.data.db.entities.TransactionType
import com.moneymanager.app.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    navController: NavController,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val editState by viewModel.editState.collectAsState()
    var showError by remember { mutableStateOf(false) }

    LaunchedEffect(transactionId) {
        if (transactionId > 0) {
            viewModel.loadTransactionForEdit(transactionId)
        } else {
            viewModel.prepareNewTransaction()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editState.isNew) "Add Transaction" else "Edit Transaction") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showError) {
                Text("Please fill in all required fields", color = MaterialTheme.colorScheme.error)
            }

            OutlinedTextField(
                value = editState.title,
                onValueChange = { viewModel.updateEditField("title", it) },
                label = { Text("Title *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = editState.amount,
                onValueChange = { viewModel.updateEditField("amount", it) },
                label = { Text("Amount *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            Text("Type", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TransactionType.values().forEach { type ->
                    FilterChip(
                        selected = editState.type == type,
                        onClick = { viewModel.updateEditField("type", type) },
                        label = { Text(type.name) }
                    )
                }
            }

            Text("Category", style = MaterialTheme.typography.labelLarge)
            var categoryExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                OutlinedTextField(
                    value = editState.category.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    TransactionCategory.values().forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                viewModel.updateEditField("category", category)
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = editState.account,
                onValueChange = { viewModel.updateEditField("account", it) },
                label = { Text("Account") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = editState.location,
                onValueChange = { viewModel.updateEditField("location", it) },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = editState.note,
                onValueChange = { viewModel.updateEditField("note", it) },
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Text(
                "Date: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(editState.date))}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (viewModel.saveTransaction()) navController.popBackStack()
                    else showError = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (editState.isNew) "Add Transaction" else "Save Changes")
            }
        }
    }
}
