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
import kotlinx.coroutines.flow.first
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import com.starklabs.moneytracker.data.AppSettingsRepository
import com.starklabs.moneytracker.sms.SmsScanner

class MainActivity : AppCompatActivity() {
    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            triggerFirstScan()
        }
    }

    private lateinit var repository: MoneyRepository
    private lateinit var appSettingsRepository: AppSettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getDatabase(this)
        repository = MoneyRepository(
            database.transactionDao(),
            database.accountDao(),
            database.categoryDao()
        )
        val securityRepository = com.starklabs.moneytracker.data.SecurityRepository(this)
        appSettingsRepository = AppSettingsRepository(this)
        
        val dashboardViewModelFactory = DashboardViewModelFactory(repository, appSettingsRepository)
        val securityViewModelFactory = com.starklabs.moneytracker.ui.security.SecurityViewModelFactory(securityRepository)
        
        // Ensure database is seeded
        lifecycleScope.launch {
            repository.seedDefaults()
            checkAndTriggerFirstLaunch()
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
                    composable(Screen.AddAccount.route) {
                        com.starklabs.moneytracker.ui.wallets.AddAccountScreen(navController, repository)
                    }
                    composable(Screen.Wallets.route) {
                         val factory = com.starklabs.moneytracker.ui.wallets.WalletsViewModelFactory(repository)
                         val viewModel: com.starklabs.moneytracker.ui.wallets.WalletsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
                         com.starklabs.moneytracker.ui.wallets.WalletsScreen(navController, viewModel)
                    }
                    composable(Screen.Settings.route) {
                        com.starklabs.moneytracker.ui.settings.SettingsScreen(navController, repository, appSettingsRepository)
                    }
                    composable(Screen.History.route) {
                        val factory = com.starklabs.moneytracker.ui.history.HistoryViewModelFactory(repository)
                        val viewModel: com.starklabs.moneytracker.ui.history.HistoryViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
                        com.starklabs.moneytracker.ui.history.HistoryScreen(navController, viewModel)
                    }
                }
            }
        }
    }

    private suspend fun checkAndTriggerFirstLaunch() {
        val isFirst = appSettingsRepository.isFirstLaunch.first()
        if (isFirst) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
                triggerFirstScan()
            } else {
                smsPermissionLauncher.launch(android.Manifest.permission.READ_SMS)
            }
        }
    }

    private fun triggerFirstScan() {
        lifecycleScope.launch {
            val result = SmsScanner.scan(this@MainActivity, repository)
            appSettingsRepository.setFirstLaunch(false)
            android.widget.Toast.makeText(this@MainActivity, "Initial Scan Complete: ${result.transactionsCreated} transactions found.", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
