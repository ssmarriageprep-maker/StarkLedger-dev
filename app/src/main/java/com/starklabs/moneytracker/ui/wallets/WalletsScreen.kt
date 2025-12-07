package com.starklabs.moneytracker.ui.wallets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.starklabs.moneytracker.data.Account
import com.starklabs.moneytracker.data.MoneyRepository
import com.starklabs.moneytracker.ui.components.GlassCard
import com.starklabs.moneytracker.ui.components.NeonText
import com.starklabs.moneytracker.ui.theme.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class WalletsViewModel(repository: MoneyRepository) : ViewModel() {
    val accounts = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

class WalletsViewModelFactory(private val repository: MoneyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return WalletsViewModel(repository) as T
    }
}

@Composable
fun WalletsScreen(
    navController: NavController,
    viewModel: WalletsViewModel
) {
    val accounts by viewModel.accounts.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(StarkBlack)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                }
                NeonText(text = "ACCOUNTS", style = MaterialTheme.typography.headlineMedium)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(accounts) { account ->
                    AccountCard(account)
                }
            }
        }
    }
}

@Composable
fun AccountCard(account: Account) {
    val color = try { Color(android.graphics.Color.parseColor(account.colorHex)) } catch (e: Exception) { NeonCyan }
    
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = color
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                NeonText(text = account.name, color = TextWhite, style = MaterialTheme.typography.titleMedium)
                NeonText(text = account.type, color = TextGrey, style = MaterialTheme.typography.labelSmall)
                if (account.maskedNumber != null) {
                    NeonText(text = "••• ${account.maskedNumber}", color = TextGrey, style = MaterialTheme.typography.labelSmall)
                }
            }
            NeonText(
                text = "₹${String.format("%.2f", account.balance)}",
                color = color,
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}
