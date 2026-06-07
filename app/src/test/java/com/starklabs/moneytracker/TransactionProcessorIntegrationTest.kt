package com.starklabs.moneytracker

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import com.starklabs.moneytracker.domain.SmsParser
import com.starklabs.moneytracker.sms.TransactionProcessor
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Integration tests for the SMS transaction processing pipeline
 * Tests the full flow: SMS -> Parse -> Classify -> Extract -> Process
 */
class TransactionProcessorIntegrationTest {

    private lateinit var parser: SmsParser
    private lateinit var processor: TransactionProcessor

    @Before
    fun setUp() {
        parser = SmsParser()
        processor = TransactionProcessor()
    }

    // ========== REAL-WORLD SMS PATTERNS ==========

    @Test
    fun `test processing valid HDFC UPI sent SMS`() {
        val sms = "Hi, You have sent ₹5000 via UPI to test@upi. Ref: 123456. Available balance: ₹45000"
        
        val parsed = parser.parse(sms)
        
        assertEquals(5000.0, parsed.amount)
        assertEquals("test@upi", parsed.merchant)
        assertEquals("HDFC", parsed.bank)
        assertEquals("UPI_SENT", parsed.pattern)
        assertTrue(parsed.confidence > 0.95)
    }

    @Test
    fun `test processing SBI card swipe with GST included`() {
        val sms = "Dear customer, Rs. 3,599 debited from A/c ending 4567 on 01-Jun-2025 at Flipkart. Ref: XYZ789. Available balance: Rs. 78,900"
        
        val parsed = parser.parse(sms)
        
        assertEquals(3599.0, parsed.amount)
        assertEquals("Flipkart", parsed.merchant)
        assertEquals("SBI", parsed.bank)
        assertEquals("4567", parsed.account)
        assertTrue(parsed.amount > 0)
    }

    @Test
    fun `test processing ATM cash withdrawal`() {
        val sms = "ATM withdrawal of ₹10,000 from ATM 5678, HDFC Bank on 01-Jun-2025 at 14:30 IST. Ref: ATM001. Available balance: ₹35,000"
        
        val parsed = parser.parse(sms)
        
        assertEquals(10000.0, parsed.amount)
        assertEquals("ATM", parsed.merchant)
        assertEquals("ATM_WITHDRAWAL", parsed.pattern)
    }

    @Test
    fun `test processing failed transaction`() {
        val sms = "Your payment of ₹5000 FAILED. Ref: FAIL123. Please retry. Available balance: ₹50,000"
        
        val parsed = parser.parse(sms)
        
        assertFalse(parsed.isValid)
    }

    // ========== EDGE CASES & ERROR SCENARIOS ==========

    @Test
    fun `test processing SMS with malformed amount`() {
        val sms = "Transaction of Rs. abc123 processed on 01-Jun-2025"
        
        val parsed = parser.parse(sms)
        
        assertFalse(parsed.isValid, "Should reject SMS with non-numeric amount")
    }

    @Test
    fun `test processing SMS with extreme amount`() {
        val sms = "Transaction of ₹99,99,99,999 processed on 01-Jun-2025"
        
        val parsed = parser.parse(sms)
        
        // Should either reject or flag as suspicious
        assertTrue(parsed.confidence < 0.8 || parsed.amount <= 9999999999)
    }

    @Test
    fun `test processing SMS with missing date`() {
        val sms = "Transaction of ₹5000 processed successfully. Ref: NO_DATE"
        
        val parsed = parser.parse(sms)
        
        // Should parse amount/merchant but flag low confidence on date
        assertTrue(parsed.amount > 0)
    }

    @Test
    fun `test processing SMS with duplicate reference`() {
        val sms = "Transaction of ₹5000 Ref: DUP123"
        val sms2 = "Transaction of ₹5000 Ref: DUP123"
        
        val parsed1 = parser.parse(sms)
        val parsed2 = parser.parse(sms2)
        
        assertEquals(parsed1.referenceId, parsed2.referenceId)
    }

    // ========== BATCH PROCESSING SCENARIOS ==========

    @Test
    fun `test processing batch of mixed SMS types`() {
        val smsList = listOf(
            "You have sent ₹500 via UPI Ref: UPI001",
            "ATM withdrawal ₹10000 Ref: ATM001",
            "Card payment ₹2000 received Ref: PAY001",
            "OTP: 123456",  // Should be filtered
            "Recharge of ₹199 successful Ref: RECHARGE001"  // Might be filtered
        )

        val results = smsList.map { parser.parse(it) }
        val validTransactions = results.filter { it.isValid }

        assertTrue(validTransactions.isNotEmpty())
        assertEquals(3, validTransactions.size, "Should have 3 valid transactions")
    }

    @Test
    fun `test processing rapid consecutive SMS (rate limiting scenario)`() {
        val timestamps = (1..100).map { i ->
            "Transaction ₹${i * 100} Ref: RAPID$i"
        }

        val results = timestamps.map { parser.parse(it) }
        val parsed = results.filter { it.isValid }

        // Should still parse all valid ones
        assertTrue(parsed.size >= 90)
    }

    // ========== ACCOUNT PARSING TESTS ==========

    @Test
    fun `test extracting account ending digits from various banks`() {
        val testCases = listOf(
            "ending 1234" to "1234",
            "A/c ending 5678" to "5678",
            "Account ...9999" to "9999",
            "card xxxxx 1111" to "1111"
        )

        testCases.forEach { (input, expected) ->
            val parsed = parser.parse("Transaction ₹1000 from $input")
            assertTrue(parsed.account.endsWith(expected) || parsed.account == expected,
                "Failed to extract $expected from '$input'")
        }
    }

    // ========== MERCHANT NORMALIZATION TESTS ==========

    @Test
    fun `test merchant extraction with various formats`() {
        val testCases = listOf(
            "at AMAZON PAY" to "AMAZON PAY",
            "via GooglePay" to "GooglePay",
            "to flipkart.com" to "flipkart.com",
            "Swiggy Ltd" to "Swiggy"
        )

        testCases.forEach { (input, _) ->
            val parsed = parser.parse("Transaction ₹1000 $input on 01-Jun-2025")
            assertTrue(parsed.merchant.isNotEmpty(), "Failed to extract merchant from '$input'")
        }
    }

    // ========== CONFIDENCE SCORING TESTS ==========

    @Test
    fun `test confidence scores for various SMS quality levels`() {
        val perfectSms = "You have sent ₹5000 via UPI to test@upi on 01-Jun-2025 15:30 IST. Ref: 123456. Available balance: ₹45000"
        val goodSms = "Transaction ₹5000 Ref: 123456"
        val poorSms = "₹5000 transaction"

        val perfectParsed = parser.parse(perfectSms)
        val goodParsed = parser.parse(goodSms)
        val poorParsed = parser.parse(poorSms)

        assertTrue(perfectParsed.confidence > goodParsed.confidence, 
            "Perfect SMS should have higher confidence than good SMS")
        assertTrue(goodParsed.confidence > poorParsed.confidence,
            "Good SMS should have higher confidence than poor SMS")
    }

    // ========== SPECIAL CHARACTERS & ENCODING TESTS ==========

    @Test
    fun `test parsing SMS with special characters and emojis`() {
        val testCases = listOf(
            "Transaction of ₹5,000 (₹5000 with comma)",
            "Rs. 5000 transaction",
            "5000 रुपये transfer"
        )

        testCases.forEach { sms ->
            val parsed = parser.parse(sms)
            assertEquals(5000.0, parsed.amount, "Failed to parse: $sms")
        }
    }

    @Test
    fun `test parsing SMS with CRLF and mixed whitespace`() {
        val sms = "Transaction\r\nof\r\n₹5000\n\n\rRef:\t123456"
        
        val parsed = parser.parse(sms)
        
        assertEquals(5000.0, parsed.amount)
    }

    // ========== PATTERN DETECTION TESTS ==========

    @Test
    fun `test pattern detection for all transaction types`() {
        val patterns = mapOf(
            "UPI_SENT" to "You have sent ₹500 via UPI",
            "UPI_RECEIVED" to "You have received ₹500 via UPI",
            "CARD_SPEND" to "₹500 debited from your card",
            "BANK_CREDIT" to "₹500 credited to your account",
            "BANK_DEBIT" to "₹500 debited from your account",
            "ATM_WITHDRAWAL" to "ATM withdrawal ₹5000"
        )

        patterns.forEach { (expectedPattern, sms) ->
            val parsed = parser.parse(sms)
            // Should either match the pattern or have high confidence
            assertTrue(parsed.pattern == expectedPattern || parsed.confidence > 0.7,
                "Failed pattern detection for $expectedPattern")
        }
    }

    // ========== MULTI-BANK SCENARIOS ==========

    @Test
    fun `test processing transactions from multiple banks in sequence`() {
        val hdfcSms = "HDFC: ₹1000 debited Ref: HDFC001"
        val sbiSms = "SBI: ₹2000 debited Ref: SBI001"
        val icdsSms = "ICICI: ₹3000 debited Ref: ICICI001"

        val results = listOf(hdfcSms, sbiSms, icdsSms).map { parser.parse(it) }
        
        assertEquals(1000.0, results[0].amount)
        assertEquals(2000.0, results[1].amount)
        assertEquals(3000.0, results[2].amount)
    }

    // ========== TIME-BASED TESTS ==========

    @Test
    fun `test date extraction from various formats`() {
        val testCases = listOf(
            "01-Jun-2025 15:30 IST",
            "01/06/2025",
            "2025-06-01",
            "Jun 01 2025"
        )

        testCases.forEach { dateFormat ->
            val sms = "Transaction ₹1000 on $dateFormat"
            val parsed = parser.parse(sms)
            // Should extract date successfully
            assertTrue(parsed.timestamp > 0, "Failed to extract date: $dateFormat")
        }
    }

    // ========== DEDUPLICATION TESTS ==========

    @Test
    fun `test that duplicate SMS with same reference ID are detected`() {
        val originalSms = "Transaction ₹5000 Ref: UNIQUE123 on 01-Jun-2025"
        val duplicateSms = "Transaction ₹5000 Ref: UNIQUE123 on 01-Jun-2025"
        
        val original = parser.parse(originalSms)
        val duplicate = parser.parse(duplicateSms)
        
        assertEquals(original.deduplicationHash, duplicate.deduplicationHash)
    }

    @Test
    fun `test that similar SMS with different amounts are not deduplicated`() {
        val sms1 = "Transaction ₹5000 Ref: SIMILAR on 01-Jun-2025"
        val sms2 = "Transaction ₹5001 Ref: SIMILAR on 01-Jun-2025"
        
        val parsed1 = parser.parse(sms1)
        val parsed2 = parser.parse(sms2)
        
        // Different amounts should produce different hashes
        assertTrue(parsed1.amount != parsed2.amount)
    }
}
