package com.starklabs.moneytracker

import com.starklabs.moneytracker.domain.SmsParser
import org.junit.Test
import org.junit.Assert.*

/**
 * Extended test suite for High-Precision SMS Parser using grounded 2024-2025 bank formats
 */
class SmsParserExtendedTest {

    @Test
    fun `NetBanking Debit from SMSGatewayCenter`() {
        val body = "Dear Customer, Rs.248,759.00 is debited from A/c XXXX6791 for BillPay/Credit Card payment via Example Bank NetBanking. Call XXXXXXXX161XXX if txn not done by you."
        val result = SmsParser.parseSms("HDFCBANK", body, 123456789L)

        assertTrue("Should be a valid transaction", result.isTransaction)
        assertEquals(248759.00, result.amount!!, 0.01)
        assertEquals("debit", result.transactionType)
        assertEquals("6791", result.accountLast4)
        // Merchant should be BillPay/Credit Card payment
        assertEquals("BillPay/Credit Card payment", result.merchant)
    }

    @Test
    fun `Account Credit from SMSGatewayCenter`() {
        val body = "UPDATE: Your A/c XX6791 credited with INR 15,160.00 on 10-12-2025 by A/c linked to mobile no XX79XX(IMPS Ref No. XX114XXXXX393) Available bal: INR 2,088,505.04"
        val result = SmsParser.parseSms("SBIUPI", body, 123456789L)

        assertTrue(result.isTransaction)
        assertEquals(15160.00, result.amount!!, 0.01)
        assertEquals("credit", result.transactionType)
        assertEquals("6791", result.accountLast4)
        assertEquals(2088505.04, result.balance!!, 0.01)
    }

    @Test
    fun `NEFT IMPS Payment from SMSGatewayCenter`() {
        val body = "Dear Customer, You have made a payment of Rs. 46000 using NEFT via IMPS from your Account XXXXXXXX0126 with reference number XXX387XXX on December 10, 2025 at 14:15."
        val result = SmsParser.parseSms("BANK", body, 123456789L)

        assertTrue(result.isTransaction)
        assertEquals(46000.0, result.amount!!, 0.1)
        assertEquals("debit", result.transactionType)
        assertEquals("0126", result.accountLast4)
    }

    @Test
    fun `IMPS Debit from SMSGatewayCenter`() {
        val body = "Acct XX126 debited with INR 46,000.00 on 10-Dec-2025 & Acct XX791 credited. IMPS: XXX410XX. Call XX0026XX for dispute or SMS BLOCK 126 to XXX5676XXX."
        val result = SmsParser.parseSms("BANK", body, 123456789L)

        assertTrue(result.isTransaction)
        assertEquals(46000.00, result.amount!!, 0.01)
        assertEquals("debit", result.transactionType)
        assertEquals("126", result.accountLast4)
    }

    @Test
    fun `Card Credit from SMSGatewayCenter`() {
        val body = "Dear bank cardmember, Payment of Rs 248759 was credited to your card ending 12344 on 10/Dec/2025."
        val result = SmsParser.parseSms("BANK", body, 123456789L)

        assertTrue(result.isTransaction)
        assertEquals(248759.00, result.amount!!, 0.01)
        assertEquals("credit", result.transactionType)
        assertEquals("2344", result.accountLast4) // last 4 of 12344 is 2344
    }

    @Test
    fun `Card Spend from SMSGatewayCenter`() {
        val body = "Alert: You've spent INR 555.00 on your Delhi Example Bank card **91234 at BD JIO MONEY on 10/12/2025 at 11:07AM IST."
        val result = SmsParser.parseSms("BANK", body, 123456789L)

        assertTrue(result.isTransaction)
        assertEquals(555.00, result.amount!!, 0.01)
        assertEquals("debit", result.transactionType)
        assertEquals("1234", result.accountLast4) // last 4 of 91234
        assertEquals("BD JIO MONEY", result.merchant)
    }

    @Test
    fun `ATM Debit from 2Factor`() {
        val body = "Alert: ₹5,000 debited from A/C ****2341 at ATM on 15-Jan-25 14:32. Available balance: ₹45,230."
        val result = SmsParser.parseSms("SBIUPI", body, 123456789L)

        assertTrue(result.isTransaction)
        assertEquals(5000.0, result.amount!!, 0.1)
        assertEquals("debit", result.transactionType)
        assertEquals("2341", result.accountLast4)
        assertEquals(45230.0, result.balance!!, 0.1)
        assertEquals("ATM", result.merchant)
    }

    @Test
    fun `HDFC Transaction from D7 Networks`() {
        val body = "HDFCBANK: ₹2,000 debited from A/C ****4321."
        val result = SmsParser.parseSms("HDFCBANK", body, 123456789L)

        assertTrue(result.isTransaction)
        assertEquals(2000.0, result.amount!!, 0.1)
        assertEquals("debit", result.transactionType)
        assertEquals("4321", result.accountLast4)
        assertEquals("HDFC", result.bank)
    }

    @Test
    fun `Axis Transaction from Medium`() {
        val body = "Rs.5000 debited from A/C 1234 at Amazon."
        val result = SmsParser.parseSms("AXISBANK", body, 123456789L)

        assertTrue(result.isTransaction)
        assertEquals(5000.0, result.amount!!, 0.1)
        assertEquals("debit", result.transactionType)
        assertEquals("1234", result.accountLast4)
        assertEquals("Amazon", result.merchant)
        assertEquals("AXIS", result.bank)
    }

    @Test
    fun `Kotak Transaction from Kaggle`() {
        val body = "Sent Rs.10000.00 from Kotak Bank"
        val result = SmsParser.parseSms("KOTAKB", body, 123456789L)

        assertTrue(result.isTransaction)
        assertEquals(10000.0, result.amount!!, 0.1)
        assertEquals("debit", result.transactionType)
        assertEquals("KOTAK", result.bank)
    }

    @Test
    fun `Rejection Case from SMSGatewayCenter`() {
        val body = "Dear Customer, Min payment Rs.175.00/total payment Rs.3499.00 for Hyderabad Example Card **********91000 is due by 17-12-2025. Please ignore if already paid."
        val result = SmsParser.parseSms("BANK", body, 123456789L)

        assertFalse("Should be rejected as bill due", result.isTransaction)
    }
}
