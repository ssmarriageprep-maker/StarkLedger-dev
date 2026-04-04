package com.starklabs.moneytracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
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
    title: String = "StarkLedger",
    onMenuClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(SurfaceContainerLow)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMenuClick, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Sharp.Menu, contentDescription = "Menu", tint = Primary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = StarkTypography.headlineLarge.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp
                ),
                color = PrimaryContainer
            )
        }
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                Icons.Sharp.Settings,
                contentDescription = "Settings",
                tint = Primary
            )
        }
    }
}

@Composable
fun StarkCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = SurfaceContainerLow,
    cornerRadius: androidx.compose.ui.unit.Dp = Dimen.cornerRadiusLarge,
    contentPadding: PaddingValues = PaddingValues(Dimen.cardPadding),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.05f), Color.Transparent)
                ),
                shape = RoundedCornerShape(cornerRadius)
            ),
        color = backgroundColor,
        shape = RoundedCornerShape(cornerRadius)
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
    backgroundColor: Color = SurfaceContainerLow,
    cornerRadius: androidx.compose.ui.unit.Dp = Dimen.cornerRadiusLarge,
    contentPadding: PaddingValues = PaddingValues(Dimen.cardPadding),
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.98f else 1f, animationSpec = tween(150), label = "card_scale")

    Surface(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(cornerRadius))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        color = if (isPressed) SurfaceContainerHigh else backgroundColor,
        shape = RoundedCornerShape(cornerRadius)
    ) {
        Column(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
fun TransactionRow(
    transaction: com.starklabs.moneytracker.data.Transaction,
    modifier: Modifier = Modifier,
    accountName: String? = null,
    onClick: () -> Unit = {}
) {
    val isDebit = transaction.type == "DEBIT"
    val amountColor = if (isDebit) OnSurface else TertiaryContainer
    val prefix = if (isDebit) "-" else "+"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            // Circular Icon Box
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                 Icon(
                    imageVector = when(transaction.merchant.lowercase()) {
                        "food", "dining" -> Icons.Sharp.Restaurant
                        "shopping" -> Icons.Sharp.ShoppingBag
                        "bills", "utilities" -> Icons.Sharp.Bolt
                        "transport", "travel" -> Icons.Sharp.DirectionsCar
                        "health", "wellness" -> Icons.Sharp.MedicalServices
                        "salary", "income" -> Icons.Sharp.AccountBalanceWallet
                        else -> Icons.Sharp.ReceiptLong
                    },
                    contentDescription = null,
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.padding(end = 16.dp)) {
                Text(
                    text = transaction.merchant,
                    style = StarkTypography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = OnSurface,
                    maxLines = 2
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatStarkDate(transaction.date),
                        style = StarkTypography.labelSmall,
                        color = OnSurfaceVariant
                    )
                    if (accountName != null) {
                        Text(
                            text = " • ",
                            style = StarkTypography.labelSmall,
                            color = OnSurfaceVariant
                        )
                        Text(
                            text = accountName,
                            style = StarkTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = PrimaryContainer
                        )
                    }
                }
            }
        }
        Text(
            text = "$prefix₹${String.format("%,.2f", transaction.amount)}",
            color = amountColor,
            style = StarkTypography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
fun GlobalAccountSelector(
    accounts: List<com.starklabs.moneytracker.data.Account>,
    selectedAccountId: Int,
    onAccountSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedAccount = if (selectedAccountId == -1) "All Accounts"
                          else accounts.find { it.id == selectedAccountId }?.name ?: "All Accounts"

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            color = SurfaceContainerHigh.copy(alpha = 0.5f),
            shape = RoundedCornerShape(100.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedAccount,
                    style = StarkTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (expanded) Icons.Sharp.KeyboardArrowUp else Icons.Sharp.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(SurfaceContainerHigh)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
        ) {
            DropdownMenuItem(
                text = { Text("All Accounts", style = StarkTypography.bodyMedium) },
                onClick = {
                    onAccountSelected(-1)
                    expanded = false
                },
                colors = MenuDefaults.itemColors(textColor = OnSurface)
            )
            accounts.filter { it.isActive }.forEach { account ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(account.name, style = StarkTypography.bodyMedium)
                            if (account.last4Digits != null) {
                                Text("•••• ${account.last4Digits}", style = StarkTypography.labelSmall, color = OnSurfaceVariant)
                            }
                        }
                    },
                    onClick = {
                        onAccountSelected(account.id)
                        expanded = false
                    },
                    colors = MenuDefaults.itemColors(textColor = OnSurface)
                )
            }
        }
    }
}

fun formatStarkDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault())
    val now = java.util.Calendar.getInstance()
    val date = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }

    return when {
        now.get(java.util.Calendar.DATE) == date.get(java.util.Calendar.DATE) -> "Today"
        now.get(java.util.Calendar.DATE) - 1 == date.get(java.util.Calendar.DATE) -> "Yesterday"
        else -> sdf.format(java.util.Date(timestamp))
    }
}

@Composable
fun StarkBottomNavigationBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = listOf(
        Triple(Screen.Dashboard.route, Icons.Sharp.GridView, "Home"),
        Triple(Screen.Analytics.route, Icons.Sharp.Insights, "Analytics"),
        Triple(Screen.History.route, Icons.Sharp.ReceiptLong, "History"),
        Triple(Screen.Wallets.route, Icons.Sharp.AccountBalanceWallet, "Budget")
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC0E0E0E)))) // Deep fade
            .padding(bottom = 12.dp)
    ) {
        // Blur background simulation
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0x991C1B1B))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
        )
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (route, icon, label) ->
                val selected = currentRoute == route

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) PrimaryContainer.copy(alpha = 0.1f) else Color.Transparent)
                        .clickable {
                            navController.navigate(route) {
                                popUpTo(Screen.Dashboard.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (selected) PrimaryContainer else OnSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = label,
                        style = StarkTypography.labelSmall.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 10.sp
                        ),
                        color = if (selected) PrimaryContainer else OnSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryIcon(categoryName: String?, modifier: Modifier = Modifier, tint: Color = OnSurfaceVariant) {
    Icon(
        imageVector = when(categoryName?.lowercase()) {
            "food", "dining" -> Icons.Sharp.Restaurant
            "shopping" -> Icons.Sharp.ShoppingBag
            "bills", "utilities" -> Icons.Sharp.Bolt
            "transport", "travel" -> Icons.Sharp.DirectionsCar
            "health", "wellness" -> Icons.Sharp.MedicalServices
            "salary", "income" -> Icons.Sharp.AccountBalanceWallet
            else -> Icons.Sharp.ReceiptLong
        },
        contentDescription = categoryName,
        tint = tint,
        modifier = modifier
    )
}

@Composable
fun StarkStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = OnSurface
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = StarkTypography.labelSmall,
            color = OnSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = StarkTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = valueColor
        )
    }
}

@Composable
fun StarkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = isEnabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryContainer,
            contentColor = OnPrimary,
            disabledContainerColor = SurfaceContainerHigh,
            disabledContentColor = OnSurfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            style = StarkTypography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}
