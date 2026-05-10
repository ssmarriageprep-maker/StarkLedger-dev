package com.starklabs.moneytracker.ui.wallets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.starklabs.moneytracker.data.Account
import com.starklabs.moneytracker.data.MoneyRepository
import com.starklabs.moneytracker.data.Transaction
import com.starklabs.moneytracker.data.Category
import com.starklabs.moneytracker.ui.components.*
import com.starklabs.moneytracker.ui.theme.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@Composable
fun AccountDetailScreen(
    navController: NavController,
    accountId: Int,
    repository: MoneyRepository
) {
    var account by remember { mutableStateOf<Account?>(null) }
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }

    LaunchedEffect(accountId) {
        account = repository.getAccount(accountId)
        repository.allTransactions.collect { all ->
            transactions = all.filter { it.accountId == accountId }
        }
    }

    LaunchedEffect(Unit) {
        repository.allCategories.collect { categories = it }
    }

    val scope = rememberCoroutineScope()
    var showRenameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Account") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Account Name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    account?.let {
                        val updated = it.copy(name = newName)
                        account = updated
                        scope.launch {
                            repository.updateAccount(updated)
                        }
                    }
                    showRenameDialog = false
                }) { Text("SAVE") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("CANCEL") }
            },
            containerColor = SurfaceContainer
        )
    }

    Scaffold(
        containerColor = SurfaceContainerLowest,
        topBar = {
            StarkHeader(
                title = account?.name ?: "Account Details",
                onMenuClick = { navController.popBackStack() },
                onSettingsClick = {
                    newName = account?.name ?: ""
                    showRenameDialog = true
                }
            )
        }
    ) { paddingValues ->
        if (account == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            val acc = account!!
            val monthlyTransactions = transactions.filter {
                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                it.date >= cal.timeInMillis
            }
            val spentThisMonth = monthlyTransactions.filter { it.type == "DEBIT" }.sumOf { it.amount }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
                contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
            ) {
                item {
                    StarkCard(modifier = Modifier.fillMaxWidth()) {
                        Text("TOTAL BALANCE", style = StarkTypography.labelSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "₹${String.format("%,.2f", acc.balance)}",
                            style = StarkTypography.headlineLarge.copy(fontSize = 36.sp),
                            color = OnSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = PrimaryContainer.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    acc.type,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = StarkTypography.labelSmall,
                                    color = PrimaryContainer
                                )
                            }
                            if (acc.last4Digits != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("•••• ${acc.last4Digits}", style = StarkTypography.bodySmall, color = OnSurfaceVariant)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("MONTHLY SUMMARY", style = StarkTypography.titleMedium, color = OnSurface)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StarkCard(modifier = Modifier.weight(1f)) {
                            Text("SPENT", style = StarkTypography.labelSmall)
                            Text("₹${String.format("%,.0f", spentThisMonth)}", style = StarkTypography.headlineSmall, color = Error)
                        }
                        StarkCard(modifier = Modifier.weight(1f)) {
                            Text("TXNS", style = StarkTypography.labelSmall)
                            Text("${monthlyTransactions.size}", style = StarkTypography.headlineSmall, color = Primary)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("CATEGORY BREAKDOWN", style = StarkTypography.titleMedium, color = OnSurface)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                val categorySpending = monthlyTransactions.filter { it.type == "DEBIT" }
                    .groupBy { it.categoryId }
                    .map { (catId, txs) ->
                        val cat = categories.find { it.id == catId }
                        cat to txs.sumOf { it.amount }
                    }
                    .sortedByDescending { it.second }

                if (categorySpending.isEmpty()) {
                    item {
                        Text("No data for this month", style = StarkTypography.bodyMedium, color = OnSurfaceVariant)
                    }
                } else {
                    items(categorySpending) { (cat, amount) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(32.dp).clip(CircleShape).background(SurfaceContainerHighest),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CategoryIcon(cat?.name, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(cat?.name ?: "Uncategorized", style = StarkTypography.bodyMedium, color = OnSurface)
                            }
                            Text("₹${String.format("%,.0f", amount)}", style = StarkTypography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = OnSurface)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("RECENT ACTIVITY", style = StarkTypography.titleMedium, color = OnSurface)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                items(transactions.take(10)) { t ->
                    TransactionRow(transaction = t, onClick = { /* Navigate to detail? */ })
                }
            }
        }
    }
}
