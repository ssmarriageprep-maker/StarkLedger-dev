package com.starklabs.moneytracker.domain

import com.starklabs.moneytracker.data.Transaction
import java.util.regex.Pattern

data class ParsedSms(
    val amount: Double,
    val merchant: String,
    val date: Long,
    val type: String,
    val smsBody: String,
    val accountLast4: String? = null,
    val balance: Double? = null
)

object SmsParser {

    private const val CURRENCY_REGEX = "(?i)(?:Rs\\.?|INR|₹|\\u20B9)"
    
    // Matches strings like "Rs. 1,234.50", "INR 1234", "₹ 1234"
    private val AMOUNT_PATTERN = Pattern.compile("$CURRENCY_REGEX\\s*(\\d+(?:,\\d+)*(?:\\.\\d{1,2})?)")
    
    // Matches "Avl Bal Rs 1234", "Balance ₹ 1234", "Bal: 1234"
    private val BALANCE_PATTERN = Pattern.compile("(?i)(?:Avl|Avail|Available)?\\.?\\s*Bal(?:ance)?\\.?\\s*(?:is|:)?\\s*(?:$CURRENCY_REGEX)?\\s*([\\d,]+\\.?\\d{0,2})")

    private val DEBIT_KEYWORDS = listOf("debited", "spent", "paid", "sent", "withdrawal", "purchase", "charged", "used at", "txn")
    private val CREDIT_KEYWORDS = listOf("credited", "received", "deposited", "added", "refund", "salary", "inward")

    // Pattern to find "ac 1234" or "ending 1234" or "XX1234" or "*1234"
    private val ACCOUNT_PATTERN = Pattern.compile("(?i)(?:a/c|ac|account|ending|no\\.|xx|\\*)\\s*[\\-:]?\\s*([xX\\*]*\\d{3,4})")

    fun parseSms(sender: String, body: String, timestamp: Long): ParsedSms? {
        // Basic filtering
        if (body.contains("otp", ignoreCase = true) || body.contains("verification code", ignoreCase = true)) {
            return null
        }

        val amountMatcher = AMOUNT_PATTERN.matcher(body)
        if (!amountMatcher.find()) {
            return null
        }

        val amountStr = amountMatcher.group(1)?.replace(",", "") ?: return null
        val amount = amountStr.toDoubleOrNull() ?: return null

        val lowerBody = body.lowercase()
        var type = "UNKNOWN"
        for (keyword in DEBIT_KEYWORDS) {
            if (lowerBody.contains(keyword)) {
                type = "DEBIT"
                break
            }
        }
        for (keyword in CREDIT_KEYWORDS) {
            if (lowerBody.contains(keyword)) {
                type = "CREDIT"
                break
            }
        }
        if (type == "UNKNOWN") return null

        var merchant = "Unknown Merchant"
        
        // Strategy 1: "Paid to [Merchant]", "Transfer to [Merchant]", "Sent to [Merchant]"
        val paidToMatcher = Pattern.compile("(?i)(?:paid\\s+to|transfer\\s+to|sent\\s+to)\\s+([A-Za-z0-9\\s.]+?)(?:\\.|\\s|$)").matcher(body)
        if (paidToMatcher.find()) {
            merchant = paidToMatcher.group(1)?.trim()?.replace(Regex("(?i)(?:via|upi|ref|no).*"), "")?.trim() ?: "UPI Transfer"
        } else {
            // Strategy 2: "Spent [Amount] at [Merchant]", "Debited for [Merchant]", "Purchase at [Merchant]"
            // We look for 'at' or 'for' followed by the merchant name
            val atForMatcher = Pattern.compile("(?i)(?:at|for)\\s+([A-Za-z0-9\\s]+?)(?:\\.|\\s|via|on|ref|txn|$)").matcher(body)
            if (atForMatcher.find()) {
                 merchant = atForMatcher.group(1)?.trim() ?: "Unknown"
            } else {
                // Strategy 3: Fallback to sender suffix
                // Example: AD-HDFCBK -> HDFCBK, JM-ZOMATO -> ZOMATO
                 merchant = sender.replace(Regex(".*-"), "") 
            }
        }
        
        // Extract Account Digits
        var accountDigits: String? = null
        val accMatcher = ACCOUNT_PATTERN.matcher(body)
        if (accMatcher.find()) {
            // Keep only digits, remove 'x' or '*'
            val raw = accMatcher.group(1) ?: ""
            accountDigits = raw.replace(Regex("[^0-9]"), "")
        }

        // Extract Balance
        var balance: Double? = null
        val balMatcher = BALANCE_PATTERN.matcher(body)
        if (balMatcher.find()) {
            val balStr = balMatcher.group(1)?.replace(",", "")
            balance = balStr?.toDoubleOrNull()
        }

        return ParsedSms(
            amount = amount,
            merchant = merchant,
            date = timestamp,
            type = type,
            smsBody = body,
            accountLast4 = accountDigits,
            balance = balance
        )
    }
}
