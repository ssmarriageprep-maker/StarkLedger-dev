package com.starklabs.moneytracker.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    var transactionToEdit by remember { mutableStateOf<com.starklabs.moneytracker.data.Transaction?>(null) }
    
    if (transactionToEdit != null) {
        AlertDialog(
            onDismissRequest = { transactionToEdit = null },
            title = { Text("Edit Category") },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    items(categories) { cat ->
                        Text(
                            text = cat.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    transactionToEdit?.let {
                                        viewModel.updateTransactionCategory(it.id, cat.id, it.merchant)
                                    }
                                    transactionToEdit = null
                                }
                                .padding(16.dp),
                            color = OnSurface
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { transactionToEdit = null }) {
                    Text("Cancel", color = Primary)
                }
            },
            containerColor = SurfaceContainer
        )
    }

    Scaffold(
        containerColor = SurfaceContainerLowest,
        topBar = {
            StarkHeader(
                title = "StarkLedger",
                onSettingsClick = { navController.navigate(com.starklabs.moneytracker.ui.Screen.Settings.route) }
            )
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

            Spacer(modifier = Modifier.height(32.dp))

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
                        items(transactions) { transaction ->
                            StarkCard(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                backgroundColor = SurfaceContainerLow,
                                cornerRadius = 12.dp,
                                contentPadding = PaddingValues(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(24.dp))
                                                .background(SurfaceContainerHighest),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CategoryIcon(null, tint = PrimaryFixedDim, modifier = Modifier.size(24.dp))
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.padding(end = 16.dp)) {
                                            Text(
                                                text = transaction.merchant,
                                                style = StarkTypography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                                color = OnSurface,
                                                maxLines = 2
                                            )
                                            Text("${formatStarkTime(transaction.date)}", style = StarkTypography.labelLarge.copy(fontSize = 12.sp), color = OnSurfaceVariant)
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        val isDebit = transaction.type == "DEBIT"
                                        Text(
                                            text = "${if (isDebit) "-" else "+"}₹${String.format("%,.2f", transaction.amount)}",
                                            style = StarkTypography.headlineSmall.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                                            color = if (isDebit) OnSurface else TertiaryContainer
                                        )
                                        Text(
                                            text = transaction.type.uppercase(),
                                            style = StarkTypography.labelSmall.copy(fontSize = 10.sp),
                                            color = if (isDebit) Outline else TertiaryContainer.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
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
                                    onClick = {},
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
