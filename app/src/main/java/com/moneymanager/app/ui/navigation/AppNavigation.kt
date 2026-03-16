package com.moneymanager.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.moneymanager.app.ui.screens.DashboardScreen
import com.moneymanager.app.ui.screens.ExportScreen
import com.moneymanager.app.ui.screens.TransactionDetailScreen
import com.moneymanager.app.ui.screens.TransactionListScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object TransactionList : Screen("transactions", "Transactions", Icons.Default.List)
    object TransactionDetail : Screen("transaction/{id}", "Detail", Icons.Default.List) {
        fun createRoute(id: Long = -1L) = "transaction/$id"
    }
    object Export : Screen("export", "Export", Icons.Default.Share)
}

@Composable
fun AppNavigation(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = Screen.Dashboard.route, modifier = modifier) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(navController = navController)
        }
        composable(Screen.TransactionList.route) {
            TransactionListScreen(navController = navController)
        }
        composable(
            route = Screen.TransactionDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: -1L
            TransactionDetailScreen(transactionId = id, navController = navController)
        }
        composable(Screen.Export.route) {
            ExportScreen(navController = navController)
        }
    }
}
