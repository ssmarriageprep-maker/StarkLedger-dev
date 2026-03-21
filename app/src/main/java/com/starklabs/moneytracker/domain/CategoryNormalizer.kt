package com.starklabs.moneytracker.domain

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

    fun normalizeMerchant(rawMerchant: String): String {
        var clean = rawMerchant
        clean = clean.replace(Regex("(?i)\\b(pvt|ltd|limited|private)\\b"), "").trim()
        clean = clean.replace(Regex("[^a-zA-Z0-9 ]"), " ").replace(Regex(" +"), " ").trim()
        
        val mapped = mappings.entries.find { clean.contains(it.key, ignoreCase = true) }
        return mapped?.key ?: clean
    }

    fun inferCategory(merchant: String): String {
        return mappings.entries.find { merchant.contains(it.key, ignoreCase = true) }?.value ?: "Others"
    }
}
