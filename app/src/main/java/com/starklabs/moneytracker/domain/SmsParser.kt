package com.starklabs.moneytracker.domain

import com.starklabs.moneytracker.data.Transaction
import java.util.regex.Pattern

data class ParsedSms(
    val amount: Double,
    val merchant: String,
    val date: Long,
    val type: String,
    val smsBody: String,
    val accountLast4: String? = null
)

object SmsParser {

    private val AMOUNT_PATTERN = Pattern.compile("(?i)(?:Rs\\.?|INR)\\s*(\\d+(?:,\\d+)*(?:\\.\\d{1,2})?)")
    private val DEBIT_KEYWORDS = listOf("debited", "spent", "paid", "sent", "withdrawal", "purchase", "charged", "used at", "txn")
    private val CREDIT_KEYWORDS = listOf("credited", "received", "deposited", "added", "refund", "salary", "inward")

    // Pattern to find "ac 1234" or "ending 1234" or "XX1234"
    private val ACCOUNT_PATTERN = Pattern.compile("(?i)(?:a/c|ac|account|ending|no\\.|xx)\\s*[\\-:]?\\s*([xX]*\\d{3,4})")

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
        val merchantPattern = Pattern.compile("(?i)(?:at|to|for)\\s+([A-Za-z0-9\\s]+?)(?:\\.|\\s|$)")
        val merchantMatcher = merchantPattern.matcher(body)
        if (merchantMatcher.find()) {
            merchant = merchantMatcher.group(1)?.trim() ?: "Unknown"
        } else {
             merchant = sender
        }
        
        // Extract Account Digits
        var accountDigits: String? = null
        val accMatcher = ACCOUNT_PATTERN.matcher(body)
        if (accMatcher.find()) {
            // Keep only digits, remove 'x'
            val raw = accMatcher.group(1) ?: ""
            accountDigits = raw.replace(Regex("[^0-9]"), "")
        }

        return ParsedSms(
            amount = amount,
            merchant = merchant,
            date = timestamp,
            type = type,
            smsBody = body,
            accountLast4 = accountDigits
        )
    }
}
