package com.starklabs.moneytracker.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.ArrowBack
import androidx.compose.material.icons.sharp.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
        containerColor = StarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Insights", style = StarkTypography.titleLarge, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Sharp.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StarkBackground)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // AI Insights Box
            StarkCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Sharp.Lightbulb, contentDescription = "Insights", tint = AccentSecondary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("AI Insights", style = StarkTypography.titleMedium, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(12.dp))
                val percentage = if (state.totalIncome > 0) ((state.totalExpense / state.totalIncome) * 100).toInt() else 0
                val insightText = if (percentage > 90) {
                    "Warning: You spent $percentage% of your income. Reduce expenses soon."
                } else if (state.topCategory.isNotEmpty()) {
                    "${state.topCategory} is your highest spending category this month."
                } else {
                    "Your spending is well balanced so far."
                }
                Text(text = insightText, style = StarkTypography.bodyMedium, color = TextSecondary)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Overview", style = StarkTypography.titleLarge, color = TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                StarkCard(modifier = Modifier.weight(1f)) {
                    StarkStat(label = "Top Expense", value = state.topCategory.ifEmpty { "None" }, valueColor = TextPrimary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                StarkCard(modifier = Modifier.weight(1f)) {
                    val health = if (state.totalExpense > state.totalIncome) "Poor" else "Good"
                    val healthColor = if (state.totalExpense > state.totalIncome) ExpenseRed else IncomeGreen
                    StarkStat(label = "Health Score", value = health, valueColor = healthColor)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Sector Distribution", style = StarkTypography.titleLarge, color = TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                AnimatedDonutChart(slices = state.pieSlices, modifier = Modifier.size(220.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Spent", style = StarkTypography.labelSmall, color = TextSecondary)
                    Text("₹${String.format("%,.0f", state.totalExpense)}", style = StarkTypography.titleLarge, color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("Expenditure Velocity", style = StarkTypography.titleLarge, color = TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            StarkCard(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                GlowingLineChart(data = state.weeklySpending, modifier = Modifier.fillMaxSize())
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            if (state.categoryPerformance.isNotEmpty()) {
                Text("Category Limits", style = StarkTypography.titleLarge, color = TextPrimary)
                Spacer(modifier = Modifier.height(16.dp))
                state.categoryPerformance.forEach { perf ->
                    CategoryAuditItem(perf)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun CategoryAuditItem(perf: CategoryPerformance) {
    StarkCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(perf.name, style = StarkTypography.titleMedium, color = TextPrimary)
            Text("${(perf.percentage * 100).toInt()}%", style = StarkTypography.titleMedium, color = if (perf.percentage > 0.9f) ExpenseRed else TextSecondary)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { perf.percentage.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = if (perf.percentage > 0.9f) ExpenseRed else AccentSecondary,
            trackColor = StarkSurfaceVariant,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Used: ₹${String.format("%,.0f", perf.spent)}", style = StarkTypography.labelSmall, color = TextSecondary)
            Text("Limit: ₹${String.format("%,.0f", perf.budget)}", style = StarkTypography.labelSmall, color = TextSecondary)
        }
    }
}
