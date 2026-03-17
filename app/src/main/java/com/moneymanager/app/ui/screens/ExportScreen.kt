package com.moneymanager.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
    var credentialsSaved by remember { mutableStateOf(false) }
    var hasInitialized by remember { mutableStateOf(false) }

    // Populate fields from saved credentials only on first load
    LaunchedEffect(state.savedNotionApiKey, state.savedNotionDatabaseId) {
        if (!hasInitialized && (state.savedNotionApiKey.isNotEmpty() || state.savedNotionDatabaseId.isNotEmpty())) {
            notionApiKey = state.savedNotionApiKey
            notionDatabaseId = state.savedNotionDatabaseId
            hasInitialized = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export Data", fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Status banner ─────────────────────────────────────────────────
            if (state.exportMessage.isNotEmpty()) {
                val isError = state.isExportError
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isError) MaterialTheme.colorScheme.errorContainer
                                         else MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isError) MaterialTheme.colorScheme.onErrorContainer
                                   else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            state.exportMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isError) MaterialTheme.colorScheme.onErrorContainer
                                    else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── CSV Export Card ───────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.TableChart,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "CSV Export",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Download all transactions as a spreadsheet file.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Button(
                        onClick = { viewModel.exportToCsv() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Export to CSV")
                    }
                }
            }

            // ── Notion Export Card ────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CloudUpload,
                                null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Notion Export",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Sync transactions directly to your Notion database.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    TextButton(
                        onClick = { instructionsExpanded = !instructionsExpanded },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(if (instructionsExpanded) "Hide setup guide" else "Show setup guide")
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            if (instructionsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }

                    if (instructionsExpanded) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "1. Go to notion.so/my-integrations → \"New integration\"\n" +
                                    "2. Copy the \"Internal Integration Token\" (API Key)\n" +
                                    "3. Create a Notion database with these columns:\n" +
                                    "   • Name (Title)  • Amount (Number)\n" +
                                    "   • Type (Text)   • Category (Text)  • Date (Date)\n" +
                                    "4. Database → \"…\" → \"Connections\" → add integration\n" +
                                    "5. Copy 32-char database ID from the URL:\n" +
                                    "   notion.so/<workspace>/<DATABASE_ID>?v=…",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    OutlinedTextField(
                        value = notionApiKey,
                        onValueChange = {
                            notionApiKey = it
                            credentialsSaved = false
                        },
                        label = { Text("API Key") },
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
                        supportingText = { Text("From notion.so/my-integrations") },
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = notionDatabaseId,
                        onValueChange = {
                            notionDatabaseId = it
                            credentialsSaved = false
                        },
                        label = { Text("Database ID") },
                        placeholder = { Text("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = { Text("32-character ID from your database URL") },
                        shape = RoundedCornerShape(10.dp)
                    )

                    if (credentialsSaved) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Credentials saved",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                viewModel.saveNotionCredentials(notionApiKey, notionDatabaseId)
                                credentialsSaved = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            enabled = notionApiKey.isNotBlank() && notionDatabaseId.isNotBlank()
                        ) {
                            Text("Save")
                        }
                        Button(
                            onClick = {
                                viewModel.saveNotionCredentials(notionApiKey, notionDatabaseId)
                                credentialsSaved = true
                                viewModel.exportToNotion(notionApiKey, notionDatabaseId)
                            },
                            modifier = Modifier.weight(2f),
                            shape = RoundedCornerShape(10.dp),
                            enabled = notionApiKey.isNotBlank() && notionDatabaseId.isNotBlank()
                        ) {
                            Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Export to Notion")
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

