package com.moneymanager.shared.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.moneymanager.shared.data.createAppContainer
import com.moneymanager.shared.ui.navigation.Screen
import com.moneymanager.shared.ui.screens.DashboardScreen
import com.moneymanager.shared.ui.screens.ExportScreen
import com.moneymanager.shared.ui.screens.TransactionDetailScreen
import com.moneymanager.shared.ui.screens.TransactionListScreen
import com.moneymanager.shared.ui.theme.MoneyManagerTheme
import com.moneymanager.shared.viewmodel.DashboardViewModel
import com.moneymanager.shared.viewmodel.TransactionViewModel

interface PlatformFileBridge {
    fun importCsv(onLoaded: (String) -> Unit, onError: (String) -> Unit)
    fun exportCsv(content: String, onSuccess: (String) -> Unit, onError: (String) -> Unit)
}

@Composable
fun MoneyManagerApp(fileBridge: PlatformFileBridge) {
    val container = remember { createAppContainer() }
    val dashboardViewModel = remember { DashboardViewModel(container.transactionRepository, container.preferencesRepository) }
    val transactionViewModel = remember { TransactionViewModel(container.transactionRepository, container.preferencesRepository) }
    var currentScreen: Screen by remember { mutableStateOf(Screen.Dashboard) }
    val scope = rememberCoroutineScope()

    MoneyManagerTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                val bottomItems = listOf(Screen.Dashboard, Screen.TransactionList, Screen.Export)
                NavigationBar {
                    bottomItems.forEach { screen ->
                        val selected = currentScreen::class == screen::class
                        NavigationBarItem(
                            icon = {
                                when (screen) {
                                    Screen.Dashboard -> Icon(Icons.Default.List, "Dashboard")
                                    Screen.TransactionList -> Icon(Icons.Default.List, "Transactions")
                                    Screen.Export -> Icon(Icons.Default.Share, "Export")
                                    else -> Icon(Icons.Default.List, null)
                                }
                            },
                            label = {
                                Text(
                                    when (screen) {
                                        Screen.Dashboard -> "Dashboard"
                                        Screen.TransactionList -> "Transactions"
                                        Screen.Export -> "Export"
                                        else -> ""
                                    }
                                )
                            },
                            selected = selected,
                            onClick = { currentScreen = screen }
                        )
                    }
                }
            }
        ) { padding ->
            when (val screen = currentScreen) {
                Screen.Dashboard -> DashboardScreen(
                    viewModel = dashboardViewModel,
                    onAddTransaction = {
                        transactionViewModel.prepareNewTransaction()
                        currentScreen = Screen.TransactionDetail(-1)
                    },
                    onOpenTransactionList = { currentScreen = Screen.TransactionList },
                    onOpenTransaction = { currentScreen = Screen.TransactionDetail(it) },
                    onImportCsv = {
                        fileBridge.importCsv(
                            onLoaded = { csv -> dashboardViewModel.importCsvContent(csv) },
                            onError = { err -> dashboardViewModel.setImportError(err) }
                        )
                    }
                )

                Screen.TransactionList -> TransactionListScreen(
                    viewModel = transactionViewModel,
                    onAdd = { currentScreen = Screen.TransactionDetail(-1) },
                    onOpen = { currentScreen = Screen.TransactionDetail(it) }
                )

                is Screen.TransactionDetail -> TransactionDetailScreen(
                    transactionId = screen.id,
                    viewModel = transactionViewModel,
                    onBack = { currentScreen = Screen.TransactionList }
                )

                Screen.Export -> ExportScreen(
                    viewModel = transactionViewModel,
                    onBack = { currentScreen = Screen.Dashboard },
                    onExportCsv = { csv ->
                        fileBridge.exportCsv(
                            content = csv,
                            onSuccess = { path -> transactionViewModel.onCsvExported(path) },
                            onError = { error -> transactionViewModel.onCsvExportFailed(error) }
                        )
                    }
                )
            }
        }
    }
}
