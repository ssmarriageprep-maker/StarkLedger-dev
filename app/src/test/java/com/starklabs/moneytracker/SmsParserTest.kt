package com.starklabs.moneytracker

import com.starklabs.moneytracker.domain.SmsParser
import org.junit.Test
import org.junit.Assert.*

/**
 * Comprehensive test suite for High-Precision SMS Parser
 * 
 * Tests cover:
 * - ✅ Valid transaction messages (debit/credit)
 * - ❌ Promotional/offer messages (must reject)
 * - ❌ OTP/verification messages (must reject)
 * - ❌ Bill reminders (must reject)
 * - 💰 Amount extraction accuracy (never 0.0)
 * - 🏦 Bank and account extraction
 * - 🧾 Merchant extraction
 */
class SmsParserTest {

    // ========================================
    // ✅ VALID TRANSACTION TESTS
    // ========================================

    @Test
    fun `valid HDFC debit transaction with all details`() {
        val body = "Rs.318.00 sent to Dreamplug Service Private Limited from HDFC Bank A/C *3263 on 25-10-2025. Ref: 566413406309"
        val result = SmsParser.parseSms("HDFCBK", body, 123456789L)
        
        assertTrue("Should be a valid transaction", result.isTransaction)
        assertEquals(318.00, result.amount!!, 0.01)
        assertEquals("INR", result.currency)
        assertEquals("debit", result.transactionType)
        assertEquals("HDFC", result.bank)
        assertEquals("3263", result.accountLast4)
        assertEquals("Dreamplug Service Private Limited", result.merchant)
        assertEquals("25-10-2025", result.date)
        assertEquals("566413406309", result.reference)
    }

    @Test
    fun `valid SBI debit with rupee symbol and comma`() {
        val body = "₹1,250.50 debited from SBI A/C XX4567 for AMAZON INDIA on 17-OCT-25. UPI Ref: UPI123456"
        val result = SmsParser.parseSms("SBIUPI", body, 123456789L)
        
        assertTrue(result.isTransaction)
        assertEquals(1250.50, result.amount!!, 0.01)
        assertEquals("debit", result.transactionType)
        assertEquals("SBI", result.bank)
        assertEquals("4567", result.accountLast4)
        assertNotNull(result.reference)
    }

    @Test
    fun `valid ICICI credit transaction`() {
        val body = "Rs 5000.00 credited to your ICICI Bank account *1234 on 01/01/2026. Salary transfer."
        val result = SmsParser.parseSms("ICICIB", body, 123456789L)
        
        assertTrue(result.isTransaction)
        assertEquals(5000.00, result.amount!!, 0.01)
        assertEquals("credit", result.transactionType)
        assertEquals("ICICI", result.bank)
        assertEquals("1234", result.accountLast4)
    }

    @Test
    fun `valid UPI payment with merchant`() {
        val body = "INR 450.00 paid to ZOMATO via UPI from HDFC Bank. Txn ID: 123456789"
        val result = SmsParser.parseSms("UPIHDFC", body, 123456789L)
        
        assertTrue(result.isTransaction)
        assertEquals(450.00, result.amount!!, 0.01)
        assertEquals("debit", result.transactionType)
        assertEquals("ZOMATO", result.merchant)
        assertEquals("123456789", result.reference)
    }

    @Test
    fun `valid card purchase at merchant`() {
        val body = "Rs 943.00 spent at STARBUCKS COFFEE using AXIS Bank card *8899 on 15/12/2025"
        val result = SmsParser.parseSms("AXISBK", body, 123456789L)
        
        assertTrue(result.isTransaction)
        assertEquals(943.00, result.amount!!, 0.01)
        assertEquals("debit", result.transactionType)
        assertEquals("AXIS", result.bank)
        assertEquals("8899", result.accountLast4)
        assertEquals("STARBUCKS COFFEE", result.merchant)
    }

    @Test
    fun `valid NEFT transfer`() {
        val body = "Rs 10000 transferred to RAMESH KUMAR via NEFT from KOTAK Bank A/C 5678. UTR: KOTAK123456"
        val result = SmsParser.parseSms("KOTAK", body, 123456789L)
        
        assertTrue(result.isTransaction)
        assertEquals(10000.00, result.amount!!, 0.01)
        assertEquals("debit", result.transactionType)
        assertEquals("KOTAK", result.bank)
        assertEquals("RAMESH KUMAR", result.merchant)
    }

    @Test
    fun `valid refund credit`() {
        val body = "Refund of Rs 599.00 credited to your HDFC account *1122. Ref: REF123456"
        val result = SmsParser.parseSms("HDFCBK", body, 123456789L)
        
        assertTrue(result.isTransaction)
        assertEquals(599.00, result.amount!!, 0.01)
        assertEquals("credit", result.transactionType)
        assertEquals("HDFC", result.bank)
    }

    @Test
    fun `valid transaction with balance`() {
        val body = "Rs 200.00 debited from SBI A/C *3344 for SWIGGY. Avl Bal: Rs 15,000.50"
        val result = SmsParser.parseSms("SBIUPI", body, 123456789L)
        
        assertTrue(result.isTransaction)
        assertEquals(200.00, result.amount!!, 0.01)
        assertEquals(15000.50, result.balance!!, 0.01)
    }

    // ========================================
    // ❌ REJECTION TESTS - PROMOTIONAL
    // ========================================

    @Test
    fun `reject Airtel recharge offer`() {
        val body = "Recharge with Rs.318 and get 2GB/day + OTT benefits. Valid for 28 days. Call 9606XXX997"
        val result = SmsParser.parseSms("AIRTEL", body, 123456789L)
        
        assertFalse("Should reject recharge offer", result.isTransaction)
        assertNotNull("Should have rejection reason", result.reason)
        assertTrue(result.reason!!.contains("recharge", ignoreCase = true))
    }

    @Test
    fun `reject Jio offer message`() {
        val body = "Get unlimited calls + 5G data at Rs 299. Offer valid till 31st Dec. Recharge now!"
        val result = SmsParser.parseSms("JIOTEL", body, 123456789L)
        
        assertFalse(result.isTransaction)
        assertTrue(result.reason!!.contains("offer", ignoreCase = true) || 
                   result.reason!!.contains("recharge", ignoreCase = true))
    }

    @Test
    fun `reject Vodafone validity message`() {
        val body = "Your plan validity expires on 25-Dec-2025. Recharge with Rs 399 for 84 days validity."
        val result = SmsParser.parseSms("VICARE", body, 123456789L)
        
        assertFalse(result.isTransaction)
        assertNotNull(result.reason)
    }

    @Test
    fun `reject OTT subscription offer`() {
        val body = "Subscribe to Xstream Premium for Rs 149/month. Get Disney+ Hotstar free!"
        val result = SmsParser.parseSms("AIRTEL", body, 123456789L)
        
        assertFalse(result.isTransaction)
        assertTrue(result.reason!!.contains("ott", ignoreCase = true) || 
                   result.reason!!.contains("xstream", ignoreCase = true))
    }

    // ========================================
    // ❌ REJECTION TESTS - BILLS & REMINDERS
    // ========================================

    @Test
    fun `reject bill due reminder`() {
        val body = "Your HDFC credit card bill of Rs 5000 is due on 15-Jan-2026. Minimum due: Rs 500"
        val result = SmsParser.parseSms("HDFCBK", body, 123456789L)
        
        assertFalse(result.isTransaction)
        assertTrue(result.reason!!.contains("bill due", ignoreCase = true) || 
                   result.reason!!.contains("minimum due", ignoreCase = true))
    }

    @Test
    fun `reject bill payment reminder`() {
        val body = "Bill payment reminder: Your electricity bill of Rs 1200 is pending. Pay now to avoid late fee."
        val result = SmsParser.parseSms("BESCOM", body, 123456789L)
        
        assertFalse(result.isTransaction)
        assertNotNull(result.reason)
    }

    @Test
    fun `reject statement generated message`() {
        val body = "Your ICICI Bank statement for Dec 2025 is generated. Total spends: Rs 25000. Download now."
        val result = SmsParser.parseSms("ICICIB", body, 123456789L)
        
        assertFalse(result.isTransaction)
        assertTrue(result.reason!!.contains("statement generated", ignoreCase = true))
    }

    @Test
    fun `reject BSNL landline bill`() {
        val body = "Your BSNL landline bill for Rs 500 is generated. Due date: 20-Jan-2026"
        val result = SmsParser.parseSms("BSNLBB", body, 123456789L)
        
        assertFalse(result.isTransaction)
        assertTrue(result.reason!!.contains("bsnl landline", ignoreCase = true) || 
                   result.reason!!.contains("bill due", ignoreCase = true))
    }

    // ========================================
    // ❌ REJECTION TESTS - OTP & VERIFICATION
    // ========================================

    @Test
    fun `reject OTP message`() {
        val body = "Your OTP for transaction is 123456. Valid for 10 minutes. Do not share with anyone."
        val result = SmsParser.parseSms("HDFCBK", body, 123456789L)
        
        assertFalse(result.isTransaction)
        assertTrue(result.reason!!.contains("otp", ignoreCase = true))
    }

    @Test
    fun `reject verification code`() {
        val body = "Your verification code is 9876. Use this to complete your registration."
        val result = SmsParser.parseSms("VERIFY", body, 123456789L)
        
        assertFalse(result.isTransaction)
        assertTrue(result.reason!!.contains("verification code", ignoreCase = true))
    }

    // ========================================
    // ❌ REJECTION TESTS - MARKETING
    // ========================================

    @Test
    fun `reject cashback offer`() {
        val body = "Get 20% cashback offer on all UPI payments above Rs 500. Valid till 31st Dec."
        val result = SmsParser.parseSms("PAYTM", body, 123456789L)
        
        assertFalse(result.isTransaction)
        assertTrue(result.reason!!.contains("cashback offer", ignoreCase = true))
    }

    @Test
    fun `reject loan offer`() {
        val body = "Pre-approved loan offer of Rs 5,00,000 at 10% interest. Apply now!"
        val result = SmsParser.parseSms("HDFCBK", body, 123456789L)
        
        assertFalse(result.isTransaction)
        assertTrue(result.reason!!.contains("loan offer", ignoreCase = true))
    }

    @Test
    fun `reject credit limit increase offer`() {
        val body = "Congratulations! Your credit limit has been increased to Rs 2,00,000. Enjoy more spending power."
        val result = SmsParser.parseSms("ICICIB", body, 123456789L)
        
        assertFalse(result.isTransaction)
        assertTrue(result.reason!!.contains("credit limit", ignoreCase = true))
    }

    // ========================================
    // ❌ REJECTION TESTS - INSUFFICIENT INDICATORS
    // ========================================

    @Test
    fun `reject message with only amount, no transaction indicators`() {
        val body = "Your balance is Rs 5000. Thank you for banking with us."
        val result = SmsParser.parseSms("BANK", body, 123456789L)
        
        assertFalse(result.isTransaction)
        assertTrue(result.reason!!.contains("Insufficient transaction indicators", ignoreCase = true))
    }

    @Test
    fun `reject message with only one indicator`() {
        val body = "Rs 1000 available. Contact customer care for details."
        val result = SmsParser.parseSms("BANK", body, 123456789L)
        
        assertFalse(result.isTransaction)
        assertNotNull(result.reason)
    }

    // ========================================
    // 💰 AMOUNT EXTRACTION TESTS
    // ========================================

    @Test
    fun `extract amount with decimal - Rs dot format`() {
        val body = "Rs.318.50 debited from HDFC Bank A/C *1234 via UPI"
        val result = SmsParser.parseSms("HDFCBK", body, 123456789L)
        
        assertTrue(result.isTransaction)
        assertEquals(318.50, result.amount!!, 0.01)
    }

    @Test
    fun `extract amount with comma separator`() {
        val body = "Rs 1,25,000.00 credited to your SBI account *5678 via NEFT"
        val result = SmsParser.parseSms("SBIUPI", body, 123456789L)
        
        assertTrue(result.isTransaction)
        assertEquals(125000.00, result.amount!!, 0.01)
    }

    @Test
    fun `extract amount with rupee symbol no space`() {
        val body = "₹943 spent at ZOMATO using ICICI card *9999. Txn: 123456"
        val result = SmsParser.parseSms("ICICIB", body, 123456789L)
        
        assertTrue(result.isTransaction)
        assertEquals(943.00, result.amount!!, 0.01)
    }

    @Test
    fun `extract amount INR format`() {
        val body = "INR 2,500.75 transferred to AMIT KUMAR via IMPS from AXIS Bank account 4321"
        val result = SmsParser.parseSms("AXISBK", body, 123456789L)
        
        assertTrue(result.isTransaction)
        assertEquals(2500.75, result.amount!!, 0.01)
    }

    @Test
    fun `never return zero amount for invalid extraction`() {
        val body = "Transaction successful. Contact support for details."
        val result = SmsParser.parseSms("BANK", body, 123456789L)
        
        assertFalse(result.isTransaction)
        assertNull("Amount should be null, never 0.0", result.amount)
    }

    @Test
    fun `reject message where account is actually a mobile number`() {
        val body = "Recharge of Rs.318 done for account 9606881234. Your plan is valid till 28-Feb."
        val result = SmsParser.parseSms("AD-RECHRG", body, 123456789L)
        
        assertFalse("Should reject recharge/mobile number", result.isTransaction)
        // reason should contain promotional keywords or not be a valid transaction
    }

    @Test
    fun `extract account number exactly 4 digits without trailing digits`() {
        val body = "Rs 100 debited from A/C 1234 via UPI. Ref 1234567890"
        val result = SmsParser.parseSms("BANK", body, 123456789L)
        
        assertTrue(result.isTransaction)
        assertEquals("1234", result.accountLast4)
    }

    // ========================================
    // 🏦 BANK & ACCOUNT EXTRACTION TESTS
    // ========================================

    @Test
    fun `extract bank name from message body`() {
        val banks = mapOf(
            "HDFC" to "Rs 100 debited from HDFC Bank A/C *1234 via UPI",
            "SBI" to "Rs 200 sent from SBI account *5678 via NEFT",
            "ICICI" to "Rs 300 paid via ICICI Bank card *9999",
            "AXIS" to "Rs 400 transferred from AXIS Bank A/C 1111",
            "KOTAK" to "Rs 500 debited from KOTAK account *2222 via UPI"
        )
        
        banks.forEach { (expectedBank, message) ->
            val result = SmsParser.parseSms("BANK", message, 123456789L)
            assertTrue(result.isTransaction)
            assertEquals(expectedBank, result.bank)
        }
    }

    @Test
    fun `extract account last 4 digits - various formats`() {
        val testCases = mapOf(
            "1234" to "Rs 100 debited from A/C *1234 via UPI from HDFC Bank",
            "5678" to "Rs 200 sent from account XX5678 via NEFT from SBI",
            "9999" to "Rs 300 paid using card 9999 from ICICI Bank"
        )
        
        testCases.forEach { (expectedAccount, message) ->
            val result = SmsParser.parseSms("BANK", message, 123456789L)
            assertTrue(result.isTransaction)
            assertEquals(expectedAccount, result.accountLast4)
        }
    }

    // ========================================
    // 🧾 MERCHANT EXTRACTION TESTS
    // ========================================

    @Test
    fun `extract merchant from 'paid to' pattern`() {
        val body = "Rs 500 paid to AMAZON INDIA via UPI from HDFC Bank"
        val result = SmsParser.parseSms("HDFCBK", body, 123456789L)
        
        assertTrue(result.isTransaction)
        assertEquals("AMAZON INDIA", result.merchant)
    }

    @Test
    fun `extract merchant from 'transferred to' pattern`() {
        val body = "Rs 1000 transferred to RAMESH KUMAR via NEFT from SBI A/C *1234"
        val result = SmsParser.parseSms("SBIUPI", body, 123456789L)
        
        assertTrue(result.isTransaction)
        assertEquals("RAMESH KUMAR", result.merchant)
    }

    @Test
    fun `extract merchant from 'at' pattern`() {
        val body = "Rs 750 spent at STARBUCKS COFFEE using ICICI card *5678"
        val result = SmsParser.parseSms("ICICIB", body, 123456789L)
        
        assertTrue(result.isTransaction)
        assertEquals("STARBUCKS COFFEE", result.merchant)
    }

    @Test
    fun `fallback to sender ID for merchant`() {
        val body = "Rs 300 debited from HDFC Bank A/C *1234 via UPI. Txn: 123456"
        val result = SmsParser.parseSms("AD-ZOMATO", body, 123456789L)
        
        assertTrue(result.isTransaction)
        assertEquals("ZOMATO", result.merchant)
    }

    // ========================================
    // 📅 DATE & REFERENCE EXTRACTION TESTS
    // ========================================

    @Test
    fun `extract date from message`() {
        val testCases = mapOf(
            "25/10/2025" to "Rs 100 debited from HDFC A/C *1234 on 25/10/2025",
            "17-OCT-25" to "Rs 200 sent from SBI account *5678 on 17-OCT-25"
        )
        
        testCases.forEach { (expectedDate, message) ->
            val result = SmsParser.parseSms("BANK", message, 123456789L)
            assertTrue(result.isTransaction)
            assertEquals(expectedDate, result.date)
        }
    }

    @Test
    fun `extract reference number`() {
        val body = "Rs 500 paid to ZOMATO via UPI. Ref: 566413406309"
        val result = SmsParser.parseSms("HDFCBK", body, 123456789L)
        
        assertTrue(result.isTransaction)
        assertEquals("566413406309", result.reference)
    }

    @Test
    fun `extract UTR reference`() {
        val body = "Rs 10000 transferred via NEFT from SBI A/C *1234. UTR: SBI123456789"
        val result = SmsParser.parseSms("SBIUPI", body, 123456789L)
        
        assertTrue(result.isTransaction)
        assertEquals("SBI123456789", result.reference)
    }

    // ========================================
    // 💵 BALANCE EXTRACTION TESTS
    // ========================================

    @Test
    fun `extract available balance`() {
        val body = "Rs 200 debited from HDFC A/C *1234. Avl Bal: Rs 15,000.50"
        val result = SmsParser.parseSms("HDFCBK", body, 123456789L)
        
        assertTrue(result.isTransaction)
        assertEquals(15000.50, result.balance!!, 0.01)
    }

    @Test
    fun `extract balance without rupee symbol`() {
        val body = "Rs 500 sent via UPI from SBI A/C *5678. Available Balance: 25000"
        val result = SmsParser.parseSms("SBIUPI", body, 123456789L)
        
        assertTrue(result.isTransaction)
        assertEquals(25000.00, result.balance!!, 0.01)
    }
}
