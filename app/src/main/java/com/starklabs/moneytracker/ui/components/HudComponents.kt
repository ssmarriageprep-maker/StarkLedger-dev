package com.starklabs.moneytracker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starklabs.moneytracker.ui.theme.*

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = NeonCyanDim,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = CutCornerShape(topStart = 0.dp, bottomEnd = 16.dp, topEnd = 16.dp, bottomStart = 16.dp), // Futuristic Shape
        colors = CardDefaults.cardColors(containerColor = StarkSurface.copy(alpha = 0.7f)),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun NeonText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = NeonCyan,
    style: androidx.compose.ui.text.TextStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge
) {
    // In a real customized engine, we'd draw twice for glow. 
    // For now, we use vibrant colors and high contrast.
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = style,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun HudButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = NeonCyan
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(color.copy(alpha = 0.1f), shape = CutCornerShape(8.dp))
            .border(1.dp, color, shape = CutCornerShape(8.dp))
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text.uppercase(), color = color, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    }
}

@Composable
fun ArcReactor(
    percentage: Float, // 0.0 to 1.0
    modifier: Modifier = Modifier,
    color: Color = NeonCyan // Or Red if over budget
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = modifier) {
        // Outer Ring
        drawCircle(
            color = color.copy(alpha = 0.3f),
            style = Stroke(width = 4.dp.toPx())
        )
        
        // Progress Arc
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360 * percentage,
            useCenter = false,
            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // Core Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.8f), Color.Transparent)
            ),
            radius = size.minDimension / 2.5f * pulse
        )
    }
}
