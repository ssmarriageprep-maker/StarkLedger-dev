package com.starklabs.moneytracker.ui

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object AddTransaction : Screen("add_transaction")
    object Analytics : Screen("analytics")
    object Wallets : Screen("wallets")
    object Security : Screen("security")
    object Settings : Screen("settings")
    object AddAccount : Screen("add_account")
    object History : Screen("history")
    object AccountDetail : Screen("account_detail/{accountId}") {
        fun createRoute(accountId: Int) = "account_detail/$accountId"
    }
}
