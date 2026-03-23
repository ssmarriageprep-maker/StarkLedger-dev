package com.starklabs.moneytracker.ui.analytics

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.sharp.ArrowBack
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
    
    // Add entrance animation trigger
    var isLaunched by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isLaunched = true }
    val entryAlpha by animateFloatAsState(if (isLaunched) 1f else 0f, tween(800), label = "alpha")
    val entryOffset by animateFloatAsState(if (isLaunched) 0f else 40f, tween(800, easing = FastOutSlowInEasing), label = "offset")

    Scaffold(
        containerColor = StarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Insights", style = StarkTypography.titleLarge.copy(fontWeight = FontWeight.Bold), color = TextPrimary) },
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
                .background(StarkBackground)
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Hero Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .offset(y = entryOffset.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Spent This Month", style = StarkTypography.labelLarge, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("₹${String.format("%,.0f", state.totalExpense)}", style = StarkTypography.headlineLarge.copy(fontSize = 42.sp, fontWeight = FontWeight.ExtraBold), color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val balanceColor = if (state.totalIncome > state.totalExpense) IncomeGreen else ExpenseRed
                    val symbol = if (state.totalIncome > state.totalExpense) "+" else ""
                    val balance = state.totalIncome - state.totalExpense
                    
                    ContainerPill(color = balanceColor.copy(alpha = 0.15f)) {
                        Text(
                            text = "Remaining: $symbol₹${String.format("%,.0f", balance)}",
                            style = StarkTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = balanceColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Beautiful Ring Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .offset(y = entryOffset.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.pieSlices.isNotEmpty()) {
                    AnimatedDonutChart(slices = state.pieSlices, modifier = Modifier.size(220.dp))
                } else {
                    Text("No data to display chart", style = StarkTypography.labelLarge, color = TextSecondary)
                }
                
                // Center text for ring hole
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Overview", style = StarkTypography.labelMedium, color = TextSecondary)
                    Text(state.topCategory, style = StarkTypography.titleLarge.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
            Text("Category Breakdown", style = StarkTypography.titleLarge.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))

            if (state.categoryPerformance.isNotEmpty()) {
                state.categoryPerformance.forEachIndexed { index, perf ->
                    CategoryAuditRow(perf)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                Text("No categorized expenses yet.", style = StarkTypography.bodyMedium, color = TextSecondary)
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun ContainerPill(color: Color, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun CategoryAuditRow(perf: CategoryPerformance) {
    // Dynamic Icon Mapping based on typical substrings
    val lowerName = perf.name.lowercase()
    val icon = when {
        "food" in lowerName || "din" in lowerName || "restaurant" in lowerName -> Icons.Rounded.Fastfood
        "travel" in lowerName || "transport" in lowerName || "fuel" in lowerName -> Icons.Rounded.DirectionsCar
        "shop" in lowerName || "cloth" in lowerName || "mall" in lowerName -> Icons.Rounded.ShoppingBag
        "health" in lowerName || "med" in lowerName -> Icons.Rounded.MedicalServices
        "grocery" in lowerName || "mart" in lowerName -> Icons.Rounded.LocalGroceryStore
        "ent" in lowerName || "movie" in lowerName || "fun" in lowerName -> Icons.Rounded.Movie
        "bill" in lowerName || "util" in lowerName || "pay" in lowerName -> Icons.Rounded.Receipt
        "edu" in lowerName || "school" in lowerName -> Icons.Rounded.School
        else -> Icons.Rounded.Category
    }

    val isOverBudget = perf.percentage >= 1f
    val barColor = if (isOverBudget) ExpenseRed else perf.color

    StarkCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(perf.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = perf.name, tint = perf.color, modifier = Modifier.size(24.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(perf.name, style = StarkTypography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp), color = TextPrimary)
                    Text("₹${String.format("%,.0f", perf.spent)}", style = StarkTypography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${(perf.percentage * 100).toInt()}% Used", style = StarkTypography.labelSmall, color = if (isOverBudget) ExpenseRed else TextSecondary)
                    Text("of ₹${String.format("%,.0f", perf.budget)}", style = StarkTypography.labelSmall, color = TextSecondary)
                }
                Spacer(modifier = Modifier.height(10.dp))
                
                // Advanced Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(StarkSurfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(perf.percentage.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(Brush.horizontalGradient(listOf(barColor.copy(alpha = 0.7f), barColor)))
                    )
                }
            }
        }
    }
}
