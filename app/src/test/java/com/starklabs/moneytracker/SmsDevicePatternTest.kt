package com.starklabs.moneytracker

import com.starklabs.moneytracker.domain.SmsParser
import com.starklabs.moneytracker.domain.SmsPattern
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests grounded in REAL SMS messages pulled from user's device (Jan 2026).
 * Each test case is a verbatim or minimally cleaned message from the SMS inbox.
 */
class SmsDevicePatternTest {

    // ════════════════════════════════════════════════════════════════════════
    //  PATTERN 1: HDFC Multi-Line UPI Send
    //  "Sent Rs.933.00 From HDFC Bank A/C *3263 To Dreamplug Service Private On 13/01/26 Ref 637902358734"
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `HDFC UPI send - multi-line format`() {
        val body = """
            Sent Rs.933.00
            From HDFC Bank A/C *3263
            To Dreamplug Service Private
            On 13/01/26
            Ref 637902358734
            Not You?
            Call 18002586161/SMS BLOCK UPI to 7308080808
        """.trimIndent()
        val result = SmsParser.parseSms("VM-HDFCBK-S", body, 0L)

        assertTrue(result.isTransaction)
        assertEquals(933.00, result.amount!!, 0.01)
        assertEquals("debit", result.transactionType)
        assertEquals("HDFC", result.bank)
        assertEquals("3263", result.accountLast4)
        assertEquals("Dreamplug Service Private", result.merchant)
        assertEquals("637902358734", result.reference)
        assertNotNull(result.messageHash)
    }

    @Test
    fun `HDFC UPI send - to CRED Club`() {
        val body = """
            Sent Rs.11489.00
            From HDFC Bank A/C *3263
            To CRED Club
            On 10/01/26
            Ref 637614866173
            Not You?
            Call 18002586161/SMS BLOCK UPI to 7308080808
        """.trimIndent()
        val result = SmsParser.parseSms("AD-HDFCBK-S", body, 0L)

        assertTrue(result.isTransaction)
        assertEquals(11489.00, result.amount!!, 0.01)
        assertEquals("CRED Club", result.merchant)
    }

    @Test
    fun `HDFC UPI send - to Amazon Pay Groceries`() {
        val body = """
            Sent Rs.223.00
            From HDFC Bank A/C *3263
            To Amazon Pay Groceries
            On 08/01/26
            Ref 637409658621
            Not You?
            Call 18002586161/SMS BLOCK UPI to 7308080808
        """.trimIndent()
        val result = SmsParser.parseSms("VM-HDFCBK-T", body, 0L)

        assertTrue(result.isTransaction)
        assertEquals(223.00, result.amount!!, 0.01)
        assertEquals("Amazon Pay Groceries", result.merchant)
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PATTERN 2: HDFC Credit Card Spend
    //  "Spent Rs.105 On HDFC Bank Card 6763 At ..MR DIY_ On 2026-01-10:22:17:59"
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `HDFC card spend - MR DIY`() {
        val body = "Spent Rs.105 On HDFC Bank Card 6763 At ..MR DIY_ On 2026-01-10:22:17:59.Not You? To Block+Reissue Call 18002586161/SMS BLOCK CC 6763 to 7308080808"
        val result = SmsParser.parseSms("JD-HDFCBK-S", body, 0L)

        assertTrue(result.isTransaction)
        assertEquals(105.0, result.amount!!, 0.01)
        assertEquals("debit", result.transactionType)
        assertEquals("HDFC", result.bank)
        assertEquals("6763", result.accountLast4)
        assertEquals(SmsPattern.CARD_SPEND, result.patternUsed)
    }

    @Test
    fun `HDFC card spend - LULU INTERNATION`() {
        val body = "Spent Rs.2360 On HDFC Bank Card 6763 At ..LULU INTERNATION_ On 2026-01-10:21:23:02.Not You? To Block+Reissue Call 18002586161/SMS BLOCK CC 6763 to 7308080808"
        val result = SmsParser.parseSms("AX-HDFCBK-S", body, 0L)

        assertTrue(result.isTransaction)
        assertEquals(2360.0, result.amount!!, 0.01)
        assertEquals("6763", result.accountLast4)
    }

    @Test
    fun `HDFC card spend - GOOGLECLOUD`() {
        val body = "Spent Rs.2 On HDFC Bank Card 6763 At GOOGLECLOUD On 2025-12-09:18:15:23.Not You? To Block+Reissue Call 18002586161/SMS BLOCK CC 6763 to 7308080808"
        val result = SmsParser.parseSms("AD-HDFCBK-S", body, 0L)

        assertTrue(result.isTransaction)
        assertEquals(2.0, result.amount!!, 0.01)
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PATTERN 3: HDFC Credit Alert (UPI credit)
    //  "Credit Alert! Rs.2000.00 credited to HDFC Bank A/c XX3263 on 10-01-26 from VPA ..."
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `HDFC Credit Alert`() {
        val body = "Credit Alert! Rs.2000.00 credited to HDFC Bank A/c XX3263 on 10-01-26 from VPA dhanalakshmig1522-1@okhdfcbank (UPI 116963242466)"
        val result = SmsParser.parseSms("JD-HDFCBK-S", body, 0L)

        assertTrue(result.isTransaction)
        assertEquals(2000.00, result.amount!!, 0.01)
        assertEquals("credit", result.transactionType)
        assertEquals("HDFC", result.bank)
        assertEquals("3263", result.accountLast4)
    }

    @Test
    fun `HDFC Credit Alert - large amount`() {
        val body = "Credit Alert! Rs.53000.00 credited to HDFC Bank A/c XX3263 on 10-01-26 from VPA sumantra5597@okhdfcbank (UPI 116960379024)"
        val result = SmsParser.parseSms("VM-HDFCBK-S", body, 0L)

        assertTrue(result.isTransaction)
        assertEquals(53000.00, result.amount!!, 0.01)
        assertEquals("credit", result.transactionType)
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PATTERN 4: HDFC UPDATE Debit (Autopay / ACH / EMI)
    //  "UPDATE: INR 7,316.43 debited from HDFC Bank XX3263 on 10-JAN-26. Info: ..."
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `HDFC UPDATE - autopay debit`() {
        val body = "UPDATE: INR 7,316.43 debited from HDFC Bank XX3263 on 10-JAN-26. Info: CC 000517635XXXXXX7182 Autopay SI-TAD. Avl bal:INR 7,590.77"
        val result = SmsParser.parseSms("VD-HDFCBK-S", body, 0L)

        assertTrue(result.isTransaction)
        assertEquals(7316.43, result.amount!!, 0.01)
        assertEquals("debit", result.transactionType)
        assertEquals("HDFC", result.bank)
        assertEquals("3263", result.accountLast4)
        assertEquals(7590.77, result.balance!!, 0.01)
        assertEquals(SmsPattern.AUTOPAY_DEBIT, result.patternUsed)
    }

    @Test
    fun `HDFC UPDATE - EMI debit`() {
        val body = "UPDATE: INR 15,613.00 debited from HDFC Bank XX3263 on 07-JAN-26. Info: EMI 465768092 Chq S4657680920091 0126465768092. Avl bal:INR 35,436.20"
        val result = SmsParser.parseSms("VM-HDFCBK-S", body, 0L)

        assertTrue(result.isTransaction)
        assertEquals(15613.00, result.amount!!, 0.01)
        assertEquals(35436.20, result.balance!!, 0.01)
    }

    @Test
    fun `HDFC UPDATE - ACH debit`() {
        val body = "UPDATE: INR 11,045.00 debited from HDFC Bank XX3263 on 29-DEC-25. Info: ACH D- UNION BANK OF INDIA-TCP3057212744. Avl bal:INR 85,588.20"
        val result = SmsParser.parseSms("VM-HDFCBK-S", body, 0L)

        assertTrue(result.isTransaction)
        assertEquals(11045.00, result.amount!!, 0.01)
        assertEquals(SmsPattern.ACH_DEBIT, result.patternUsed)
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PATTERN 5: HDFC Cardmember Payment Received
    //  "DEAR HDFCBANK CARDMEMBER, PAYMENT OF Rs. 11493.00 RECEIVED..."
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `HDFC Cardmember payment received`() {
        val body = "DEAR HDFCBANK CARDMEMBER, PAYMENT OF Rs. 11493.00 RECEIVED TOWARDS YOUR CREDIT CARD ENDING WITH 6763 ON 10-1-2026.YOUR AVAILABLE LIMIT IS RS. 254617.04"
        val result = SmsParser.parseSms("AD-HDFCBK-S", body, 0L)

        assertTrue(result.isTransaction)
        assertEquals(11493.00, result.amount!!, 0.01)
        assertEquals("credit", result.transactionType)
        assertEquals("HDFC", result.bank)
        assertEquals(SmsPattern.CARD_PAYMENT_RECEIVED, result.patternUsed)
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PATTERN 6: HDFC Cardmember Online Payment
    //  "HDFC Bank Cardmember, Online Payment of Rs.11493 vide Ref# ..."
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `HDFC Cardmember online payment credited to card`() {
        val body = "HDFC Bank Cardmember, Online Payment of Rs.11493 vide Ref# 010120531ZPbP6y was credited to your card ending 6763 On 10/JAN/2026_value Date 10/JAN/2026"
        val result = SmsParser.parseSms("VM-HDFCBK-S", body, 0L)

        assertTrue(result.isTransaction)
        assertEquals(11493.0, result.amount!!, 0.01)
        assertEquals("credit", result.transactionType)
        assertEquals("HDFC", result.bank)
        assertEquals("010120531ZPbP6y", result.reference)
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PATTERN 7: Union Bank Debit/Credit
    //  "Your SB A/c *3868 Debited for Rs:236.00 on 30-12-2025 05:40:37 by BRANCH"
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `Union Bank SB debit`() {
        val body = "Your SB A/c *3868 Debited for Rs:236.00 on 30-12-2025 05:40:37 by BRANCH Avl Bal Rs:5091.46.If not you, Call 1800222243 -Union Bank of India"
        val result = SmsParser.parseSms("JM-UNIONB-S", body, 0L)

        assertTrue("Union Bank debit should parse", result.isTransaction)
        assertEquals(236.00, result.amount!!, 0.01)
        assertEquals("debit", result.transactionType)
        assertEquals("3868", result.accountLast4)
        assertEquals("UNION", result.bank)
    }

    @Test
    fun `Union Bank loan credit`() {
        val body = "Your Loan A/c *0062 Credited for Rs:11045.00 on 29-12-2025 17:01:13 by BRANCH Avl Bal Rs:0.00.Don't share Card PIN/CVV -Union Bank of India"
        val result = SmsParser.parseSms("JM-UNIONB-S", body, 0L)

        assertTrue(result.isTransaction)
        assertEquals(11045.00, result.amount!!, 0.01)
        assertEquals("credit", result.transactionType)
    }

    @Test
    fun `Union Bank mobile banking debit`() {
        val body = "A/c *3868 Debited for Rs:5300.00 on 21-12-2025 16:08:09 by Mob Bk ref no 572117578097 Avl Bal Rs:21869.77.If not you, Call 1800222243 -Union Bank of India"
        val result = SmsParser.parseSms("JD-UNIONB-S", body, 0L)

        assertTrue(result.isTransaction)
        assertEquals(5300.00, result.amount!!, 0.01)
        assertEquals("debit", result.transactionType)
        assertEquals("3868", result.accountLast4)
    }

    @Test
    fun `Union Bank mobile credit`() {
        val body = "A/c *3868 Credited for Rs:10000.00 on 21-12-2025 21:27:43 by Mob Bk ref no 588524975994 Avl Bal Rs:26129.77.Never Share OTP/PIN/CVV-Union Bank of India"
        val result = SmsParser.parseSms("VM-UNIONB-S", body, 0L)

        assertTrue(result.isTransaction)
        assertEquals(10000.00, result.amount!!, 0.01)
        assertEquals("credit", result.transactionType)
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PATTERN 8: Pluxee Meal Wallet
    //  "Rs. 850.64 spent from Pluxee Meal wallet, card no.xx1322 at ETERNAL LIM."
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `Pluxee meal wallet spend`() {
        val body = "Rs. 269.90 spent from Pluxee  Meal wallet, card no.xx1322 on 11-01-2026 22:06:41 at ETERNAL LIM . Avl bal Rs.1244.01. Not you call 18002106919"
        val result = SmsParser.parseSms("VM-Pluxee-S", body, 0L)

        assertTrue("Pluxee spend should parse", result.isTransaction)
        assertEquals(269.90, result.amount!!, 0.01)
        assertEquals("debit", result.transactionType)
        assertEquals(SmsPattern.WALLET_PAYMENT, result.patternUsed)
    }

    @Test
    fun `Pluxee convenience fee deduction`() {
        val body = "Rs. 2.00 deducted from your Pluxee Card xxxx1322 towards ONLINE CONVENIENCE FEE. Pluxee"
        val result = SmsParser.parseSms("VD-Pluxee-S", body, 0L)

        assertTrue(result.isTransaction)
        assertEquals(2.00, result.amount!!, 0.01)
        assertEquals("debit", result.transactionType)
    }

    @Test
    fun `Pluxee card credit`() {
        val body = "Your Pluxee Card has been successfully credited with Rs.2083 towards  Meal Wallet on Thu Jan 01 2026 01:10:07. Your current Meal Wallet balance is Rs.2366.55."
        val result = SmsParser.parseSms("VD-Pluxee-S", body, 0L)

        assertTrue(result.isTransaction)
        assertEquals(2083.0, result.amount!!, 0.01)
        assertEquals("credit", result.transactionType)
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PATTERN 9: HDFC ACH / UMRN Deduction
    //  "INR 11045.00 deducted from HDFC Bank A/C No 3263 towards UNION BANK OF INDIA UMRN: ..."
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `HDFC ACH UMRN deduction`() {
        val body = "INR 11045.00 deducted from HDFC Bank A/C No 3263 towards UNION BANK OF INDIA UMRN: HDFC7022907251024195"
        val result = SmsParser.parseSms("AD-HDFCBK-S", body, 0L)

        assertTrue(result.isTransaction)
        assertEquals(11045.0, result.amount!!, 0.01)
        assertEquals("debit", result.transactionType)
        assertEquals("HDFC", result.bank)
        assertEquals(SmsPattern.ACH_DEBIT, result.patternUsed)
    }

    // ════════════════════════════════════════════════════════════════════════
    //  REJECTION: Real promotional messages from device
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `reject HDFC Personal Loan offer`() {
        val body = "Get Cash Anytime, Anywhere: HDFC Bank Personal Loan of Rs. 8.15 Lacs at exclusive rates. No Paperwork. Click: https://hdfcbk.io/HDFCBN/MJPZRW2M5ASi T&C"
        val result = SmsParser.parseSms("AX-HDFCBN-P", body, 0L)
        assertFalse("Loan offer should be rejected", result.isTransaction)
    }

    @Test
    fun `reject HDFC EMI conversion offer`() {
        val body = "HDFC Bank Credit Card xx6763 Update: Convert bill of Rs. 50348 to an EMI of Rs. 4544 p.m & ease this month: https://hdfcbk.io/HDFCBN/HQ54RW2M5AqA T&C"
        val result = SmsParser.parseSms("AX-HDFCBN-P", body, 0L)
        assertFalse("EMI conversion offer should be rejected", result.isTransaction)
    }

    @Test
    fun `reject Airtel recharge reminder with validity`() {
        val body = "Do you know? With Airtel if you Recharge before expiry, your validity will get added in your current pack, So don't wait for last day to recharge. Try now u.airtel.in/rch3"
        val result = SmsParser.parseSms("AX-ARWINF-P", body, 0L)
        assertFalse("Airtel promo should be rejected", result.isTransaction)
    }

    @Test
    fun `reject Airtel OTT plus data offer`() {
        val body = "Watching OTTs or streaming music? Rs.50 more gives you both - 20+ OTTs, 2GB/day & Apple Music (up to 6 months) at just Rs.349. Recharge your Airtel Prepaid now i.airtel.in/rc349grid"
        val result = SmsParser.parseSms("AX-AIRTEL-P", body, 0L)
        assertFalse("Airtel OTT offer should be rejected", result.isTransaction)
    }

    @Test
    fun `reject BSNL invoice`() {
        val body = "Dear Customer, BSNL Invoice No.STNR26013115581 dtd 02.01.2026 against Telephone/FTTH No.04257-299832 (A/c no.9041179907) for Rs.943/- is due by 19.01.2026. So, please pay the dues at the earliest. Pls ignore if already paid."
        val result = SmsParser.parseSms("BT-BSNLTN-S", body, 0L)
        assertFalse("BSNL invoice should be rejected", result.isTransaction)
    }

    @Test
    fun `reject HDFC autopay reminder`() {
        val body = "AutoPay Reminder: Rs.7316.43 will be deducted from HDFC Bank A/c 3263 on 10/JAN/2026 for Credit Card 7182 bill. For Query: Ask EVA resu.io/ZPCKZ7"
        val result = SmsParser.parseSms("VM-HDFCBN-S", body, 0L)
        assertFalse("Autopay reminder (bill) should be rejected", result.isTransaction)
    }
}
