package com.starklabs.moneytracker.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

val StarkBlack = Color(0xFF000000)
val StarkDarkGrey = Color(0xFF121212)
val StarkSurface = Color(0xFF1E1E1E)

// Neon Accents
val NeonCyan = Color(0xFF00E6FF)
val NeonCyanDim = Color(0xFF005A82)
val ArcReactorBlue = Color(0xFF00B0FF)
val ElectricBlue = Color(0xFF2979FF)

// Metallic Accents
val MetallicRed = Color(0xFFB3001B)
val HotRed = Color(0xFFFF1744)
val JarvisGold = Color(0xFFFFB400)
val JarvisOrange = Color(0xFFFF6D00)
val MetallicGold = Color(0xFFD4AF37)

// Text
val TextWhite = Color(0xFFFFFFFF)
val TextGrey = Color(0xFFB0B0B0)
val TextCyan = Color(0xFFE0F7FA)

// Gradients
val BrushArcReactor = Brush.radialGradient(
    colors = listOf(Color.White, NeonCyan, ArcReactorBlue, Color.Transparent)
)

val BrushMetallicGold = Brush.horizontalGradient(
    colors = listOf(JarvisGold, MetallicGold, JarvisOrange)
)
