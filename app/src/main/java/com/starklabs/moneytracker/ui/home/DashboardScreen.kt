package com.starklabs.moneytracker.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.starklabs.moneytracker.ui.Screen
import com.starklabs.moneytracker.ui.components.*
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
            .background(StarkBackground)
    ) {
        // Decorative background scanning lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridStep = 50.dp.toPx()
            val gridWidth = size.width
            val gridHeight = size.height

            var x = 0f
            while (x < gridWidth) {
                drawLine(
                    color = InfoBlue.copy(alpha = 0.05f),
                    start = Offset(x, 0f),
                    end = Offset(x, gridHeight),
                    strokeWidth = 1f
                )
                x += gridStep
            }

            var y = 0f
            while (y < gridHeight) {
                drawLine(
                    color = InfoBlue.copy(alpha = 0.05f),
                    start = Offset(0f, y),
                    end = Offset(gridWidth, y),
                    strokeWidth = 1f
                )
                y += gridStep
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    NeonText(text = "STARK INDUSTRIES", style = MaterialTheme.typography.labelSmall, color = TextWhite)
                    NeonText(text = "LEDGER // PROTOCOL 1.2", color = NeonCyan, style = MaterialTheme.typography.titleMedium)
                    NeonText(text = "SYSTEM STATUS: SECURE", color = IncomeGreen, style = MaterialTheme.typography.labelSmall)
                }
                Row {
                    HudButton(text = "ACCOUNTS", onClick = { navController.navigate(Screen.Wallets.route) }, color = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    HudButton(text = "CONFIG", onClick = { navController.navigate(Screen.Settings.route) }, color = TextGrey)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            // Central Arc Reactor (Balance)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clickable { navController.navigate(Screen.Analytics.route) },
                contentAlignment = Alignment.Center
            ) {
                ArcReactor(
                    percentage = state.budgetProgress,
                    modifier = Modifier.size(240.dp),
                    color = if (state.budgetProgress > 0.9f) ExpenseRed else NeonCyan
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    NeonText(text = "TOTAL LIQUIDITY", color = TextGrey, style = MaterialTheme.typography.labelSmall)
                    NeonText(
                        text = "₹${String.format("%,.0f", state.balance)}",
                        style = MaterialTheme.typography.displayMedium,
                        color = TextWhite
                    )
                    NeonText(
                        text = "BUDGET: ${(state.budgetProgress * 100).toInt()}%",
                        color = if (state.budgetProgress > 0.9f) ExpenseRed else JarvisGold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Stats
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItemCard("MONTHLY IN", state.totalIncome, IncomeGreen, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(16.dp))
                StatItemCard("MONTHLY OUT", state.totalSpent, JarvisOrange, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(2.dp, 16.dp).background(JarvisGold))
                    Spacer(modifier = Modifier.width(8.dp))
                    NeonText(text = "TRANSACTION LOGS", color = JarvisGold, style = MaterialTheme.typography.titleSmall)
                }

                TextButton(onClick = { navController.navigate(Screen.History.route) }) {
                    NeonText(text = "VIEW ALL ARCHIVES //", color = NeonCyan, style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Transaction List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(state.recentTransactions) { t ->
                    TransactionItem(t)
                }
            }
        }
        
        // Floating Action Button Styled
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.BottomEnd) {
             FloatingActionButton(
                 onClick = { navController.navigate(Screen.AddTransaction.route) },
                 containerColor = NeonCyan,
                 contentColor = StarkBlack,
                 shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp)
             ) {
                 Icon(Icons.Sharp.Add, contentDescription = "Add")
             }
        }
    }
}

@Composable
fun StatItemCard(label: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    GlassCard(borderColor = color.copy(alpha = 0.4f), modifier = modifier) {
        NeonText(text = label, color = color.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
        NeonText(text = "₹${String.format("%,.0f", amount)}", color = color, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun TransactionItem(t: com.starklabs.moneytracker.data.Transaction) {
    GlassCard(borderColor = (if (t.type == "DEBIT") JarvisOrange else IncomeGreen).copy(alpha = 0.2f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                NeonText(text = t.merchant.uppercase(), style = MaterialTheme.typography.titleSmall, color = TextWhite)
                NeonText(text = formatDate(t.date), style = MaterialTheme.typography.bodySmall, color = TextGrey)
            }
            Column(horizontalAlignment = Alignment.End) {
                NeonText(
                    text = (if (t.type == "DEBIT") "-" else "+") + "₹${String.format("%,.2f", t.amount)}",
                    color = if (t.type == "DEBIT") JarvisOrange else IncomeGreen,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM // HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
