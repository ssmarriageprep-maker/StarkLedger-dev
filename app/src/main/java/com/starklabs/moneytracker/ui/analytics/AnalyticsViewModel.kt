package com.starklabs.moneytracker.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.starklabs.moneytracker.data.Category
import com.starklabs.moneytracker.data.MoneyRepository
import com.starklabs.moneytracker.data.Transaction
import com.starklabs.moneytracker.domain.FilterDimension
import com.starklabs.moneytracker.domain.MerchantAnalyticsEngine
import com.starklabs.moneytracker.domain.MerchantInsight
import com.starklabs.moneytracker.domain.MerchantInsightsEngine
import com.starklabs.moneytracker.domain.MerchantNormalizationEngine
import com.starklabs.moneytracker.domain.MerchantSummary
import com.starklabs.moneytracker.domain.TransactionFilter
import com.starklabs.moneytracker.domain.TransactionFilterEngine
import com.starklabs.moneytracker.domain.TransactionFilterStore
import com.starklabs.moneytracker.domain.YearlyAnalyticsEngine
import com.starklabs.moneytracker.ui.theme.NeonCyan
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class Slice(
    val value: Float,
    val color: Color,
    val label: String
)

data class CategoryPerformance(
    val name: String,
    val spent: Double,
    val budget: Double,
    val color: Color,
    val percentage: Float
)

data class AnalyticsState(
    val pieSlices: List<Slice> = emptyList(),
    val weeklySpending: List<Float> = emptyList(), // Last 7 days
    val topCategory: String = "N/A",
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val categoryPerformance: List<CategoryPerformance> = emptyList(),
    val pulseTitle: String = "Calculating Pulse...",
    val pulseDescription: String = "Analyzing your spending patterns...",
    val pulseColor: Color = Color(0xFFFEB300) // SecondaryContainer
)

/** Month vs Year toggle for the Analytics screen — drives which aggregation is displayed. */
enum class AnalyticsViewMode { MONTHLY, YEARLY }

/**
 * Yearly aggregation surfaced to the screen. Built on top of [YearlyAnalyticsEngine]
 * so chart inputs ([Slice] for the donut, normalized 0..1 series for [com.starklabs.moneytracker.ui.components.GlowingLineChart])
 * are derived here rather than duplicated in Compose — "no duplicated chart logic".
 */
data class YearlyAnalyticsState(
    val year: Int = Calendar.getInstance().get(Calendar.YEAR),
    val availableYears: List<Int> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val savings: Double = 0.0,
    val topCategorySlices: List<Slice> = emptyList(),
    val monthlyIncomeTrend: List<Float> = emptyList(),
    val monthlyExpenseTrend: List<Float> = emptyList(),
    val monthLabels: List<String> = emptyList()
)

private data class AnalyticsScope(
    val transactions: List<Transaction>,
    val categories: List<Category>
)

class AnalyticsViewModel(
    private val repository: MoneyRepository,
    private val appSettingsRepository: com.starklabs.moneytracker.data.AppSettingsRepository,
    private val filterStore: TransactionFilterStore
) : ViewModel() {

    val accounts: StateFlow<List<com.starklabs.moneytracker.data.Account>> = repository.allAccounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val categories: StateFlow<List<Category>> = repository.allCategories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val selectedAccountId: StateFlow<Int> = appSettingsRepository.selectedAccountId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1)

    fun setSelectedAccount(id: Int) {
        viewModelScope.launch {
            appSettingsRepository.setSelectedAccountId(id)
        }
    }

    /** Shared advanced-filter state — same instance History reads, so filters persist across screens. */
    val filter: StateFlow<TransactionFilter> = filterStore.filter

    fun applyFilter(newFilter: TransactionFilter) = filterStore.update(newFilter)

    fun clearFilterDimension(dimension: FilterDimension) = filterStore.clearDimension(dimension)

    fun clearAllFilters() = filterStore.clear()

    private val _viewMode = MutableStateFlow(AnalyticsViewMode.MONTHLY)
    val viewMode: StateFlow<AnalyticsViewMode> = _viewMode.asStateFlow()

    fun setViewMode(mode: AnalyticsViewMode) {
        _viewMode.value = mode
    }

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    fun setSelectedYear(year: Int) {
        _selectedYear.value = year
    }

    /** Account + advanced-filter scoped transactions, shared by both the monthly and yearly aggregations below. */
    private val scopedData: StateFlow<AnalyticsScope> = combine(
        repository.allTransactions,
        repository.allCategories,
        appSettingsRepository.selectedAccountId,
        filterStore.filter
    ) { allTransactions, allCategories, selectedId, activeFilter ->
        val byAccount = if (selectedId == -1) allTransactions else allTransactions.filter { it.accountId == selectedId }
        AnalyticsScope(TransactionFilterEngine.apply(byAccount, activeFilter), allCategories)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsScope(emptyList(), emptyList()))

    val uiState: StateFlow<AnalyticsState> = scopedData.map { scope ->
        val transactions = scope.transactions
        val categories = scope.categories

        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis

        val currentMonthTransactions = transactions.filter { it.date >= startOfMonth }

        // 1. Process Pie Chart (By Category)
        val categoryMap = currentMonthTransactions.filter { it.type == "DEBIT" }.groupBy { it.categoryId }
        val totalDebit = currentMonthTransactions.filter { it.type == "DEBIT" }.sumOf { it.amount }
        val totalCredit = currentMonthTransactions.filter { it.type == "CREDIT" }.sumOf { it.amount }

        val slices = categoryMap.mapNotNull { (catId, list) ->
            val category = categories.find { it.id == catId }
            val name = category?.name ?: "Uncategorized"
            val colorHex = category?.colorHex ?: "#808080"

            val sum = list.sumOf { it.amount }
            if (sum == 0.0) return@mapNotNull null

            val portion = if (totalDebit > 0.0) (sum / totalDebit).toFloat() else 0f

            Slice(
                value = portion,
                color = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { NeonCyan },
                label = name
            )
        }.sortedByDescending { it.value }.take(5) // Top 5

        // 2. Process Weekly Spending (Last 7 days approx)
        val trend = if (transactions.isEmpty()) emptyList()
                   else transactions.take(7).map { (it.amount / 5000.0).toFloat().coerceIn(0f, 1f) }

        // 3. Process Category Performance
        val performance = categoryMap.mapNotNull { (catId, list) ->
            val category = categories.find { it.id == catId } ?: return@mapNotNull null
            val sum = list.sumOf { it.amount }
            if (sum == 0.0) return@mapNotNull null

            CategoryPerformance(
                name = category.name,
                spent = sum,
                budget = category.budgetLimit,
                color = try { Color(android.graphics.Color.parseColor(category.colorHex)) } catch (e: Exception) { NeonCyan },
                percentage = (sum / category.budgetLimit).toFloat().coerceIn(0f, 1f)
            )
        }.sortedByDescending { it.percentage }

        // 4. Calculate Weekly Pulse (Rolling 7 days)
        val now = System.currentTimeMillis()
        val sevenDaysMillis = 7 * 24 * 60 * 60 * 1000L
        val last7DaysTransactions = transactions.filter { it.date in (now - sevenDaysMillis)..now && it.type == "DEBIT" }
        val previous7DaysTransactions = transactions.filter { it.date in (now - 2 * sevenDaysMillis)..(now - sevenDaysMillis) && it.type == "DEBIT" }

        val last7DaysSpend = last7DaysTransactions.sumOf { it.amount }
        val previous7DaysSpend = previous7DaysTransactions.sumOf { it.amount }

        val (pulseTitle, pulseDescription, pulseColor) = if (previous7DaysSpend > 0) {
            val percentChange = ((last7DaysSpend - previous7DaysSpend) / previous7DaysSpend * 100).toInt()
            if (percentChange > 0) {
                // Determine spike category
                val last7DaysByCat = last7DaysTransactions.groupBy { it.categoryId }
                val prev7DaysByCat = previous7DaysTransactions.groupBy { it.categoryId }

                val spikeCategory = last7DaysByCat.mapNotNull { (catId, list) ->
                    val lastSum = list.sumOf { it.amount }
                    val prevSum = prev7DaysByCat[catId]?.sumOf { it.amount } ?: 0.0
                    val increase = lastSum - prevSum
                    if (increase > 0) catId to increase else null
                }.maxByOrNull { it.second }?.first?.let { catId -> categories.find { it.id == catId }?.name } ?: "multiple categories"

                Triple(
                    "You spent $percentChange% more this week",
                    "Unusual spikes detected in the $spikeCategory. Consider reviewing your last three transactions.",
                    Color(0xFFFFB4AB) // Error color for spending increase
                )
            } else {
                Triple(
                    "You saved ${Math.abs(percentChange)}% this week!",
                    "Excellent progress! Your spending velocity has decreased compared to last week. Keep it up.",
                    Color(0xFF5FEC79) // TertiaryContainer for savings
                )
            }
        } else if (last7DaysSpend > 0) {
            Triple(
                "Fresh activity detected",
                "You've started tracking ₹${String.format("%,.0f", last7DaysSpend)} this week. We'll compare this to your next 7 days.",
                Color(0xFF00DAF2) // PrimaryFixedDim for new data
            )
        } else {
            Triple(
                "Pulse check complete",
                "No spending detected in the last 7 days. Your budget is currently under zero pressure.",
                Color(0xFF00DAF2)
            )
        }

        AnalyticsState(
            pieSlices = slices,
            weeklySpending = trend,
            topCategory = slices.firstOrNull()?.label ?: "N/A",
            totalIncome = totalCredit,
            totalExpense = totalDebit,
            categoryPerformance = performance,
            pulseTitle = pulseTitle,
            pulseDescription = pulseDescription,
            pulseColor = pulseColor
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsState())

    /**
     * Yearly aggregation for the Year toggle. Delegates all math to [YearlyAnalyticsEngine]
     * (account + filter scoped via [scopedData]) and only shapes the result into chart-ready
     * [Slice]/normalized-series form here, reusing [com.starklabs.moneytracker.ui.components.AnimatedDonutChart]
     * and [com.starklabs.moneytracker.ui.components.GlowingLineChart] in the screen.
     */
    /** Top 5 merchants by spend for the current account/filter scope, alias-resolved. */
    val topMerchants: StateFlow<List<MerchantSummary>> = combine(
        scopedData,
        repository.allMerchantAliases
    ) { scope, aliases ->
        val aliasMap = aliases.associateBy({ it.aliasKey }, { it.canonicalMerchant })
        val resolve = { raw: String ->
            val key = raw.trim().lowercase()
            aliasMap[key] ?: MerchantNormalizationEngine.normalize(raw).canonicalName
        }
        MerchantAnalyticsEngine.computeAll(scope.transactions, scope.categories, resolve)
            .sortedByDescending { it.totalSpent }
            .take(5)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Merchant intelligence insights for the current scope.
     * Computes current-month vs previous-month summaries to generate period-over-period signals.
     */
    val merchantInsights: StateFlow<List<MerchantInsight>> = combine(
        scopedData,
        repository.allMerchantAliases
    ) { scope, aliases ->
        val aliasMap = aliases.associateBy({ it.aliasKey }, { it.canonicalMerchant })
        val resolve = { raw: String ->
            val key = raw.trim().lowercase()
            aliasMap[key] ?: MerchantNormalizationEngine.normalize(raw).canonicalName
        }

        val cal = Calendar.getInstance()
        // Start of current month
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val startOfCurrentMonth = cal.timeInMillis
        // Start of previous month
        cal.add(Calendar.MONTH, -1)
        val startOfPrevMonth = cal.timeInMillis

        val currentTxns = scope.transactions.filter { it.date >= startOfCurrentMonth }
        val prevTxns    = scope.transactions.filter { it.date in startOfPrevMonth until startOfCurrentMonth }

        val currentSummaries = MerchantAnalyticsEngine.computeAll(currentTxns, scope.categories, resolve)
        val prevSummaries    = MerchantAnalyticsEngine.computeAll(prevTxns,    scope.categories, resolve)

        MerchantInsightsEngine.generate(currentSummaries, prevSummaries, scope.categories)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val yearlyState: StateFlow<YearlyAnalyticsState> = combine(scopedData, _selectedYear) { scope, year ->
        val summary = YearlyAnalyticsEngine.compute(scope.transactions, scope.categories, year)
        val availableYears = YearlyAnalyticsEngine.availableYears(scope.transactions).ifEmpty { listOf(year) }

        val topCategoryTotal = summary.topCategories.sumOf { it.amount }
        val slices = summary.topCategories.take(5).map { categoryAmount ->
            Slice(
                value = if (topCategoryTotal > 0.0) (categoryAmount.amount / topCategoryTotal).toFloat() else 0f,
                color = try { Color(android.graphics.Color.parseColor(categoryAmount.colorHex)) } catch (e: Exception) { NeonCyan },
                label = categoryAmount.name
            )
        }

        val maxMonthly = summary.monthlyTrend.maxOfOrNull { maxOf(it.income, it.expense) } ?: 0.0
        fun normalize(value: Double): Float = if (maxMonthly > 0.0) (value / maxMonthly).toFloat().coerceIn(0f, 1f) else 0f

        YearlyAnalyticsState(
            year = summary.year,
            availableYears = availableYears,
            totalIncome = summary.totalIncome,
            totalExpense = summary.totalExpense,
            savings = summary.savings,
            topCategorySlices = slices,
            monthlyIncomeTrend = summary.monthlyTrend.map { normalize(it.income) },
            monthlyExpenseTrend = summary.monthlyTrend.map { normalize(it.expense) },
            monthLabels = summary.monthlyTrend.map { it.label }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), YearlyAnalyticsState())
}

class AnalyticsViewModelFactory(
    private val repository: MoneyRepository,
    private val appSettingsRepository: com.starklabs.moneytracker.data.AppSettingsRepository,
    private val filterStore: TransactionFilterStore
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AnalyticsViewModel(repository, appSettingsRepository, filterStore) as T
    }
}
