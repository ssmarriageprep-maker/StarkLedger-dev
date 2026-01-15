package com.starklabs.moneytracker.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    secondary = AccentGold,
    background = StarkBackground,
    surface = StarkSurface,
    onPrimary = StarkBlack,
    onSecondary = StarkBlack,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = ExpenseRed
)

@Composable
fun StarkLedgerTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme // Enforce dark theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = StarkBackground.toArgb()
            window.navigationBarColor = StarkBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = StarkTypography,
        content = content
    )
}
