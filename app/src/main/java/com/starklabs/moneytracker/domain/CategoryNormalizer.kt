package com.starklabs.moneytracker.domain

import java.util.regex.Pattern

object CategoryNormalizer {
    private val mappings = mapOf(
        "Dreamplug" to "Shopping",
        "CRED" to "Bills",
        "Swiggy" to "Food",
        "Zomato" to "Food",
        "Uber" to "Travel",
        "Ola" to "Travel",
        "Amazon" to "Shopping",
        "Flipkart" to "Shopping",
        "Netflix" to "Bills",
        "Spotify" to "Bills",
        "Jio" to "Bills",
        "Airtel" to "Bills",
        "UPI" to "Transfer"
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
        
        val mapped = mappings.entries.find { clean.contains(it.key, ignoreCase = true) }
        return mapped?.key ?: clean
    }

    fun inferCategory(merchant: String): String {
        return mappings.entries.find { merchant.contains(it.key, ignoreCase = true) }?.value ?: "Others"
    }
}
