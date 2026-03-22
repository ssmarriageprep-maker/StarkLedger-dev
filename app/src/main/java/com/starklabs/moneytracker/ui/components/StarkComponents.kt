package com.starklabs.moneytracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.starklabs.moneytracker.ui.theme.*
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Home
import androidx.compose.material.icons.sharp.PieChart
import androidx.compose.material.icons.sharp.AccountBalanceWallet
import androidx.compose.material.icons.sharp.Settings
import com.starklabs.moneytracker.ui.Screen


@Composable
fun StarkCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(StarkSurface)
            .padding(contentPadding)
    ) {
        content()
    }
}

@Composable
fun StarkClickableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.98f else 1f, label = "card_scale")

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(StarkSurface)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(contentPadding)
    ) {
        content()
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
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "button_scale")

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isEnabled) AccentPrimary else StarkSurfaceVariant)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isEnabled,
                onClick = onClick
            )
            .padding(vertical = 16.dp, horizontal = 24.dp),
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
            text = label.uppercase(),
            style = StarkTypography.labelSmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = StarkTypography.titleLarge,
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
    val prefix = if (isDebit) "" else "+"

    StarkClickableCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = transaction.merchant.uppercase(),
                    style = StarkTypography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatStarkDate(transaction.date),
                    style = StarkTypography.bodyMedium,
                    color = TextSecondary
                )
            }
            Text(
                text = "$prefix₹${String.format("%,.2f", transaction.amount)}",
                color = amountColor,
                style = StarkTypography.titleLarge,
                fontWeight = FontWeight.SemiBold
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
        containerColor = StarkSurface,
        contentColor = TextPrimary,
        tonalElevation = 8.dp
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
