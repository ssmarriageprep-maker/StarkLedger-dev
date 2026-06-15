package com.starklabs.moneytracker.domain

enum class MerchantInsightType {
    SPENDING_INCREASE,
    SPENDING_DECREASE,
    TOP_MERCHANT,
    MERCHANT_CONCENTRATION,
    RECURRING_MERCHANT,
    FASTEST_GROWING,
    HIGH_AVERAGE_SPEND,
    CATEGORY_DOMINANCE
}

/**
 * A single user-facing merchant intelligence insight.
 *
 * [priority] controls display order — lower value appears first.
 * Insights are deterministic: same inputs always produce the same list in the same order.
 */
data class MerchantInsight(
    val title: String,
    val description: String,
    val type: MerchantInsightType,
    val priority: Int
)
