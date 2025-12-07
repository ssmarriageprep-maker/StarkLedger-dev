package com.starklabs.moneytracker

import com.starklabs.moneytracker.domain.SmsParser
import org.junit.Test
import org.junit.Assert.*

class SmsParserTest {

    @Test
    fun parseDebitMessage_Successful() {
        val body = "Rs 500 debited from acc 1234 for STARBUCKS on 12th Dec."
        val result = SmsParser.parseSms("BANK123", body, 123456789L)

        assertNotNull(result)
        assertEquals(500.0, result!!.amount, 0.01)
        assertEquals("DEBIT", result.type)
        assertEquals("STARBUCKS", result.merchant) 
        assertEquals(1, result.accountId)
    }

    @Test
    fun parseCreditMessage_Successful() {
        val body = "Your account is credited with Rs 10,000.50 by SALARY TRANSFER."
        val result = SmsParser.parseSms("BANK123", body, 123456789L)

        assertNotNull(result)
        assertEquals(10000.50, result!!.amount, 0.01)
        assertEquals("CREDIT", result.type)
    }

    @Test
    fun parseOtpMessage_ReturnsNull() {
        val body = "Your OTP for transaction is 1234. Do not share."
        val result = SmsParser.parseSms("BANK123", body, 123456789L)

        assertNull(result)
    }
}
