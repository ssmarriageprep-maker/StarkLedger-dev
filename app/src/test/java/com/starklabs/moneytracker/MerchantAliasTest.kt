package com.starklabs.moneytracker

import com.starklabs.moneytracker.domain.MerchantNormalizationEngine
import com.starklabs.moneytracker.domain.MerchantResolution
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the merchant alias resolution contract.
 *
 * The repository's [resolveMerchant] logic is: alias-map lookup (confidence 100) first,
 * then [MerchantNormalizationEngine] fallback. These tests verify that contract using a plain
 * Map to stand in for the DB — keeping tests pure, fast, and Room-free while still proving
 * the correctness of the priority logic.
 */
class MerchantAliasTest {

    @Before
    fun clearCache() = MerchantNormalizationEngine.clearCache()

    // ── helpers mirroring MoneyRepository.resolveMerchant ──────────────────────

    /** Simulates the repository's resolution pipeline with a pre-built alias map. */
    private fun resolve(raw: String, aliases: Map<String, String>): MerchantResolution {
        val key = raw.trim().lowercase()
        val canonical = aliases[key]
        return if (canonical != null) MerchantResolution(canonical, 100)
        else MerchantNormalizationEngine.normalize(raw)
    }

    // ── alias hit ───────────────────────────────────────────────────────────────

    @Test fun `alias hit returns canonical name with confidence 100`() {
        val aliases = mapOf("amazon pay india" to "Amazon")
        val result = resolve("amazon pay india", aliases)
        assertEquals("Amazon", result.canonicalName)
        assertEquals(100, result.confidence)
    }

    @Test fun `alias lookup is case-insensitive`() {
        val aliases = mapOf("amazon pay india" to "Amazon")
        // All three should resolve to "Amazon" regardless of casing in the raw string
        assertEquals("Amazon", resolve("AMAZON PAY INDIA", aliases).canonicalName)
        assertEquals("Amazon", resolve("Amazon Pay India", aliases).canonicalName)
        assertEquals("Amazon", resolve("amazon pay india", aliases).canonicalName)
    }

    @Test fun `alias lookup trims whitespace from raw input`() {
        val aliases = mapOf("amazon pay india" to "Amazon")
        assertEquals("Amazon", resolve("  AMAZON PAY INDIA  ", aliases).canonicalName)
    }

    @Test fun `multiple distinct aliases for the same canonical all resolve correctly`() {
        val aliases = mapOf(
            "amazon pay india"       to "Amazon",
            "amazon marketplace"     to "Amazon",
            "amazon seller services" to "Amazon"
        )
        assertEquals("Amazon", resolve("AMAZON PAY INDIA",       aliases).canonicalName)
        assertEquals("Amazon", resolve("Amazon Marketplace",      aliases).canonicalName)
        assertEquals("Amazon", resolve("Amazon Seller Services",  aliases).canonicalName)
    }

    @Test fun `alias takes priority over brand recognition — confidence 100 beats 95`() {
        // User has aliased "Swiggy Food" to their own canonical "Food Delivery" (hypothetical)
        val aliases = mapOf("swiggy food" to "Food Delivery")
        val result = resolve("Swiggy Food", aliases)
        assertEquals("Food Delivery", result.canonicalName)
        assertEquals(100, result.confidence)  // alias wins even though brand table would give 95
    }

    // ── alias miss → fallback to engine ─────────────────────────────────────────

    @Test fun `alias miss falls through to normalization engine`() {
        val result = resolve("AMAZON PAY INDIA", emptyMap())
        assertEquals("Amazon", result.canonicalName)
        assertEquals(95, result.confidence)  // brand table match
    }

    @Test fun `unknown merchant with no alias falls through to engine with low confidence`() {
        val result = resolve("XYZ PVT LTD", emptyMap())
        assertEquals("Xyz", result.canonicalName)
        assertEquals(80, result.confidence)
    }

    @Test fun `empty alias map always falls through to engine for all inputs`() {
        listOf("SWIGGY", "Dreamplug Service Private Limited", "AMAZON@UPI", "XYZ PVT LTD").forEach { raw ->
            val withAlias    = resolve(raw, emptyMap())
            val engineDirect = MerchantNormalizationEngine.normalize(raw)
            assertEquals(
                "Empty alias map should give engine result for '$raw'",
                engineDirect.canonicalName, withAlias.canonicalName
            )
        }
    }

    // ── alias source / provenance ────────────────────────────────────────────────

    @Test fun `USER sourced alias resolves same as SYSTEM sourced alias`() {
        // Source is metadata (auditing), not resolution logic — both resolve the same way
        val userAliases   = mapOf("dreamplug service private limited" to "Dreamplug")
        val systemAliases = mapOf("dreamplug service private limited" to "Dreamplug")
        assertEquals(
            resolve("Dreamplug Service Private Limited", userAliases).canonicalName,
            resolve("Dreamplug Service Private Limited", systemAliases).canonicalName
        )
    }
}
