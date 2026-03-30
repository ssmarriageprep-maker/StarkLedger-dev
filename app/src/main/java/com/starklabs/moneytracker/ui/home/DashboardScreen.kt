package com.starklabs.moneytracker.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.starklabs.moneytracker.ui.Screen
import com.starklabs.moneytracker.ui.components.*
import com.starklabs.moneytracker.ui.theme.*

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var transactionToEdit by remember { mutableStateOf<com.starklabs.moneytracker.data.Transaction?>(null) }
    
    if (transactionToEdit != null) {
        AlertDialog(
            onDismissRequest = { transactionToEdit = null },
            title = { Text("Edit Category") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    items(categories) { cat ->
                        Text(
                            text = cat.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    transactionToEdit?.let {
                                        viewModel.updateTransactionCategory(it.id, cat.id, it.merchant)
                                    }
                                    transactionToEdit = null
                                }
                                .padding(16.dp),
                            color = OnSurface
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { transactionToEdit = null }) {
                    Text("Cancel", color = Primary)
                }
            },
            containerColor = SurfaceContainer
        )
    }

    Scaffold(
        containerColor = SurfaceContainerLowest,
        topBar = {
            StarkHeader(
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddTransaction.route) },
                containerColor = PrimaryContainer,
                contentColor = OnPrimary,
                shape = CircleShape,
                modifier = Modifier.offset(y = (-16).dp)
            ) {
                Icon(Icons.Sharp.Add, contentDescription = "Add Transaction", modifier = Modifier.size(32.dp))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
        ) {
            // 1. Total Balance Hero Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CURRENT LIQUIDITY",
                        style = StarkTypography.labelSmall,
                        color = OnSurfaceVariant,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "₹",
                            style = StarkTypography.headlineLarge,
                            color = Primary,
                            modifier = Modifier.padding(bottom = 8.dp, end = 4.dp),
                            fontWeight = FontWeight.Light
                        )
                        Text(
                            text = String.format("%,.2f", state.balance),
                            style = StarkTypography.displayLarge.copy(fontSize = 56.sp),
                            color = OnSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = TertiaryContainer.copy(alpha = 0.1f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, TertiaryContainer.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Sharp.TrendingUp, contentDescription = null, tint = TertiaryContainer, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "+12.4% this month",
                                style = StarkTypography.labelSmall,
                                color = TertiaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 2. Bento Grid
            item {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(1000)) + slideInVertically(tween(1000), initialOffsetY = { it / 2 })
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Left Column: Income vs Expenses
                        Column(modifier = Modifier.weight(1.2f)) {
                            // Income Card
                            StarkCard(modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("MONTHLY INCOME", style = StarkTypography.labelSmall)
                                    Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(12.dp)).background(TertiaryContainer.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Sharp.ArrowDownward, contentDescription = null, tint = TertiaryContainer, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "₹${String.format("%,.0f", state.totalIncome)}",
                                    style = StarkTypography.headlineMedium.copy(fontSize = if (state.totalIncome > 1000000) 20.sp else 24.sp)
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(SurfaceContainerHigh)) {
                                    Box(modifier = Modifier.fillMaxWidth().fillMaxHeight().background(TertiaryContainer))
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text("₹${String.format("%,.0f", state.totalIncome)}", style = StarkTypography.headlineMedium)
                            Spacer(modifier = Modifier.height(24.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(SurfaceContainerHigh)) {
                                Box(modifier = Modifier.fillMaxWidth().fillMaxHeight().background(TertiaryContainer))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Expense Card
                        StarkCard(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("MONTHLY EXPENSES", style = StarkTypography.labelSmall)
                                Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(12.dp)).background(Error.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Sharp.ArrowUpward, contentDescription = null, tint = Error, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text("₹${String.format("%,.0f", state.totalSpent)}", style = StarkTypography.headlineMedium)
                            Spacer(modifier = Modifier.height(24.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(SurfaceContainerHigh)) {
                                Box(modifier = Modifier.fillMaxWidth(state.budgetProgress.coerceIn(0f, 1f)).fillMaxHeight().background(Error))
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        // Right Column: Budget Health
                        StarkCard(modifier = Modifier.weight(1f).heightIn(min = 340.dp), contentPadding = PaddingValues(12.dp)) {
                            Text("Budget\nHealth", style = StarkTypography.titleMedium.copy(lineHeight = 18.sp), maxLines = 2)
                            Spacer(modifier = Modifier.weight(1f))
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(128.dp).align(Alignment.CenterHorizontally)) {
                                CircularProgressIndicator(
                                    progress = { 1f },
                                    modifier = Modifier.fillMaxSize(),
                                    color = SurfaceContainerHigh,
                                    strokeWidth = 12.dp,
                                    trackColor = Color.Transparent,
                                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                                Text(
                                    text = "SAFE",
                                    style = StarkTypography.labelSmall.copy(fontSize = 10.sp),
                                    color = OnSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Surface(
                                color = SurfaceContainerHigh,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().border(BorderStroke(1.dp, SecondaryContainer.copy(alpha = 0.5f)), RoundedCornerShape(16.dp))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "AI INSIGHT", 
                                        style = StarkTypography.labelSmall.copy(color = SecondaryContainer, fontSize = 9.sp),
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Safe to spend\n₹${String.format("%.0f", (state.balance / 30))}/day",
                                        style = StarkTypography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                                        color = OnSurface,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Transactions Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transactions",
                        style = StarkTypography.headlineSmall,
                        color = OnSurface
                    )
                    Text(
                        text = "VIEW ALL",
                        style = StarkTypography.labelLarge.copy(color = PrimaryContainer, letterSpacing = 1.sp),
                        modifier = Modifier.clickable { navController.navigate(Screen.History.route) }
                    )
                }
            }

            if (state.recentTransactions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No recent activity", style = StarkTypography.bodyMedium, color = OnSurfaceVariant)
                    }
                }
            } else {
                items(state.recentTransactions) { t ->
                    TransactionRow(transaction = t, onClick = { transactionToEdit = t })
                }
            }
        }
    }
}
