package com.starklabs.moneytracker.examples

import com.starklabs.moneytracker.domain.SmsParser

/**
 * Example usage of the High-Precision SMS Parser
 * 
 * This file demonstrates how the parser handles various types of SMS messages
 */
object SmsParserExamples {

    @JvmStatic
    fun main(args: Array<String>) {
        println("=".repeat(80))
        println("HIGH-PRECISION SMS PARSER - EXAMPLES")
        println("=".repeat(80))
        println()

        // ✅ VALID TRANSACTIONS
        println("✅ VALID TRANSACTIONS")
        println("-".repeat(80))
        
        testMessage(
            "HDFCBK",
            "Rs.318.00 sent to Dreamplug Service Private Limited from HDFC Bank A/C *3263 on 25-10-2025. Ref: 566413406309"
        )
        
        testMessage(
            "SBIUPI",
            "₹1,250.50 debited from SBI A/C XX4567 for AMAZON INDIA on 17-OCT-25. UPI Ref: UPI123456"
        )
        
        testMessage(
            "ICICIB",
            "Rs 5000.00 credited to your ICICI Bank account *1234 on 01/01/2026. Salary transfer."
        )
        
        testMessage(
            "AXISBK",
            "Rs 943.00 spent at STARBUCKS COFFEE using AXIS Bank card *8899 on 15/12/2025"
        )

        println()
        
        // ❌ REJECTED - PROMOTIONAL
        println("❌ REJECTED - PROMOTIONAL MESSAGES")
        println("-".repeat(80))
        
        testMessage(
            "AIRTEL",
            "Recharge with Rs.318 and get 2GB/day + OTT benefits. Valid for 28 days. Call 9606XXX997"
        )
        
        testMessage(
            "JIOTEL",
            "Get unlimited calls + 5G data at Rs 299. Offer valid till 31st Dec. Recharge now!"
        )
        
        testMessage(
            "VICARE",
            "Your plan validity expires on 25-Dec-2025. Recharge with Rs 399 for 84 days validity."
        )

        println()
        
        // ❌ REJECTED - BILLS & OTP
        println("❌ REJECTED - BILLS, OTP & OTHER NON-TRANSACTIONS")
        println("-".repeat(80))
        
        testMessage(
            "HDFCBK",
            "Your HDFC credit card bill of Rs 5000 is due on 15-Jan-2026. Minimum due: Rs 500"
        )
        
        testMessage(
            "HDFCBK",
            "Your OTP for transaction is 123456. Valid for 10 minutes. Do not share with anyone."
        )
        
        testMessage(
            "PAYTM",
            "Get 20% cashback offer on all UPI payments above Rs 500. Valid till 31st Dec."
        )

        println()
        
        // ❌ REJECTED - INSUFFICIENT INDICATORS
        println("❌ REJECTED - INSUFFICIENT TRANSACTION INDICATORS")
        println("-".repeat(80))
        
        testMessage(
            "BANK",
            "Your balance is Rs 5000. Thank you for banking with us."
        )

        println()
        println("=".repeat(80))
        println("SUMMARY")
        println("=".repeat(80))
        println("✅ Valid transactions are parsed with full details")
        println("❌ Promotional messages are rejected with clear reasons")
        println("💰 Amount extraction is accurate (never returns 0.0)")
        println("🔍 Requires at least 2 transaction indicators")
        println("=".repeat(80))
    }

    private fun testMessage(sender: String, body: String) {
        val result = SmsParser.parseSms(sender, body, System.currentTimeMillis())
        
        println("Sender: $sender")
        println("Message: ${body.take(100)}${if (body.length > 100) "..." else ""}")
        println()
        
        if (result.isTransaction) {
            println("✅ VALID TRANSACTION")
            println("   Amount: ${result.currency} ${result.amount}")
            println("   Type: ${result.transactionType}")
            result.bank?.let { println("   Bank: $it") }
            result.accountLast4?.let { println("   Account: ****$it") }
            result.merchant?.let { println("   Merchant: $it") }
            result.date?.let { println("   Date: $it") }
            result.reference?.let { println("   Reference: $it") }
            result.reference?.let { println("   Reference: $it") }
        } else {
            println("❌ REJECTED")
            println("   Reason: ${result.reason}")
        }
        
        println()
    }
}
