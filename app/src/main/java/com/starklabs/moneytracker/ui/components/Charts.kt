package com.starklabs.moneytracker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.starklabs.moneytracker.ui.analytics.Slice
import com.starklabs.moneytracker.ui.theme.NeonCyan
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedDonutChart(
    slices: List<Slice>,
    modifier: Modifier = Modifier,
    animationDuration: Int = 1000
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing)
    )

    LaunchedEffect(key1 = true) {
        animationPlayed = true
    }

    Canvas(modifier = modifier) {
        val strokeWidth = 24.dp.toPx()
        val radius = size.minDimension / 2 - strokeWidth
        val center = Offset(size.width / 2, size.height / 2)
        var startAngle = -90f

        slices.forEach { slice ->
            val sweepAngle = slice.value * 360f * progress
            
            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle - 2f, // Gap for aesthetic
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle += sweepAngle
        }
        
        // Inner tech details (thin rings)
        drawCircle(
            color = NeonCyan.copy(alpha = 0.1f),
            radius = radius - strokeWidth,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
fun AnimatedBarChart(
    data: Map<String, Double>, // Label -> Value
    modifier: Modifier = Modifier,
    barColor: Color = NeonCyan
) {
    if (data.isEmpty()) return
    
    val maxValue = data.values.maxOrNull() ?: 1.0
    var animationPlayed by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(1000, easing = LinearEasing)
    )
    
    LaunchedEffect(true) { animationPlayed = true }

    Canvas(modifier = modifier) {
        val barWidth = size.width / (data.size * 2f)
        val spacing = size.width / data.size
        
        data.entries.forEachIndexed { index, entry ->
            val barHeight = (entry.value / maxValue).toFloat() * size.height * progress
            val x = spacing * index + (spacing - barWidth) / 2
            val y = size.height - barHeight
            
            // Draw Bar
            drawRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight)
            )
            
            // Glow effect (simple duplicate with alpha)
            drawRect(
                color = barColor.copy(alpha = 0.3f),
                topLeft = Offset(x - 2, y - 2),
                size = Size(barWidth + 4, barHeight + 4)
            )
        }
    }
}
