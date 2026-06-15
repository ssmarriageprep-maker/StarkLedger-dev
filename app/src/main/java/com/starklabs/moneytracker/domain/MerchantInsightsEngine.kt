package com.starklabs.moneytracker.domain

import com.starklabs.moneytracker.data.Category
import kotlin.math.abs

/**
 * Pure, deterministic merchant intelligence engine.
 *
 * Inputs: two periods of [MerchantSummary] (current + previous) pre-computed by
 * [MerchantAnalyticsEngine], plus the live categories list for name resolution.
 *
 * No Room, no ViewModel, no Compose — fully unit-testable with plain lists.
 *
 * Priority scale (lower = shown first):
 *   1 SPENDING_INCREASE   — urgent: money flowing faster than last month
 *   2 SPENDING_DECREASE   — positive: savings signal
 *   3 FASTEST_GROWING     — one standout growth merchant
 *   4 TOP_MERCHANT        — "who do you spend most at / visit most"
 *   5 MERCHANT_CONCENTRATION — "one merchant is eating your budget"
 *   6 RECURRING_MERCHANT  — loyalty / subscription patterns
 *   7 HIGH_AVERAGE_SPEND  — "this place costs the most per visit"
 *   8 CATEGORY_DOMINANCE  — "in Food, Swiggy is 60%"
 */
object MerchantInsightsEngine {

    /**
     * @param currentSummaries  Summaries for the current period (e.g. this month)
     * @param previousSummaries Summaries for the prior period (e.g. last month); may be empty
     * @param categories        Live category list for name resolution
     * @param minSpendThreshold Minimum total spend to qualify a merchant for noisy signals
     * @param changeThresholdPct Minimum absolute % change to emit SPENDING_INCREASE/DECREASE
     */
    fun generate(
        currentSummaries: List<MerchantSummary>,
        previousSummaries: List<MerchantSummary>,
        categories: List<Category>,
        minSpendThreshold: Double = 100.0,
        changeThresholdPct: Double = 15.0
    ): List<MerchantInsight> {
        if (currentSummaries.isEmpty()) return emptyList()

        val insights = mutableListOf<MerchantInsight>()
        val prevByName = previousSummaries.associateBy { it.canonicalName }
        val totalCurrentSpend = currentSummaries.sumOf { it.totalSpent }

        val increaseMerchants = mutableSetOf<String>()

        // Priority 1 & 2 — spending changes vs previous period
        insights += spendingChangeInsights(
            currentSummaries, prevByName, minSpendThreshold, changeThresholdPct, increaseMerchants
        )

        // Priority 3 — fastest growing (skip if already mentioned in SPENDING_INCREASE)
        fastestGrowingInsight(currentSummaries, prevByName, minSpendThreshold, increaseMerchants)
            ?.let { insights += it }

        // Priority 4 — top merchant
        insights += topMerchantInsights(currentSummaries, minSpendThreshold)

        // Priority 5 — concentration
        if (totalCurrentSpend > 0) {
            insights += concentrationInsights(currentSummaries, totalCurrentSpend)
        }

        // Priority 6 — recurring
        insights += recurringInsights(currentSummaries)

        // Priority 7 — high average spend
        highAverageInsight(currentSummaries, minSpendThreshold)?.let { insights += it }

        // Priority 8 — category dominance
        if (categories.isNotEmpty() && totalCurrentSpend > 0) {
            insights += categoryDominanceInsights(currentSummaries, categories)
        }

        return insights.sortedWith(compareBy({ it.priority }, { it.title }))
    }

    // ── private generators ───────────────────────────────────────────────────────

    private fun spendingChangeInsights(
        current: List<MerchantSummary>,
        prevByName: Map<String, MerchantSummary>,
        minSpend: Double,
        threshold: Double,
        increaseMerchants: MutableSet<String>
    ): List<MerchantInsight> {
        data class Change(val name: String, val pct: Double)

        val changes = current
            .filter { it.totalSpent >= minSpend }
            .mapNotNull { curr ->
                val prevSpend = prevByName[curr.canonicalName]?.totalSpent ?: return@mapNotNull null
                if (prevSpend <= 0.0) return@mapNotNull null
                val pct = (curr.totalSpent - prevSpend) / prevSpend * 100.0
                if (abs(pct) < threshold) return@mapNotNull null
                Change(curr.canonicalName, pct)
            }

        val increases = changes.filter { it.pct > 0 }
            .sortedByDescending { it.pct }
            .take(2)
            .map { (name, pct) ->
                increaseMerchants += name
                MerchantInsight(
                    title = "Spending increase detected",
                    description = "$name spending increased ${pct.toInt()}% compared to last month.",
                    type = MerchantInsightType.SPENDING_INCREASE,
                    priority = 1
                )
            }

        val decreases = changes.filter { it.pct < 0 }
            .sortedBy { it.pct }
            .take(1)
            .map { (name, pct) ->
                MerchantInsight(
                    title = "Spending decrease",
                    description = "$name spending decreased ${abs(pct).toInt()}% compared to last month.",
                    type = MerchantInsightType.SPENDING_DECREASE,
                    priority = 2
                )
            }

        return increases + decreases
    }

    private fun fastestGrowingInsight(
        current: List<MerchantSummary>,
        prevByName: Map<String, MerchantSummary>,
        minSpend: Double,
        excludeNames: Set<String>
    ): MerchantInsight? {
        val candidate = current
            .filter { it.totalSpent >= minSpend && it.canonicalName !in excludeNames }
            .mapNotNull { curr ->
                val prevSpend = prevByName[curr.canonicalName]?.totalSpend ?: return@mapNotNull null
                if (prevSpend <= 0.0) return@mapNotNull null
                val pct = (curr.totalSpent - prevSpend) / prevSpend * 100.0
                if (pct <= 0.0) return@mapNotNull null
                curr.canonicalName to pct
            }
            .maxByOrNull { it.second } ?: return null

        return MerchantInsight(
            title = "Fastest growing merchant",
            description = "${candidate.first} — spending up ${candidate.second.toInt()}% vs last month.",
            type = MerchantInsightType.FASTEST_GROWING,
            priority = 3
        )
    }

    private fun topMerchantInsights(
        summaries: List<MerchantSummary>,
        minSpend: Double
    ): List<MerchantInsight> {
        val qualified = summaries.filter { it.totalSpent >= minSpend }
        if (qualified.isEmpty()) return emptyList()

        val insights = mutableListOf<MerchantInsight>()

        val mostVisited = qualified.maxByOrNull { it.transactionCount }
        if (mostVisited != null) {
            insights += MerchantInsight(
                title = "Most visited merchant",
                description = "${mostVisited.canonicalName} — ${mostVisited.transactionCount} transaction${if (mostVisited.transactionCount == 1) "" else "s"}.",
                type = MerchantInsightType.TOP_MERCHANT,
                priority = 4
            )
        }

        // Emit "most money spent" only when it's a different merchant from most-visited
        val highestSpend = qualified.maxByOrNull { it.totalSpent }
        if (highestSpend != null && highestSpend.canonicalName != mostVisited?.canonicalName) {
            insights += MerchantInsight(
                title = "Most money spent at",
                description = "${highestSpend.canonicalName} — ₹${String.format("%,.0f", highestSpend.totalSpent)} total.",
                type = MerchantInsightType.TOP_MERCHANT,
                priority = 4
            )
        }

        return insights
    }

    private fun concentrationInsights(
        summaries: List<MerchantSummary>,
        totalSpend: Double
    ): List<MerchantInsight> =
        summaries
            .filter { (it.totalSpent / totalSpend) >= 0.20 }
            .sortedByDescending { it.totalSpent }
            .take(3)
            .map { s ->
                val pct = (s.totalSpent / totalSpend * 100).toInt()
                MerchantInsight(
                    title = "Merchant concentration",
                    description = "${s.canonicalName} accounts for $pct% of your spending.",
                    type = MerchantInsightType.MERCHANT_CONCENTRATION,
                    priority = 5
                )
            }

    private fun recurringInsights(summaries: List<MerchantSummary>): List<MerchantInsight> =
        summaries
            .filter { it.monthsActive >= 3 }
            .sortedByDescending { it.monthsActive }
            .take(2)
            .map { s ->
                MerchantInsight(
                    title = "Recurring merchant",
                    description = "${s.canonicalName} has been active for ${s.monthsActive} months.",
                    type = MerchantInsightType.RECURRING_MERCHANT,
                    priority = 6
                )
            }

    private fun highAverageInsight(
        summaries: List<MerchantSummary>,
        minSpend: Double
    ): MerchantInsight? {
        val top = summaries
            .filter { it.transactionCount >= 2 && it.totalSpent >= minSpend }
            .maxByOrNull { it.averageTransaction } ?: return null
        return MerchantInsight(
            title = "Highest average transaction",
            description = "${top.canonicalName} averages ₹${String.format("%,.0f", top.averageTransaction)} per visit.",
            type = MerchantInsightType.HIGH_AVERAGE_SPEND,
            priority = 7
        )
    }

    private fun categoryDominanceInsights(
        summaries: List<MerchantSummary>,
        categories: List<Category>
    ): List<MerchantInsight> {
        val insights = mutableListOf<MerchantInsight>()

        for (cat in categories) {
            // Total spend attributed to this category across all merchants
            val catTotal = summaries.sumOf { s ->
                s.topCategories.find { it.categoryId == cat.id }?.amount ?: 0.0
            }
            if (catTotal <= 0) continue

            // Merchant with the highest amount in this category
            val dominant = summaries.maxByOrNull { s ->
                s.topCategories.find { it.categoryId == cat.id }?.amount ?: 0.0
            } ?: continue
            val dominantAmount = dominant.topCategories.find { it.categoryId == cat.id }?.amount ?: 0.0
            if (dominantAmount <= 0) continue

            val share = dominantAmount / catTotal
            if (share >= 0.30) {
                insights += MerchantInsight(
                    title = "Category leader",
                    description = "${dominant.canonicalName} represents ${(share * 100).toInt()}% of ${cat.name} spending.",
                    type = MerchantInsightType.CATEGORY_DOMINANCE,
                    priority = 8
                )
            }
        }

        // Cap at 2 to avoid flooding from many categories
        return insights.sortedByDescending { it.description }.take(2)
    }

    // Extension so callers can use prevByName[name]?.totalSpend without null-safe noise
    private val MerchantSummary.totalSpend: Double get() = totalSpent
}
