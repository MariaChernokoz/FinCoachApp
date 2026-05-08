package com.example.fincoach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fincoach.ui.FinCoachBottomBar
import com.example.fincoach.ui.screens.analytics.AnalyticsScreen
import com.example.fincoach.ui.screens.assistant.AssistantScreen
import com.example.fincoach.ui.screens.settings.SettingsScreen
import com.example.fincoach.ui.screens.transactions.AddTransactionScreen
import com.example.fincoach.ui.screens.transactions.TransactionHistoryScreen
import com.example.fincoach.ui.screens.transactions.TransactionsScreen
import com.example.fincoach.ui.theme.BgLightGray
import com.example.fincoach.ui.theme.FinCoachTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() убран — он вызывал полоску системной навигации
        setContent {
            FinCoachTheme {
                FinCoachApp()
            }
        }
    }
}

@Composable
fun FinCoachApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarRoutes = setOf("transactions", "history", "analytics", "assistant", "settings")

    Scaffold(
        containerColor = BgLightGray,
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                FinCoachBottomBar(
                    activeTab = when (currentRoute) {
                        "history"      -> 0
                        "transactions" -> 1
                        "analytics"    -> 2
                        "assistant"    -> 3
                        "settings"     -> 4
                        else           -> 1
                    },
                    onNavigate = { route ->
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo("transactions") { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = if (Firebase.auth.currentUser != null) "transactions" else "auth",
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable("auth") {
                RegistrationScreen(onSuccess = {
                    navController.navigate("transactions") {
                        popUpTo("auth") { inclusive = true }
                    }
                })
            }
            composable("transactions") {
                TransactionsScreen(
                    onNavigateToHistory  = { navController.navigate("history") },
                    onNavigateToAdd      = { navController.navigate("add_transaction") },
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
            composable("history") {
                TransactionHistoryScreen(onBack = { navController.popBackStack() })
            }
            composable("analytics") {
                AnalyticsScreen()
            }
            composable("settings") {
                SettingsScreen(
                    onBack   = { navController.popBackStack() },
                    onLogout = {
                        navController.navigate("auth") { popUpTo(0) { inclusive = true } }
                    }
                )
            }
            composable("add_transaction") {
                AddTransactionScreen(
                    onBack    = { navController.popBackStack() },
                    onSuccess = { navController.popBackStack() }
                )
            }
            composable("assistant") {
                AssistantScreen()
            }
        }
    }
}