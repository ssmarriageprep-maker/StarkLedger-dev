package com.starklabs.moneytracker

import com.starklabs.moneytracker.domain.SmsParser
import org.junit.Test
import org.junit.Assert.*

/**
 * Comprehensive test suite for High-Precision SMS Parser
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
        val body = "₹1,250.50 debited from SBI A/C XX4567 to AMAZON INDIA on 17-OCT-25. UPI Ref: UPI123456"
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
        val body = "Rs 5,000.00 credited to your ICICI Bank account *1234 on 01/01/2026. Salary transfer."
        val result = SmsParser.parseSms("ICICIB", body, 123456789L)
        
        assertTrue(result.isTransaction)
        assertEquals(5000.00, result.amount!!, 0.01)
        assertEquals("credit", result.transactionType)
        assertEquals("ICICI", result.bank)
        assertEquals("1234", result.accountLast4)
    }

    @Test
    fun `valid UPI payment with merchant`() {
        val body = "INR 450.00 paid to ZOMATO via UPI from HDFC A/C *1234. Txn ID: 123456789"
        val result = SmsParser.parseSms("UPIHDFC", body, 123456789L)
        
        assertTrue(result.isTransaction)
        assertEquals(450.00, result.amount!!, 0.01)
        assertEquals("debit", result.transactionType)
        assertEquals("ZOMATO", result.merchant)
        assertEquals("123456789", result.reference)
    }

    @Test
    fun `valid card purchase at merchant`() {
        val body = "Rs 943.00 Paid to STARBUCKS COFFEE from AXIS A/C *8899 on 15/12/2025"
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
        val body = "Rs 10,000 transferred to RAMESH KUMAR via NEFT from KOTAK A/C *5678. UTR: KOTAK123456"
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
        val body = "Rs 200.00 debited from SBI A/C *3344 to SWIGGY. Avl Bal: Rs 15,000.50"
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
        assertEquals("Promotional or non-bank message", result.reason)
    }

    @Test
    fun `reject Jio offer message`() {
        val body = "Get unlimited calls + 5G data at Rs 299. Offer valid till 31st Dec. Recharge now!"
        val result = SmsParser.parseSms("JIOTEL", body, 123456789L)
        
        assertFalse(result.isTransaction)
        assertEquals("Promotional or non-bank message", result.reason)
    }

    @Test
    fun `reject Vodafone validity message`() {
        val body = "Your plan validity expires on 25-Dec-2025. Recharge with Rs 399 for 84 days validity."
        val result = SmsParser.parseSms("VICARE", body, 123456789L)
        
        assertFalse(result.isTransaction)
        assertEquals("Promotional or non-bank message", result.reason)
    }

    @Test
    fun `reject OTT subscription offer`() {
        val body = "Subscribe to Xstream Premium for Rs 149/month. Get Disney+ Hotstar free!"
        val result = SmsParser.parseSms("AIRTEL", body, 123456789L)
        
        assertFalse(result.isTransaction)
        assertEquals("Promotional or non-bank message", result.reason)
    }

    // ========================================
    // ❌ REJECTION TESTS - BILLS & REMINDERS
    // ========================================

    @Test
    fun `reject bill due reminder`() {
        val body = "Your HDFC credit card bill of Rs 5,000 is due on 15-Jan-2026. Minimum due: Rs 500"
        val result = SmsParser.parseSms("HDFCBK", body, 123456789L)
        
        assertFalse(result.isTransaction)
        assertEquals("Promotional or non-bank message", result.reason)
    }

    @Test
    fun `reject statement generated message`() {
        val body = "Your ICICI Bank statement for Dec 2025 is generated. Total spends: Rs 25,000. Download now."
        val result = SmsParser.parseSms("ICICIB", body, 123456789L)
        
        assertFalse(result.isTransaction)
        assertEquals("Promotional or non-bank message", result.reason)
    }

    // ========================================
    // ❌ REJECTION TESTS - OTP & VERIFICATION
    // ========================================

    @Test
    fun `reject OTP message`() {
        val body = "Your OTP for transaction is 123456. Valid for 10 minutes. Do not share with anyone."
        val result = SmsParser.parseSms("HDFCBK", body, 123456789L)
        
        assertFalse(result.isTransaction)
        assertEquals("Promotional or non-bank message", result.reason)
    }

    @Test
    fun `reject verification code`() {
        val body = "Your verification code is 9876. Use this to complete your registration."
        val result = SmsParser.parseSms("VERIFY", body, 123456789L)
        
        assertFalse(result.isTransaction)
        assertEquals("Promotional or non-bank message", result.reason)
    }

    // ========================================
    // ❌ REJECTION TESTS - MARKETING
    // ========================================

    @Test
    fun `reject cashback offer`() {
        val body = "Get 20% cashback offer on all UPI payments above Rs 500. Valid till 31st Dec."
        val result = SmsParser.parseSms("PAYTM", body, 123456789L)
        
        assertFalse(result.isTransaction)
        assertEquals("Promotional or non-bank message", result.reason)
    }

    @Test
    fun `reject loan offer`() {
        val body = "Pre-approved loan offer of Rs 5,00,000 at 10% interest. Apply now!"
        val result = SmsParser.parseSms("HDFCBK", body, 123456789L)
        
        assertFalse(result.isTransaction)
        assertEquals("Promotional or non-bank message", result.reason)
    }

    // ========================================
    // ❌ REJECTION TESTS - INSUFFICIENT INDICATORS
    // ========================================

    @Test
    fun `reject message with only amount, no transaction indicators`() {
        val body = "Your balance is Rs 5,000. Thank you for banking with us."
        val result = SmsParser.parseSms("BANK", body, 123456789L)
        
        assertFalse(result.isTransaction)
        assertEquals("Insufficient transaction confidence", result.reason)
    }

    // ========================================
    // 💰 AMOUNT EXTRACTION TESTS
    // ========================================

    @Test
    fun `extract amount with decimal - Rs dot format`() {
        val body = "Rs.318.50 paid to HDFC A/C *1234 via UPI"
        val result = SmsParser.parseSms("HDFCBK", body, 123456789L)
        
        assertTrue(result.isTransaction)
        assertEquals(318.50, result.amount!!, 0.01)
    }

    @Test
    fun `extract amount with comma separator`() {
        // Use international comma formatting for the mandatory regex
        val body = "Rs 125,000.00 credited to your SBI account XX5678 via NEFT"
        val result = SmsParser.parseSms("SBIUPI", body, 123456789L)
        
        assertTrue(result.isTransaction)
        assertEquals(125000.00, result.amount!!, 0.01)
    }

    @Test
    fun `extract amount INR format`() {
        val body = "INR 2,500.75 Sent to AMIT KUMAR via IMPS from AXIS A/C *4321"
        val result = SmsParser.parseSms("AXISBK", body, 123456789L)
        
        assertTrue(result.isTransaction)
        assertEquals(2500.75, result.amount!!, 0.01)
    }

    // ========================================
    // 🏦 BANK & ACCOUNT EXTRACTION TESTS
    // ========================================

    @Test
    fun `extract bank name from message body`() {
        val banks = mapOf(
            "HDFC" to "Rs 100 paid to ZOMATO from HDFC Bank A/C *1234 via UPI",
            "SBI" to "Rs 200 Sent to RAMESH from SBI account XX5678 via NEFT",
            "ICICI" to "Rs 300 debited from ICICI Bank A/C *9999",
            "AXIS" to "Rs 400 transferred to SWIGGY from AXIS Bank A/C *1111",
            "KOTAK" to "Rs 500 paid to MERCHANT from KOTAK account *2222 via UPI"
        )
        
        banks.forEach { (expectedBank, message) ->
            val result = SmsParser.parseSms("BANK", message, 123456789L)
            assertTrue(message, result.isTransaction)
            assertEquals(expectedBank, result.bank)
        }
    }

    @Test
    fun `extract account last 4 digits - various formats`() {
        val testCases = mapOf(
            "1234" to "Rs 100 paid to ZOMATO from A/C *1234 via UPI from HDFC Bank",
            "5678" to "Rs 200 Sent to RAMESH from account XX5678 via NEFT from SBI",
            "9999" to "Rs 300 debited from ICICI A/C XX9999"
        )
        
        testCases.forEach { (expectedAccount, message) ->
            val result = SmsParser.parseSms("BANK", message, 123456789L)
            assertTrue(message, result.isTransaction)
            assertEquals(expectedAccount, result.accountLast4)
        }
    }

    // ========================================
    // 🧾 MERCHANT EXTRACTION TESTS
    // ========================================

    @Test
    fun `extract merchant from 'Paid to' pattern`() {
        val body = "Rs 500 Paid to AMAZON INDIA via UPI from HDFC A/C *1234"
        val result = SmsParser.parseSms("HDFCBK", body, 123456789L)
        
        assertTrue(result.isTransaction)
        assertEquals("AMAZON INDIA", result.merchant)
    }

    @Test
    fun `extract merchant from 'Transferred to' pattern`() {
        val body = "Rs 1,000 Transferred to RAMESH KUMAR via NEFT from SBI A/C XX1234"
        val result = SmsParser.parseSms("SBIUPI", body, 123456789L)
        
        assertTrue(result.isTransaction)
        assertEquals("RAMESH KUMAR", result.merchant)
    }

    @Test
    fun `reject merchant that looks like a phone number`() {
        val body = "Paid Rs 52.00 to 7308080808 via UPI from HDFC A/C *1234"
        val result = SmsParser.parseSms("BANK", body, 123456789L)

        assertTrue(result.isTransaction)
        assertNull(result.merchant)
    }

    // ========================================
    // 📅 DATE & REFERENCE EXTRACTION TESTS
    // ========================================

    @Test
    fun `extract date from message`() {
        val testCases = mapOf(
            "25/10/2025" to "Rs 100 debited from HDFC A/C *1234 on 25/10/2025",
            "17-OCT-25" to "Rs 200 Sent to FRIEND from SBI account XX5678 on 17-OCT-25"
        )
        
        testCases.forEach { (expectedDate, message) ->
            val result = SmsParser.parseSms("BANK", message, 123456789L)
            assertTrue(message, result.isTransaction)
            assertEquals(expectedDate, result.date)
        }
    }

    @Test
    fun `extract reference number`() {
        val body = "Rs 500 Paid to ZOMATO via UPI from HDFC A/C *1234. Ref:566413406309"
        val result = SmsParser.parseSms("HDFCBK", body, 123456789L)
        
        assertTrue(result.isTransaction)
        assertEquals("566413406309", result.reference)
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
    fun `extract merchant with newline before 'On' delimiter`() {
        val body = """
            Sent Rs.2000.00
            From HDFC Bank A/C *3263
            To BHAGINI HOSPITALITIES PVT LTD SUITES
            On 31/12/25
            Ref 573106200893
        """.trimIndent()
        val result = SmsParser.parseSms("HDFCBK", body, 123456789L)

        assertTrue(result.isTransaction)
        assertEquals("BHAGINI HOSPITALITIES PVT LTD SUITES", result.merchant)
    }

    @Test
    fun `extract merchant with ON 30 pattern`() {
        val body = """
            Sent Rs.500.00
            From HDFC Bank A/C *3263
            To IDC KITCHEN PRIVATE LIMITED MALL OF ASIA
            ON 30/12/25
            Ref 573106200893
        """.trimIndent()
        val result = SmsParser.parseSms("HDFCBK", body, 123456789L)

        assertTrue(result.isTransaction)
        assertEquals("IDC KITCHEN PRIVATE LIMITED MALL OF ASIA", result.merchant)
    }

    @Test
    fun `extract amount correctly for Rs dot 2000`() {
        val body = "Sent Rs.2000.00 from HDFC Bank A/C *3263"
        val result = SmsParser.parseSms("HDFCBK", body, 123456789L)

        assertTrue(result.isTransaction)
        assertEquals(2000.00, result.amount!!, 0.01)
    }
}
