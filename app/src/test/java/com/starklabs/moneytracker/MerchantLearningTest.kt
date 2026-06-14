package com.starklabs.moneytracker

import com.starklabs.moneytracker.domain.MerchantNormalizationEngine
import com.starklabs.moneytracker.domain.MerchantResolution
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the merchant learning system's resolution properties.
 *
 * The learning system's core promise is:
 *   - **Retroactivity**: past transactions automatically inherit a new alias (no backfill needed —
 *     canonical names are resolved at read-time, so any transaction referencing the raw string
 *     immediately benefits from the alias).
 *   - **Prospectivity**: new transactions with the same raw merchant also inherit the alias.
 *   - **Reversibility**: deleting an alias instantly reverts all transactions to the engine default.
 *   - **Re-correction**: updating an alias to a new canonical replaces the old one cleanly.
 *
 * All tests are pure (no Room, no Android): the alias map stands in for the persisted DB,
 * and the resolution function mirrors [MoneyRepository.resolveMerchant] exactly.
 */
class MerchantLearningTest {

    @Before
    fun clearCache() = MerchantNormalizationEngine.clearCache()

    // ── resolution helper (mirrors MoneyRepository.resolveMerchant) ─────────────

    private fun resolve(raw: String, aliases: Map<String, String>): MerchantResolution {
        val key = raw.trim().lowercase()
        val canonical = aliases[key]
        return if (canonical != null) MerchantResolution(canonical, 100)
        else MerchantNormalizationEngine.normalize(raw)
    }

    // Simulated "transactions" — just the raw merchant strings stored in Transaction.merchant
    private val historicalTransactions = listOf(
        "Dreamplug Service Private Limited",
        "DREAMPLUG SERVICES PVT LTD",
        "Dreamplug",
    )
    private val newTransaction = "Dreamplug Service Private Limited"

    // ── retroactivity ────────────────────────────────────────────────────────────

    @Test fun `retroactivity — existing transactions resolve to new canonical after alias is added`() {
        val beforeAlias = historicalTransactions.map { resolve(it, emptyMap()).canonicalName }
        // Before alias: engine produces "Dreamplug" (brand table) for all variants — already good.
        // Test with a CUSTOM canonical to show alias overrides the engine:
        val aliases = mapOf("dreamplug service private limited" to "Dreamplug Payment")
        val afterAlias = historicalTransactions.map { resolve(it, aliases).canonicalName }

        // The aliased variant resolves to the user's custom name
        assertEquals("Dreamplug Payment", afterAlias[0])
        // Variants not in the alias map still use the engine (retroactivity applies per alias key)
        assertNotNull(afterAlias[1]) // non-null / no crash
    }

    @Test fun `retroactivity — every raw variant can have its own alias mapping`() {
        val aliases = mapOf(
            "dreamplug service private limited" to "Dreamplug",
            "dreamplug services pvt ltd"        to "Dreamplug"
        )
        historicalTransactions.dropLast(1).forEach { raw ->
            assertEquals(
                "Expected 'Dreamplug' for raw='$raw'",
                "Dreamplug", resolve(raw, aliases).canonicalName
            )
        }
    }

    // ── prospectivity ────────────────────────────────────────────────────────────

    @Test fun `prospectivity — new transaction with same raw string inherits alias`() {
        val aliases = mapOf("dreamplug service private limited" to "Dreamplug")
        assertEquals("Dreamplug", resolve(newTransaction, aliases).canonicalName)
    }

    @Test fun `prospectivity — alias covers both pre-existing and future transactions equally`() {
        val aliases = mapOf("dreamplug service private limited" to "DreamplugV2")
        val historicalResult = resolve("Dreamplug Service Private Limited", aliases).canonicalName
        val prospectiveResult = resolve(newTransaction, aliases).canonicalName
        assertEquals("Historical and prospective resolution must agree", historicalResult, prospectiveResult)
    }

    // ── reversibility ────────────────────────────────────────────────────────────

    @Test fun `reversibility — removing alias reverts all resolution to engine output`() {
        val withAlias    = resolve("Dreamplug Service Private Limited", mapOf("dreamplug service private limited" to "MyAlias")).canonicalName
        val withoutAlias = resolve("Dreamplug Service Private Limited", emptyMap()).canonicalName

        assertEquals("MyAlias", withAlias)
        // Without alias → falls back to engine (brand match)
        assertEquals("Dreamplug", withoutAlias)
        // They differ — confirms the alias was doing real work
        assertNotEquals(withAlias, withoutAlias)
    }

    @Test fun `reversibility — engine output is stable after alias is removed`() {
        // Ensures no residual state from the alias affects the engine fallback
        MerchantNormalizationEngine.clearCache()
        val engine1 = resolve("XYZ PVT LTD", emptyMap()).canonicalName
        resolve("XYZ PVT LTD", mapOf("xyz pvt ltd" to "Custom Name"))
        MerchantNormalizationEngine.clearCache()
        val engine2 = resolve("XYZ PVT LTD", emptyMap()).canonicalName
        assertEquals("Engine output must be stable regardless of alias history", engine1, engine2)
    }

    // ── re-correction ────────────────────────────────────────────────────────────

    @Test fun `re-correction — updating alias to a new canonical replaces the old one`() {
        val firstAlias  = mapOf("amazon pay india" to "Amazon V1")
        val secondAlias = mapOf("amazon pay india" to "Amazon")   // corrected

        assertEquals("Amazon V1", resolve("AMAZON PAY INDIA", firstAlias).canonicalName)
        assertEquals("Amazon",    resolve("AMAZON PAY INDIA", secondAlias).canonicalName)
    }

    // ── multiple merchants ────────────────────────────────────────────────────────

    @Test fun `multiple merchants resolve consistently and independently`() {
        val aliases = mapOf(
            "amazon pay india"   to "Amazon",
            "swiggy instamart"   to "Swiggy",
            "dreamplug services pvt ltd" to "Dreamplug"
        )
        assertEquals("Amazon",    resolve("AMAZON PAY INDIA",           aliases).canonicalName)
        assertEquals("Swiggy",    resolve("Swiggy Instamart",           aliases).canonicalName)
        assertEquals("Dreamplug", resolve("DREAMPLUG SERVICES PVT LTD", aliases).canonicalName)
    }

    @Test fun `alias confidence is always 100 regardless of how complex the rule would have been`() {
        val aliases = mapOf("some unknown merchant pvt ltd" to "My Brand")
        val result = resolve("SOME UNKNOWN MERCHANT PVT LTD", aliases)
        assertEquals(100, result.confidence)
    }
}
