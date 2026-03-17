package com.moneymanager.app.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.moneymanager.app.data.db.entities.AccountType
import com.moneymanager.app.data.db.entities.TransactionCategory
import com.moneymanager.app.data.db.entities.TransactionType
import com.moneymanager.app.ui.theme.*
import com.moneymanager.app.ui.util.badgeColor
import com.moneymanager.app.ui.util.emoji
import com.moneymanager.app.ui.util.toCategoryTitle
import com.moneymanager.app.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

private val quickAmounts = listOf("100", "500", "1,000", "2,000", "5,000", "10,000")

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

    // Derive accent colour from selected transaction type
    val typeAccent by animateColorAsState(
        targetValue = when (editState.type) {
            TransactionType.INCOME   -> IncomeColor
            TransactionType.EXPENSE  -> ExpenseColor
            TransactionType.TRANSFER -> CategoryTransferColor
        },
        animationSpec = tween(300),
        label = "typeAccent"
    )

    LaunchedEffect(transactionId) {
        if (transactionId > 0) {
            viewModel.loadTransactionForEdit(transactionId)
        } else {
            viewModel.prepareNewTransaction()
        }
    }

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
                        text = if (editState.isNew) "New Transaction" else "Edit Transaction",
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
                            Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            // ── Hero amount area ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(typeAccent.copy(alpha = 0.12f), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    // Transaction type big pill selector
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TransactionType.entries.forEach { type ->
                            val selected = editState.type == type
                            val accent = when (type) {
                                TransactionType.INCOME   -> IncomeColor
                                TransactionType.EXPENSE  -> ExpenseColor
                                TransactionType.TRANSFER -> CategoryTransferColor
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (selected) accent else Color.Transparent)
                                    .clickable { viewModel.updateEditField("type", type) }
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    type.name.lowercase().replaceFirstChar { it.uppercase() },
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Big amount input
                    val amountError = showError && editState.amount.replace(",", "").toDoubleOrNull() == null
                    OutlinedTextField(
                        value = editState.amount,
                        onValueChange = { viewModel.updateEditField("amount", it) },
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            color = typeAccent
                        ),
                        placeholder = {
                            Text(
                                "0",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                color = typeAccent.copy(alpha = 0.3f)
                            )
                        },
                        prefix = {
                            Text(
                                "₹",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = typeAccent
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = amountError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = typeAccent,
                            unfocusedBorderColor = typeAccent.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    // Quick-amount chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        quickAmounts.forEach { preset ->
                            val raw = preset.replace(",", "")
                            SuggestionChip(
                                onClick = { viewModel.updateEditField("amount", raw) },
                                label = { Text("₹$preset", fontSize = 12.sp) },
                                shape = CircleShape,
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = typeAccent.copy(alpha = 0.08f),
                                    labelColor = typeAccent
                                ),
                                border = SuggestionChipDefaults.suggestionChipBorder(
                                    enabled = true,
                                    borderColor = typeAccent.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }
                }
            }

            // ── Form fields ──────────────────────────────────────────────────
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Error banner
                if (showError) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            "Please fill in Title and a valid Amount",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // ── Title ────────────────────────────────────────────────────
                OutlinedTextField(
                    value = editState.title,
                    onValueChange = { viewModel.updateEditField("title", it) },
                    label = { Text("Title *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = showError && editState.title.isBlank(),
                    shape = RoundedCornerShape(12.dp)
                )

                // ── Category grid ─────────────────────────────────────────────
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
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (selected) accent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(
                                    width = if (selected) 1.5.dp else 0.dp,
                                    color = if (selected) accent else Color.Transparent,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { viewModel.updateEditField("category", category) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(category.emoji(), fontSize = 18.sp)
                                Text(
                                    text = category.name.toCategoryTitle(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // ── Account with dropdown ────────────────────────────────────
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
                        label = { Text("Account name") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            if (savedAccounts.isNotEmpty()) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded)
                            }
                        }
                    )
                    val filtered = if (editState.account.isBlank()) savedAccounts
                                   else savedAccounts.filter { it.contains(editState.account, ignoreCase = true) }
                    if (filtered.isNotEmpty()) {
                        ExposedDropdownMenu(expanded = accountDropdownExpanded, onDismissRequest = { accountDropdownExpanded = false }) {
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

                // ── Account Type: 2-option pill toggle ───────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AccountType.entries.forEach { type ->
                        val selected = editState.accountType == type
                        val (icon, label) = when (type) {
                            AccountType.BANK        -> Icons.Default.AccountBalance to "Bank Account"
                            AccountType.CREDIT_CARD -> Icons.Default.CreditCard to "Credit Card"
                        }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable { viewModel.updateEditField("accountType", type) }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                icon,
                                null,
                                modifier = Modifier.size(18.dp),
                                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                label,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ── Date picker ──────────────────────────────────────────────
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
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Box(
                        modifier = Modifier.matchParentSize().clickable {
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

                // ── Note ─────────────────────────────────────────────────────
                OutlinedTextField(
                    value = editState.note,
                    onValueChange = { viewModel.updateEditField("note", it) },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(4.dp))

                // ── Save button ───────────────────────────────────────────────
                Button(
                    onClick = {
                        if (viewModel.saveTransaction()) navController.popBackStack()
                        else showError = true
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = typeAccent)
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (editState.isNew) "Add Transaction" else "Save Changes",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // ── Delete button ─────────────────────────────────────────────
                if (!editState.isNew) {
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Delete", fontSize = 15.sp)
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
