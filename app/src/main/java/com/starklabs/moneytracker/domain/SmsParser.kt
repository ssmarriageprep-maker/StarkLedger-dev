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
    val balance: Double? = null,
    val date: String? = null,
    val reference: String? = null,
    val rawMessage: String,
    val reason: String? = null
)

object SmsParser {
    private val REJECT_KEYWORDS = listOf(
        "recharge", "offer", "ott", "validity", "data/day", "ul calls", "xstream",
        "airtel", "jio", "vi", "vodafone", "bsnl landline",
        "bill due", "bill payment reminder", "thank you for making payment of bill",
        "statement generated", "minimum due", "due date",
        "otp", "verification code", "promo", "cashback offer",
        "loan offer", "credit limit", "emi offer"
    )

    private val REJECT_PATTERN = Pattern.compile("(?i)\\b(?:" + REJECT_KEYWORDS.joinToString("|") { Pattern.quote(it) } + ")\\b")

    private val TRANSACTION_INDICATORS = listOf(
        "sent", "debited", "paid", "spent", "transferred", "transfer",
        "credited", "received", "refund", "cashback received",
        "upi", "imps", "neft", "rtgs",
        "hdfc", "sbi", "icici", "axis", "kotak", "pnb", "yes bank", "idfc", "federal", "canara",
        "a/c", "acct", "account",
        "ref", "txn", "utr", "bal", "balance"
    )

    private val DEBIT_INDICATORS = setOf("sent", "paid", "debited", "spent", "transfer", "transferred")
    private val CREDIT_INDICATORS = setOf("credited", "received", "refund", "cashback received")

    private val AMOUNT_PATTERN = Pattern.compile("(?i)(?:Rs\\.?|INR|₹)\\s?([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?)")
    private val BANK_PATTERN = Pattern.compile("(?i)(HDFC|SBI|ICICI|AXIS|KOTAK|PNB|YES BANK|IDFC|FEDERAL|CANARA)")
    private val ACCOUNT_PATTERN = Pattern.compile("(?i)(?:A/C|Acct|Account)\\s+(?:[*X]+|XX)?([0-9]{4})")
    private val MERCHANT_PATTERN = Pattern.compile("(?i)(?:To|Paid to|Transferred to|spent at|at|paid for|for)\\s+([A-Za-z0-9\\s&'.\\-]{2,60})")
    private val DATE_PATTERN = Pattern.compile("(?i)(?:On)\\s+([0-9]{2}[/-][0-9]{2}[/-](?:[0-9]{2}|[0-9]{4})|[0-9]{2}-[A-Za-z]{3}-[0-9]{2})")
    private val REF_PATTERN = Pattern.compile("(?i)(?:Ref|Txn|UTR)\\s*[:\\-]?\\s*([A-Za-z0-9]+)")
    private val BALANCE_PATTERN = Pattern.compile("(?i)(?:Avl Bal|Available Balance|Bal|Balance)\\s*(?:[:\\-]|is)?\\s*(?:Rs\\.?|INR|₹)?\\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?)")
    private val NUMERIC_MERCHANT_PATTERN = Pattern.compile("\\d{10,}")

    fun parseSms(sender: String, body: String, timestamp: Long): ParsedSms {
        val lowerBody = body.lowercase()

        if (REJECT_PATTERN.matcher(body).find()) {
            return ParsedSms(isTransaction = false, reason = "Promotional / Non-bank message", rawMessage = body)
        }

        var indicatorsFound = 0
        for (indicator in TRANSACTION_INDICATORS) {
            if (lowerBody.contains(indicator)) {
                indicatorsFound++
            }
        }
        
        if (indicatorsFound < 2) {
             return ParsedSms(isTransaction = false, reason = "Insufficient transaction indicators", rawMessage = body)
        }

        val amountMatcher = AMOUNT_PATTERN.matcher(body)
        var amount: Double = 0.0
        if (amountMatcher.find()) {
            val amountStr = amountMatcher.group(1)?.replace(",", "")
            try {
                amount = amountStr?.toDouble() ?: 0.0
            } catch (e: Exception) {}
        } else {
             return ParsedSms(isTransaction = false, reason = "No valid amount found", rawMessage = body)
        }

        if (amount == 0.0) {
            return ParsedSms(isTransaction = false, reason = "Amount is 0.0", rawMessage = body)
        }

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

        var bank: String? = null
        val bankMatcher = BANK_PATTERN.matcher(body)
        if (bankMatcher.find()) bank = bankMatcher.group(1)

        var accountLast4: String? = null
        val accountMatcher = ACCOUNT_PATTERN.matcher(body)
        if (accountMatcher.find()) accountLast4 = accountMatcher.group(1)

        var merchant: String? = null
        val merchantMatcher = MERCHANT_PATTERN.matcher(body)
        val delimiters = listOf(" on ", " from ", " using ", " via ", " ref ", " txn ", " utr ", " a/c ", " acct ", " account ", " to ", " (", ". ")
        while (merchantMatcher.find()) {
            var candidate = merchantMatcher.group(1)?.trim() ?: continue
            for (delim in delimiters) {
                val idx = candidate.lowercase().indexOf(delim.lowercase())
                if (idx != -1) candidate = candidate.substring(0, idx).trim()
            }
            if (candidate.equals("report", ignoreCase = true)) continue
            if (NUMERIC_MERCHANT_PATTERN.matcher(candidate).matches()) {
                if (merchant == null) merchant = candidate
                continue
            }
            merchant = candidate
            break
        }
        if (merchant == null && sender.contains("-")) {
            merchant = sender.split("-").last()
        }

        var date: String? = null
        val dateMatcher = DATE_PATTERN.matcher(body)
        if (dateMatcher.find()) date = dateMatcher.group(1)
        
        var reference: String? = null
        val refMatcher = REF_PATTERN.matcher(body)
        if (refMatcher.find()) reference = refMatcher.group(1)

        var balance: Double? = null
        val balanceMatcher = BALANCE_PATTERN.matcher(body)
        if (balanceMatcher.find()) {
            try {
                balance = balanceMatcher.group(1)?.replace(",", "")?.toDouble()
            } catch (e: Exception) {}
        }

        return ParsedSms(
            isTransaction = true,
            amount = amount,
            currency = "INR",
            transactionType = type,
            bank = bank,
            accountLast4 = accountLast4,
            merchant = merchant,
            balance = balance,
            date = date,
            reference = reference,
            rawMessage = body
        )
    }
}
