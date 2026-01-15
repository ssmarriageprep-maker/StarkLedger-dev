package com.starklabs.moneytracker.domain

import java.util.regex.Pattern

/**
 * High-Precision Financial SMS Parser for StarkLedger
 * 
 * Designed to extract bank transaction details ONLY from genuine financial SMS messages.
 * Implements strict filtering to reject promotional, OTP, and non-transaction messages.
 */

data class ParsedSms(
    val isTransaction: Boolean,
    val amount: Double? = null,
    val currency: String? = null,
    val transactionType: String? = null,
    val bank: String? = null,
    val accountLast4: String? = null,
    val merchant: String? = null,
    val date: String? = null,
    val reference: String? = null,
    val rawMessage: String,
    val reason: String? = null
)

object SmsParser {

    // 🚫 MANDATORY FILTER KEYWORDS - Immediately reject if found
    private val REJECT_KEYWORDS = setOf(
        "recharge", "offer", "ott", "validity", "data/day", "ul calls", "xstream",
        "airtel", "jio", "vi", "vodafone", "bsnl landline",
        "bill due", "bill payment reminder", "thank you for making payment of bill",
        "statement generated", "minimum due", "due date",
        "otp", "verification code", "promo", "cashback offer",
        "loan offer", "credit limit", "emi offer"
    )

    // ✅ TRANSACTION INDICATORS - Need at least 2 for valid transaction
    private val TRANSACTION_INDICATORS = setOf(
        // Sent, Debited, Paid, Spent, Transferred
        "sent", "debited", "paid", "spent", "transferred",
        // Credited, Received
        "credited", "received",
        // Payment Types
        "upi", "imps", "neft", "rtgs",
        // Bank Names (Lowercase for matching)
        "hdfc", "sbi", "icici", "axis", "kotak", "pnb", "yes bank", "idfc", "federal", "canara",
        // Account References
        "a/c", "acct", "account",
        // References
        "ref", "txn", "utr"
    )

    private val DEBIT_INDICATORS = setOf("sent", "paid", "debited", "spent", "transfer", "transferred")
    private val CREDIT_INDICATORS = setOf("credited", "received", "refund", "cashback received")

    // 💰 RUPEE AMOUNT EXTRACTION (CRITICAL FIX)
    // Regex: (?:Rs\.?|INR|₹)\s?([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)
    private val AMOUNT_PATTERN = Pattern.compile("(?:Rs\\.?|INR|₹)\\s?([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?)")

    // 🏦 BANK NAME EXTRACTION
    private val BANK_PATTERN = Pattern.compile("(?i)(HDFC|SBI|ICICI|AXIS|KOTAK|PNB|YES BANK|IDFC|FEDERAL|CANARA)")

    // 🔢 ACCOUNT NUMBER EXTRACTION (Masked only)
    // Matches: A/C *3263, A/C XX1234, Account *1234
    // Be careful not to match phone numbers. The prompt implies explicit prefixes.
    private val ACCOUNT_PATTERN = Pattern.compile("(?i)(?:A/C|Acct|Account)\\s+(?:[*X]+|XX)?([0-9]{4})")

    // 🧾 MERCHANT / RECEIVER EXTRACTION
    // Extract receiver only if prefixed by: To, Paid to, Transferred to
    private val MERCHANT_PATTERN = Pattern.compile("(?i)(?:To|Paid to|Transferred to)\\s+([A-Za-z0-9\\s]+)(?:\\.|$)")

    // 📅 DATE EXTRACTION
    // Extract date only if explicit: On 25/10/25, 17-OCT-25
    private val DATE_PATTERN = Pattern.compile("(?i)(?:On)\\s+([0-9]{2}[/-][0-9]{2}[/-](?:[0-9]{2}|[0-9]{4})|[0-9]{2}-[A-Za-z]{3}-[0-9]{2})")
    
    // REFERENCE EXTRACTION
    private val REF_PATTERN = Pattern.compile("(?i)(?:Ref|Txn|UTR)\\s*[:\\-]?\\s*([A-Za-z0-9]+)")

    /**
     * Parse SMS message and extract transaction details
     */
    fun parseSms(sender: String, body: String, timestamp: Long): ParsedSms {
        val lowerBody = body.lowercase()

        // 🚫 STEP 1: MANDATORY REJECTION FILTER
        for (keyword in REJECT_KEYWORDS) {
             if (lowerBody.contains(keyword)) {
                return ParsedSms(
                    isTransaction = false,
                    reason = "Promotional / Non-bank message",
                    rawMessage = body
                )
            }
        }

        // ✅ STEP 2: CHECK FOR AT LEAST TWO TRANSACTION INDICATORS
        var indicatorsFound = 0
        
        // We scan for the set of indicators. 
        // Note: The prompt says "Bank name ... Account reference ... Reference number" are also indicators.
        // I combined them into TRANSACTION_INDICATORS for simpler counting, or I can check them by category.
        // Let's iterate the TRANSACTION_INDICATORS set.
        for (indicator in TRANSACTION_INDICATORS) {
            if (lowerBody.contains(indicator)) {
                indicatorsFound++
            }
        }
        
        // Need specific checks for Account number pattern match as an indicator too?
        // Prompt says: "Sent, Debited... Credited... Bank Name... Account Reference... UPI... Reference number"
        // My TRANSACTION_INDICATORS set covers keywords.
        // If we want to be very precise about "Bank Name" indicator, we can check the BANK_PATTERN too.
        // But the set should cover the text presence.
        
        if (indicatorsFound < 2) {
             return ParsedSms(
                isTransaction = false,
                reason = "Insufficient transaction indicators",
                rawMessage = body
            )
        }

        // 💰 STEP 3: AMOUNT EXTRACTION (CRITICAL)
        val amountMatcher = AMOUNT_PATTERN.matcher(body)
        var amount: Double = 0.0
        if (amountMatcher.find()) {
            val amountStr = amountMatcher.group(1)?.replace(",", "")
            try {
                if (amountStr != null) {
                    amount = amountStr.toDouble()
                }
            } catch (e: NumberFormatException) {
                return ParsedSms(isTransaction = false, reason = "Amount conversion error", rawMessage = body)
            }
        } else {
             return ParsedSms(isTransaction = false, reason = "No valid amount found", rawMessage = body)
        }

        if (amount == 0.0) {
            return ParsedSms(isTransaction = false, reason = "Amount is 0.0", rawMessage = body)
        }

        // 🧠 STEP 4: TRANSACTION TYPE DETECTION
        var type = "unknown"
        for (keyword in DEBIT_INDICATORS) {
            if (lowerBody.contains(keyword)) {
                type = "debit"
                break
            }
        }
        if (type == "unknown") {
            for (keyword in CREDIT_INDICATORS) {
                if (lowerBody.contains(keyword)) {
                    type = "credit"
                    break
                }
            }
        }

        // 🏦 STEP 5: BANK NAME
        var bank: String? = null
        val bankMatcher = BANK_PATTERN.matcher(body)
        if (bankMatcher.find()) {
            bank = bankMatcher.group(1)
        }

        // 🔢 STEP 6: ACCOUNT NUMBER
        var accountLast4: String? = null
        val accountMatcher = ACCOUNT_PATTERN.matcher(body)
        if (accountMatcher.find()) {
            accountLast4 = accountMatcher.group(1)
        }

        // 🧾 STEP 7: MERCHANT
        var merchant: String? = null
        val merchantMatcher = MERCHANT_PATTERN.matcher(body)
        if (merchantMatcher.find()) {
            merchant = merchantMatcher.group(1)?.trim()
        }

        // 📅 STEP 8: DATE
        var date: String? = null
        val dateMatcher = DATE_PATTERN.matcher(body)
        if (dateMatcher.find()) {
            date = dateMatcher.group(1)
        }
        
        // REFERENCE
         var reference: String? = null
        val refMatcher = REF_PATTERN.matcher(body)
        if (refMatcher.find()) {
            reference = refMatcher.group(1)
        }


        // Final JSON Object Construction
        return ParsedSms(
            isTransaction = true,
            amount = amount,
            currency = "INR",
            transactionType = type,
            bank = bank,
            accountLast4 = accountLast4,
            merchant = merchant,
            date = date,
            reference = reference,
            rawMessage = body
        )
    }
}
