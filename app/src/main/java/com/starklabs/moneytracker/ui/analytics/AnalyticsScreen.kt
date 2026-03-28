package com.starklabs.moneytracker.ui.analytics

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.sharp.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.starklabs.moneytracker.ui.components.*
import com.starklabs.moneytracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    navController: NavController,
    viewModel: AnalyticsViewModel
) {
    val state by viewModel.uiState.collectAsState()
    
    Scaffold(
        containerColor = SurfaceContainerLowest,
        topBar = {
            StarkHeader(
                title = "StarkLedger",
                onSettingsClick = { navController.navigate(com.starklabs.moneytracker.ui.Screen.Settings.route) }
            )
        }
    ) { paddingValues ->
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Insight Banner
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(800)) + slideInVertically(tween(800), initialOffsetY = { it / 2 })
            ) {
            Surface(
                color = SurfaceContainerLow,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = OutlineVariant.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp))
                    .border(width = 2.dp, color = state.pulseColor, shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("WEEKLY PULSE", style = StarkTypography.labelSmall.copy(color = state.pulseColor, fontWeight = FontWeight.SemiBold))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.pulseTitle,
                            style = StarkTypography.headlineMedium.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                            color = OnSurface,
                            lineHeight = 30.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.pulseDescription,
                            style = StarkTypography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Surface(
                        color = SurfaceContainerHigh,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Sharp.Lightbulb, contentDescription = null, tint = state.pulseColor, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 1. Spending Velocity (High-Impact Line Chart)
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000)) + slideInVertically(tween(1000), initialOffsetY = { it / 3 })
            ) {
                StarkCard(modifier = Modifier.fillMaxWidth().height(280.dp), contentPadding = PaddingValues(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column {
                            Text("Spending Velocity", style = StarkTypography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                            Text("JAN 01 — JUN 30", style = StarkTypography.labelSmall.copy(letterSpacing = 1.sp))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("₹${String.format("%,.0f", state.totalExpense)}", style = StarkTypography.headlineMedium.copy(color = PrimaryContainer))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Sharp.TrendingUp, contentDescription = null, tint = Tertiary, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("4.2% GROWTH", style = StarkTypography.labelSmall.copy(color = Tertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        GlowingLineChart(
                            data = listOf(0.2f, 0.4f, 0.3f, 0.6f, 0.8f, 1.0f),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN").forEach { month ->
                            Text(month, style = StarkTypography.labelSmall.copy(fontSize = 10.sp, color = OnSurfaceVariant.copy(alpha = 0.6f)))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Composition (Horizontal Breakdown)
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1200)) + slideInVertically(tween(1200), initialOffsetY = { it / 3 })
            ) {
                StarkCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
                    Text("Composition", style = StarkTypography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                    Text("BY PERCENTAGE", style = StarkTypography.labelSmall.copy(letterSpacing = 1.sp))
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        // Donut on Left
                        Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                            AnimatedDonutChart(slices = state.pieSlices, modifier = Modifier.fillMaxSize())
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("78%", style = StarkTypography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = OnSurface)
                                Text("TRACKED", style = StarkTypography.labelSmall.copy(fontSize = 9.sp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(32.dp))
                        
                        // Detailed Legend on Right
                        Column(modifier = Modifier.weight(1f)) {
                            state.categoryPerformance.take(4).forEach { perf ->
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(perf.color))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(perf.name, style = StarkTypography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = OnSurface)
                                        }
                                        Text("${(perf.percentage * 100).toInt()}%", style = StarkTypography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(modifier = Modifier.fillMaxWidth().height(2.dp).clip(CircleShape).background(SurfaceContainerHigh)) {
                                        Box(modifier = Modifier.fillMaxWidth(perf.percentage.coerceIn(0f, 1f)).fillMaxHeight().background(perf.color))
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            // 3. Category Comparison (Benchmarking)
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1400)) + slideInVertically(tween(1400), initialOffsetY = { it / 3 })
            ) {
                StarkCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Benchmarking", style = StarkTypography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                        Icon(Icons.Sharp.FilterList, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Column {
                        state.categoryPerformance.take(4).forEach { perf ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                                    Text(perf.name, style = StarkTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                                    Text("₹${String.format("%.0f", perf.spent)} / ₹${String.format("%.0f", perf.budget)} Budget", style = StarkTypography.labelSmall.copy(color = OnSurfaceVariant))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(SurfaceContainerHighest)) {
                                    Box(modifier = Modifier.fillMaxWidth(perf.percentage.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(if (perf.percentage >= 1f) Error else PrimaryContainer))
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // 4. Active Timeline (Premium Feed)
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1600)) + slideInVertically(tween(1600), initialOffsetY = { it / 3 })
            ) {
                StarkCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
                    Text("Transaction Flow", style = StarkTypography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(24.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        state.recentTransactions.take(5).forEachIndexed { index, transaction ->
                            val isDebit = transaction.type == "DEBIT"
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                                // Date Column
                                Column(modifier = Modifier.width(56.dp), horizontalAlignment = Alignment.Start) {
                                    Text(formatStarkDate(transaction.date).uppercase(), style = StarkTypography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OnSurfaceVariant))
                                    Text(java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(transaction.date)), style = StarkTypography.labelSmall.copy(fontSize = 9.sp))
                                }
                                
                                // Category Icon with Timeline Line
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(if(isDebit) SurfaceContainerHighest else transaction.categoryColor.copy(alpha = 0.2f)).border(width = 1.dp, color = OutlineVariant.copy(alpha = 0.2f), shape = CircleShape), contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = when(transaction.categoryName.lowercase()) {
                                                "food", "dining" -> Icons.Sharp.Restaurant
                                                "shopping" -> Icons.Sharp.ShoppingBag
                                                "salary" -> Icons.Sharp.AccountBalanceWallet
                                                "travel", "transport" -> Icons.Sharp.DirectionsCar
                                                else -> Icons.Sharp.ReceiptLong
                                            }, 
                                            contentDescription = null, 
                                            tint = if (isDebit) OnSurfaceVariant else transaction.categoryColor, 
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    if (index < 4) {
                                        Box(modifier = Modifier.width(2.dp).height(24.dp).background(OutlineVariant.copy(alpha = 0.2f)))
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                // Merchant & Category
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(transaction.merchant, style = StarkTypography.bodyLarge.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                                    Text(transaction.categoryName.uppercase(), style = StarkTypography.labelSmall.copy(color = OnSurfaceVariant, letterSpacing = 0.5.sp))
                                }
                                
                                // Amount
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${if(isDebit) "-" else "+"}₹${String.format("%,.0f", transaction.amount)}", style = StarkTypography.titleMedium.copy(fontWeight = FontWeight.Bold, color = if(isDebit) OnSurface else TertiaryContainer))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
