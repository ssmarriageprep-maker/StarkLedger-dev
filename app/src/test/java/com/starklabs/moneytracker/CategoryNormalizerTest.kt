package com.starklabs.moneytracker

import com.starklabs.moneytracker.domain.CategoryNormalizer
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for merchant display-normalization.
 *
 * Documents current behavior (display-only, hardcoded brand list). When P1 promotes
 * this into a first-class engine, these become the regression baseline.
 */
class CategoryNormalizerTest {

    @Test
    fun `strips entity suffixes when no brand matches`() {
        assertEquals("Acme Traders", CategoryNormalizer.normalizeMerchant("Acme Traders Pvt Ltd"))
    }

    @Test
    fun `known brand wins over the rest of the string`() {
        assertEquals("Dreamplug", CategoryNormalizer.normalizeMerchant("Dreamplug Service Private Limited"))
        assertEquals("Amazon", CategoryNormalizer.normalizeMerchant("AMAZON PAY"))
        assertEquals("Swiggy", CategoryNormalizer.normalizeMerchant("swiggy instamart"))
    }

    @Test
    fun `brand match is case-insensitive`() {
        assertEquals("Uber", CategoryNormalizer.normalizeMerchant("UBER INDIA SYSTEMS"))
    }

    @Test
    fun `special characters are replaced with spaces and collapsed`() {
        assertEquals("MR DIY", CategoryNormalizer.normalizeMerchant("MR DIY_"))
        // "@" is removed and the result is title-cased by MerchantNormalizationEngine
        assertEquals("Starbucks Mall", CategoryNormalizer.normalizeMerchant("STARBUCKS @ MALL"))
    }

    @Test
    fun `plain merchant without brand or suffix is unchanged`() {
        assertEquals("Local Kirana Store", CategoryNormalizer.normalizeMerchant("Local Kirana Store"))
    }

    @Test
    fun `suffix word inside another word is not stripped (word boundary)`() {
        // "limited" inside "Unlimited" must not be removed.
        assertEquals("Unlimited Cloud", CategoryNormalizer.normalizeMerchant("Unlimited Cloud"))
    }

    @Test
    fun `empty input returns empty string`() {
        // MerchantNormalizationEngine preserves backward-compat by returning "" for blank input
        assertEquals("", CategoryNormalizer.normalizeMerchant(""))
    }
}
