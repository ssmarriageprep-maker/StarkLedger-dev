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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.starklabs.moneytracker.ui.theme.NeonCyan
import com.starklabs.moneytracker.ui.theme.NeonCyanDim
import com.starklabs.moneytracker.ui.theme.StarkSurface
import com.starklabs.moneytracker.ui.theme.TextWhite
import com.starklabs.moneytracker.ui.theme.JarvisGold
import com.starklabs.moneytracker.ui.theme.StarkBlack

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = NeonCyanDim,
    shape: androidx.compose.ui.graphics.Shape = CutCornerShape(topStart = 0.dp, bottomEnd = 16.dp, topEnd = 0.dp, bottomStart = 16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = StarkSurface.copy(alpha = 0.6f)),
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(borderColor, borderColor.copy(alpha = 0.1f))))
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun NeonText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = NeonCyan,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = style,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@Composable
fun HudHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(4.dp, 24.dp).background(NeonCyan))
            Spacer(modifier = Modifier.width(8.dp))
            NeonText(text = title.uppercase(), style = MaterialTheme.typography.headlineSmall)
        }
        if (subtitle != null) {
            NeonText(
                text = subtitle.uppercase(),
                color = JarvisGold,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(NeonCyan, Color.Transparent))))
    }
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
            .background(color.copy(alpha = 0.05f), shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp))
            .border(1.dp, color.copy(alpha = 0.5f), shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            color = color,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = NeonCyan
) {
    HudButton(text = text, onClick = onClick, modifier = modifier, color = color)
}

@Composable
fun NeonTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label.uppercase(), color = NeonCyanDim, letterSpacing = 1.sp, fontSize = 10.sp) },
        modifier = modifier,
        textStyle = androidx.compose.ui.text.TextStyle(color = TextWhite),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = NeonCyan,
            unfocusedIndicatorColor = NeonCyanDim,
            cursorColor = NeonCyan,
            focusedLabelColor = NeonCyan,
            unfocusedLabelColor = NeonCyanDim
        ),
        keyboardOptions = keyboardOptions,
        shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
    )
}

@Composable
fun ArcReactor(
    percentage: Float,
    modifier: Modifier = Modifier,
    color: Color = NeonCyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arc_reactor")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2

        // 1. Static Outer Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.1f), Color.Transparent),
                center = center,
                radius = radius * 1.2f
            )
        )

        // 2. Rotating Dash Ring
        withTransform({
            rotate(rotation, center)
        }) {
            drawCircle(
                color = color.copy(alpha = 0.2f),
                radius = radius * 0.95f,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 20f))
                )
            )
        }

        // 3. Main Progress Arc
        drawArc(
            color = color.copy(alpha = 0.2f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = 8.dp.toPx())
        )
        
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * percentage,
            useCenter = false,
            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
        )

        // 4. Inner Technical Rings
        drawCircle(
            color = color.copy(alpha = 0.4f),
            radius = radius * 0.7f,
            style = Stroke(width = 1.dp.toPx())
        )
        
        // 5. Core Reactor
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.9f * pulse), color.copy(alpha = 0.3f), Color.Transparent),
                center = center,
                radius = radius * 0.6f
            )
        )

        // Inner technical markings
        for (i in 0 until 8) {
            val angle = i * 45f
            withTransform({
                rotate(angle + rotation * -0.5f, center)
            }) {
                drawLine(
                    color = color,
                    start = Offset(center.x, center.y - radius * 0.7f),
                    end = Offset(center.x, center.y - radius * 0.6f),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}
