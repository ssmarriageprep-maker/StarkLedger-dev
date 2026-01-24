package com.starklabs.moneytracker.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.foundation.shape.CutCornerShape
import com.starklabs.moneytracker.ui.components.*
import com.starklabs.moneytracker.ui.theme.*

@Composable
fun AnalyticsScreen(
    navController: NavController,
    viewModel: AnalyticsViewModel
) {
    val state by viewModel.uiState.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize().background(StarkBackground)) {
        Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
             HudHeader(title = "FINANCIAL INTELLIGENCE", subtitle = "ANALYZING EXPENDITURE PROTOCOLS")
             
             Spacer(modifier = Modifier.height(24.dp))
             
             // Intelligence Summary
             Row(modifier = Modifier.fillMaxWidth()) {
                 GlassCard(modifier = Modifier.weight(1f)) {
                     NeonText(text = "CRITICAL LEAK", color = JarvisGold, style = MaterialTheme.typography.labelSmall)
                     NeonText(text = state.topCategory.uppercase(), style = MaterialTheme.typography.headlineSmall, color = TextWhite)
                 }
                 Spacer(modifier = Modifier.width(16.dp))
                 GlassCard(modifier = Modifier.weight(0.8f)) {
                     NeonText(text = "HEALTH INDEX", color = NeonCyan, style = MaterialTheme.typography.labelSmall)
                     NeonText(text = "92%", style = MaterialTheme.typography.headlineSmall, color = IncomeGreen)
                 }
             }
             
             Spacer(modifier = Modifier.height(24.dp))
             
             // Donut Chart Section
             NeonText(text = "SECTOR DISTRIBUTION", color = NeonCyan, style = MaterialTheme.typography.titleSmall)
             Spacer(modifier = Modifier.height(12.dp))
             Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                 AnimatedDonutChart(slices = state.pieSlices, modifier = Modifier.size(220.dp))
                 Column(horizontalAlignment = Alignment.CenterHorizontally) {
                     NeonText(text = "TOTAL LOSS", color = TextGrey, style = MaterialTheme.typography.labelSmall)
                     NeonText(text = "₹${String.format("%,.0f", state.totalExpense)}", color = TextWhite, style = MaterialTheme.typography.titleLarge)
                 }
             }
             
             Spacer(modifier = Modifier.height(24.dp))
             
             // Trend Line
             NeonText(text = "EXPENDITURE VELOCITY", color = NeonCyan, style = MaterialTheme.typography.titleSmall)
             Spacer(modifier = Modifier.height(12.dp))
             GlassCard(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                 GlowingLineChart(data = state.weeklySpending, modifier = Modifier.fillMaxSize())
             }
             
             Spacer(modifier = Modifier.height(24.dp))

             // Income vs Expense
             NeonText(text = "LIQUIDITY FLOW", color = NeonCyan, style = MaterialTheme.typography.titleSmall)
             Spacer(modifier = Modifier.height(12.dp))
             GlassCard(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                 IncomeExpenseChart(income = state.totalIncome, expense = state.totalExpense)
             }
             
             Spacer(modifier = Modifier.height(24.dp))
             
             // Category Breakdown
             if (state.categoryPerformance.isNotEmpty()) {
                 NeonText(text = "SYSTEM AUDIT BY CATEGORY", color = JarvisGold, style = MaterialTheme.typography.titleSmall)
                 state.categoryPerformance.forEach { perf ->
                     Spacer(modifier = Modifier.height(10.dp))
                     CategoryAuditItem(perf)
                 }
             }
             
             Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun CategoryAuditItem(perf: CategoryPerformance) {
    val isNearLimit = perf.percentage > 0.85f
    val glowColor = if (perf.percentage > 0.95f) ExpenseRed else if (isNearLimit) JarvisOrange else NeonCyan

    GlassCard(
        borderColor = glowColor.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth(),
        shape = CutCornerShape(8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(perf.color))
                    Spacer(modifier = Modifier.width(8.dp))
                    NeonText(text = perf.name.uppercase(), color = TextWhite, style = MaterialTheme.typography.bodyMedium)
                }
                NeonText(
                    text = "${(perf.percentage * 100).toInt()}%",
                    color = if (perf.percentage > 0.9f) ExpenseRed else NeonCyan
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            // Technical Progress Bar Enhanced
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(StarkSurface, shape = CutCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(perf.percentage.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(glowColor.copy(alpha = 0.3f), glowColor)
                            ),
                            shape = CutCornerShape(2.dp)
                        )
                )

                // Technical Grid markers on progress bar
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val steps = 10
                    val stepWidth = size.width / steps
                    for (i in 1 until steps) {
                        drawLine(
                            color = StarkBackground.copy(alpha = 0.5f),
                            start = Offset(i * stepWidth, 0f),
                            end = Offset(i * stepWidth, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                NeonText(
                    text = "USED: ₹${String.format("%,.0f", perf.spent)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGrey
                )
                NeonText(
                    text = "LIMIT: ₹${String.format("%,.0f", perf.budget)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGrey.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun IncomeExpenseChart(income: Double, expense: Double) {
    val max = maxOf(income, expense, 1.0)
    
    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Income Bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            NeonText(text = "IN", color = IncomeGreen, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(35.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .background(StarkSurface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((income / max).toFloat())
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(listOf(IncomeGreen.copy(alpha = 0.3f), IncomeGreen))
                        )
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            NeonText(text = "₹${String.format("%,.0f", income)}", color = TextWhite, style = MaterialTheme.typography.labelSmall)
        }
        
        // Expense Bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            NeonText(text = "OUT", color = ExpenseRed, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(35.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .background(StarkSurface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((expense / max).toFloat())
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(listOf(ExpenseRed.copy(alpha = 0.3f), ExpenseRed))
                        )
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            NeonText(text = "₹${String.format("%,.0f", expense)}", color = TextWhite, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun GlowingLineChart(data: List<Float>, modifier: Modifier = Modifier) {
    if (data.isEmpty()) return
    Canvas(modifier = modifier.padding(8.dp)) {
        val width = size.width
        val height = size.height
        val stepX = if (data.size > 1) width / (data.size - 1) else width
        
        val path = Path()
        data.forEachIndexed { index, ratio ->
            val x = index * stepX
            val y = height - (ratio * height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        
        // Background Area under path
        val fillPath = path.asAndroidPath()
        fillPath.lineTo(width, height)
        fillPath.lineTo(0f, height)
        fillPath.close()

        drawContext.canvas.nativeCanvas.drawPath(
            fillPath,
            android.graphics.Paint().apply {
                shader = android.graphics.LinearGradient(
                    0f, 0f, 0f, height,
                    NeonCyan.copy(alpha = 0.2f).toArgb(),
                    Color.Transparent.toArgb(),
                    android.graphics.Shader.TileMode.CLAMP
                )
                style = android.graphics.Paint.Style.FILL
                isAntiAlias = true
            }
        )

        drawPath(
            path = path,
            color = NeonCyan,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // Draw Technical Data Points
        data.forEachIndexed { index, ratio ->
             val x = index * stepX
             val y = height - (ratio * height)
             drawCircle(color = TextWhite, radius = 2.dp.toPx(), center = Offset(x, y))
             drawCircle(color = NeonCyan.copy(alpha = 0.3f), radius = 5.dp.toPx(), center = Offset(x, y), style = Stroke(1.dp.toPx()))
        }
    }
}
