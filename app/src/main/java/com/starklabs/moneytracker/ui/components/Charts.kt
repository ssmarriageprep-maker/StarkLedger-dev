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
    
    // Additional rotation for dynamic feel
    val rotation by animateFloatAsState(
        targetValue = if (animationPlayed) 0f else -90f,
        animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing)
    )

    LaunchedEffect(key1 = true) {
        animationPlayed = true
    }

    Canvas(modifier = modifier) {
        val strokeWidth = 24.dp.toPx()
        val radius = size.minDimension / 2 - strokeWidth
        val center = Offset(size.width / 2, size.height / 2)
        var startAngle = -90f + rotation // Apply rotation

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
            color = NeonCyan.copy(alpha = 0.1f * progress),
            radius = radius - strokeWidth - 5.dp.toPx(),
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = NeonCyan.copy(alpha = 0.05f * progress),
            radius = radius + strokeWidth + 5.dp.toPx(),
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

@Composable
fun GlowingLineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = NeonCyan,
    fillStartColor: Color = NeonCyan.copy(alpha = 0.3f),
    fillEndColor: Color = Color.Transparent
) {
    if (data.isEmpty()) return

    var animationPlayed by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing)
    )

    LaunchedEffect(true) { animationPlayed = true }

    Canvas(modifier = modifier.padding(16.dp)) {
        val width = size.width
        val height = size.height
        val spacing = width / (data.size - 1)

        val path = androidx.compose.ui.graphics.Path()
        val fillPath = android.graphics.Path()

        // Calculate points
        val points = data.mapIndexed { index, ratio ->
            val x = index * spacing
            val y = height - (ratio * height)
            Offset(x, y)
        }

        // Move to start
        path.moveTo(points.first().x, points.first().y)
        fillPath.moveTo(points.first().x, points.first().y)

        // Cubic Bezier Curve for smoothness
        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]

            val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2, p0.y)
            val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2, p1.y)

            path.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
            fillPath.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
        }

        // Close fill path
        fillPath.lineTo(width, height)
        fillPath.lineTo(0f, height)
        fillPath.close()

        // Draw Gradient Fill
        drawContext.canvas.nativeCanvas.drawPath(
            fillPath,
            android.graphics.Paint().apply {
                shader = android.graphics.LinearGradient(
                    0f, 0f, 0f, height,
                    fillStartColor.toArgb(),
                    fillEndColor.toArgb(),
                    android.graphics.Shader.TileMode.CLAMP
                )
                style = android.graphics.Paint.Style.FILL
                isAntiAlias = true
                alpha = (255 * progress).toInt()
            }
        )

        // Draw Line with Animation
        // Note: For true path trimming animation we'd need PathMeasure, but simple alpha fade is safer for basic Canvas
        // Implementing simple clipping for "drawing" effect
        drawPath(
            path = path,
            color = lineColor.copy(alpha = progress),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw dots at points
        points.forEach { point ->
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx() * progress,
                center = point
            )
            drawCircle(
                color = lineColor.copy(alpha = 0.5f),
                radius = 6.dp.toPx() * progress,
                center = point,
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}
