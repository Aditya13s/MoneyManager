package com.moneymanager.shared.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moneymanager.shared.model.TransactionType
import com.moneymanager.shared.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionViewModel,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit
) {
    val state by viewModel.listState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions") },
                actions = {
                    IconButton(onClick = { viewModel.toggleAmountsHidden() }) {
                        Icon(
                            imageVector = if (state.amountsHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (state.amountsHidden) "Show amounts" else "Hide amounts"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.prepareNewTransaction()
                onAdd()
            }) {
                Icon(Icons.Default.Add, "Add")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search transactions...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )

            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = state.selectedType == null, onClick = { viewModel.setTypeFilter(null) }, label = { Text("All") })
                FilterChip(selected = state.selectedType == TransactionType.INCOME, onClick = { viewModel.setTypeFilter(TransactionType.INCOME) }, label = { Text("Income") })
                FilterChip(selected = state.selectedType == TransactionType.EXPENSE, onClick = { viewModel.setTypeFilter(TransactionType.EXPENSE) }, label = { Text("Expense") })
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.transactions, key = { it.id }) { transaction ->
                        TransactionCard(
                            transaction = transaction,
                            amountsHidden = state.amountsHidden,
                            onClick = { onOpen(transaction.id) }
                        )
                    }
                }
            }
        }
    }
}
