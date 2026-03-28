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

            // Main Grid
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000)) + slideInVertically(tween(1000), initialOffsetY = { it / 2 })
            ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Spending Velocity (Line Chart)
                StarkCard(modifier = Modifier.weight(1.1f).height(400.dp), contentPadding = PaddingValues(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Column {
                            Text("Spending Velocity", style = StarkTypography.titleLarge)
                            Text("JAN 01 — JUN 30", style = StarkTypography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("₹${String.format("%,.0f", state.totalExpense)}", style = StarkTypography.headlineSmall.copy(color = PrimaryContainer))
                            Text("+4.2% GROWTH", style = StarkTypography.labelSmall.copy(color = Tertiary, fontSize = 10.sp))
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        // Mock data for line chart
                        GlowingLineChart(
                            data = listOf(0.2f, 0.4f, 0.3f, 0.6f, 0.8f, 1.0f),
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            color = SurfaceContainerHighest,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).border(width = 1.dp, color = PrimaryContainer.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp))
                        ) {
                            Text("PEAK JUNE: ₹3.2K", style = StarkTypography.labelSmall.copy(color = PrimaryContainer, fontSize = 8.sp), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN").forEach { month ->
                            Text(month, style = StarkTypography.labelSmall.copy(fontSize = 10.sp))
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Composition (Pie Chart)
                StarkCard(modifier = Modifier.weight(1f).height(400.dp), contentPadding = PaddingValues(12.dp)) {
                    Text("Composition", style = StarkTypography.titleLarge.copy(fontSize = 18.sp), maxLines = 1)
                    Text("BY PERCENTAGE", style = StarkTypography.labelSmall, maxLines = 1)

                    Spacer(modifier = Modifier.weight(1f))

                    Box(modifier = Modifier.size(140.dp).align(Alignment.CenterHorizontally), contentAlignment = Alignment.Center) {
                        AnimatedDonutChart(slices = state.pieSlices, modifier = Modifier.fillMaxSize())
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("78%", style = StarkTypography.headlineMedium.copy(fontSize = 20.sp), color = OnSurface)
                            Text("TRACKED", style = StarkTypography.labelSmall.copy(fontSize = 9.sp))
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Column {
                        state.categoryPerformance.take(3).forEach { perf ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(perf.color))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(perf.name, style = StarkTypography.bodySmall, color = OnSurface)
                                }
                                Text("${(perf.percentage * 100).toInt()}%", style = StarkTypography.labelSmall)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            // Category Comparison
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1200)) + slideInVertically(tween(1200), initialOffsetY = { it / 2 })
            ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                StarkCard(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Benchmarking", style = StarkTypography.titleLarge)
                        Icon(Icons.Sharp.FilterList, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Column {
                        state.categoryPerformance.take(3).forEach { perf ->
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                                    Text(perf.name, style = StarkTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                    Text("₹${String.format("%.0f", perf.spent)} / ₹${String.format("%.0f", perf.budget)} Budget", style = StarkTypography.labelSmall)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(SurfaceContainerHighest)) {
                                    Box(modifier = Modifier.fillMaxWidth(perf.percentage.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(if (perf.percentage >= 1f) Error else PrimaryContainer))
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Timeline
                StarkCard(modifier = Modifier.weight(1f)) {
                    Text("Active Timeline", style = StarkTypography.titleLarge)
                    Spacer(modifier = Modifier.height(24.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Using a simple vertical line and items
                        state.categoryPerformance.take(3).forEachIndexed { index, perf ->
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(if(index==0) PrimaryContainer else SurfaceContainerHighest).border(width = 1.dp, color = PrimaryContainer.copy(alpha = 0.2f), shape = CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Sharp.ShoppingBag, contentDescription = null, tint = if(index==0) OnPrimary else PrimaryContainer, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Merchant ${index + 1}", style = StarkTypography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                    Text("TODAY, 14:22", style = StarkTypography.labelSmall)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("-₹1,299.00", style = StarkTypography.headlineSmall.copy(fontSize = 18.sp))
                                    Text("HARDWARE", style = StarkTypography.labelSmall)
                                }
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
