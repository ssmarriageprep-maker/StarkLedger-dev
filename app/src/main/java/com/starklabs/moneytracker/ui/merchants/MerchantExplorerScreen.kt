package com.starklabs.moneytracker.ui.merchants

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
import com.starklabs.moneytracker.domain.MerchantSortOrder
import com.starklabs.moneytracker.domain.MerchantSummary
import com.starklabs.moneytracker.domain.TrendDirection
import com.starklabs.moneytracker.ui.Screen
import com.starklabs.moneytracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantExplorerScreen(
    navController: NavController,
    viewModel: MerchantExplorerViewModel
) {
    val merchants by viewModel.filteredMerchants.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()

    Scaffold(
        containerColor = SurfaceContainerLowest,
        topBar = {
            Column(modifier = Modifier.background(SurfaceContainerLow)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Sharp.ArrowBack, contentDescription = "Back", tint = Primary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "Merchants",
                        style = StarkTypography.headlineLarge.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1).sp
                        ),
                        color = PrimaryContainer
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearch(it) },
                    placeholder = { Text("Search merchants…", color = OnSurfaceVariant) },
                    leadingIcon = {
                        Icon(Icons.Sharp.Search, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.updateSearch("") }) {
                                Icon(Icons.Sharp.Close, contentDescription = "Clear", tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryContainer.copy(alpha = 0.5f),
                        unfocusedBorderColor = OutlineVariant.copy(alpha = 0.15f),
                        focusedContainerColor = SurfaceContainerLow,
                        unfocusedContainerColor = SurfaceContainerLow,
                        cursorColor = PrimaryContainer,
                        focusedTextColor = OnSurface,
                        unfocusedTextColor = OnSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SortChip(
                        label = "Highest Spend",
                        selected = sortOrder == MerchantSortOrder.HIGHEST_SPEND,
                        onClick = { viewModel.updateSort(MerchantSortOrder.HIGHEST_SPEND) }
                    )
                    SortChip(
                        label = "Most Active",
                        selected = sortOrder == MerchantSortOrder.MOST_TRANSACTIONS,
                        onClick = { viewModel.updateSort(MerchantSortOrder.MOST_TRANSACTIONS) }
                    )
                    SortChip(
                        label = "Recent",
                        selected = sortOrder == MerchantSortOrder.RECENTLY_ACTIVE,
                        onClick = { viewModel.updateSort(MerchantSortOrder.RECENTLY_ACTIVE) }
                    )
                    SortChip(
                        label = "A–Z",
                        selected = sortOrder == MerchantSortOrder.ALPHABETICAL,
                        onClick = { viewModel.updateSort(MerchantSortOrder.ALPHABETICAL) }
                    )
                }
            }
        }
    ) { paddingValues ->
        if (merchants.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Sharp.Store,
                        contentDescription = null,
                        tint = OnSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        if (searchQuery.isBlank()) "No merchants yet" else "No results for \"$searchQuery\"",
                        style = StarkTypography.bodyLarge,
                        color = OnSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    Text(
                        "${merchants.size} merchant${if (merchants.size == 1) "" else "s"}",
                        style = StarkTypography.labelSmall,
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }
                items(merchants, key = { it.canonicalName }) { summary ->
                    MerchantRow(
                        summary = summary,
                        onClick = {
                            navController.navigate(Screen.MerchantDetail.createRoute(summary.canonicalName))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) PrimaryContainer.copy(alpha = 0.15f) else SurfaceContainerLow)
            .border(
                1.dp,
                if (selected) PrimaryContainer.copy(alpha = 0.6f) else OutlineVariant.copy(alpha = 0.2f),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            style = StarkTypography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (selected) PrimaryContainer else OnSurfaceVariant
        )
    }
}

@Composable
private fun MerchantRow(summary: MerchantSummary, onClick: () -> Unit) {
    val initial = summary.canonicalName.firstOrNull()?.uppercaseChar() ?: '?'
    val trendIcon = when (summary.trendDirection) {
        TrendDirection.INCREASING -> Icons.Sharp.TrendingUp
        TrendDirection.DECREASING -> Icons.Sharp.TrendingDown
        TrendDirection.STABLE -> Icons.Sharp.TrendingFlat
        TrendDirection.INSUFFICIENT_DATA -> null
    }
    val trendColor = when (summary.trendDirection) {
        TrendDirection.INCREASING -> Error
        TrendDirection.DECREASING -> TertiaryContainer
        TrendDirection.STABLE -> OnSurfaceVariant
        TrendDirection.INSUFFICIENT_DATA -> OnSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = SurfaceContainerLow,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PrimaryContainer.copy(alpha = 0.12f))
                    .border(1.dp, PrimaryContainer.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    initial.toString(),
                    style = StarkTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    summary.canonicalName,
                    style = StarkTypography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = OnSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${summary.transactionCount} txn${if (summary.transactionCount == 1) "" else "s"}",
                        style = StarkTypography.labelSmall,
                        color = OnSurfaceVariant
                    )
                    summary.topCategories.firstOrNull()?.let { cat ->
                        Text(
                            "  ·  ${cat.name}",
                            style = StarkTypography.labelSmall,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "₹${String.format("%,.0f", summary.totalSpent)}",
                    style = StarkTypography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = PrimaryContainer
                )
                if (trendIcon != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(trendIcon, contentDescription = null, tint = trendColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            String.format("%.1f/mo", summary.frequencyPerMonth),
                            style = StarkTypography.labelSmall.copy(fontSize = 10.sp),
                            color = trendColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Sharp.ChevronRight, contentDescription = null, tint = OnSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
        }
    }
}
