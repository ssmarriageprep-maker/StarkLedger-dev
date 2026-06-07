package com.starklabs.moneytracker

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Parametrized tests for SMS parsing
 * Tests multiple variations of the same scenario efficiently
 */
@RunWith(Parameterized::class)
class SmsParsingParametrizedTest(
    private val testName: String,
    private val sms: String,
    private val expectedAmount: Double?,
    private val expectedMerchant: String?,
    private val shouldSucceed: Boolean
) {

    private lateinit var parser: TestSMSParser

    interface TestSMSParser {
        fun parse(sms: String): ParsedTransaction
    }

    data class ParsedTransaction(
        val amount: Double? = null,
        val merchant: String? = null,
        val isValid: Boolean = false,
        val confidence: Double = 0.0
    )

    @Before
    fun setUp() {
        parser = object : TestSMSParser {
            override fun parse(sms: String): ParsedTransaction {
                if (sms.isBlank()) return ParsedTransaction(isValid = false)

                val amountRegex = Regex("""₹([\d,]+)|Rs\.?\s*([\d,]+)""")
                val amount = amountRegex.find(sms)?.groupValues?.let {
                    (it[1].ifEmpty { it[2] }).replace(",", "").toDoubleOrNull()
                }

                val merchantRegex = Regex("""(at|to|via|from|from)\s+([A-Za-z\s\.@]+?)(?:\s|$)""")
                val merchant = merchantRegex.find(sms)?.groupValues?.get(2)?.trim()

                val isValid = amount != null && merchant != null
                
                return ParsedTransaction(
                    amount = amount,
                    merchant = merchant,
                    isValid = isValid,
                    confidence = if (isValid) 0.85 else 0.0
                )
            }
        }
    }

    @Test
    fun `test parsing SMS: $testName`() {
        val result = parser.parse(sms)

        if (shouldSucceed) {
            assertTrue(result.isValid, "Expected parse to succeed for: $sms")
            assertEquals(expectedAmount, result.amount, "Amount mismatch for: $sms")
            assertEquals(expectedMerchant, result.merchant, "Merchant mismatch for: $sms")
        } else {
            assertTrue(!result.isValid, "Expected parse to fail for: $sms")
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<Array<Any?>> = listOf(
            // ===== VALID UPI TRANSACTIONS =====
            arrayOf(
                "Valid UPI sent",
                "You have sent ₹5000 via UPI to test@upi",
                5000.0,
                "test@upi",
                true
            ),
            arrayOf(
                "Valid UPI received",
                "You have received ₹10000 from test via UPI",
                10000.0,
                "test",
                true
            ),

            // ===== VALID CARD TRANSACTIONS =====
            arrayOf(
                "Card debit Flipkart",
                "₹3599 debited from card at Flipkart on 01-Jun-2025",
                3599.0,
                "Flipkart",
                true
            ),
            arrayOf(
                "Card credit refund",
                "₹2000 credited from Amazon",
                2000.0,
                "Amazon",
                true
            ),

            // ===== BANK TRANSACTIONS =====
            arrayOf(
                "NEFT transfer",
                "₹50000 transferred via NEFT to account ending 1234",
                50000.0,
                "account",
                true
            ),
            arrayOf(
                "ATM withdrawal",
                "ATM withdrawal ₹10000 on 01-Jun-2025 15:30",
                10000.0,
                "ATM",
                true
            ),

            // ===== COMMA-SEPARATED AMOUNTS =====
            arrayOf(
                "Amount with commas",
                "Transaction ₹1,00,000 to Merchant",
                100000.0,
                "Merchant",
                true
            ),
            arrayOf(
                "Amount with single comma",
                "Rs. 5,000 at BigShop",
                5000.0,
                "BigShop",
                true
            ),

            // ===== DECIMAL AMOUNTS =====
            arrayOf(
                "Decimal amount .99",
                "₹100.99 payment to vendor",
                100.99,
                "vendor",
                true
            ),
            arrayOf(
                "Decimal amount .5",
                "₹5000.5 transferred via UPI",
                5000.5,
                "via",
                true
            ),

            // ===== VARIOUS MERCHANT FORMATS =====
            arrayOf(
                "Merchant with @",
                "Sent ₹100 to user@email.com",
                100.0,
                "user@email.com",
                true
            ),
            arrayOf(
                "Merchant with dots",
                "₹1000 to flipkart.com",
                1000.0,
                "flipkart.com",
                true
            ),
            arrayOf(
                "Merchant with multiple words",
                "₹500 to Pizza Hut Order",
                500.0,
                "Pizza",
                true
            ),

            // ===== DIFFERENT AMOUNT INDICATORS =====
            arrayOf(
                "Rs. with dot and space",
                "Rs. 5000 transferred to merchant",
                5000.0,
                "transferred",
                true
            ),
            arrayOf(
                "Rs without dot",
                "Rs 2000 debited via ATM",
                2000.0,
                "debited",
                true
            ),

            // ===== EDGE CASES - SHOULD FAIL =====
            arrayOf(
                "No amount",
                "Transaction processed successfully",
                null,
                null,
                false
            ),
            arrayOf(
                "No merchant",
                "₹5000 transaction completed",
                null,
                null,
                false
            ),
            arrayOf(
                "Empty SMS",
                "",
                null,
                null,
                false
            ),
            arrayOf(
                "Whitespace only",
                "   \n\t  ",
                null,
                null,
                false
            ),

            // ===== INVALID FORMAT TESTS =====
            arrayOf(
                "Malformed amount",
                "Transaction of ₹5k amount",
                null,
                null,
                false
            ),
            arrayOf(
                "Text only",
                "Please verify your account details with the bank",
                null,
                null,
                false
            ),

            // ===== REAL-WORLD BANK MESSAGES =====
            arrayOf(
                "HDFC Bank format",
                "Hi, You have sent ₹5000 via UPI to test@upi. Ref: 123456. Available balance: ₹45000",
                5000.0,
                "test@upi",
                true
            ),
            arrayOf(
                "SBI Bank format",
                "Dear customer, Rs. 3,599 debited from A/c ending 4567 on 01-Jun-2025 at Flipkart.",
                3599.0,
                "from",
                true
            ),
            arrayOf(
                "ICICI Bank format",
                "Debit alert: ₹10,000 has been debited from your account. Merchant: Amazon",
                10000.0,
                "has",
                true
            ),

            // ===== PROMOTIONAL MESSAGES (SHOULD FAIL) =====
            arrayOf(
                "Promotional offer",
                "Get ₹500 cashback on ₹5000 transaction this week",
                null,
                null,
                false
            ),
            arrayOf(
                "Bill payment offer",
                "Pay ₹2000+ bills and get ₹100 reward",
                null,
                null,
                false
            ),

            // ===== SPECIAL CASES =====
            arrayOf(
                "Multiple amounts - should parse first",
                "Options: ₹1000 or ₹2000 to merchant",
                1000.0,
                "or",
                true
            ),
            arrayOf(
                "Amount at end",
                "Payment to Merchant confirmed at ₹5000",
                5000.0,
                "to",
                true
            ),

            // ===== INTERNATIONAL FORMATS =====
            arrayOf(
                "USD amount (should fail INR parser)",
                "Payment $100 to merchant",
                null,
                "to",
                false
            ),

            // ===== BOUNDARY VALUES =====
            arrayOf(
                "Single rupee",
                "₹1 charged by merchant",
                1.0,
                "charged",
                true
            ),
            arrayOf(
                "Very large amount",
                "₹99,99,999 transaction at store",
                9999999.0,
                "transaction",
                true
            ),

            // ===== ZERO AMOUNT =====
            arrayOf(
                "Zero amount",
                "₹0 refund from vendor",
                0.0,
                "refund",
                true
            ),

            // ===== RECHARGE/TOPUP (OFTEN FILTERED) =====
            arrayOf(
                "Recharge message",
                "Your ₹199 recharge was successful",
                199.0,
                "recharge",
                true
            )
        )
    }
}
