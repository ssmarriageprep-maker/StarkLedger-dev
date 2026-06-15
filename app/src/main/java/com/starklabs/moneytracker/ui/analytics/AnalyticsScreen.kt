package com.starklabs.moneytracker.ui.analytics

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.sharp.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.starklabs.moneytracker.ui.components.*
import com.starklabs.moneytracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    navController: NavController,
    viewModel: AnalyticsViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val selectedAccountId by viewModel.selectedAccountId.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val activeFilter by viewModel.filter.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val yearlyState by viewModel.yearlyState.collectAsState()
    val topMerchants by viewModel.topMerchants.collectAsState()
    val merchantInsights by viewModel.merchantInsights.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }

    if (showFilterSheet) {
        TransactionFilterSheet(
            filter = activeFilter,
            categories = categories,
            accounts = accounts,
            onApply = {
                viewModel.applyFilter(it)
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
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
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // View mode toggle + advanced filter entry point
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnalyticsViewModeToggle(
                    selected = viewMode,
                    onSelect = { viewModel.setViewMode(it) }
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (activeFilter.isActive) PrimaryContainer.copy(alpha = 0.2f) else SurfaceContainerLow)
                        .border(
                            1.dp,
                            if (activeFilter.isActive) Primary else OutlineVariant.copy(alpha = 0.15f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { showFilterSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Sharp.Tune,
                        contentDescription = "Filter transactions",
                        tint = if (activeFilter.isActive) Primary else OnSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ActiveFilterChipsRow(
                filter = activeFilter,
                categories = categories,
                accounts = accounts,
                onClearDimension = { viewModel.clearFilterDimension(it) },
                onClearAll = { viewModel.clearAllFilters() },
                modifier = Modifier.fillMaxWidth()
            )

            if (activeFilter.isActive) {
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (viewMode == AnalyticsViewMode.YEARLY) {
                Spacer(modifier = Modifier.height(8.dp))
                YearlyAnalyticsContent(
                    state = yearlyState,
                    visible = visible,
                    onYearSelected = { viewModel.setSelectedYear(it) }
                )
                Spacer(modifier = Modifier.height(40.dp))
                return@Column
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Insight Banner
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(800)) + slideInVertically(tween(800), initialOffsetY = { it / 2 })
            ) {
            Surface(
                color = SurfaceContainerLow,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = OutlineVariant.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp))
                    .border(width = 2.dp, color = state.pulseColor, shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 0.dp))
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("WEEKLY PULSE", style = StarkTypography.labelSmall.copy(color = state.pulseColor, fontWeight = FontWeight.SemiBold))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.pulseTitle,
                            style = StarkTypography.headlineMedium.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                            color = OnSurface,
                            lineHeight = 30.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.pulseDescription,
                            style = StarkTypography.bodyMedium,
                            color = OnSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Surface(
                        color = SurfaceContainerHigh,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Sharp.Lightbulb, contentDescription = null, tint = state.pulseColor, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Top Merchants
            if (topMerchants.isNotEmpty()) {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(950)) + slideInVertically(tween(950), initialOffsetY = { it / 2 })
                ) {
                    com.starklabs.moneytracker.ui.components.StarkCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Top Merchants", style = StarkTypography.titleLarge)
                                Text("BY SPEND THIS PERIOD", style = StarkTypography.labelSmall)
                            }
                            androidx.compose.foundation.text.ClickableText(
                                text = androidx.compose.ui.text.buildAnnotatedString {
                                    pushStyle(androidx.compose.ui.text.SpanStyle(color = PrimaryContainer, fontSize = 12.sp))
                                    append("View All →")
                                    pop()
                                },
                                onClick = { navController.navigate(com.starklabs.moneytracker.ui.Screen.MerchantExplorer.route) }
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            topMerchants.forEachIndexed { index, merchant ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${index + 1}",
                                        style = StarkTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = OnSurfaceVariant,
                                        modifier = Modifier.width(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            merchant.canonicalName,
                                            style = StarkTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = OnSurface
                                        )
                                        merchant.topCategories.firstOrNull()?.let {
                                            Text(it.name, style = StarkTypography.labelSmall, color = OnSurfaceVariant)
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "₹${String.format("%,.0f", merchant.totalSpent)}",
                                            style = StarkTypography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = PrimaryContainer
                                        )
                                        Text(
                                            "${merchant.transactionCount} txns",
                                            style = StarkTypography.labelSmall,
                                            color = OnSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Merchant Insights
            if (merchantInsights.isNotEmpty()) {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(975)) + slideInVertically(tween(975), initialOffsetY = { it / 2 })
                ) {
                    MerchantInsightsSection(merchantInsights)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Main Grid
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000)) + slideInVertically(tween(1000), initialOffsetY = { it / 2 })
            ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Spending Velocity (Line Chart)
                StarkCard(modifier = Modifier.weight(2f).height(400.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Column {
                            Text("Spending Velocity", style = StarkTypography.titleLarge)
                            Text("JAN 01 — JUN 30", style = StarkTypography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("₹${String.format("%,.0f", state.totalExpense)}", style = StarkTypography.headlineSmall.copy(color = PrimaryContainer))
                            Text("+4.2% GROWTH", style = StarkTypography.labelSmall.copy(color = Tertiary, fontSize = 10.sp))
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        // Mock data for line chart
                        GlowingLineChart(
                            data = listOf(0.2f, 0.4f, 0.3f, 0.6f, 0.8f, 1.0f),
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            color = SurfaceContainerHighest,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).border(width = 1.dp, color = PrimaryContainer.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp))
                        ) {
                            Text("PEAK JUNE: ₹3.2K", style = StarkTypography.labelSmall.copy(color = PrimaryContainer, fontSize = 8.sp), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN").forEach { month ->
                            Text(month, style = StarkTypography.labelSmall.copy(fontSize = 10.sp))
                        }
                    }
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Composition (Pie Chart)
                StarkCard(modifier = Modifier.weight(1f).height(400.dp)) {
                    Text("Composition", style = StarkTypography.titleLarge)
                    Text("BY PERCENTAGE", style = StarkTypography.labelSmall)

                    Spacer(modifier = Modifier.weight(1f))

                    Box(modifier = Modifier.size(160.dp).align(Alignment.CenterHorizontally), contentAlignment = Alignment.Center) {
                        AnimatedDonutChart(slices = state.pieSlices, modifier = Modifier.fillMaxSize())
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("78%", style = StarkTypography.headlineMedium, color = OnSurface)
                            Text("TRACKED", style = StarkTypography.labelSmall.copy(fontSize = 10.sp))
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Column {
                        state.categoryPerformance.take(3).forEach { perf ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(perf.color))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(perf.name, style = StarkTypography.bodySmall, color = OnSurface)
                                }
                                Text("${(perf.percentage * 100).toInt()}%", style = StarkTypography.labelSmall)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            // Category Comparison
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1200)) + slideInVertically(tween(1200), initialOffsetY = { it / 2 })
            ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                StarkCard(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Benchmarking", style = StarkTypography.titleLarge)
                        Icon(Icons.Sharp.FilterList, contentDescription = null, tint = OnSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Column {
                        state.categoryPerformance.take(3).forEach { perf ->
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                                    Text(perf.name, style = StarkTypography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                    Text("₹${String.format("%.0f", perf.spent)} / ₹${String.format("%.0f", perf.budget)} Budget", style = StarkTypography.labelSmall)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(SurfaceContainerHighest)) {
                                    Box(modifier = Modifier.fillMaxWidth(perf.percentage.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(if (perf.percentage >= 1f) Error else PrimaryContainer))
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Timeline
                StarkCard(modifier = Modifier.weight(1f)) {
                    Text("Active Timeline", style = StarkTypography.titleLarge)
                    Spacer(modifier = Modifier.height(24.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Using a simple vertical line and items
                        state.categoryPerformance.take(3).forEachIndexed { index, perf ->
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(if(index==0) PrimaryContainer else SurfaceContainerHighest).border(width = 1.dp, color = PrimaryContainer.copy(alpha = 0.2f), shape = CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Sharp.ShoppingBag, contentDescription = null, tint = if(index==0) OnPrimary else PrimaryContainer, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Merchant ${index + 1}", style = StarkTypography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                    Text("TODAY, 14:22", style = StarkTypography.labelSmall)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("-₹1,299.00", style = StarkTypography.headlineSmall.copy(fontSize = 18.sp))
                                    Text("HARDWARE", style = StarkTypography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun MerchantInsightsSection(insights: List<com.starklabs.moneytracker.domain.MerchantInsight>) {
    com.starklabs.moneytracker.ui.components.StarkCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Merchant Intelligence", style = StarkTypography.titleLarge)
                Text("SPENDING INSIGHTS", style = StarkTypography.labelSmall)
            }
            Surface(
                color = PrimaryContainer.copy(alpha = 0.12f),
                shape = androidx.compose.foundation.shape.CircleShape,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        androidx.compose.material.icons.Icons.Sharp.Insights,
                        contentDescription = null,
                        tint = PrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            insights.forEach { insight -> InsightRow(insight) }
        }
    }
}

@Composable
private fun InsightRow(insight: com.starklabs.moneytracker.domain.MerchantInsight) {
    val (icon, tint) = when (insight.type) {
        com.starklabs.moneytracker.domain.MerchantInsightType.SPENDING_INCREASE ->
            androidx.compose.material.icons.Icons.Sharp.TrendingUp to Error
        com.starklabs.moneytracker.domain.MerchantInsightType.SPENDING_DECREASE ->
            androidx.compose.material.icons.Icons.Sharp.TrendingDown to TertiaryContainer
        com.starklabs.moneytracker.domain.MerchantInsightType.FASTEST_GROWING ->
            androidx.compose.material.icons.Icons.Sharp.Bolt to Error.copy(alpha = 0.8f)
        com.starklabs.moneytracker.domain.MerchantInsightType.TOP_MERCHANT ->
            androidx.compose.material.icons.Icons.Sharp.EmojiEvents to SecondaryContainer
        com.starklabs.moneytracker.domain.MerchantInsightType.MERCHANT_CONCENTRATION ->
            androidx.compose.material.icons.Icons.Sharp.PieChart to OnSurface
        com.starklabs.moneytracker.domain.MerchantInsightType.RECURRING_MERCHANT ->
            androidx.compose.material.icons.Icons.Sharp.Repeat to PrimaryContainer.copy(alpha = 0.7f)
        com.starklabs.moneytracker.domain.MerchantInsightType.HIGH_AVERAGE_SPEND ->
            androidx.compose.material.icons.Icons.Sharp.AttachMoney to OnSurface
        com.starklabs.moneytracker.domain.MerchantInsightType.CATEGORY_DOMINANCE ->
            androidx.compose.material.icons.Icons.Sharp.Category to OnSurfaceVariant
    }

    Row(verticalAlignment = Alignment.Top) {
        Surface(
            color = tint.copy(alpha = 0.10f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                insight.title,
                style = StarkTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = OnSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                insight.description,
                style = StarkTypography.bodySmall,
                color = OnSurfaceVariant
            )
        }
    }
}

/** Segmented Month/Year switch driving [AnalyticsViewMode] — mirrors the chip styling used elsewhere. */
@Composable
private fun AnalyticsViewModeToggle(
    selected: AnalyticsViewMode,
    onSelect: (AnalyticsViewMode) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow)
            .border(1.dp, OutlineVariant.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        listOf(AnalyticsViewMode.MONTHLY to "Month", AnalyticsViewMode.YEARLY to "Year").forEach { (mode, label) ->
            val isSelected = selected == mode
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) PrimaryContainer.copy(alpha = 0.2f) else Color.Transparent)
                    .clickable { onSelect(mode) }
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    label,
                    style = StarkTypography.labelLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                    color = if (isSelected) Primary else OnSurfaceVariant
                )
            }
        }
    }
}

/**
 * Yearly Analytics body: Total Income/Expense/Savings, Top Categories, Monthly Trend.
 * Reuses [AnimatedDonutChart] and [GlowingLineChart] from Charts.kt — no bespoke chart code.
 */
@Composable
private fun YearlyAnalyticsContent(
    state: YearlyAnalyticsState,
    visible: Boolean,
    onYearSelected: (Int) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(800)) + slideInVertically(tween(800), initialOffsetY = { it / 2 })
    ) {
        Column {
            // Year selector
            if (state.availableYears.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.availableYears.forEach { year ->
                        val isSelected = year == state.year
                        FilterChip(
                            selected = isSelected,
                            onClick = { onYearSelected(year) },
                            label = { Text(year.toString()) },
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
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Totals
            StarkCard(modifier = Modifier.fillMaxWidth()) {
                Text("${state.year} Overview", style = StarkTypography.titleLarge)
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StarkStat(label = "Total Income", value = "₹${String.format("%,.0f", state.totalIncome)}", valueColor = TertiaryContainer)
                    StarkStat(label = "Total Expense", value = "₹${String.format("%,.0f", state.totalExpense)}", valueColor = Error)
                    StarkStat(
                        label = "Savings",
                        value = "₹${String.format("%,.0f", state.savings)}",
                        valueColor = if (state.savings >= 0) TertiaryContainer else Error
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                // Monthly Trend (Line Chart) — reuses GlowingLineChart, same as Spending Velocity
                StarkCard(modifier = Modifier.weight(2f).height(360.dp)) {
                    Text("Monthly Trend", style = StarkTypography.titleLarge)
                    Text("INCOME VS EXPENSE", style = StarkTypography.labelSmall)
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        GlowingLineChart(
                            data = state.monthlyExpenseTrend,
                            modifier = Modifier.fillMaxSize()
                        )
                        GlowingLineChart(
                            data = state.monthlyIncomeTrend,
                            modifier = Modifier.fillMaxSize(),
                            lineColor = TertiaryContainer,
                            fillStartColor = TertiaryContainer.copy(alpha = 0.25f),
                            fillEndColor = Color.Transparent
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        state.monthLabels.forEach { label ->
                            Text(label, style = StarkTypography.labelSmall.copy(fontSize = 9.sp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(PrimaryContainer))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Expense", style = StarkTypography.labelSmall, color = OnSurfaceVariant)
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(TertiaryContainer))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Income", style = StarkTypography.labelSmall, color = OnSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Top Categories (Donut Chart) — reuses AnimatedDonutChart, same as Composition
                StarkCard(modifier = Modifier.weight(1f).height(360.dp)) {
                    Text("Top Categories", style = StarkTypography.titleLarge)
                    Text("BY YEARLY SPEND", style = StarkTypography.labelSmall)

                    Spacer(modifier = Modifier.weight(1f))

                    Box(modifier = Modifier.size(140.dp).align(Alignment.CenterHorizontally), contentAlignment = Alignment.Center) {
                        AnimatedDonutChart(slices = state.topCategorySlices, modifier = Modifier.fillMaxSize())
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Column {
                        if (state.topCategorySlices.isEmpty()) {
                            Text("No spending recorded yet", style = StarkTypography.bodySmall, color = OnSurfaceVariant)
                        } else {
                            state.topCategorySlices.take(3).forEach { slice ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(slice.color))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(slice.label, style = StarkTypography.bodySmall, color = OnSurface)
                                    }
                                    Text("${(slice.value * 100).toInt()}%", style = StarkTypography.labelSmall)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
