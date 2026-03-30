package com.starklabs.moneytracker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.starklabs.moneytracker.ui.analytics.Slice
import com.starklabs.moneytracker.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedDonutChart(
    slices: List<Slice>,
    modifier: Modifier = Modifier,
    animationDuration: Int = 1200
) {
    var animationPlayed by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing),
        label = "donut_progress"
    )

    LaunchedEffect(key1 = true) {
        animationPlayed = true
    }

    Canvas(modifier = modifier) {
        val strokeWidth = 14.dp.toPx()
        val radius = size.minDimension / 2 - strokeWidth
        val center = Offset(size.width / 2, size.height / 2)
        var startAngle = -90f

        // Background Track
        drawCircle(
            color = SurfaceContainerHigh,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth)
        )

        slices.forEach { slice ->
            val sweepAngle = slice.value * 360f * progress
            
            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun GlowingLineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = PrimaryContainer,
    fillStartColor: Color = PrimaryContainer.copy(alpha = 0.2f),
    fillEndColor: Color = Color.Transparent
) {
    if (data.isEmpty()) return

    var animationPlayed by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "line_chart_progress"
    )

    LaunchedEffect(true) { animationPlayed = true }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val spacing = if (data.size > 1) width / (data.size - 1) else 0f

        val path = androidx.compose.ui.graphics.Path()
        val fillPath = androidx.compose.ui.graphics.Path()

        val points = data.mapIndexed { index, ratio ->
            val x = index * spacing
            val y = height - (ratio * height)
            Offset(x, y)
        }

        if (points.isNotEmpty()) {
            path.moveTo(points.first().x, points.first().y)
            fillPath.moveTo(points.first().x, points.first().y)

            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]

                val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2, p0.y)
                val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2, p1.y)

                path.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
                fillPath.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
            }

            fillPath.lineTo(width, height)
            fillPath.lineTo(0f, height)
            fillPath.close()

            // Draw Fill with Gradient
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(fillStartColor, Color.Transparent),
                    startY = 0f,
                    endY = height
                ),
                alpha = progress * 0.5f
            )

            // Draw Glow (Multi-layered line)
            drawPath(
                path = path,
                color = lineColor.copy(alpha = 0.2f),
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
                alpha = progress
            )
            
            // Draw Main Line
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                alpha = progress
            )

            // Draw last point with glow
            val lastPoint = points.last()
            drawCircle(
                color = lineColor.copy(alpha = 0.3f),
                radius = 10.dp.toPx(),
                center = lastPoint,
                alpha = progress
            )
            drawCircle(
                color = lineColor,
                radius = 5.dp.toPx(),
                center = lastPoint,
                alpha = progress
            )
        }
    }
}
