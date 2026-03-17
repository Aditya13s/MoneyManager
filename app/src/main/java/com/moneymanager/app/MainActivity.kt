package com.moneymanager.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.moneymanager.app.ui.navigation.AppNavigation
import com.moneymanager.app.ui.navigation.Screen
import com.moneymanager.app.ui.theme.MoneyManagerTheme
import com.moneymanager.app.viewmodel.DashboardViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MoneyManagerTheme {
                val dashboardViewModel: DashboardViewModel = hiltViewModel()
                val dashboardState by dashboardViewModel.state.collectAsState()

                val isOnboardingShown = dashboardState.isOnboardingShown
                if (isOnboardingShown != null) {
                    val startDestination = if (isOnboardingShown) {
                        Screen.Dashboard.route
                    } else {
                        Screen.Onboarding.route
                    }

                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    // Hide bottom nav on full-screen / detail routes
                    val fullScreenRoutes = setOf(
                        Screen.Onboarding.route,
                        Screen.Settings.route,
                        Screen.TransactionDetail.route
                    )
                    val showBottomBar = currentRoute !in fullScreenRoutes &&
                        !currentRoute.orEmpty().startsWith("transaction/")

                    data class NavItem(val screen: Screen, val icon: ImageVector, val label: String)
                    val navItems = listOf(
                        NavItem(Screen.Dashboard, Icons.Default.Dashboard, "Home"),
                        NavItem(Screen.TransactionList, Icons.Default.List, "Txns"),
                        NavItem(Screen.Export, Icons.Default.Share, "Export")
                    )

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            if (showBottomBar) {
                                // Minimal floating pill nav bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Background pill
                                    Row(
                                        modifier = Modifier
                                            .shadow(12.dp, RoundedCornerShape(50))
                                            .clip(RoundedCornerShape(50))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                                    ) {
                                        // Left items
                                        navItems.take(2).forEach { item ->
                                            val selected = currentRoute == item.screen.route
                                            val tint = if (selected) MaterialTheme.colorScheme.primary
                                                       else MaterialTheme.colorScheme.onSurfaceVariant
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .clickable {
                                                        navController.navigate(item.screen.route) {
                                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    }
                                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(item.icon, item.label, tint = tint, modifier = Modifier.size(22.dp))
                                            }
                                        }

                                        // Centre Add FAB
                                        Spacer(Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                                .clickable {
                                                    navController.navigate(Screen.TransactionDetail.createRoute())
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                "Add transaction",
                                                tint = Color.White,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))

                                        // Right items
                                        navItems.drop(2).forEach { item ->
                                            val selected = currentRoute == item.screen.route
                                            val tint = if (selected) MaterialTheme.colorScheme.primary
                                                       else MaterialTheme.colorScheme.onSurfaceVariant
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .clickable {
                                                        navController.navigate(item.screen.route) {
                                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    }
                                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(item.icon, item.label, tint = tint, modifier = Modifier.size(22.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        AppNavigation(
                            navController = navController,
                            startDestination = startDestination,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

