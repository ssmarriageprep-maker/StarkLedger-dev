package com.starklabs.moneytracker.domain

import java.util.regex.Pattern

/**
 * High-Precision Financial SMS Parser for StarkLedger
 * 
 * Designed to extract bank transaction details ONLY from genuine financial SMS messages.
 * Implements strict filtering to reject promotional, OTP, and non-transaction messages.
 * 
 * Key Features:
 * - Mandatory promotional message filtering
 * - Accurate rupee amount extraction (never returns 0.0)
 * - Transaction validation requiring at least 2 indicators
 * - Proper bank and account extraction
 * - Precision over recall
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
    val balance: Double? = null,
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
        "loan offer", "credit limit", "emi offer", "topup", "recharged",
        "plan", "pack", "internet", "expiry", "unlimited", "subscription",
        "bonus", "gift", "limited time", "mobile", "landline", "broadband", "utility"
    )

    // ✅ TRANSACTION INDICATORS - Need at least 2 for valid transaction
    private val DEBIT_INDICATORS = setOf("sent", "debited", "paid", "spent", "transferred", "withdrawal", "purchase")
    private val CREDIT_INDICATORS = setOf("credited", "received", "deposited", "refund")
    private val BANK_INDICATORS = setOf("hdfc", "sbi", "icici", "axis", "kotak", "pnb", "yes bank", "idfc", "federal", "canara", "bank of baroda", "union bank")
    private val ACCOUNT_INDICATORS = setOf("a/c", "acct", "account", "card")
    private val PAYMENT_INDICATORS = setOf("upi", "imps", "neft", "rtgs")
    private val REFERENCE_INDICATORS = setOf("ref", "txn", "utr", "transaction")

    // 💰 CRITICAL: Rupee amount extraction regex (MANDATORY FORMAT)
    private val AMOUNT_PATTERN = Pattern.compile("(?:Rs\\.?|INR|₹)\\s?([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?)")
    
    // 🏦 Bank name extraction
    private val BANK_PATTERN = Pattern.compile("(?i)(HDFC|SBI|ICICI|AXIS|KOTAK|PNB|YES BANK|IDFC|FEDERAL|CANARA|BANK OF BARODA|UNION BANK)")
    
    // 🔢 Account number extraction (masked format only)
    // Refined to ensure it matches exactly 3-4 digits and is not the start of a longer mobile number
    private val ACCOUNT_PATTERN = Pattern.compile("(?i)(?:a/c|acct|account|card)\\s*(?:no\\.?)?\\s*[:\\-]?\\s*(?:XX|\\*)*([0-9]{3,4})(?!\\d)")
    
    // 🧾 Merchant extraction
    private val MERCHANT_PATTERN = Pattern.compile("(?i)(?:to|paid to|transferred to|at)\\s+([A-Za-z0-9\\s.&]+?)(?:\\s+(?:via|on|ref|upi|a/c|Rs|INR|₹)|\\.|$)")
    
    // 📅 Date extraction
    private val DATE_PATTERN = Pattern.compile("(?i)(?:on|dated?)\\s+([0-9]{1,2}[-/][0-9]{1,2}[-/][0-9]{2,4}|[0-9]{1,2}-[A-Z]{3}-[0-9]{2,4})")
    
    // 🔖 Reference number extraction
    private val REFERENCE_PATTERN = Pattern.compile("(?i)(?:ref|txn|utr|transaction)\\s*(?:no\\.?|id)?\\s*[:\\-]?\\s*([A-Za-z0-9]+)")
    
    // 💵 Balance extraction
    private val BALANCE_PATTERN = Pattern.compile("(?i)(?:avl|available|avail)?\\.?\\s*bal(?:ance)?\\.?\\s*(?:is|:)?\\s*(?:Rs\\.?|INR|₹)?\\s*([0-9,]+\\.?[0-9]{0,2})")

    /**
     * Parse SMS message and extract transaction details
     * 
     * @param sender SMS sender ID
     * @param body SMS message body
     * @param timestamp Message timestamp in milliseconds
     * @return ParsedSms object with transaction details or rejection reason
     */
    fun parseSms(sender: String, body: String, timestamp: Long): ParsedSms {
        val lowerBody = body.lowercase()
        
        // 🚫 STEP 1: MANDATORY REJECTION FILTER
        for (keyword in REJECT_KEYWORDS) {
            if (lowerBody.contains(keyword)) {
                return ParsedSms(
                    isTransaction = false,
                    rawMessage = body,
                    reason = "Promotional / Non-bank message (contains: $keyword)"
                )
            }
        }
        
        // ✅ STEP 2: COUNT TRANSACTION INDICATORS
        var indicatorCount = 0
        var hasDebitIndicator = false
        var hasCreditIndicator = false
        
        // Check debit/credit indicators
        for (indicator in DEBIT_INDICATORS) {
            if (lowerBody.contains(indicator)) {
                indicatorCount++
                hasDebitIndicator = true
                break
            }
        }
        
        for (indicator in CREDIT_INDICATORS) {
            if (lowerBody.contains(indicator)) {
                indicatorCount++
                hasCreditIndicator = true
                break
            }
        }
        
        // Check bank indicators
        for (indicator in BANK_INDICATORS) {
            if (lowerBody.contains(indicator)) {
                indicatorCount++
                break
            }
        }
        
        // Check account indicators
        for (indicator in ACCOUNT_INDICATORS) {
            if (lowerBody.contains(indicator)) {
                indicatorCount++
                break
            }
        }
        
        // Check payment method indicators
        for (indicator in PAYMENT_INDICATORS) {
            if (lowerBody.contains(indicator)) {
                indicatorCount++
                break
            }
        }
        
        // Check reference indicators
        for (indicator in REFERENCE_INDICATORS) {
            if (lowerBody.contains(indicator)) {
                indicatorCount++
                break
            }
        }
        
        // ❌ REJECT if less than 2 indicators
        if (indicatorCount < 2) {
            return ParsedSms(
                isTransaction = false,
                rawMessage = body,
                reason = "Insufficient transaction indicators (found $indicatorCount, need at least 2)"
            )
        }
        
        // 💰 STEP 3: EXTRACT AMOUNT (CRITICAL - NEVER RETURN 0.0)
        val amountMatcher = AMOUNT_PATTERN.matcher(body)
        if (!amountMatcher.find()) {
            return ParsedSms(
                isTransaction = false,
                rawMessage = body,
                reason = "No valid rupee amount found"
            )
        }
        
        val amountStr = amountMatcher.group(1)?.replace(",", "")
        val amount = try {
            amountStr?.toDouble() ?: return ParsedSms(
                isTransaction = false,
                rawMessage = body,
                reason = "Amount extraction failed - invalid format"
            )
        } catch (e: NumberFormatException) {
            return ParsedSms(
                isTransaction = false,
                rawMessage = body,
                reason = "Amount conversion failed: ${e.message}"
            )
        }
        
        // ❌ CRITICAL: Never allow 0.0 amounts
        if (amount <= 0.0) {
            return ParsedSms(
                isTransaction = false,
                rawMessage = body,
                reason = "Invalid amount: $amount (must be > 0)"
            )
        }
        
        // 🧠 STEP 4: DETERMINE TRANSACTION TYPE
        val transactionType = when {
            hasCreditIndicator -> "credit"
            hasDebitIndicator -> "debit"
            else -> "unknown"
        }
        
        // 🏦 STEP 5: EXTRACT BANK NAME
        val bankMatcher = BANK_PATTERN.matcher(body)
        val bank = if (bankMatcher.find()) {
            bankMatcher.group(1)?.uppercase()
        } else null
        
        // 🔢 STEP 6: EXTRACT ACCOUNT NUMBER (last 4 digits only)
        val accountMatcher = ACCOUNT_PATTERN.matcher(body)
        val accountLast4 = if (accountMatcher.find()) {
            val digits = accountMatcher.group(1)
            // Ensure we only keep numeric digits, max 4
            digits?.replace(Regex("[^0-9]"), "")?.takeLast(4)
        } else null
        
        // 🧾 STEP 7: EXTRACT MERCHANT/RECEIVER
        val merchantMatcher = MERCHANT_PATTERN.matcher(body)
        val merchant = if (merchantMatcher.find()) {
            merchantMatcher.group(1)?.trim()?.replace(Regex("\\s+"), " ")
        } else {
            // Fallback: extract from sender ID
            sender.replace(Regex(".*-"), "").takeIf { it.isNotBlank() }
        }
        
        // 📅 STEP 8: EXTRACT DATE
        val dateMatcher = DATE_PATTERN.matcher(body)
        val date = if (dateMatcher.find()) {
            dateMatcher.group(1)
        } else null
        
        // 🔖 STEP 9: EXTRACT REFERENCE NUMBER
        val refMatcher = REFERENCE_PATTERN.matcher(body)
        val reference = if (refMatcher.find()) {
            refMatcher.group(1)
        } else null
        
        // 💵 STEP 10: EXTRACT BALANCE
        val balanceMatcher = BALANCE_PATTERN.matcher(body)
        val balance = if (balanceMatcher.find()) {
            val balStr = balanceMatcher.group(1)?.replace(",", "")
            balStr?.toDoubleOrNull()
        } else null
        
        // ✅ SUCCESS: Return valid transaction
        return ParsedSms(
            isTransaction = true,
            amount = amount,
            currency = "INR",
            transactionType = transactionType,
            bank = bank,
            accountLast4 = accountLast4,
            merchant = merchant,
            date = date,
            reference = reference,
            balance = balance,
            rawMessage = body,
            reason = null
        )
    }
}
