package com.moneymanager.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.moneymanager.app.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    navController: NavController,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val state by viewModel.listState.collectAsState()
    var notionApiKey by remember { mutableStateOf("") }
    var notionDatabaseId by remember { mutableStateOf("") }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var instructionsExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export Data") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
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
            if (state.exportMessage.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.isExportError) MaterialTheme.colorScheme.errorContainer
                                         else MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        state.exportMessage,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.isExportError) MaterialTheme.colorScheme.onErrorContainer
                                else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("CSV Export", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Export all transactions to a CSV file stored on your device.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = { viewModel.exportToCsv() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Export to CSV")
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Notion Export", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Export transactions to a Notion database.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    TextButton(
                        onClick = { instructionsExpanded = !instructionsExpanded },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(if (instructionsExpanded) "Hide setup instructions" else "Show setup instructions")
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            if (instructionsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }

                    if (instructionsExpanded) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "1. Go to https://www.notion.so/my-integrations and click \"New integration\".\n" +
                                    "2. Give it a name (e.g. MoneyManager), select your workspace, and click Submit.\n" +
                                    "3. Copy the \"Internal Integration Token\" — this is your API Key.\n" +
                                    "4. In Notion, create a new database with these columns:\n" +
                                    "   • Name (Title)\n" +
                                    "   • Amount (Number)\n" +
                                    "   • Type (Select: INCOME, EXPENSE, TRANSFER)\n" +
                                    "   • Category (Select)\n" +
                                    "   • Date (Date)\n" +
                                    "5. Open the database, click \"...\" → \"Connections\" and add your integration.\n" +
                                    "6. Copy the database ID from its URL:\n" +
                                    "   notion.so/<workspace>/<DATABASE_ID>?v=...",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    HorizontalDivider()

                    Text("Enter your credentials below:", style = MaterialTheme.typography.labelMedium)

                    OutlinedTextField(
                        value = notionApiKey,
                        onValueChange = { notionApiKey = it },
                        label = { Text("Notion API Key") },
                        placeholder = { Text("secret_xxxxxxxxxxxx") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                Icon(
                                    imageVector = if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (apiKeyVisible) "Hide API key" else "Show API key"
                                )
                            }
                        },
                        supportingText = { Text("Found at notion.so/my-integrations") }
                    )

                    OutlinedTextField(
                        value = notionDatabaseId,
                        onValueChange = { notionDatabaseId = it },
                        label = { Text("Database ID") },
                        placeholder = { Text("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = { Text("32-character ID from your database URL") }
                    )

                    Button(
                        onClick = { viewModel.exportToNotion(notionApiKey, notionDatabaseId) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = notionApiKey.isNotBlank() && notionDatabaseId.isNotBlank()
                    ) {
                        Text("Export to Notion")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
