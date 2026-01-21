package com.starklabs.moneytracker.ui.wallets

import android.content.Context
import android.provider.Telephony
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.starklabs.moneytracker.data.Account
import com.starklabs.moneytracker.ui.Screen
import com.starklabs.moneytracker.ui.components.*
import com.starklabs.moneytracker.ui.theme.*
import android.content.pm.PackageManager
import androidx.compose.foundation.border
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.starklabs.moneytracker.data.MoneyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WalletsViewModel(private val repository: MoneyRepository) : ViewModel() {
    val accounts = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun scanForAccounts(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val projection = arrayOf("address", "body", "date")
            val cursor = context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                null,
                null,
                "date DESC LIMIT 500"
            )

            val sessionAccounts = mutableMapOf<String, Int>()
            var transactionsCreated = 0
            var messagesRejected = 0
            var newAccountsCreated = 0
            
            cursor?.use {
                val addressIdx = it.getColumnIndex("address")
                val bodyIdx = it.getColumnIndex("body")
                val dateIdx = it.getColumnIndex("date")
                
                while (it.moveToNext()) {
                    try {
                        val sender = it.getString(addressIdx) ?: "Unknown"
                        val body = it.getString(bodyIdx) ?: continue
                        val timestamp = it.getLong(dateIdx)
                        
                        val parsed = com.starklabs.moneytracker.domain.SmsParser.parseSms(sender, body, timestamp)
                        
                        if (parsed.isTransaction && parsed.accountLast4 != null) {
                            val last4 = parsed.accountLast4!!
                            
                            var accountId = sessionAccounts[last4]
                            if (accountId == null) {
                                val existingAccount = repository.findAccountForSms(last4)
                                if (existingAccount != null) {
                                    accountId = existingAccount.id
                                } else {
                                    val bankName = parsed.bank ?: "Bank"
                                    val newAccount = Account(
                                        name = "$bankName - $last4",
                                        type = "BANK",
                                        balance = 0.0,
                                        maskedNumber = last4,
                                        colorHex = "#FFD700"
                                    )
                                    val id = repository.addAccount(newAccount)
                                    accountId = id.toInt()
                                    newAccountsCreated++
                                }
                                sessionAccounts[last4] = accountId
                            }
                            
                            val amount = parsed.amount ?: 0.0
                            if (amount > 0.0) {
                                val merchant = parsed.merchant ?: "Unknown Merchant"
                                val transactionType = parsed.transactionType?.uppercase() ?: "DEBIT"
                                
                                val transaction = com.starklabs.moneytracker.data.Transaction(
                                    amount = amount,
                                    merchant = merchant,
                                    date = timestamp,
                                    type = transactionType,
                                    smsBody = body,
                                    accountId = accountId,
                                    categoryId = repository.identifyCategory(merchant, body)
                                )
                                
                                repository.addTransaction(transaction)
                                transactionsCreated++
                            }
                        } else {
                            messagesRejected++
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("WalletsScan", "Error processing message", e)
                    }
                }
            }
            
            withContext(Dispatchers.Main) {
                val summary = "Scan Complete!\n" +
                    "• Accounts Added: $newAccountsCreated\n" +
                    "• Transactions Created: $transactionsCreated\n" +
                    "• Messages Filtered: $messagesRejected"
                Toast.makeText(context, summary, Toast.LENGTH_LONG).show()
            }
        }
    }
}

class WalletsViewModelFactory(private val repository: MoneyRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WalletsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WalletsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
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

    Box(modifier = Modifier.fillMaxSize().background(StarkBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Sharp.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                }
                HudHeader(
                    title = "ACCOUNTS",
                    subtitle = "MANAGING LIQUIDITY SOURCES",
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(onClick = {
                    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
                         viewModel.scanForAccounts(context)
                    } else {
                         launcher.launch(android.Manifest.permission.READ_SMS)
                    }
                }) {
                   Icon(Icons.Sharp.Message, contentDescription = "Scan SMS", tint = NeonCyan)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (accounts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    NeonText("NO ACCOUNTS DETECTED // SCAN OR ADD MANUALLY", color = TextGrey, style = MaterialTheme.typography.labelSmall)
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

        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.BottomEnd) {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddAccount.route) },
                containerColor = NeonCyan,
                contentColor = StarkBlack,
                shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp)
            ) {
                Icon(Icons.Sharp.Add, contentDescription = "Add Account")
            }
        }
    }
}

@Composable
fun AccountCard(account: Account) {
    val color = try { Color(android.graphics.Color.parseColor(account.colorHex)) } catch (e: Exception) { NeonCyan }
    
    val icon = when (account.type) {
        "CASH" -> Icons.Sharp.AccountBalanceWallet
        "BANK" -> Icons.Sharp.AccountBalance
        "CREDIT_CARD" -> Icons.Sharp.CreditCard
        "UPI" -> Icons.Sharp.QrCode
        else -> Icons.Sharp.AccountBalance
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = color.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(color.copy(alpha = 0.1f), shape = CutCornerShape(8.dp))
                        .border(1.dp, color.copy(alpha = 0.3f), shape = CutCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    NeonText(text = account.name.uppercase(), color = TextWhite, style = MaterialTheme.typography.titleSmall)
                    NeonText(text = account.type, color = TextGrey, style = MaterialTheme.typography.labelSmall)
                    if (account.maskedNumber != null) {
                        NeonText(text = "ID: •••• ${account.maskedNumber}", color = TextGrey.copy(alpha = 0.6f), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                NeonText(
                    text = "₹${String.format("%,.2f", account.balance)}",
                    color = if (account.balance < 0) ExpenseRed else color,
                    style = MaterialTheme.typography.titleMedium
                )
                NeonText(text = "STATUS: ACTIVE", color = IncomeGreen.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
