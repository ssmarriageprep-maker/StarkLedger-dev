package com.starklabs.moneytracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
            .height(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "STARKLEDGER",
            style = StarkTypography.titleMedium.copy(fontSize = 18.sp),
            color = TextPrimary
        )
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.size(48.dp) // Touch area 48dp, icon 24dp
        ) {
            Icon(
                Icons.Sharp.Settings,
                contentDescription = "Settings",
                modifier = Modifier.size(24.dp),
                tint = TextSecondary
            )
        }
    }
}

@Composable
fun StarkCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        color = StarkSurface,
        shadowElevation = 2.dp
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
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.98f else 1f, animationSpec = tween(100), label = "card_scale")

    Surface(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        color = StarkSurface,
        shadowElevation = 2.dp
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
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.96f else 1f, animationSpec = tween(100), label = "button_scale")

    Box(
        modifier = modifier
            .scale(scale)
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isEnabled) AccentPrimary else StarkBorder)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isEnabled,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = StarkTypography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                color = if (isEnabled) StarkBackground else TextDisabled
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
            style = StarkTypography.labelSmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = StarkTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
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
    val amountColor = if (isDebit) ExpenseRed else IncomeGreen
    val prefix = if (isDebit) "-" else "+"

    StarkClickableCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp) // Row height 56dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Icon placeholder (32dp)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StarkBorder),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        transaction.merchant.take(1).uppercase(), 
                        style = StarkTypography.labelSmall, 
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = transaction.merchant,
                        style = StarkTypography.bodyMedium.copy(color = TextPrimary),
                        maxLines = 1
                    )
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
                style = StarkTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
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
        containerColor = StarkBorder, // Minimal distinct bar
        contentColor = TextPrimary,
        tonalElevation = 0.dp
    ) {
        items.forEach { (route, iconAndLabel) ->
            val (icon, label) = iconAndLabel
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, style = StarkTypography.labelSmall) },
                selected = currentRoute == route,
                onClick = {
                    navController.navigate(route) {
                        popUpTo(Screen.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = StarkBackground,
                    selectedTextColor = AccentPrimary,
                    indicatorColor = AccentPrimary,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )
        }
    }
}
