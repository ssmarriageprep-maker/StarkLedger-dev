package com.starklabs.moneytracker.domain

/**
 * Thin compatibility wrapper — all normalization logic lives in [MerchantNormalizationEngine].
 * Existing call sites ([InsightsEngine], [AccountAnalyticsEngine]) remain unchanged.
 */
object CategoryNormalizer {
    fun normalizeMerchant(rawMerchant: String): String =
        MerchantNormalizationEngine.normalize(rawMerchant).canonicalName
}
