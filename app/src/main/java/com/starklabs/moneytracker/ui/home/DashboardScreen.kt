package com.starklabs.moneytracker.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.starklabs.moneytracker.ui.Screen
import com.starklabs.moneytracker.ui.components.*
import com.starklabs.moneytracker.ui.theme.*

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = StarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddTransaction.route) },
                containerColor = AccentPrimary,
                contentColor = StarkBlack,
                shape = CircleShape
            ) {
                Icon(Icons.Sharp.Add, contentDescription = "Add Transaction")
            }
        }
    ) { paddingValues ->
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 40.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. Balance Section
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TOTAL BALANCE",
                            style = StarkTypography.labelSmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "₹${String.format("%,.0f", state.balance)}",
                            style = StarkTypography.displayLarge,
                            color = TextPrimary
                        )
                    }
                }

                // 2. Budget Health & Monthly Summary
                item {
                    StarkCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                StarkStat(label = "Monthly In", value = "₹${String.format("%,.0f", state.totalIncome)}", valueColor = IncomeGreen)
                                Spacer(modifier = Modifier.height(16.dp))
                                StarkStat(label = "Monthly Out", value = "₹${String.format("%,.0f", state.totalSpent)}", valueColor = TextPrimary)
                            }
                            
                            // Budget Health Circle
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                                CircularProgressIndicator(
                                    progress = { 1f },
                                    modifier = Modifier.fillMaxSize(),
                                    color = StarkSurfaceVariant,
                                    strokeWidth = 6.dp,
                                    trackColor = Color.Transparent
                                )
                                CircularProgressIndicator(
                                    progress = { state.budgetProgress },
                                    modifier = Modifier.fillMaxSize(),
                                    color = if (state.budgetProgress > 0.9f) ExpenseRed else AccentSecondary,
                                    strokeWidth = 6.dp,
                                    trackColor = Color.Transparent
                                )
                                Text(
                                    text = "${(state.budgetProgress * 100).toInt()}%",
                                    style = StarkTypography.labelLarge,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                // 3. Settings / Config Link (Extremely minimal)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                         Text(
                             text = "Go to Settings",
                             style = StarkTypography.labelSmall,
                             color = TextSecondary,
                             modifier = Modifier.clickable { navController.navigate(Screen.Settings.route) }
                         )
                    }
                }

                // 4. Header for Recent Transactions
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Logs",
                            style = StarkTypography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "View All",
                            style = StarkTypography.labelLarge,
                            color = AccentSecondary,
                            modifier = Modifier.clickable { navController.navigate(Screen.History.route) }
                        )
                    }
                }

                // 5. Recent Transactions List (LazyColumn ensures high performance)
                if (state.recentTransactions.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No recent logs", style = StarkTypography.bodyMedium, color = TextSecondary)
                        }
                    }
                } else {
                    items(state.recentTransactions) { t ->
                        TransactionRow(transaction = t)
                    }
                }
            }
        }
    }
}
