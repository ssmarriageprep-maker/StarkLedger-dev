package com.starklabs.moneytracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.starklabs.moneytracker.ui.Screen
import com.starklabs.moneytracker.ui.theme.*

@Composable
fun StarkHeader(
    onSettingsClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App branding with subtle gradient text effect could go here, but keeping it clean
        Text(
            text = "STARKLEDGER",
            style = StarkTypography.titleLarge.copy(
                fontSize = 20.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.ExtraBold
            ),
            color = TextPrimary
        )
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.Sharp.Settings,
                contentDescription = "Settings",
                modifier = Modifier.size(26.dp),
                tint = TextSecondary
            )
        }
    }
}

@Composable
fun StarkCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, StarkBorder, RoundedCornerShape(20.dp)), // Glassmorphic Edge
        color = StarkSurface.copy(alpha = 0.95f),
        shadowElevation = 8.dp,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
fun StarkClickableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.97f else 1f, animationSpec = tween(150), label = "card_scale")
    val alpha = if (isPressed) 0.8f else 0.95f

    Surface(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, StarkBorder, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        color = StarkSurface.copy(alpha = alpha),
        shadowElevation = if (isPressed) 2.dp else 8.dp,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
fun StarkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, animationSpec = tween(150), label = "button_scale")

    val bgModifier = if (isEnabled) {
        Modifier.background(Brush.horizontalGradient(listOf(GradientVioletStart, GradientVioletEnd)))
    } else {
        Modifier.background(StarkBorder)
    }

    Box(
        modifier = modifier
            .scale(scale)
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(bgModifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isEnabled,
                onClick = onClick
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = StarkTypography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = if (isEnabled) Color.White else TextDisabled
            )
        )
    }
}

@Composable
fun StarkStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = StarkTypography.labelSmall.copy(letterSpacing = 0.5.sp),
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = StarkTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = valueColor
        )
    }
}

@Composable
fun TransactionRow(
    transaction: com.starklabs.moneytracker.data.Transaction,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val isDebit = transaction.type == "DEBIT"
    val amountColor = if (isDebit) TextPrimary else IncomeGreen
    val prefix = if (isDebit) "-" else "+"

    StarkClickableCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Circular Icon Box
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDebit) StarkSurfaceVariant else IncomeGreen.copy(alpha = 0.15f))
                        .border(1.dp, if (isDebit) StarkBorder else IncomeGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        transaction.merchant.take(1).uppercase(), 
                        style = StarkTypography.titleMedium.copy(fontWeight = FontWeight.Bold), 
                        color = if (isDebit) TextSecondary else IncomeGreen
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = transaction.merchant,
                        style = StarkTypography.bodyLarge.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatStarkDate(transaction.date),
                        style = StarkTypography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
            Text(
                text = "$prefix₹${String.format("%,.0f", transaction.amount)}",
                color = amountColor,
                style = StarkTypography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

fun formatStarkDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("dd MMM • HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

@Composable
fun StarkBottomNavigationBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = listOf(
        Pair(Screen.Dashboard.route, Pair(Icons.Sharp.Home, "Home")),
        Pair(Screen.Analytics.route, Pair(Icons.Sharp.PieChart, "Insights")),
        Pair(Screen.Wallets.route, Pair(Icons.Sharp.AccountBalanceWallet, "Wallets")),
        Pair(Screen.Settings.route, Pair(Icons.Sharp.Settings, "Settings"))
    )

    NavigationBar(
        containerColor = StarkSurfaceVariant, // Raised from background
        contentColor = TextSecondary,
        tonalElevation = 8.dp,
        modifier = Modifier.offset(y = 1.dp) // Hide bottom border imperfection
    ) {
        items.forEach { (route, iconAndLabel) ->
            val (icon, label) = iconAndLabel
            val selected = currentRoute == route
            NavigationBarItem(
                icon = { 
                    Icon(
                        icon, 
                        contentDescription = label,
                        modifier = Modifier.size(if (selected) 28.dp else 24.dp)
                    ) 
                },
                label = { 
                    if (selected) {
                        Text(label, style = StarkTypography.labelSmall.copy(fontWeight = FontWeight.Bold)) 
                    }
                },
                selected = selected,
                onClick = {
                    navController.navigate(route) {
                        popUpTo(Screen.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AccentPrimary,
                    selectedTextColor = AccentPrimary,
                    indicatorColor = Color.Transparent, // Transparent so icon glows without pill
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )
        }
    }
}
