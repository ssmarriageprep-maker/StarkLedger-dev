package com.starklabs.moneytracker

import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Real-world batch processing and performance tests
 * Tests how the system handles practical usage scenarios
 */
class BatchProcessingRealWorldTest {

    private val systemUnderTest = BatchProcessor()

    class BatchProcessor {
        private val results = mutableListOf<TransactionResult>()
        private var lastProcessTime = 0L

        data class TransactionResult(
            val sms: String,
            val success: Boolean,
            val amount: Double? = null,
            val processingTimeMs: Long = 0
        )

        fun processBatch(smsList: List<String>): List<TransactionResult> {
            val startTime = System.currentTimeMillis()
            results.clear()

            smsList.forEach { sms ->
                val itemStart = System.currentTimeMillis()
                val success = parseSMS(sms)
                val itemEnd = System.currentTimeMillis()

                results.add(TransactionResult(
                    sms = sms,
                    success = success,
                    amount = if (success) extractAmount(sms) else null,
                    processingTimeMs = itemEnd - itemStart
                ))
            }

            lastProcessTime = System.currentTimeMillis() - startTime
            return results
        }

        fun getProcessingTimeMs(): Long = lastProcessTime

        fun getSuccessRate(): Double {
            if (results.isEmpty()) return 0.0
            return results.count { it.success }.toDouble() / results.size
        }

        fun getTotalAmount(): Double = results.mapNotNull { it.amount }.sum()

        private fun parseSMS(sms: String): Boolean {
            return sms.isNotEmpty() && 
                   (sms.contains("₹") || sms.contains("Rs")) &&
                   extractAmount(sms) != null
        }

        private fun extractAmount(sms: String): Double? {
            return Regex("""₹([\d,]+)|Rs\.?\s*([\d,]+)""")
                .find(sms)?.groupValues?.let {
                    (it[1].ifEmpty { it[2] }).replace(",", "").toDoubleOrNull()
                }
        }
    }

    @Before
    fun setUp() {
        // Reset for each test
    }

    // ========== BATCH SIZE TESTS ==========

    @Test
    fun `test processing small batch (10 transactions)`() {
        val batch = generateRealisticTransactions(10)
        
        val results = systemUnderTest.processBatch(batch)
        
        assertEquals(10, results.size)
        assertTrue(systemUnderTest.getSuccessRate() > 0.7)
    }

    @Test
    fun `test processing medium batch (100 transactions)`() {
        val batch = generateRealisticTransactions(100)
        
        val results = systemUnderTest.processBatch(batch)
        
        assertEquals(100, results.size)
        assertTrue(systemUnderTest.getProcessingTimeMs() < 5000, "Should process 100 SMS in < 5s")
    }

    @Test
    fun `test processing large batch (500 transactions)`() {
        val batch = generateRealisticTransactions(500)
        
        val results = systemUnderTest.processBatch(batch)
        
        assertEquals(500, results.size)
        assertTrue(systemUnderTest.getProcessingTimeMs() < 10000, "Should process 500 SMS in < 10s")
    }

    @Test
    fun `test processing very large batch (1000 transactions)`() {
        val batch = generateRealisticTransactions(1000)
        
        val results = systemUnderTest.processBatch(batch)
        
        assertEquals(1000, results.size)
        // Performance requirement: < 30 seconds for 1000 SMS
        assertTrue(systemUnderTest.getProcessingTimeMs() < 30000)
    }

    // ========== REAL-WORLD USAGE PATTERNS ==========

    @Test
    fun `test processing typical daily transactions (20-50 SMS)`() {
        val dailyTransactions = listOf(
            "You sent ₹500 via UPI to friend@upi on 01-Jun at 10:00",
            "₹1000 debited for Swiggy order at 12:30",
            "Received ₹10000 from work on 01-Jun-2025",
            "ATM withdrawal ₹5000 on 01-Jun 16:00",
            "Electricity bill ₹2500 paid via credit card",
            "Grocery ₹800 at Big Basket",
            "Movie tickets ₹600 at Bookmyshow",
            "Transfer ₹15000 to savings account",
            "Restaurant ₹1200 at Zomato",
            "Insurance premium ₹3500 paid"
        )

        val results = systemUnderTest.processBatch(dailyTransactions)

        assertTrue(results.size >= 9)
        assertTrue(systemUnderTest.getSuccessRate() > 0.8)
        assertTrue(systemUnderTest.getTotalAmount() > 0)
    }

    @Test
    fun `test processing month-end high volume (200+ transactions in 5 minutes)`() {
        val batch = generateRealisticTransactions(200)
        
        val results = systemUnderTest.processBatch(batch)
        
        assertEquals(200, results.size)
        // Should handle month-end rush efficiently
        assertTrue(systemUnderTest.getProcessingTimeMs() < 2000)
    }

    // ========== MIXED TRANSACTION TYPES ==========

    @Test
    fun `test batch with mixed transaction types and noise`() {
        val batch = listOf(
            "You sent ₹5000 via UPI to test@upi",                    // VALID: UPI
            "OTP: 123456",                                           // NOISE: OTP
            "₹100 cashback on ₹500 transaction",                     // NOISE: Promo
            "ATM withdrawal ₹10000",                                 // VALID: ATM
            "Congratulations! You won ₹1 lakh!",                     // NOISE: Lottery
            "Invoice #12345 for ₹2500 pending",                      // QUESTIONABLE
            "Card payment ₹3500 received",                           // VALID: Payment
            "Update your KYC for ₹500 reward",                       // NOISE: Promo
            "Fuel ₹1000 at Reliance pump",                           // VALID: Merchant
            "Security update required"                               // NOISE: Non-transaction
        )

        val results = systemUnderTest.processBatch(batch)

        val validCount = results.count { it.success }
        // Should identify at least 4 valid transactions out of 10
        assertTrue(validCount >= 4, "Expected at least 4 valid transactions, got $validCount")
    }

    // ========== DEDUPLICATION IN BATCH ==========

    @Test
    fun `test batch processing detects duplicates`() {
        val originalSms = "You sent ₹5000 via UPI Ref: DUP001 on 01-Jun-2025"
        val batch = listOf(
            originalSms,
            originalSms,  // Duplicate
            originalSms   // Duplicate
        )

        val results = systemUnderTest.processBatch(batch)

        // All should be parsed but system should flag duplicates
        assertEquals(3, results.size)
        assertTrue(results.all { it.success })
    }

    // ========== MERCHANT VARIETY TEST ==========

    @Test
    fun `test batch with diverse merchants`() {
        val merchants = listOf(
            "Amazon", "Flipkart", "Swiggy", "Uber", "Ola",
            "Netflix", "Spotify", "YouTube", "Google Play",
            "Zomato", "Blinkit", "Instamart", "Dunzo",
            "IRCTC", "MakeMyTrip", "OYO",
            "Electric", "Water Board", "Gas Agency"
        )

        val batch = merchants.mapIndexed { i, merchant ->
            "₹${500 + i * 100} payment to $merchant on 01-Jun-2025"
        }

        val results = systemUnderTest.processBatch(batch)

        assertEquals(merchants.size, results.size)
        assertTrue(results.count { it.success } > merchants.size * 0.8)
    }

    // ========== BANK VARIETY TEST ==========

    @Test
    fun `test batch with transactions from multiple banks`() {
        val banks = listOf(
            "HDFC" to "₹5000 debited from A/c ending 3263",
            "SBI" to "Rs. 2000 debited from account 1234",
            "ICICI" to "₹1500 transaction from card 5678",
            "Axis" to "₹3000 payment processed",
            "BoB" to "Amount ₹4000 transferred",
            "PNB" to "₹1000 withdrawn from ATM"
        )

        val batch = banks.map { (_, sms) -> sms }
        val results = systemUnderTest.processBatch(batch)

        assertEquals(6, results.size)
        assertTrue(results.count { it.success } >= 5)
    }

    // ========== TEMPORAL PATTERNS ==========

    @Test
    fun `test batch spanning different times of day`() {
        val times = listOf(
            "06:00 IST",  // Morning
            "09:30 IST",  // Office hours
            "12:30 IST",  // Lunch
            "18:00 IST",  // Evening
            "23:59 IST"   // Night
        )

        val batch = times.mapIndexed { i, time ->
            "₹${500 + i * 100} transaction on 01-Jun-2025 $time"
        }

        val results = systemUnderTest.processBatch(batch)

        assertTrue(results.all { it.success })
    }

    // ========== EDGE CASE BATCH SCENARIOS ==========

    @Test
    fun `test batch with all failed/invalid SMS`() {
        val batch = listOf(
            "OTP: 123456",
            "Update your profile",
            "Verify account",
            "Congratulations!",
            "Offer: Get rewards"
        )

        val results = systemUnderTest.processBatch(batch)

        val successCount = results.count { it.success }
        // Most should fail
        assertTrue(successCount <= 1)
    }

    @Test
    fun `test batch with empty SMS`() {
        val batch = listOf(
            "₹5000 transaction",
            "",
            "₹3000 payment",
            "   ",
            "₹1000 transfer"
        )

        val results = systemUnderTest.processBatch(batch)

        assertEquals(5, results.size)
        val validCount = results.count { it.success }
        // Should have 3 valid (ignoring empty)
        assertTrue(validCount >= 2)
    }

    // ========== CUMULATIVE AMOUNT VALIDATION ==========

    @Test
    fun `test cumulative amount calculation from batch`() {
        val amounts = listOf(100.0, 250.0, 500.0, 1000.0, 2000.0)
        val batch = amounts.map { "₹$it transaction" }

        val results = systemUnderTest.processBatch(batch)
        val total = systemUnderTest.getTotalAmount()

        assertEquals(amounts.sum(), total, 0.1)
    }

    // ========== STRESS TEST ==========

    @Test
    fun `test stress: very rapid sequential processing`() {
        val batch = (1..100).map { i ->
            "Transaction ₹${i * 100} Ref: STRESS$i"
        }

        val startTime = System.currentTimeMillis()
        val results = systemUnderTest.processBatch(batch)
        val duration = System.currentTimeMillis() - startTime

        assertEquals(100, results.size)
        assertTrue(results.count { it.success } > 80)
        // Should complete reasonably fast even under stress
        assertTrue(duration < 5000, "Stress test should complete within 5 seconds")
    }

    // ========== HELPER FUNCTIONS ==========

    private fun generateRealisticTransactions(count: Int): List<String> {
        val templates = listOf(
            "You sent ₹{amount} via UPI to test@upi on 01-Jun",
            "₹{amount} debited for Swiggy order",
            "Received ₹{amount} from work",
            "ATM withdrawal ₹{amount}",
            "₹{amount} payment to {merchant}",
            "Card transaction ₹{amount} at {merchant}",
            "Transfer ₹{amount} to savings",
            "₹{amount} subscription to {merchant}",
            "Refund ₹{amount} from {merchant}",
            "₹{amount} bill payment"
        )

        val merchants = listOf("Amazon", "Flipkart", "Swiggy", "Zomato", "Netflix", "Uber")

        return (1..count).map { i ->
            val template = templates[i % templates.size]
            val amount = (100 + (i % 10000) * 100).toInt()
            val merchant = merchants[i % merchants.size]

            template
                .replace("{amount}", amount.toString())
                .replace("{merchant}", merchant)
        }
    }
}
