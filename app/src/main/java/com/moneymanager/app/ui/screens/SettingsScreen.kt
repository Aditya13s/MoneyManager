package com.moneymanager.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.moneymanager.app.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val listState by viewModel.listState.collectAsState()
    val savedAccounts = listState.savedAccounts
    val amountsHidden = listState.amountsHidden

    var showAddAccountDialog by remember { mutableStateOf(false) }
    var newAccountName by remember { mutableStateOf("") }
    var accountError by remember { mutableStateOf(false) }

    // Add account dialog
    if (showAddAccountDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddAccountDialog = false
                newAccountName = ""
                accountError = false
            },
            title = { Text("Add Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter the account name to save for quick selection when adding transactions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newAccountName,
                        onValueChange = {
                            newAccountName = it
                            accountError = false
                        },
                        label = { Text("Account name") },
                        singleLine = true,
                        isError = accountError,
                        supportingText = if (accountError) {
                            { Text("Account name cannot be empty") }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newAccountName.isBlank()) {
                            accountError = true
                        } else {
                            viewModel.addSavedAccount(newAccountName.trim())
                            newAccountName = ""
                            showAddAccountDialog = false
                            accountError = false
                        }
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddAccountDialog = false
                    newAccountName = ""
                    accountError = false
                }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Saved Accounts section ────────────────────────────────────────
            item {
                SectionHeader(
                    icon = { Icon(Icons.Default.AccountBalance, null, modifier = Modifier.size(20.dp)) },
                    title = "Saved Accounts"
                )
            }
            item {
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        if (savedAccounts.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No saved accounts yet. Add transactions with an account name and they will appear here automatically.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            savedAccounts.forEach { account ->
                                ListItem(
                                    headlineContent = { Text(account) },
                                    trailingContent = {
                                        IconButton(onClick = { viewModel.removeSavedAccount(account) }) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove $account",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                )
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                        // Add account button
                        ListItem(
                            headlineContent = {
                                Text(
                                    "Add Account",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier.clickableWithRipple { showAddAccountDialog = true }
                        )
                    }
                }
            }

            // ── Privacy section ────────────────────────────────────────────────
            item {
                SectionHeader(
                    icon = { Icon(Icons.Default.Visibility, null, modifier = Modifier.size(20.dp)) },
                    title = "Privacy"
                )
            }
            item {
                Card(shape = RoundedCornerShape(12.dp)) {
                    ListItem(
                        headlineContent = { Text("Hide Amounts") },
                        supportingContent = {
                            Text(
                                "Replace all amounts with •••••• for privacy",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = {
                            Icon(
                                if (amountsHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = amountsHidden,
                                onCheckedChange = { viewModel.toggleAmountsHidden() }
                            )
                        }
                    )
                }
            }

            // ── About section ────────────────────────────────────────────────
            item {
                SectionHeader(
                    icon = { Icon(Icons.Default.Info, null, modifier = Modifier.size(20.dp)) },
                    title = "About"
                )
            }
            item {
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        ListItem(
                            headlineContent = { Text("Money Manager") },
                            supportingContent = { Text("Version 1.0") }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ListItem(
                            headlineContent = { Text("Data Backup") },
                            supportingContent = {
                                Text(
                                    "Your transactions and account names are automatically backed up to your Google account and restored when you reinstall the app.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: @Composable () -> Unit,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        icon()
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun Modifier.clickableWithRipple(onClick: () -> Unit): Modifier =
    this.then(
        Modifier.clickable(onClick = onClick)
    )
