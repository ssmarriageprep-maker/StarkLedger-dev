package com.starklabs.moneytracker.ui

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object AddTransaction : Screen("add_transaction")
    object Analytics : Screen("analytics")
    object Wallets : Screen("wallets")
    object Categories : Screen("categories")
    object Security : Screen("security")
    object Settings : Screen("settings")
}
