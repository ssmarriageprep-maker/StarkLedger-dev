package com.starklabs.moneytracker

import com.starklabs.moneytracker.domain.SmsParser
import org.junit.Test
import org.junit.Assert.*

class SmsParserTest {

    // --- DEBIT & AMOUNT TESTS ---

    @Test
    fun parseDebit_StandardFormat() {
        val body = "Rs 500 debited from acc 1234 for STARBUCKS on 12th Dec."
        val result = SmsParser.parseSms("BANK123", body, 123456789L)
        assertNotNull(result)
        assertEquals(500.0, result!!.amount, 0.01)
        assertEquals("DEBIT", result.type)
        assertEquals("STARBUCKS", result.merchant) 
        assertEquals("1234", result.accountLast4)
    }

    @Test
    fun parseDebit_RupeeSymbol_Space() {
        val body = "₹ 1,250.00 spent on AMAZON using card ending 8899."
        val result = SmsParser.parseSms("BANK123", body, 123456789L)
        assertNotNull(result)
        assertEquals(1250.0, result!!.amount, 0.01)
        assertEquals("AMAZON", result.merchant)
        assertEquals("8899", result.accountLast4)
    }

    @Test
    fun parseDebit_RupeeSymbol_NoSpace() {
        val body = "Transaction of ₹450.50 at ZOMATO completes successfully."
        val result = SmsParser.parseSms("HDFCBK", body, 123456789L)
        assertNotNull(result)
        assertEquals(450.50, result!!.amount, 0.01)
        assertEquals("ZOMATO", result.merchant)
    }

    @Test
    fun parseDebit_INRFormat() {
        val body = "INR 2000.00 withdrawn from a/c XX4455 at ATM."
        val result = SmsParser.parseSms("SBI", body, 123456789L)
        assertNotNull(result)
        assertEquals(2000.0, result!!.amount, 0.01)
        assertEquals("DEBIT", result.type) // withdrawn is a debit keyword
        assertEquals("4455", result.accountLast4)
    }

    // --- CREDIT & INCOME TESTS ---

    @Test
    fun parseCredit_Salary() {
        val body = "Your account is credited with Rs 10,000.50 by SALARY TRANSFER."
        val result = SmsParser.parseSms("BANK123", body, 123456789L)
        assertNotNull(result)
        assertEquals(10000.50, result!!.amount, 0.01)
        assertEquals("CREDIT", result.type)
    }

    @Test
    fun parseCredit_Refund() {
        val body = "Refund of ₹ 500.00 processed for your request."
        val result = SmsParser.parseSms("AMAZON", body, 123456789L)
        assertNotNull(result)
        assertEquals("CREDIT", result.type) // refund is a credit keyword
    }

    // --- MERCHANT EXTRACTION STRATEGIES ---

    @Test
    fun parseMerchant_PaidTo() {
        val body = "Paid to UBER via UPI ref 123456. Rs 350.00 debited."
        val result = SmsParser.parseSms("UPI", body, 123456789L)
        assertEquals("UBER", result?.merchant)
    }

    @Test
    fun parseMerchant_TransferTo() {
        val body = "Transfer to RAMESH KUMAR successful. INR 5000."
        val result = SmsParser.parseSms("UPI", body, 123456789L)
        assertEquals("RAMESH KUMAR", result?.merchant)
    }

    @Test
    fun parseMerchant_DebitedFor() {
        val body = "Ac XX1234 debited for NETFLIX COM. Rs 649.00"
        val result = SmsParser.parseSms("HDFC", body, 123456789L)
        assertEquals("NETFLIX COM", result?.merchant)
    }

    @Test
    fun parseMerchant_At_Strategy() {
        val body = "Purchase of Rs 200 at MCDONALDS POS made on 12-Dec."
        val result = SmsParser.parseSms("HDFC", body, 123456789L)
        assertEquals("MCDONALDS POS", result?.merchant)
    }

    @Test
    fun parseMerchant_Fallback_SenderID() {
        // No "at", "to", or "for" clearly linking a merchant
        val body = "Rs 150.00 spent on card 1122."
        val result = SmsParser.parseSms("AD-SWIGGY", body, 123456789L)
        assertEquals("SWIGGY", result?.merchant)
    }

    // --- BALANCE TESTS ---

    @Test
    fun parseBalance_WithRupeeSymbol() {
        val body = "Tran successful. Avl Bal: ₹ 50,000.00"
        val result = SmsParser.parseSms("BANK123", body, 123456789L)
        assertNotNull(result)
        assertEquals(50000.00, result!!.balance!!, 0.01)
    }

    @Test
    fun parseBalance_Standard() {
        val body = "Sent. Avl Bal Rs 123.45"
        val result = SmsParser.parseSms("BANK", body, 123456789L)
        assertEquals(123.45, result!!.balance!!, 0.01)
    }

    // --- IGNORE CASES ---

    @Test
    fun parseOtpMessage_ReturnsNull() {
        val body = "Your OTP for transaction is 1234. Do not share."
        val result = SmsParser.parseSms("BANK123", body, 123456789L)
        assertNull(result)
    }
}
