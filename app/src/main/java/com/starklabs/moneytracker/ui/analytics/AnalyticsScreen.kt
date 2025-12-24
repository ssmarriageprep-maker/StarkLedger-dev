package com.starklabs.moneytracker.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.starklabs.moneytracker.ui.components.GlassCard
import com.starklabs.moneytracker.ui.components.NeonText
import com.starklabs.moneytracker.ui.theme.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun AnalyticsScreen(
    navController: NavController,
    viewModel: AnalyticsViewModel
) {
    val state by viewModel.uiState.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize().background(StarkBlack)) {
        Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
             NeonText(text = "FINANCIAL INTELLIGENCE", style = MaterialTheme.typography.headlineMedium)
             
             Spacer(modifier = Modifier.height(24.dp))
             
             // Top Spending Card
             GlassCard(modifier = Modifier.fillMaxWidth()) {
                 NeonText(text = "TOP CATEGORY LEAK", color = JarvisGold)
                 NeonText(text = state.topCategory, style = MaterialTheme.typography.displayMedium)
             }
             
             Spacer(modifier = Modifier.height(24.dp))
             
             // Pie Chart Section
             NeonText(text = "DISTRIBUTION", color = NeonCyan)
             Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                 HolographicPieChart(slices = state.pieSlices, modifier = Modifier.size(180.dp))
             }
             
             Spacer(modifier = Modifier.height(24.dp))
             
             // Trend Line
             NeonText(text = "WEEKLY TREND", color = NeonCyan)
             GlassCard(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                 GlowingLineChart(data = state.weeklySpending, modifier = Modifier.fillMaxSize())
             }
             
             Spacer(modifier = Modifier.height(24.dp))

             // Income vs Expense
             NeonText(text = "CASH FLOW", color = NeonCyan)
             GlassCard(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                 IncomeExpenseChart(income = state.totalIncome, expense = state.totalExpense)
             }
             
             Spacer(modifier = Modifier.height(24.dp))
             
             // Category Breakdown
             if (state.categoryPerformance.isNotEmpty()) {
                 NeonText(text = "CATEGORY PERFORMANCE", color = NeonCyan)
                 state.categoryPerformance.forEach { perf ->
                     Spacer(modifier = Modifier.height(8.dp))
                     GlassCard(modifier = Modifier.fillMaxWidth()) {
                         Column {
                             Row(
                                 modifier = Modifier.fillMaxWidth(),
                                 horizontalArrangement = Arrangement.SpaceBetween
                             ) {
                                 NeonText(text = perf.name, color = perf.color)
                                 NeonText(
                                     text = "${(perf.percentage * 100).toInt()}%", 
                                     color = if (perf.percentage > 0.9f) MetallicRed else TextWhite
                                 )
                             }
                             Spacer(modifier = Modifier.height(8.dp))
                             
                             // Progress Bar
                             Box(
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .height(6.dp)
                                     .background(StarkSurface, MaterialTheme.shapes.small)
                             ) {
                                 Box(
                                     modifier = Modifier
                                         .fillMaxWidth(perf.percentage)
                                         .fillMaxHeight()
                                         .background(perf.color, MaterialTheme.shapes.small)
                                 )
                             }
                             
                             Spacer(modifier = Modifier.height(4.dp))
                             Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                 NeonText(
                                     text = "₹${perf.spent.toInt()} / ₹${perf.budget.toInt()}",
                                     style = MaterialTheme.typography.labelSmall,
                                     color = TextGrey
                                 )
                             }
                         }
                     }
                 }
             }
             
             Spacer(modifier = Modifier.height(32.dp)) // Bottom Padding
        }
    }
}

@Composable
fun IncomeExpenseChart(income: Double, expense: Double) {
    val max = maxOf(income, expense, 1.0)
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Income Bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            NeonText(text = "IN", color = NeonCyan, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(30.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .background(StarkSurface, MaterialTheme.shapes.small)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((income / max).toFloat())
                        .fillMaxHeight()
                        .background(NeonCyan, MaterialTheme.shapes.small)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            NeonText(text = "₹${income.toInt()}", color = TextWhite, style = MaterialTheme.typography.labelSmall)
        }
        
        // Expense Bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            NeonText(text = "OUT", color = MetallicRed, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(30.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .background(StarkSurface, MaterialTheme.shapes.small)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((expense / max).toFloat())
                        .fillMaxHeight()
                        .background(MetallicRed, MaterialTheme.shapes.small)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            NeonText(text = "₹${expense.toInt()}", color = TextWhite, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun HolographicPieChart(slices: List<Slice>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        var startAngle = -90f
        slices.forEach { slice ->
            val sweepAngle = slice.value * 360f
            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle - 2f, // Gap
                useCenter = false,
                style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Butt)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun GlowingLineChart(data: List<Float>, modifier: Modifier = Modifier) {
    if (data.isEmpty()) return
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val stepX = width / (data.size - 1)
        
        val path = Path()
        data.forEachIndexed { index, ratio ->
            val x = index * stepX
            val y = height - (ratio * height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        
        drawPath(
            path = path,
            color = NeonCyan,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // Draw Points
        data.forEachIndexed { index, ratio ->
             val x = index * stepX
             val y = height - (ratio * height)
             drawCircle(color = TextWhite, radius = 4.dp.toPx(), center = Offset(x, y))
        }
    }
}
