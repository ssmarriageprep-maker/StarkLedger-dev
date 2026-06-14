package com.starklabs.moneytracker

import com.starklabs.moneytracker.domain.MerchantNormalizationEngine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [MerchantNormalizationEngine]'s in-memory resolution cache.
 *
 * The cache is an implementation detail (transparent to callers), so these tests
 * verify correctness properties — same result before and after [MerchantNormalizationEngine.clearCache]
 * — rather than internal state.
 */
class MerchantCacheTest {

    @Before
    fun setUp() = MerchantNormalizationEngine.clearCache()

    @Test fun `repeated calls return identical results (cache hit does not corrupt)`() {
        val first  = MerchantNormalizationEngine.normalize("AMAZON PAY INDIA")
        val second = MerchantNormalizationEngine.normalize("AMAZON PAY INDIA")
        assertEquals(first, second)
    }

    @Test fun `result after clearCache matches result before clearCache`() {
        val before = MerchantNormalizationEngine.normalize("Dreamplug Service Private Limited")
        MerchantNormalizationEngine.clearCache()
        val after  = MerchantNormalizationEngine.normalize("Dreamplug Service Private Limited")
        assertEquals(before, after)
    }

    @Test fun `clearCache does not affect correctness for brand matches`() {
        MerchantNormalizationEngine.normalize("Swiggy Food")   // populate cache
        MerchantNormalizationEngine.clearCache()
        val result = MerchantNormalizationEngine.normalize("Swiggy Food")
        assertEquals("Swiggy", result.canonicalName)
        assertEquals(95, result.confidence)
    }

    @Test fun `clearCache does not affect correctness for suffix-stripped results`() {
        MerchantNormalizationEngine.normalize("XYZ PVT LTD")
        MerchantNormalizationEngine.clearCache()
        val result = MerchantNormalizationEngine.normalize("XYZ PVT LTD")
        assertEquals("Xyz", result.canonicalName)
        assertEquals(80, result.confidence)
    }

    @Test fun `multiple distinct merchants are cached independently`() {
        val a = MerchantNormalizationEngine.normalize("AMAZON PAY INDIA")
        val b = MerchantNormalizationEngine.normalize("Swiggy Food")
        assertNotEquals(a.canonicalName, b.canonicalName)

        // Second call — from cache
        assertEquals(a, MerchantNormalizationEngine.normalize("AMAZON PAY INDIA"))
        assertEquals(b, MerchantNormalizationEngine.normalize("Swiggy Food"))
    }

    @Test fun `clearCache followed by multiple different inputs all return correct results`() {
        MerchantNormalizationEngine.clearCache()
        assertEquals("Amazon",    MerchantNormalizationEngine.normalize("AMAZON PAY INDIA").canonicalName)
        assertEquals("Swiggy",    MerchantNormalizationEngine.normalize("SWIGGY").canonicalName)
        assertEquals("Dreamplug", MerchantNormalizationEngine.normalize("DREAMPLUG SERVICES PVT LTD").canonicalName)
        assertEquals("Xyz",       MerchantNormalizationEngine.normalize("XYZ PVT LTD").canonicalName)
    }
}
