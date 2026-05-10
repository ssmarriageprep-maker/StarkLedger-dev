package com.starklabs.moneytracker.domain

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

    fun normalizeMerchant(rawMerchant: String): String {
        var clean = rawMerchant
        clean = clean.replace(Regex("(?i)\\b(pvt|ltd|limited|private)\\b"), "").trim()
        clean = clean.replace(Regex("[^a-zA-Z0-9 ]"), " ").replace(Regex(" +"), " ").trim()

        val matchedBrand = knownBrands.find { clean.contains(it, ignoreCase = true) }
        return matchedBrand ?: clean
    }
}
