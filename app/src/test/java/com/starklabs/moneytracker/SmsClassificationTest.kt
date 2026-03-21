package com.starklabs.moneytracker

import com.starklabs.moneytracker.domain.SmsParser
import com.starklabs.moneytracker.domain.SmsPattern
import org.junit.Test
import org.junit.Assert.*

/**
 * Phase 1 classification tests — smart rejection, confidence scoring,
 * pattern detection, edge cases, false positives, and ambiguous messages.
 */
class SmsClassificationTest {

    // ════════════════════════════════════════════════════════════════════════
    //  SMART REJECTION TESTS
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `smart reject - recharge without transaction signals`() {
        val result = SmsParser.parseSms(
            "AIRTEL",
            "Recharge with Rs.318 and get 2GB/day + OTT benefits. Valid for 28 days.",
            0L
        )
        assertFalse(result.isTransaction)
        assertEquals("non-transactional", result.category)
    }

    @Test
    fun `smart pass - bill keyword WITH transaction signals`() {
        val result = SmsParser.parseSms(
            "HDFCBK",
            "Rs 943.00 paid for BSNL bill from HDFC A/C *1234 via UPI",
            0L
        )
        assertTrue("Bill + transaction signals should PASS", result.isTransaction)
        assertEquals("transactional", result.category)
        assertTrue(result.confidence >= 80)
    }

    @Test
    fun `smart pass - recharge keyword but is a real debit`() {
        val result = SmsParser.parseSms(
            "HDFCBK",
            "Rs 299.00 debited from HDFC A/C *5678 for recharge via UPI. Ref: 123456",
            0L
        )
        assertTrue("Recharge keyword but real debit should PASS", result.isTransaction)
        assertEquals(299.00, result.amount!!, 0.01)
    }

    @Test
    fun `reject OTP even with Rs mentioned`() {
        val result = SmsParser.parseSms(
            "HDFCBK",
            "Your OTP for transaction of Rs 500 is 123456. Valid for 10 minutes.",
            0L
        )
        assertFalse("OTP should be rejected", result.isTransaction)
    }

    @Test
    fun `reject pure promo even with amount`() {
        val result = SmsParser.parseSms(
            "PAYTM",
            "Get 20% cashback offer on all UPI payments above Rs 500. Valid till 31st Dec.",
            0L
        )
        assertFalse(result.isTransaction)
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CONFIDENCE SCORING TESTS
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `high confidence - full transaction SMS`() {
        val result = SmsParser.parseSms(
            "HDFCBK",
            "Rs.318.00 sent to Dreamplug Service Private Limited from HDFC Bank A/C *3263 on 25-10-2025. Ref: 566413406309",
            0L
        )
        assertTrue(result.isTransaction)
        assertTrue("Full SMS should have high confidence", result.confidence >= 80)
    }

    @Test
    fun `low confidence - balance only message`() {
        val result = SmsParser.parseSms(
            "BANK",
            "Your balance is Rs 5000. Thank you for banking with us.",
            0L
        )
        assertFalse(result.isTransaction)
        assertTrue("Balance-only should have low confidence", result.confidence < 80)
    }

    @Test
    fun `medium confidence - minimal signals`() {
        val result = SmsParser.parseSms(
            "GPAY",
            "You paid ₹350 to Swiggy",
            0L
        )
        // Should pass — has amount + action + merchant
        assertTrue(result.isTransaction)
        assertTrue(result.confidence >= 80)
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PATTERN DETECTION TESTS
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `detect UPI_SENT pattern`() {
        val result = SmsParser.parseSms(
            "HDFCBK",
            "Rs 500.00 paid to ZOMATO via UPI from HDFC A/C *1234. Ref: 123456",
            0L
        )
        assertTrue(result.isTransaction)
        assertTrue(result.patternDetected)
        assertEquals(SmsPattern.UPI_SENT, result.patternUsed)
    }

    @Test
    fun `detect UPI_RECEIVED pattern`() {
        val result = SmsParser.parseSms(
            "SBIUPI",
            "Rs 1000.00 credited to your SBI account *5678 via UPI. Ref: UPI987654",
            0L
        )
        assertTrue(result.isTransaction)
        assertEquals(SmsPattern.UPI_RECEIVED, result.patternUsed)
    }

    @Test
    fun `detect NEFT_TRANSFER pattern`() {
        val result = SmsParser.parseSms(
            "KOTAK",
            "Rs 10,000 transferred to RAMESH KUMAR via NEFT from KOTAK A/C *5678. UTR: KOTAK123456",
            0L
        )
        assertTrue(result.isTransaction)
        assertEquals(SmsPattern.NEFT_TRANSFER, result.patternUsed)
    }

    @Test
    fun `detect ATM_WITHDRAWAL pattern`() {
        val result = SmsParser.parseSms(
            "SBIUPI",
            "Alert: ₹5,000 debited from A/C ****2341 at ATM on 15-Jan-25. Available balance: ₹45,230.",
            0L
        )
        assertTrue(result.isTransaction)
        assertEquals(SmsPattern.ATM_WITHDRAWAL, result.patternUsed)
    }

    @Test
    fun `detect BANK_DEBIT pattern`() {
        val result = SmsParser.parseSms(
            "HDFCBK",
            "HDFCBANK: ₹2,000 debited from A/C ****4321.",
            0L
        )
        assertTrue(result.isTransaction)
        assertEquals(SmsPattern.BANK_DEBIT, result.patternUsed)
    }

    // ════════════════════════════════════════════════════════════════════════
    //  FALSE POSITIVE TESTS (must reject)
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `reject Airtel promo`() {
        val result = SmsParser.parseSms(
            "AIRTEL",
            "Get Rs 100 cashback on recharge of Rs 399. Offer valid for Airtel prepaid users only.",
            0L
        )
        assertFalse(result.isTransaction)
    }

    @Test
    fun `reject BSNL validity`() {
        val result = SmsParser.parseSms(
            "BSNL",
            "Dear Customer, your BSNL plan of Rs 447 with 60 days validity has been activated.",
            0L
        )
        assertFalse(result.isTransaction)
    }

    @Test
    fun `reject Jio data offer`() {
        val result = SmsParser.parseSms(
            "JIOTEL",
            "Jio: Get 2GB/day data + unlimited calls at Rs 239. Recharge now!",
            0L
        )
        assertFalse(result.isTransaction)
    }

    @Test
    fun `reject EMI reminder`() {
        val result = SmsParser.parseSms(
            "HDFCBK",
            "Your EMI of Rs 5000 for loan A/C *1234 is due on 15-Jan. Please ensure sufficient balance.",
            0L
        )
        assertFalse(result.isTransaction)
    }

    // ════════════════════════════════════════════════════════════════════════
    //  BROKEN FORMAT & EDGE CASE TESTS
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `handle lowercase message`() {
        val result = SmsParser.parseSms(
            "hdfcbk",
            "rs 500.00 paid to zomato via upi from hdfc a/c *1234",
            0L
        )
        assertTrue("Lowercase should still parse", result.isTransaction)
        assertEquals(500.00, result.amount!!, 0.01)
    }

    @Test
    fun `handle multi-line broken SMS`() {
        val body = """
            Sent Rs.2000.00
            From HDFC Bank A/C *3263
            To BHAGINI HOSPITALITIES PVT LTD SUITES
            On 31/12/25
            Ref 573106200893
        """.trimIndent()
        val result = SmsParser.parseSms("HDFCBK", body, 0L)
        assertTrue(result.isTransaction)
        assertEquals(2000.00, result.amount!!, 0.01)
        assertEquals("BHAGINI HOSPITALITIES PVT LTD SUITES", result.merchant)
    }

    @Test
    fun `handle no To keyword - at pattern`() {
        val result = SmsParser.parseSms(
            "AXISBK",
            "Rs 943.00 spent at STARBUCKS COFFEE using AXIS Bank card *8899 on 15/12/2025",
            0L
        )
        assertTrue(result.isTransaction)
        assertEquals("STARBUCKS COFFEE", result.merchant)
    }

    @Test
    fun `handle amount without space after currency`() {
        val result = SmsParser.parseSms(
            "HDFCBK",
            "Rs318 debited from HDFC A/C *1234 via UPI",
            0L
        )
        assertTrue("Should parse Rs318 without space", result.isTransaction)
        assertEquals(318.0, result.amount!!, 0.01)
    }

    @Test
    fun `transaction with INR no space`() {
        val body = "Txn of INR 200.00 done on 12-12-25. Avl Bal: INR 5000.00"
        val result = SmsParser.parseSms("BANK", body, 0L)
        assertTrue(result.isTransaction)
        assertEquals(200.00, result.amount!!, 0.01)
    }

    // ════════════════════════════════════════════════════════════════════════
    //  AMBIGUOUS CASE TESTS
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `ambiguous - Recharge done but is a real transaction`() {
        val result = SmsParser.parseSms(
            "HDFCBK",
            "Rs 399.00 debited from HDFC A/C *5678 for mobile recharge. UPI Ref: 789456",
            0L
        )
        // Should pass — real debit with strong signals
        assertTrue("Real debit for recharge should pass", result.isTransaction)
        assertEquals(399.00, result.amount!!, 0.01)
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DUPLICATE DETECTION
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `same message produces same hash`() {
        val body = "Rs 500.00 paid to ZOMATO from HDFC A/C *1234 via UPI"
        val r1 = SmsParser.parseSms("HDFCBK", body, 0L)
        val r2 = SmsParser.parseSms("HDFCBK", body, 0L)
        assertNotNull(r1.messageHash)
        assertEquals(r1.messageHash, r2.messageHash)
    }

    @Test
    fun `different messages produce different hashes`() {
        val r1 = SmsParser.parseSms("HDFCBK", "Rs 500 paid to ZOMATO from HDFC A/C *1234 via UPI", 0L)
        val r2 = SmsParser.parseSms("HDFCBK", "Rs 300 paid to SWIGGY from HDFC A/C *1234 via UPI", 0L)
        assertNotEquals(r1.messageHash, r2.messageHash)
    }

    // ════════════════════════════════════════════════════════════════════════
    //  METADATA FIELDS
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `valid transaction has all metadata fields`() {
        val result = SmsParser.parseSms(
            "HDFCBK",
            "Rs 500.00 paid to ZOMATO via UPI from HDFC A/C *1234. Ref: 123456",
            0L
        )
        assertTrue(result.isTransaction)
        assertNotNull(result.category)
        assertNotNull(result.messageHash)
        assertTrue(result.confidence > 0)
        assertTrue(result.patternDetected)
        assertNotEquals(SmsPattern.UNKNOWN, result.patternUsed)
    }

    @Test
    fun `rejected message still has metadata`() {
        val result = SmsParser.parseSms(
            "AIRTEL",
            "Recharge with Rs 399 for 84 days validity.",
            0L
        )
        assertFalse(result.isTransaction)
        assertNotNull(result.category)
        assertNotNull(result.messageHash)
        assertNotNull(result.reason)
    }
}
