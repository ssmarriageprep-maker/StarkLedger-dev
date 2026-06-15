package com.starklabs.moneytracker.ui.merchants

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.starklabs.moneytracker.data.MoneyRepository
import com.starklabs.moneytracker.domain.CategoryAmount
import com.starklabs.moneytracker.domain.MerchantAnalyticsEngine
import com.starklabs.moneytracker.domain.MerchantNormalizationEngine
import com.starklabs.moneytracker.domain.MerchantSummary
import com.starklabs.moneytracker.domain.TrendDirection
import com.starklabs.moneytracker.ui.components.StarkCard
import com.starklabs.moneytracker.ui.components.TransactionRow
import com.starklabs.moneytracker.ui.components.formatStarkDate
import com.starklabs.moneytracker.ui.theme.*
import kotlinx.coroutines.flow.combine

@Composable
fun MerchantDetailScreen(
    navController: NavController,
    canonicalName: String,
    repository: MoneyRepository
) {
    val summary by produceState<MerchantSummary?>(null, canonicalName) {
        combine(
            repository.allTransactions,
            repository.allCategories,
            repository.allMerchantAliases
        ) { txns, cats, aliases ->
            val aliasMap = aliases.associateBy({ it.aliasKey }, { it.canonicalMerchant })
            val resolve = { raw: String ->
                val key = raw.trim().lowercase()
                aliasMap[key] ?: MerchantNormalizationEngine.normalize(raw).canonicalName
            }
            MerchantAnalyticsEngine.computeFor(canonicalName, txns, cats, resolve)
        }.collect { value = it }
    }

    Scaffold(
        containerColor = SurfaceContainerLowest,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(SurfaceContainerLow)
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
                Column {
                    Text(
                        canonicalName,
                        style = StarkTypography.headlineLarge.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = PrimaryContainer
                    )
                    if (summary != null) {
                        Text(
                            "${summary!!.transactionCount} transactions",
                            style = StarkTypography.labelSmall,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        val s = summary
        if (s == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryContainer, modifier = Modifier.size(32.dp))
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SpendingStatsCard(s) }
            item { ActivityCard(s) }
            item { TrendCard(s) }
            if (s.topCategories.isNotEmpty()) {
                item { TopCategoriesCard(s.topCategories) }
            }
            if (s.recentTransactions.isNotEmpty()) {
                item {
                    Text(
                        "RECENT TRANSACTIONS",
                        style = StarkTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }
                items(s.recentTransactions) { tx ->
                    TransactionRow(transaction = tx)
                }
            }
        }
    }
}

@Composable
private fun SpendingStatsCard(s: MerchantSummary) {
    StarkCard {
        Text("Spending Overview", style = StarkTypography.titleLarge)
        Text("ALL TIME", style = StarkTypography.labelSmall)
        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatCell("Total Spent", "₹${String.format("%,.0f", s.totalSpent)}", valueColor = PrimaryContainer)
            StatCell("Transactions", "${s.transactionCount}")
            StatCell("Average", "₹${String.format("%,.0f", s.averageTransaction)}")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            StatCell("Largest", "₹${String.format("%,.0f", s.largestTransaction)}", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ActivityCard(s: MerchantSummary) {
    StarkCard {
        Text("Activity", style = StarkTypography.titleLarge)
        Spacer(modifier = Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatCell("First Seen", formatStarkDate(s.firstSeen))
            StatCell("Last Seen", formatStarkDate(s.lastSeen))
            StatCell("Months Active", "${s.monthsActive}")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            StatCell(
                label = "Frequency",
                value = if (s.frequencyPerMonth >= 1) "${String.format("%.1f", s.frequencyPerMonth)}x / mo"
                        else "${String.format("%.1f", s.frequencyPerMonth * 30)} days / txn",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TrendCard(s: MerchantSummary) {
    val (icon, label, color) = when (s.trendDirection) {
        TrendDirection.INCREASING -> Triple(Icons.Sharp.TrendingUp, "Increasing", Error)
        TrendDirection.DECREASING -> Triple(Icons.Sharp.TrendingDown, "Decreasing", TertiaryContainer)
        TrendDirection.STABLE -> Triple(Icons.Sharp.TrendingFlat, "Stable", OnSurface)
        TrendDirection.INSUFFICIENT_DATA -> Triple(Icons.Sharp.HelpOutline, "Not enough data", OnSurfaceVariant)
    }

    StarkCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("Spending Trend", style = StarkTypography.titleLarge)
                Text("VS MONTHLY AVERAGE", style = StarkTypography.labelSmall)
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(color.copy(alpha = 0.12f))
                    .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, style = StarkTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = color)
            }
        }
    }
}

@Composable
private fun TopCategoriesCard(categories: List<CategoryAmount>) {
    val total = categories.sumOf { it.amount }

    StarkCard {
        Text("Top Categories", style = StarkTypography.titleLarge)
        Text("BY SPEND", style = StarkTypography.labelSmall)
        Spacer(modifier = Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            categories.forEach { cat ->
                val pct = if (total > 0) (cat.amount / total).toFloat() else 0f
                val catColor = try {
                    Color(android.graphics.Color.parseColor(cat.colorHex))
                } catch (e: Exception) {
                    PrimaryContainer
                }
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(catColor))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(cat.name, style = StarkTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = OnSurface)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${(pct * 100).toInt()}%", style = StarkTypography.labelSmall, color = OnSurfaceVariant)
                            Text("₹${String.format("%,.0f", cat.amount)}", style = StarkTypography.labelSmall, color = OnSurface)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(SurfaceContainerHighest)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(pct.coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(catColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = OnSurface
) {
    Column(modifier = modifier) {
        Text(label, style = StarkTypography.labelSmall, color = OnSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = StarkTypography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = valueColor)
    }
}
