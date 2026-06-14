package com.starklabs.moneytracker

import com.starklabs.moneytracker.data.MerchantAlias
import com.starklabs.moneytracker.domain.MerchantNormalizationEngine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the no-loop / no-chain alias invariant and cascade rename logic.
 *
 * The invariant:
 *   At all times, no value that appears as [MerchantAlias.canonicalMerchant] also
 *   appears as an [MerchantAlias.aliasKey] pointing to a different canonical.
 *
 * This is maintained at write time by [MoneyRepository.renameMerchant], which:
 *   1. Cascades: existing X→A entries become X→B when A is renamed to B.
 *   2. Collapses: if B is itself an alias of Z, the result is X→Z (single hop).
 *   3. Rejects loops: A→B then B→A is rejected when B→A would resolve back to A.
 *
 * These tests exercise the pure algorithmic logic of those operations on in-memory
 * [MerchantAlias] lists, mirroring what the repository does via DAOs.
 */
class MerchantInvariantTest {

    @Before
    fun clearCache() = MerchantNormalizationEngine.clearCache()

    // ── helpers (mirror MoneyRepository logic on in-memory lists) ──────────────

    /** Returns true iff no canonicalMerchant value is also an aliasKey for a different canonical. */
    private fun invariantHolds(aliases: List<MerchantAlias>): Boolean {
        val aliasKeys = aliases.map { it.aliasKey }.toSet()
        return aliases.none { aliasKeys.contains(it.canonicalMerchant.trim().lowercase()) }
    }

    private fun makeAlias(alias: String, canonical: String): MerchantAlias = MerchantAlias(
        alias = alias,
        aliasKey = alias.trim().lowercase(),
        canonicalMerchant = canonical,
        createdAt = 0L
    )

    /**
     * Pure simulation of [MoneyRepository.renameMerchant].
     * Returns the updated list, or the original list unchanged if the rename was rejected
     * (self-rename or loop detected).
     */
    private fun applyRename(aliases: MutableList<MerchantAlias>, from: String, to: String): Boolean {
        val fromTrimmed = from.trim()
        val toTrimmed   = to.trim()
        if (fromTrimmed.equals(toTrimmed, ignoreCase = true)) return false

        // Collapse: if `to` is itself an alias, use its canonical
        val finalCanonical = aliases
            .find { it.aliasKey == toTrimmed.lowercase() }
            ?.canonicalMerchant
            ?: toTrimmed

        // Loop guard
        if (finalCanonical.equals(fromTrimmed, ignoreCase = true)) return false

        // Cascade existing aliases that pointed to `from`
        val fromKey = fromTrimmed.lowercase()
        for (i in aliases.indices) {
            if (aliases[i].canonicalMerchant.trim().lowercase() == fromKey) {
                aliases[i] = aliases[i].copy(canonicalMerchant = finalCanonical)
            }
        }

        // Add / update the from→finalCanonical entry
        val existingIdx = aliases.indexOfFirst { it.aliasKey == fromKey }
        if (existingIdx >= 0) {
            aliases[existingIdx] = aliases[existingIdx].copy(canonicalMerchant = finalCanonical)
        } else {
            aliases.add(makeAlias(fromTrimmed, finalCanonical))
        }

        return true
    }

    // ── self-reference ──────────────────────────────────────────────────────────

    @Test fun `self-rename Amazon to Amazon is rejected`() {
        val aliases = mutableListOf(makeAlias("Amazon Pay India", "Amazon"))
        val accepted = applyRename(aliases, from = "Amazon", to = "Amazon")
        assertFalse("Self-rename must be rejected", accepted)
        assertEquals(1, aliases.size) // list unchanged
    }

    @Test fun `case-insensitive self-rename is rejected`() {
        val aliases = mutableListOf<MerchantAlias>()
        val accepted = applyRename(aliases, from = "amazon", to = "AMAZON")
        assertFalse(accepted)
    }

    // ── loop prevention ─────────────────────────────────────────────────────────

    @Test fun `Amazon to Shopping then Shopping to Amazon is rejected`() {
        val aliases = mutableListOf<MerchantAlias>()
        val first = applyRename(aliases, from = "Amazon", to = "Shopping")
        assertTrue("First rename should succeed", first)

        // At this point: "amazon" → "Shopping" alias exists
        val second = applyRename(aliases, from = "Shopping", to = "Amazon")
        assertFalse("Reverse rename creating a loop must be rejected", second)

        // Invariant still holds
        assertTrue(invariantHolds(aliases))
    }

    @Test fun `three-step cycle A to B, B to C, C to A — C to A is rejected`() {
        val aliases = mutableListOf<MerchantAlias>()
        applyRename(aliases, from = "A", to = "B")
        applyRename(aliases, from = "B", to = "C")
        val cycleAccepted = applyRename(aliases, from = "C", to = "A")
        assertFalse("Cycle-creating rename must be rejected", cycleAccepted)
        assertTrue(invariantHolds(aliases))
    }

    // ── cascade rename ──────────────────────────────────────────────────────────

    @Test fun `rename cascade updates all aliases pointing to the old canonical`() {
        val aliases = mutableListOf(
            makeAlias("Amazon Pay India",       "Amazon"),
            makeAlias("Amazon Marketplace",     "Amazon"),
            makeAlias("Amazon Seller Services", "Amazon")
        )
        applyRename(aliases, from = "Amazon", to = "Amazon India")

        // All three original aliases must now point to "Amazon India"
        aliases.filter { it.aliasKey != "amazon" }.forEach { alias ->
            assertEquals(
                "Alias '${alias.alias}' should cascade to new canonical",
                "Amazon India", alias.canonicalMerchant
            )
        }
        assertTrue(invariantHolds(aliases))
    }

    @Test fun `invariant holds after cascade rename`() {
        val aliases = mutableListOf(
            makeAlias("Amazon Marketplace", "Amazon"),
            makeAlias("Amazon Pay India",   "Amazon")
        )
        applyRename(aliases, from = "Amazon", to = "Amazon Store")
        assertTrue(invariantHolds(aliases))
    }

    // ── chain collapse ──────────────────────────────────────────────────────────

    @Test fun `renaming A to B where B is already an alias of Z collapses to A to Z`() {
        val aliases = mutableListOf(
            makeAlias("B_raw", "Z") // B_raw → Z already exists
        )
        // Now rename "A" to "B_raw": should collapse to A → Z
        applyRename(aliases, from = "A", to = "B_raw")

        val aEntry = aliases.find { it.aliasKey == "a" }
        assertNotNull("Alias for 'A' should have been created", aEntry)
        assertEquals("Collapse should resolve to Z, not B_raw", "Z", aEntry!!.canonicalMerchant)
        assertTrue(invariantHolds(aliases))
    }

    // ── invariant property across multiple operations ───────────────────────────

    @Test fun `invariant holds after a sequence of valid renames`() {
        val aliases = mutableListOf<MerchantAlias>()
        applyRename(aliases, from = "Raw Merchant A", to = "Merchant A")
        applyRename(aliases, from = "Raw Merchant B", to = "Merchant B")
        applyRename(aliases, from = "Merchant A",     to = "Canonical Name")
        applyRename(aliases, from = "Merchant B",     to = "Canonical Name")
        assertTrue("Invariant violated after valid rename sequence", invariantHolds(aliases))
    }

    @Test fun `invariant holds on an empty alias list`() {
        assertTrue(invariantHolds(emptyList()))
    }

    @Test fun `invariant holds after first rename on empty list`() {
        val aliases = mutableListOf<MerchantAlias>()
        applyRename(aliases, from = "Old Name", to = "New Name")
        assertTrue(invariantHolds(aliases))
    }
}
