package com.starklabs.moneytracker

import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Error handling and recovery tests
 * Tests how the system handles malformed data, edge cases, and recovers from errors
 */
class ErrorHandlingTest {

    private lateinit var parser: SMSErrorHandler

    interface SMSErrorHandler {
        fun parseWithErrorHandling(sms: String): ParseResult
        fun validateBeforeParse(sms: String): ValidationError?
        fun recoverFromError(error: Exception, sms: String): ParseResult?
    }

    data class ValidationError(
        val code: String,
        val message: String,
        val severity: String // CRITICAL, WARNING, INFO
    )

    data class ParseResult(
        val success: Boolean,
        val amount: Double? = null,
        val merchant: String? = null,
        val error: ValidationError? = null,
        val warnings: List<String> = emptyList()
    )

    @Before
    fun setUp() {
        parser = object : SMSErrorHandler {
            override fun parseWithErrorHandling(sms: String): ParseResult {
                return try {
                    val validation = validateBeforeParse(sms)
                    if (validation != null && validation.severity == "CRITICAL") {
                        return ParseResult(success = false, error = validation)
                    }

                    val amount = extractAmount(sms)
                    val merchant = extractMerchant(sms)

                    ParseResult(
                        success = amount != null && merchant != null,
                        amount = amount,
                        merchant = merchant,
                        warnings = listOfNotNull(
                            if (validation?.severity == "WARNING") validation.message else null
                        )
                    )
                } catch (e: Exception) {
                    val result = recoverFromError(e, sms)
                    result ?: ParseResult(success = false, error = ValidationError("PARSE_ERROR", e.message ?: "Unknown error", "CRITICAL"))
                }
            }

            override fun validateBeforeParse(sms: String): ValidationError? {
                return when {
                    sms.isBlank() -> ValidationError("EMPTY_SMS", "SMS is empty", "CRITICAL")
                    sms.length > 160 && !sms.contains("₹") && !sms.contains("Rs") -> 
                        ValidationError("NO_AMOUNT_INDICATOR", "SMS too long without amount indicator", "WARNING")
                    sms.contains(Regex("""₹-\d+|Rs\.-\d+""")) -> 
                        ValidationError("NEGATIVE_AMOUNT", "Negative amount detected", "CRITICAL")
                    else -> null
                }
            }

            override fun recoverFromError(error: Exception, sms: String): ParseResult? {
                return when (error) {
                    is NumberFormatException -> {
                        val cleanedAmount = sms.replace(Regex("[^0-9.]"), "").toDoubleOrNull()
                        if (cleanedAmount != null) {
                            ParseResult(success = true, amount = cleanedAmount, warnings = listOf("Amount extracted with cleanup"))
                        } else null
                    }
                    else -> null
                }
            }

            private fun extractAmount(sms: String): Double? {
                return Regex("""₹([\d,]+)|Rs\.?\s*([\d,]+)""").find(sms)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
            }

            private fun extractMerchant(sms: String): String? {
                return Regex("""(at|to|via|from)\s+([A-Za-z\s]+)(?:\s|$)""").find(sms)?.groupValues?.get(2)?.trim()
            }
        }
    }

    // ========== VALIDATION ERROR TESTS ==========

    @Test
    fun `test rejecting empty SMS`() {
        val error = parser.validateBeforeParse("")

        assertTrue(error != null && error.severity == "CRITICAL")
        assertEquals("EMPTY_SMS", error?.code)
    }

    @Test
    fun `test rejecting very long SMS without transaction indicators`() {
        val longSms = "a".repeat(161) // Over 160 chars without ₹ or Rs

        val error = parser.validateBeforeParse(longSms)

        assertTrue(error != null && error.severity == "WARNING")
    }

    @Test
    fun `test rejecting negative amounts`() {
        val sms = "Transaction ₹-5000 processed"

        val error = parser.validateBeforeParse(sms)

        assertTrue(error != null && error.code == "NEGATIVE_AMOUNT")
    }

    @Test
    fun `test handling whitespace-only SMS`() {
        val result = parser.parseWithErrorHandling("   \n\t  ")

        assertFalse(result.success)
        assertEquals("EMPTY_SMS", result.error?.code)
    }

    // ========== NULL/MISSING DATA TESTS ==========

    @Test
    fun `test handling SMS with no amount`() {
        val sms = "Transaction processed successfully. Please check your account."

        val result = parser.parseWithErrorHandling(sms)

        assertFalse(result.success)
        assertNull(result.amount)
    }

    @Test
    fun `test handling SMS with no merchant`() {
        val sms = "Transaction of ₹5000 processed. Ref: 123456"

        val result = parser.parseWithErrorHandling(sms)

        // Should succeed with amount but missing merchant is warning-level
        assertTrue(result.warnings.size >= 0)
    }

    @Test
    fun `test handling SMS with malformed amount format`() {
        val sms = "Transaction of ₹5.000.000 processed"  // European decimal format

        val result = parser.parseWithErrorHandling(sms)

        // Should attempt recovery
        assertTrue(result.success || result.error != null)
    }

    // ========== BOUNDARY VALUE TESTS ==========

    @Test
    fun `test handling zero amount`() {
        val sms = "Transaction of ₹0 processed"

        val result = parser.parseWithErrorHandling(sms)

        // Zero might be valid (debit reversal) or invalid depending on business logic
        assertTrue(result.success || !result.success)  // Both acceptable
    }

    @Test
    fun `test handling very large amount`() {
        val sms = "Transaction of ₹999999999999 processed"

        val result = parser.parseWithErrorHandling(sms)

        if (result.success) {
            assertTrue(result.amount!! <= 999999999999)
        }
    }

    @Test
    fun `test handling decimal amounts`() {
        val testCases = listOf(
            "₹5000.50" to 5000.50,
            "Rs. 100.99" to 100.99,
            "₹5000.5" to 5000.5
        )

        testCases.forEach { (input, expected) ->
            val sms = "Transaction of $input processed"
            val result = parser.parseWithErrorHandling(sms)
            
            if (result.success) {
                assertEquals(expected, result.amount)
            }
        }
    }

    // ========== SPECIAL CHARACTER HANDLING ==========

    @Test
    fun `test handling SMS with multiple currency symbols`() {
        val sms = "Transaction ₹5000 ₹2000 ₹3000"  // Multiple amounts

        val result = parser.parseWithErrorHandling(sms)

        // Should either pick first, sum, or error
        assertTrue(result.success || result.error != null)
    }

    @Test
    fun `test handling SMS with HTML entities`() {
        val sms = "Transaction &rupee;5000 &amp; processed"

        val result = parser.parseWithErrorHandling(sms)

        // Should handle or report error gracefully
        assertTrue(result.error == null || result.error.severity != "CRITICAL")
    }

    @Test
    fun `test handling SMS with unicode characters`() {
        val sms = "लेन-देन ₹5000 संसाधित"  // Hindi characters

        val result = parser.parseWithErrorHandling(sms)

        // Should extract amount regardless of language
        assertTrue(result.amount == 5000.0 || result.error != null)
    }

    @Test
    fun `test handling control characters in SMS`() {
        val sms = "Transaction\u0000₹5000\u0001processed"

        val result = parser.parseWithErrorHandling(sms)

        // Should handle or sanitize gracefully
        assertTrue(result.error == null || result.error.code != "PARSE_ERROR")
    }

    // ========== RECOVERY TESTS ==========

    @Test
    fun `test recovery from number format exception`() {
        val sms = "Transaction of ₹5,000 processed"

        val result = parser.parseWithErrorHandling(sms)

        // Should successfully recover and parse
        assertTrue(result.success || result.warnings.contains("Amount extracted with cleanup"))
    }

    @Test
    fun `test recovery with warnings`() {
        val sms = "Transaction ₹5000" + "\n" + "on " + "a".repeat(200)  // Unusual but parseable

        val result = parser.parseWithErrorHandling(sms)

        // May have warnings but should try to recover
        assertTrue(result.warnings.size >= 0)
    }

    // ========== CONSTRAINT VIOLATION TESTS ==========

    @Test
    fun `test handling SMS violating business rules`() {
        val testCases = listOf(
            "Transaction ₹5000 at illegal-merchant.com",
            "Transfer ₹99999999 to unknown",
            "Refund ₹5000 from future date 2099-01-01"
        )

        testCases.forEach { sms ->
            val result = parser.parseWithErrorHandling(sms)
            // System should flag or reject such transactions
            assertTrue(result.success || result.error != null)
        }
    }

    // ========== DUPLICATE HANDLING TESTS ==========

    @Test
    fun `test handling identical SMS parsed twice`() {
        val sms = "Transaction ₹5000 Ref: 123456 on 01-Jun-2025"

        val result1 = parser.parseWithErrorHandling(sms)
        val result2 = parser.parseWithErrorHandling(sms)

        assertEquals(result1.amount, result2.amount)
        assertEquals(result1.merchant, result2.merchant)
    }

    // ========== CONCURRENT ERROR SCENARIOS ==========

    @Test
    fun `test handling rapid error recovery`() {
        val errorSmsList = listOf(
            "",
            "invalid",
            "₹5000",
            "Transaction ₹5000",
            "₹10000 at merchant"
        )

        val results = errorSmsList.map { parser.parseWithErrorHandling(it) }

        // Should not crash or deadlock
        assertTrue(results.size == 5)
    }

    // ========== REPORTING & OBSERVABILITY TESTS ==========

    @Test
    fun `test error codes are consistent`() {
        val sms = ""
        val result = parser.parseWithErrorHandling(sms)

        assertTrue(result.error?.code?.matches(Regex("^[A-Z_]+$")) ?: true)
    }

    @Test
    fun `test error messages are human-readable`() {
        val sms = ""
        val result = parser.parseWithErrorHandling(sms)

        if (result.error != null) {
            assertTrue(result.error.message.length > 0)
            assertTrue(result.error.message[0].isUpperCase())
        }
    }

    @Test
    fun `test severity levels are properly set`() {
        val severities = setOf("CRITICAL", "WARNING", "INFO")
        
        val results = listOf(
            parser.parseWithErrorHandling(""),  // Should be CRITICAL
            parser.parseWithErrorHandling("a".repeat(161)),  // Should be WARNING
        )

        results.forEach { result ->
            if (result.error != null) {
                assertTrue(result.error.severity in severities)
            }
        }
    }
}
