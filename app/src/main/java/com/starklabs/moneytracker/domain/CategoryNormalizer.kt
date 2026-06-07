package com.starklabs.moneytracker.domain

import java.util.regex.Pattern

/**
 * Cleans up raw merchant strings extracted from SMS bodies for display.
 *
 * Category inference is intentionally NOT here — that lives in
 * [com.starklabs.moneytracker.data.MoneyRepository.identifyCategory], which is
 * driven by user-defined keywords stored in the database, so insights and the
 * dashboard stay in agreement.
 */
object CategoryNormalizer {
    private val knownBrands = listOf(
        "Dreamplug", "CRED", "Swiggy", "Zomato", "Uber", "Ola",
        "Amazon", "Flipkart", "Netflix", "Spotify", "Jio", "Airtel"
    )

    // Pre-compiled patterns for normalization to avoid redundant allocations
    private val ENTITY_SUFFIX_PATTERN = Pattern.compile("(?i)\\b(pvt|ltd|limited|private)\\b")
    private val SPECIAL_CHARS_PATTERN = Pattern.compile("[^a-zA-Z0-9 ]")
    private val MULTI_SPACE_PATTERN = Pattern.compile(" +")

    fun normalizeMerchant(rawMerchant: String): String {
        var clean = rawMerchant
        clean = ENTITY_SUFFIX_PATTERN.matcher(clean).replaceAll("").trim()
        clean = SPECIAL_CHARS_PATTERN.matcher(clean).replaceAll(" ")
        clean = MULTI_SPACE_PATTERN.matcher(clean).replaceAll(" ").trim()

        val matchedBrand = knownBrands.find { clean.contains(it, ignoreCase = true) }
        return matchedBrand ?: clean
    }
}
