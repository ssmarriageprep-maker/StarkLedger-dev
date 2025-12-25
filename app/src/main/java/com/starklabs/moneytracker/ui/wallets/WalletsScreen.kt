package com.starklabs.moneytracker.ui.wallets

import android.content.Context
import android.provider.Telephony
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.starklabs.moneytracker.data.Account
import com.starklabs.moneytracker.data.MoneyRepository
import com.starklabs.moneytracker.ui.Screen
import com.starklabs.moneytracker.ui.components.GlassCard
import com.starklabs.moneytracker.ui.components.NeonText
import com.starklabs.moneytracker.ui.components.NeonButton
import com.starklabs.moneytracker.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

class WalletsViewModel(private val repository: MoneyRepository) : ViewModel() {
    val accounts = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun scanForAccounts(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val projection = arrayOf("address", "body")
            val cursor = context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                null,
                null,
                "date DESC LIMIT 500" // Scan last 500 messages
            )

            val foundAccounts = mutableSetOf<String>() // Set of last 4 digits
            
            cursor?.use {
                val bodyIdx = it.getColumnIndex("body")
                while (it.moveToNext()) {
                    val body = it.getString(bodyIdx) ?: continue
                    // Regex to find "ending 1234", "ac X1234", "card XX1234"
                    val regex = Regex("(?i)(?:ac|a\\/c|ending|no\\.|card|account)\\s+(?:no\\.|with)?\\s*(?:[xX*.]*(\\d{4}))")
                    val match = regex.find(body)
                    if (match != null) {
                        val last4 = match.groupValues[1]
                        foundAccounts.add(last4)
                    }
                }
            }
            
            // Check if these accounts exist, if not create them
            foundAccounts.forEach { last4 ->
                val existing = repository.findAccountForSms(last4)
                if (existing == null) {
                    val newAccount = Account(
                        name = "Bank - $last4",
                        type = "BANK", // Default to Bank, user can change later
                        balance = 0.0,
                        maskedNumber = last4,
                        colorHex = "#FFD700" // Gold
                    )
                    repository.addAccount(newAccount)
                }
            }
            
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Scanned & Added ${foundAccounts.size} potential accounts", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

class WalletsViewModelFactory(private val repository: MoneyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return WalletsViewModel(repository) as T
    }
}

@Composable
fun WalletsScreen(
    navController: androidx.navigation.NavController,
    viewModel: WalletsViewModel
) {
    val accounts by viewModel.accounts.collectAsState()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.scanForAccounts(context)
            } else {
                Toast.makeText(context, "Permission Denied: Cannot scan SMS", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Scaffold(
        containerColor = StarkBlack,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddAccount.route) },
                containerColor = NeonCyan,
                contentColor = StarkBlack
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Account")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                }
                NeonText(text = "ACCOUNTS", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.weight(1f))
                
                // Scan Button
                IconButton(onClick = {
                    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
                         viewModel.scanForAccounts(context)
                    } else {
                         launcher.launch(android.Manifest.permission.READ_SMS)
                    }
                }) {
                   Icon(Icons.Default.Message, contentDescription = "Scan SMS", tint = NeonCyan)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (accounts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    NeonText("No Accounts Found. Add Manually or Scan SMS.", color = TextGrey)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(accounts) { account ->
                        AccountCard(account)
                    }
                }
            }
        }
    }
}

@Composable
fun AccountCard(account: Account) {
    val color = try { Color(android.graphics.Color.parseColor(account.colorHex)) } catch (e: Exception) { NeonCyan }
    
    val icon = when (account.type) {
        "CASH" -> Icons.Default.AccountBalanceWallet
        "BANK" -> Icons.Default.AccountBalance
        "CREDIT_CARD" -> Icons.Default.CreditCard
        "UPI" -> Icons.Default.QrCode
        else -> Icons.Default.AccountBalance
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = color.copy(alpha = 0.8f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon Box
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(color.copy(alpha = 0.2f), shape = MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color)
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    NeonText(text = account.name, color = TextWhite, style = MaterialTheme.typography.titleMedium)
                    NeonText(text = account.type, color = TextGrey, style = MaterialTheme.typography.labelSmall)
                    if (account.maskedNumber != null) {
                        NeonText(text = "••• ${account.maskedNumber}", color = TextGrey, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            
            NeonText(
                text = "₹${String.format("%.2f", account.balance)}",
                color = if (account.balance < 0) HotRed else color,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}
