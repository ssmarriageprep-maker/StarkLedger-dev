package com.starklabs.moneytracker

import com.starklabs.moneytracker.domain.MerchantNormalizationEngine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [MerchantNormalizationEngine].
 *
 * Covers: brand-family examples from the spec, SMS artifact cleanup, entity-suffix stripping,
 * confidence scoring per the approved scale (95/80/60/40), idempotency, determinism,
 * and boundary inputs.
 */
class MerchantNormalizationEngineTest {

    @Before
    fun clearCache() = MerchantNormalizationEngine.clearCache()

    // -------------------------------------------------------------------------
    // Brand-family examples (spec Part 1)
    // -------------------------------------------------------------------------

    @Test fun `Amazon variants all canonicalize to Amazon`() {
        assertEquals("Amazon", MerchantNormalizationEngine.normalize("Amazon Marketplace").canonicalName)
        assertEquals("Amazon", MerchantNormalizationEngine.normalize("Amazon Seller Services").canonicalName)
        assertEquals("Amazon", MerchantNormalizationEngine.normalize("AMAZON PAY INDIA").canonicalName)
        assertEquals("Amazon", MerchantNormalizationEngine.normalize("amazon").canonicalName)
    }

    @Test fun `Swiggy variants all canonicalize to Swiggy`() {
        assertEquals("Swiggy", MerchantNormalizationEngine.normalize("Swiggy Instamart").canonicalName)
        assertEquals("Swiggy", MerchantNormalizationEngine.normalize("Swiggy Food").canonicalName)
        assertEquals("Swiggy", MerchantNormalizationEngine.normalize("SWIGGY").canonicalName)
    }

    @Test fun `Dreamplug variants all canonicalize to Dreamplug`() {
        assertEquals("Dreamplug", MerchantNormalizationEngine.normalize("Dreamplug Service Private Limited").canonicalName)
        assertEquals("Dreamplug", MerchantNormalizationEngine.normalize("DREAMPLUG SERVICES PVT LTD").canonicalName)
        assertEquals("Dreamplug", MerchantNormalizationEngine.normalize("Dreamplug").canonicalName)
    }

    @Test fun `other brand table entries resolve correctly`() {
        assertEquals("Zomato",   MerchantNormalizationEngine.normalize("ZOMATO ORDER").canonicalName)
        assertEquals("Uber",     MerchantNormalizationEngine.normalize("Uber Eats").canonicalName)
        assertEquals("Flipkart", MerchantNormalizationEngine.normalize("FLIPKART INTERNET PVT LTD").canonicalName)
        assertEquals("Jio",      MerchantNormalizationEngine.normalize("JIOMART GROCERIES").canonicalName)
        assertEquals("Jio",      MerchantNormalizationEngine.normalize("JioCinema").canonicalName)
        assertEquals("Airtel",   MerchantNormalizationEngine.normalize("Airtel Payments Bank").canonicalName)
        assertEquals("CRED",     MerchantNormalizationEngine.normalize("CRED").canonicalName)
        assertEquals("Paytm",    MerchantNormalizationEngine.normalize("PAYTM PAYMENTS").canonicalName)
        assertEquals("PhonePe",  MerchantNormalizationEngine.normalize("PHONEPE PRIVATE LIMITED").canonicalName)
        assertEquals("Myntra",   MerchantNormalizationEngine.normalize("MYNTRA DESIGNS PVT LTD").canonicalName)
    }

    // -------------------------------------------------------------------------
    // SMS artifact cleanup
    // -------------------------------------------------------------------------

    @Test fun `trailing underscore is removed`() {
        val result = MerchantNormalizationEngine.normalize("MR DIY_")
        assertEquals("MR DIY", result.canonicalName)
    }

    @Test fun `UPI at-handle suffix is removed`() {
        // AMAZON@UPI → artifact cleanup → "AMAZON" → brand match → Amazon
        assertEquals("Amazon", MerchantNormalizationEngine.normalize("AMAZON@UPI").canonicalName)
    }

    @Test fun `generic at-handle is stripped leaving clean name`() {
        val result = MerchantNormalizationEngine.normalize("HDFC@YESBANK")
        assertFalse(result.canonicalName.contains("@"))
    }

    @Test fun `special characters are removed and spaces squeezed`() {
        val result = MerchantNormalizationEngine.normalize("SOME--MERCHANT//NAME")
        assertFalse(result.canonicalName.contains("-"))
        assertFalse(result.canonicalName.contains("/"))
        assertFalse(result.canonicalName.contains("  ")) // no double spaces
    }

    // -------------------------------------------------------------------------
    // Entity-suffix stripping
    // -------------------------------------------------------------------------

    @Test fun `Pvt Ltd stripped from unknown merchant`() {
        val result = MerchantNormalizationEngine.normalize("XYZ PVT LTD")
        assertEquals("Xyz", result.canonicalName)
        assertEquals(80, result.confidence)
    }

    @Test fun `Private Limited stripped`() {
        val result = MerchantNormalizationEngine.normalize("OMEGA PRIVATE LIMITED")
        assertEquals("Omega", result.canonicalName)
        assertEquals(80, result.confidence)
    }

    @Test fun `LLP stripped`() {
        val result = MerchantNormalizationEngine.normalize("DELTA LLP")
        assertEquals("Delta", result.canonicalName)
        assertEquals(80, result.confidence)
    }

    @Test fun `India and LLP stripped together leaving Technologies intact`() {
        val result = MerchantNormalizationEngine.normalize("ABC TECHNOLOGIES INDIA LLP")
        assertEquals("Abc Technologies", result.canonicalName)
        assertEquals(80, result.confidence)
    }

    @Test fun `Services suffix stripped`() {
        val result = MerchantNormalizationEngine.normalize("SIGMA SERVICES PVT LTD")
        assertEquals("Sigma", result.canonicalName)
        assertEquals(80, result.confidence)
    }

    @Test fun `suffix stripping does not consume entire name — falls back to artifact-cleaned form`() {
        // "India Pvt Ltd" → all words are suffixes → fallback, not blank
        val result = MerchantNormalizationEngine.normalize("INDIA PVT LTD")
        assertNotEquals("", result.canonicalName)
        assertNotEquals("Unknown", result.canonicalName)
    }

    // -------------------------------------------------------------------------
    // Confidence scoring (Adjustment 3 scale)
    // -------------------------------------------------------------------------

    @Test fun `brand match returns confidence 95`() {
        assertEquals(95, MerchantNormalizationEngine.normalize("Amazon Marketplace").confidence)
        assertEquals(95, MerchantNormalizationEngine.normalize("SWIGGY FOOD").confidence)
        assertEquals(95, MerchantNormalizationEngine.normalize("AMAZON@UPI").confidence)
    }

    @Test fun `suffix stripping returns confidence 80`() {
        assertEquals(80, MerchantNormalizationEngine.normalize("XYZ PVT LTD").confidence)
        assertEquals(80, MerchantNormalizationEngine.normalize("ABC TECHNOLOGIES INDIA LLP").confidence)
    }

    @Test fun `formatting-only change returns confidence 60`() {
        // All-caps unknown merchant: no brand, no suffix, but casing was changed → 60
        val result = MerchantNormalizationEngine.normalize("UNKNOWN MERCHANT NAME")
        assertEquals(60, result.confidence)
    }

    @Test fun `unchanged raw returns confidence 40`() {
        // Already-title-cased unknown merchant: normalize produces the same visible string → 40
        val result = MerchantNormalizationEngine.normalize("Widget")
        assertEquals(40, result.confidence)
    }

    // -------------------------------------------------------------------------
    // Idempotency
    // -------------------------------------------------------------------------

    @Test fun `idempotency — normalize of canonical name equals canonical name`() {
        val corpus = listOf(
            "Amazon Marketplace", "AMAZON PAY INDIA", "Swiggy Instamart",
            "Dreamplug Service Private Limited", "DREAMPLUG SERVICES PVT LTD",
            "MR DIY_", "AMAZON@UPI", "ABC TECHNOLOGIES INDIA LLP",
            "XYZ PVT LTD", "SIGMA SERVICES PRIVATE LIMITED",
            "UNKNOWN MERCHANT NAME", "widget", "DELTA LLP"
        )
        for (raw in corpus) {
            val first = MerchantNormalizationEngine.normalize(raw).canonicalName
            val second = MerchantNormalizationEngine.normalize(first).canonicalName
            assertEquals("Idempotency failed for '$raw': first='$first', second='$second'", first, second)
        }
    }

    // -------------------------------------------------------------------------
    // Determinism
    // -------------------------------------------------------------------------

    @Test fun `determinism — same input always produces same output across repeated calls`() {
        val inputs = listOf("AMAZON PAY INDIA", "Dreamplug Service Private Limited", "XYZ PVT LTD")
        for (input in inputs) {
            val results = (1..10).map { MerchantNormalizationEngine.normalize(input) }
            assertTrue(
                "Non-deterministic result for '$input'",
                results.all { it == results[0] }
            )
        }
    }

    // -------------------------------------------------------------------------
    // Boundary inputs
    // -------------------------------------------------------------------------

    @Test fun `empty string does not crash and returns empty canonical`() {
        // Returns "" for blank input for backward compatibility with CategoryNormalizer callers
        val result = MerchantNormalizationEngine.normalize("")
        assertNotNull(result)
        assertEquals("", result.canonicalName)
    }

    @Test fun `whitespace-only string does not crash`() {
        val result = MerchantNormalizationEngine.normalize("   ")
        assertNotNull(result)
    }

    @Test fun `single character input handled`() {
        val result = MerchantNormalizationEngine.normalize("X")
        assertNotNull(result)
        assertTrue(result.canonicalName.isNotEmpty())
    }

    @Test fun `numeric-only input handled`() {
        val result = MerchantNormalizationEngine.normalize("12345")
        assertNotNull(result)
    }

    @Test fun `already-canonical brand name is idempotent`() {
        // Normalising "Amazon" should yield "Amazon" (95) — brand table catches its own canonical
        val result = MerchantNormalizationEngine.normalize("Amazon")
        assertEquals("Amazon", result.canonicalName)
        assertEquals(95, result.confidence)
    }
}
