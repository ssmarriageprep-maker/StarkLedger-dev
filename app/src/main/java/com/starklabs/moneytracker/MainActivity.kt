package com.starklabs.moneytracker
 
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.starklabs.moneytracker.data.AppDatabase
import com.starklabs.moneytracker.data.MoneyRepository
import com.starklabs.moneytracker.ui.Screen
import com.starklabs.moneytracker.ui.home.DashboardScreen
import com.starklabs.moneytracker.ui.home.DashboardViewModel
import com.starklabs.moneytracker.ui.home.DashboardViewModelFactory
import com.starklabs.moneytracker.ui.theme.StarkLedgerTheme
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getDatabase(this)
        val repository = MoneyRepository(
            database.transactionDao(),
            database.accountDao(),
            database.categoryDao()
        )
        val securityRepository = com.starklabs.moneytracker.data.SecurityRepository(this)
        
        val dashboardViewModelFactory = DashboardViewModelFactory(repository)
        val securityViewModelFactory = com.starklabs.moneytracker.ui.security.SecurityViewModelFactory(securityRepository)
        
        // Ensure database is seeded
        lifecycleScope.launch {
            repository.seedDefaults()
        }

        setContent {
            StarkLedgerTheme {
                val navController = rememberNavController()
                val dashboardViewModel = dashboardViewModelFactory.create(DashboardViewModel::class.java)
                val securityViewModel = securityViewModelFactory.create(com.starklabs.moneytracker.ui.security.SecurityViewModel::class.java)

                NavHost(navController = navController, startDestination = Screen.Security.route) {
                    composable(Screen.Security.route) {
                        com.starklabs.moneytracker.ui.security.SecurityScreen(navController, securityViewModel)
                    }
                    composable(Screen.Dashboard.route) {
                        DashboardScreen(navController, dashboardViewModel)
                    }
                    composable(Screen.AddTransaction.route) {
                        com.starklabs.moneytracker.ui.add.AddTransactionScreen(navController, repository)
                    }
                    composable(Screen.Analytics.route) {
                        val factory = com.starklabs.moneytracker.ui.analytics.AnalyticsViewModelFactory(repository)
                        val viewModel: com.starklabs.moneytracker.ui.analytics.AnalyticsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
                        com.starklabs.moneytracker.ui.analytics.AnalyticsScreen(navController, viewModel)
                    }
                    composable(Screen.Wallets.route) {
                         val factory = com.starklabs.moneytracker.ui.wallets.WalletsViewModelFactory(repository)
                         val viewModel: com.starklabs.moneytracker.ui.wallets.WalletsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
                         com.starklabs.moneytracker.ui.wallets.WalletsScreen(navController, viewModel)
                    }
                    composable(Screen.Settings.route) {
                        com.starklabs.moneytracker.ui.settings.SettingsScreen(repository)
                    }
                    // Other screens...
                }
            }
        }
    }
}
