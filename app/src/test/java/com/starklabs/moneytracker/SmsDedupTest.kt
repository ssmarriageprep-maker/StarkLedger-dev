package com.starklabs.moneytracker

import com.starklabs.moneytracker.domain.SmsParser
import org.junit.Test
import org.junit.Assert.*

/**
 * Regression tests for the SMS deduplication hash.
 *
 * The hash is persisted on each transaction (Transaction.smsHash) and backed by a
 * unique DB index, so it must be stable across re-scans of the inbox and must only
 * collide for messages that are genuinely the same transaction.
 */
class SmsDedupTest {

    private val body =
        "Rs.318.00 sent to Dreamplug Service Private Limited from HDFC Bank A/C *3263 on 25-10-2025. Ref: 566413406309"

    @Test
    fun `identical SMS produces identical hash across re-scans`() {
        val first = SmsParser.parseSms("HDFCBK", body, 1_000L)
        val second = SmsParser.parseSms("HDFCBK", body, 2_000L) // different timestamp, same body
        assertNotNull(first.messageHash)
        assertEquals(first.messageHash, second.messageHash)
    }

    @Test
    fun `whitespace and line-break differences normalize to the same hash`() {
        val noisy = "Rs.318.00 sent to Dreamplug Service Private Limited\nfrom HDFC Bank A/C *3263   on 25-10-2025. Ref: 566413406309"
        val clean = SmsParser.parseSms("HDFCBK", body, 1_000L)
        val withNoise = SmsParser.parseSms("HDFCBK", noisy, 1_000L)
        assertEquals(clean.messageHash, withNoise.messageHash)
    }

    @Test
    fun `tabs, CRLF, lone CR and surrounding spaces all normalize to the same hash`() {
        val canonical = "Sent Rs.100 to Shop from HDFC Bank A/C *3263"
        val variants = listOf(
            "  Sent Rs.100 to Shop from HDFC Bank A/C *3263 ", // leading/trailing spaces
            "Sent  Rs.100  to Shop from HDFC Bank A/C *3263",  // duplicate spaces
            "Sent\tRs.100 to Shop from HDFC Bank A/C *3263",   // tab
            "Sent\r\nRs.100 to Shop from HDFC Bank A/C *3263", // CRLF
            "Sent\rRs.100 to Shop from HDFC Bank A/C *3263"    // lone CR
        )
        val expected = SmsParser.parseSms("HDFCBK", canonical, 1_000L).messageHash
        assertNotNull(expected)
        variants.forEach { v ->
            assertEquals("variant should dedup: <$v>", expected, SmsParser.parseSms("HDFCBK", v, 2_000L).messageHash)
        }
    }

    @Test
    fun `different transactions produce different hashes`() {
        val other =
            "Rs.500.00 sent to Some Other Merchant from HDFC Bank A/C *3263 on 25-10-2025. Ref: 999999999999"
        val a = SmsParser.parseSms("HDFCBK", body, 1_000L)
        val b = SmsParser.parseSms("HDFCBK", other, 1_000L)
        assertNotEquals(a.messageHash, b.messageHash)
    }
}
