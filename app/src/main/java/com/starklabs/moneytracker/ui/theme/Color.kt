package com.starklabs.moneytracker.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Palette (Deep & Premium)
val StarkBlack = Color(0xFF050505) 
val StarkSurface = Color(0xFF0F111A) // Slightly blue-tinted dark grey for cards
val StarkBackground = Color(0xFF000000) // True black for AMOLED pop

// Functional Colors - Neon & Vivid
val InfoBlue = Color(0xFF2979FF)      // Electric Blue
val IncomeGreen = Color(0xFF00E676)   // Vivid Malachite
val WarningAmber = Color(0xFFFFAB00)  // Solar Amber
val ExpenseRed = Color(0xFFFF1744)    // Crimson Red

// Accent & Text Colors
val AccentCyan = Color(0xFF00E5FF)    // Cyan A400
val AccentGold = Color(0xFFFFD740)    // Amber A200
val TextPrimary = Color(0xFFEEEEEE)   // Off-white for better readability
val TextSecondary = Color(0xFF9E9E9E) // Grey 500
val TextDisabled = Color(0xFF616161)

// Gradients (Start/End pairs usually handled in Brush, but defining bases here)
val GradientCyanStart = Color(0xFF00E5FF)
val GradientCyanEnd = Color(0xFF2979FF)
val GradientPurpleStart = Color(0xFFD500F9)
val GradientPurpleEnd = Color(0xFF651FFF)

// Chart Colors
val ChartBlue = Color(0xFF2962FF)
val ChartGreen = Color(0xFF00C853)
val ChartYellow = Color(0xFFFFD600)
val ChartOrange = Color(0xFFFF6D00)
val ChartPurple = Color(0xFFAA00FF)
val ChartRed = Color(0xFFD50000)

// Compatibility Layer
val NeonCyan = AccentCyan
val JarvisGold = AccentGold
val JarvisOrange = ChartOrange
val TextGrey = TextSecondary
val TextWhite = TextPrimary
val HotRed = ExpenseRed

// Preserved Metadata
val NeonCyanDim = Color(0xFF004D40)
val MetallicRed = Color(0xFF8E0000)
val MetallicGold = Color(0xFFFFD700)
val TextCyan = Color(0xFFE0F7FA)
val ArcReactorBlue = InfoBlue
val ElectricBlue = Color(0xFF2962FF)
