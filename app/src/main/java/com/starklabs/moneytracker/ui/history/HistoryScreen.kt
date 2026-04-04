package com.starklabs.moneytracker.ui.history

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.starklabs.moneytracker.ui.components.*
import com.starklabs.moneytracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val selectedAccountId by viewModel.selectedAccountId.collectAsState()
    var selectedTransaction by remember { mutableStateOf<com.starklabs.moneytracker.data.Transaction?>(null) }
    var showDetailSheet by remember { mutableStateOf(false) }

    if (showDetailSheet && selectedTransaction != null) {
        val t = selectedTransaction!!
        val isDebit = t.type == "DEBIT"

        ModalBottomSheet(
            onDismissRequest = { showDetailSheet = false },
            containerColor = SurfaceContainer,
            dragHandle = { BottomSheetDefaults.DragHandle(color = OutlineVariant) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(t.merchant.uppercase(), style = StarkTypography.labelSmall.copy(letterSpacing = 2.sp, color = PrimaryContainer))
                        Text(
                            text = if (isDebit) "-₹${String.format("%,.2f", t.amount)}" else "+₹${String.format("%,.2f", t.amount)}",
                            style = StarkTypography.displayMedium.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
                            color = if (isDebit) OnSurface else TertiaryContainer
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(SurfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        CategoryIcon(null, tint = PrimaryContainer, modifier = Modifier.size(32.dp))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Metadata Details
                StarkCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = SurfaceContainerLow,
                    cornerRadius = 16.dp,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    DetailRow("Timestamp", java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(t.date)))
                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = OutlineVariant.copy(alpha = 0.1f))
                    DetailRow("Account", accounts.find { it.id == t.accountId }?.name ?: "Unknown")
                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = OutlineVariant.copy(alpha = 0.1f))
                    DetailRow("Type", t.type)
                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = OutlineVariant.copy(alpha = 0.1f))
                    DetailRow("Category", categories.find { it.id == t.categoryId }?.name ?: "Uncategorized")
                    t.smsBody?.let {
                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = OutlineVariant.copy(alpha = 0.1f))
                        Column {
                            Text("ORIGINAL INTEL", style = StarkTypography.labelSmall.copy(color = OnSurfaceVariant.copy(alpha = 0.6f)))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(it, style = StarkTypography.bodyMedium, color = OnSurface)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = { showDetailSheet = false },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerHighest),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("CLOSE", color = OnSurfaceVariant)
                    }
                    Button(
                        onClick = {
                            viewModel.deleteTransaction(t)
                            showDetailSheet = false
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorContainer.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("DELETE", color = Error)
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = SurfaceContainerLowest,
        topBar = {
            Column(modifier = Modifier.background(SurfaceContainerLow)) {
                StarkHeader(
                    title = "StarkLedger",
                    onSettingsClick = { navController.navigate(com.starklabs.moneytracker.ui.Screen.Settings.route) }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    GlobalAccountSelector(
                        accounts = accounts,
                        selectedAccountId = selectedAccountId,
                        onAccountSelected = { viewModel.setSelectedAccount(it) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Search and Filter Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLow)
                    .border(1.dp, OutlineVariant.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Sharp.Search, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        modifier = Modifier.weight(1f),
                        textStyle = StarkTypography.bodyLarge.copy(color = Primary),
                        decorationBox = { innerTextField ->
                            if (state.searchQuery.isEmpty()) {
                                Text("Search transactions...", color = OnSurfaceVariant, style = StarkTypography.bodyLarge)
                            }
                            innerTextField()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("All", "Debit", "Credit", "Food", "Travel", "Bills")
                filters.forEach { filter ->
                    val isSelected = (filter == "All" && state.searchQuery.isEmpty()) || state.searchQuery.equals(filter, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onSearchQueryChange(if (filter == "All") "" else filter) },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryContainer.copy(alpha = 0.2f),
                            selectedLabelColor = Primary,
                            containerColor = SurfaceContainerLow,
                            labelColor = OnSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = OutlineVariant.copy(alpha = 0.3f),
                            selectedBorderColor = Primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (state.groupedTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions found", color = OnSurfaceVariant, style = StarkTypography.bodyMedium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    state.groupedTransactions.forEach { (month, transactions) ->
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (month == java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())) "Today" else month,
                                    style = StarkTypography.headlineSmall.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium),
                                    color = PrimaryFixedDim
                                )
                                Text(
                                    text = month.uppercase(),
                                    style = StarkTypography.labelSmall.copy(fontSize = 10.sp),
                                    color = OnSurfaceVariant
                                )
                            }
                        }
                        items(transactions, key = { it.id }) { transaction ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = {
                                    if (it == SwipeToDismissBoxValue.EndToStart) {
                                        viewModel.deleteTransaction(transaction)
                                        true
                                    } else false
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                backgroundContent = {
                                    val color by animateColorAsState(
                                        when (dismissState.targetValue) {
                                            SwipeToDismissBoxValue.EndToStart -> Error.copy(alpha = 0.2f)
                                            else -> Color.Transparent
                                        }, label = "dismiss_color"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(color = color),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            Icons.Sharp.Delete,
                                            contentDescription = "Delete",
                                            tint = Error,
                                            modifier = Modifier.padding(end = 24.dp)
                                        )
                                    }
                                },
                                content = {
                                    TransactionRow(
                                        transaction = transaction,
                                        accountName = accounts.find { it.id == transaction.accountId }?.name,
                                        onClick = {
                                            selectedTransaction = transaction
                                            showDetailSheet = true
                                        },
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    )
                                }
                            )
                        }
                    }

                    // Insight Component at the bottom
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                        Surface(
                            color = Color(0x33201F1F),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(width = 1.dp, color = PrimaryContainer.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp))
                                .border(width = 2.dp, color = SecondaryContainer, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Sharp.AutoAwesome, contentDescription = null, tint = SecondaryContainer, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("FINANCIAL INSIGHT", style = StarkTypography.headlineSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp), color = SecondaryContainer)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Subscription Spike Detected", style = StarkTypography.headlineLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold), color = OnSurface)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Your spending on digital services has increased by 12% this month. StarkLedger recommends consolidating your streaming accounts to save approximately ₹45/mo.",
                                    style = StarkTypography.bodyMedium,
                                    color = OnSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { navController.navigate(com.starklabs.moneytracker.ui.Screen.Analytics.route) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceContainerHighest),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Text("REVIEW SERVICES", style = StarkTypography.labelLarge.copy(color = Primary, fontWeight = FontWeight.Bold, fontSize = 12.sp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatStarkTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = StarkTypography.labelSmall, color = OnSurfaceVariant.copy(alpha = 0.6f))
        Text(value, style = StarkTypography.bodyMedium, color = OnSurface)
    }
}
