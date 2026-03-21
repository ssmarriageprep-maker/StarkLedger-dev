package com.starklabs.moneytracker.examples

import com.starklabs.moneytracker.domain.SmsParser

/**
 * Example usage of the Next-Generation SMS Intelligence Engine
 * 
 * Demonstrates the two-phase architecture:
 * Phase 1 — Classification with confidence scoring and pattern detection
 * Phase 2 — Intelligent extraction with multi-pass strategy
 */
object SmsParserExamples {

    @JvmStatic
    fun main(args: Array<String>) {
        println("=".repeat(80))
        println("NEXT-GEN SMS INTELLIGENCE ENGINE — EXAMPLES")
        println("=".repeat(80))
        println()

        // ✅ VALID TRANSACTIONS
        println("✅ VALID TRANSACTIONS (Phase 1 → Phase 2)")
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
        
        // ✅ SMART REJECTION — passes despite keyword
        println("✅ SMART REJECTION — Transaction WITH reject keyword (should PASS)")
        println("-".repeat(80))

        testMessage(
            "HDFCBK",
            "Rs 943.00 paid for BSNL bill from HDFC A/C *1234 via UPI"
        )

        println()
        
        // ❌ REJECTED - PROMOTIONAL
        println("❌ REJECTED — PROMOTIONAL MESSAGES")
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
        println("❌ REJECTED — BILLS, OTP & OTHER")
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

        // ✅ MULTI-LINE SMS
        println("✅ MULTI-LINE SMS (fragmented messages)")
        println("-".repeat(80))

        testMessage(
            "HDFCBK",
            """
            Sent Rs.2000.00
            From HDFC Bank A/C *3263
            To BHAGINI HOSPITALITIES PVT LTD SUITES
            On 31/12/25
            Ref 573106200893
            """.trimIndent()
        )

        println()
        println("=".repeat(80))
        println("SUMMARY")
        println("=".repeat(80))
        println("✅ Two-phase architecture: classify first, then extract")
        println("🧠 Smart rejection: reject keywords + signal check (not hard)")
        println("📊 Structured confidence scoring (0-100)")
        println("🔍 Pattern detection with enum (UPI_SENT, BANK_DEBIT, etc.)")
        println("💰 Multi-pass amount extraction (strict → relaxed → context)")
        println("🔐 SHA-256 message hash for duplicate detection")
        println("=".repeat(80))
    }

    private fun testMessage(sender: String, body: String) {
        val result = SmsParser.parseSms(sender, body, System.currentTimeMillis())
        
        println("Sender: $sender")
        println("Message: ${body.take(100)}${if (body.length > 100) "..." else ""}")
        println()
        
        // Phase 1 output
        println("   📊 Category: ${result.category ?: "—"}")
        println("   📊 Confidence: ${result.confidence}%")
        println("   📊 Pattern: ${result.patternUsed}")
        println("   🔐 Hash: ${result.messageHash?.take(16)}...")

        if (result.isTransaction) {
            println("   ✅ VALID TRANSACTION")
            println("   💰 Amount: ${result.currency} ${result.amount}")
            println("   🔄 Type: ${result.transactionType}")
            result.bank?.let { println("   🏦 Bank: $it") }
            result.accountLast4?.let { println("   💳 Account: ****$it") }
            result.merchant?.let { println("   👤 Merchant: $it") }
            result.date?.let { println("   📅 Date: $it") }
            result.reference?.let { println("   🧾 Reference: $it") }
            result.balance?.let { println("   💵 Balance: INR $it") }
        } else {
            println("   ❌ REJECTED")
            println("   📝 Reason: ${result.reason}")
        }
        
        println()
    }
}
