package com.starklabs.moneytracker.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.starklabs.moneytracker.data.MoneyRepository
import com.starklabs.moneytracker.ui.Screen
import com.starklabs.moneytracker.ui.components.ArcReactor
import com.starklabs.moneytracker.ui.components.GlassCard
import com.starklabs.moneytracker.ui.components.HudButton
import com.starklabs.moneytracker.ui.components.NeonText
import com.starklabs.moneytracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StarkBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    NeonText(text = "STARK LEDGER // v1.1", style = MaterialTheme.typography.labelSmall)
                    NeonText(text = "SYSTEM: ONLINE", color = JarvisGold, style = MaterialTheme.typography.labelSmall)
                }
                Row {
                    HudButton(text = "ACCOUNTS", onClick = { navController.navigate(Screen.Wallets.route) }, color = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    HudButton(text = "CONFIG", onClick = { navController.navigate(Screen.Settings.route) }, color = com.starklabs.moneytracker.ui.theme.TextGrey)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Central Arc Reactor (Balance)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clickable { navController.navigate(Screen.Analytics.route) },
                contentAlignment = Alignment.Center
            ) {
                ArcReactor(
                    percentage = state.budgetProgress,
                    modifier = Modifier.size(200.dp),
                    color = if (state.budgetProgress > 0.9f) MetallicRed else NeonCyan
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    NeonText(text = "BALANCE", color = TextGrey, style = MaterialTheme.typography.labelSmall)
                    NeonText(
                        text = "₹${String.format("%.0f", state.balance)}",
                        style = MaterialTheme.typography.displayMedium,
                        color = TextWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Stats
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("INCOME", state.totalIncome, NeonCyan)
                StatItem("SPENT", state.totalSpent, JarvisOrange)
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            NeonText(text = "RECENT LOGS", color = JarvisGold)
            Spacer(modifier = Modifier.height(8.dp))

            // Transaction List
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.recentTransactions) { t ->
                    GlassCard(borderColor = NeonCyanDim.copy(alpha = 0.3f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                NeonText(text = t.merchant, style = MaterialTheme.typography.titleMedium, color = TextWhite)
                                NeonText(text = formatDate(t.date), style = MaterialTheme.typography.bodySmall, color = TextGrey)
                            }
                            NeonText(
                                text = (if (t.type == "DEBIT") "-" else "+") + "₹${t.amount}",
                                color = if (t.type == "DEBIT") JarvisOrange else NeonCyan
                            )
                        }
                    }
                }
            }
        }
        
        // Floating Action Button Styled
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomEnd) {
             FloatingActionButton(
                 onClick = { navController.navigate(Screen.AddTransaction.route) },
                 containerColor = NeonCyan,
                 contentColor = StarkBlack
             ) {
                 Icon(Icons.Default.Add, contentDescription = "Add")
             }
        }
    }
}

@Composable
fun StatItem(label: String, amount: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        NeonText(text = label, color = color.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
        NeonText(text = "₹${String.format("%.0f", amount)}", color = color, style = MaterialTheme.typography.titleLarge)
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
