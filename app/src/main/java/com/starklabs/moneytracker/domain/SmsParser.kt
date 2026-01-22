package com.starklabs.moneytracker.domain

import java.util.regex.Pattern

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
        "offer", "recharge", "data", "validity", "ott", "xstream", "airtel", "jio", "vi",
        "vodafone", "bsnl", "bill reminder", "bill due", "statement", "minimum due",
        "emi", "loan", "credit card offer", "cashback offer", "promo", "otp", "verification code"
    )

    private val REJECT_PATTERN = Pattern.compile("(?i)\\b(?:" + REJECT_KEYWORDS.joinToString("|") { Pattern.quote(it) } + ")\\b")

    private val INDICATOR_ACTION = listOf("sent", "paid", "debited", "spent", "transferred", "transfer", "credited", "received")
    private val INDICATOR_BANK = listOf("HDFC", "SBI", "ICICI", "AXIS", "KOTAK", "PNB", "IDFC", "YES", "CANARA", "FEDERAL")
    private val INDICATOR_ACCOUNT = listOf("A/C", "Acct", "Account")
    private val INDICATOR_ACCOUNT_PATTERN = Pattern.compile("(?i)(?:[*X]{1,}|XX)[0-9]{2,}")
    private val INDICATOR_METHOD = listOf("UPI", "IMPS", "NEFT", "RTGS")
    private val INDICATOR_REF = listOf("Ref", "Txn", "UTR", "Reference")

    private val DEBIT_KEYWORDS = setOf("sent", "paid", "debited", "spent", "transfer", "transferred")
    private val CREDIT_KEYWORDS = setOf("credited", "received", "refund")

    // Match full amount including commas and decimals.
    // Group 1 captures the amount string.
    private val AMOUNT_PATTERN = Pattern.compile("(?:Rs\\.?|INR|₹)\\s*([0-9]{1,3}(?:,[0-9]{3})+(?:\\.[0-9]{1,2})?|[0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE)

    private val BANK_PATTERN = Pattern.compile("\\b(HDFC|SBI|ICICI|AXIS|KOTAK|PNB|IDFC|YES|CANARA|FEDERAL)\\b", Pattern.CASE_INSENSITIVE)
    private val ACCOUNT_PATTERN = Pattern.compile("(?:A/C|Acct|Account)\\s+([*X]+[0-9]{2,})", Pattern.CASE_INSENSITIVE)
    private val MERCHANT_PATTERN = Pattern.compile("(?:To|Paid to|Transferred to|Sent to)\\s+([A-Za-z0-9\\s&'.\\-]{2,60})", Pattern.CASE_INSENSITIVE)
    private val DATE_PATTERN = Pattern.compile("(?:On\\s+)?([0-9]{2}[/-][0-9]{2}[/-](?:[0-9]{4}|[0-9]{2})|[0-9]{2}-[A-Za-z]{3}-[0-9]{2})", Pattern.CASE_INSENSITIVE)
    private val REF_PATTERN = Pattern.compile("(?:Ref|Txn ID|Txn|UTR|Reference)[:\\s\\-]*([A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE)

    private val BALANCE_PATTERN = Pattern.compile("(?:Avl Bal|Available Balance|Bal|Balance)[:\\s\\-]*(?:Rs\\.?|INR|₹)?\\s*([0-9]{1,3}(?:,[0-9]{3})+(?:\\.[0-9]{1,2})?|[0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE)

    fun parseSms(sender: String, body: String, timestamp: Long): ParsedSms {
        val lowerBody = body.lowercase()

        // STEP 1 — HARD FILTER
        if (REJECT_PATTERN.matcher(body).find()) {
            return ParsedSms(isTransaction = false, reason = "Promotional or non-bank message", rawMessage = body)
        }

        // STEP 2 — TRANSACTION CONFIRMATION RULE
        var indicatorsFound = 0
        if (INDICATOR_ACTION.any { body.contains(it, ignoreCase = true) }) indicatorsFound++
        if (INDICATOR_BANK.any { body.contains(it, ignoreCase = true) }) indicatorsFound++
        if (INDICATOR_ACCOUNT.any { body.contains(it, ignoreCase = true) } || INDICATOR_ACCOUNT_PATTERN.matcher(body).find()) indicatorsFound++
        if (INDICATOR_METHOD.any { body.contains(it, ignoreCase = true) } || body.contains("UPI", ignoreCase = true)) indicatorsFound++
        if (INDICATOR_REF.any { body.contains(it, ignoreCase = true) } || REF_PATTERN.matcher(body).find()) indicatorsFound++

        if (indicatorsFound < 2) {
            return ParsedSms(isTransaction = false, reason = "Insufficient transaction confidence", rawMessage = body)
        }

        // STEP 3 — AMOUNT EXTRACTION
        val amountMatcher = AMOUNT_PATTERN.matcher(body)
        var amountStr: String? = null

        if (amountMatcher.find()) {
            amountStr = amountMatcher.group(1)
        }

        val amount: Double
        if (amountStr != null) {
            val cleanAmountStr = amountStr.replace(",", "")
            amount = try {
                cleanAmountStr.toDouble()
            } catch (e: Exception) {
                0.0
            }
        } else {
            return ParsedSms(isTransaction = false, reason = "Invalid or missing amount", rawMessage = body)
        }

        if (amount <= 0.0) {
            return ParsedSms(isTransaction = false, reason = "Amount cannot be 0.0", rawMessage = body)
        }

        // STEP 6 — TRANSACTION TYPE
        var type: String? = null
        if (CREDIT_KEYWORDS.any { body.contains(it, ignoreCase = true) }) {
            type = "credit"
        } else if (DEBIT_KEYWORDS.any { body.contains(it, ignoreCase = true) }) {
            type = "debit"
        }

        if (type == null) {
            return ParsedSms(isTransaction = false, reason = "Unclear transaction type", rawMessage = body)
        }

        // STEP 5 — BANK & ACCOUNT EXTRACTION
        var bank: String? = null
        val bankMatcher = BANK_PATTERN.matcher(body)
        if (bankMatcher.find()) bank = bankMatcher.group(1).uppercase()

        var accountLast4: String? = null
        val accountMatcher = ACCOUNT_PATTERN.matcher(body)
        if (accountMatcher.find()) {
            val fullAcc = accountMatcher.group(1)
            accountLast4 = fullAcc.takeLast(4).filter { it.isDigit() }
            if (accountLast4.length < 2) accountLast4 = null
        }

        // STEP 4 — SENDER / MERCHANT EXTRACTION
        var merchant: String? = null
        val merchantMatcher = MERCHANT_PATTERN.matcher(body)
        if (merchantMatcher.find()) {
            var candidate = merchantMatcher.group(1).trim()
            val delimiters = listOf(" via ", " on ", " from ", " a/c ", " account ", " ref ", " txn ", " utr ", " to ", " card ")
            for (delim in delimiters) {
                val idx = candidate.lowercase().indexOf(delim)
                if (idx != -1) candidate = candidate.substring(0, idx).trim()
            }

            val isPhone = candidate.matches(Regex(".*\\b\\d{10,}\\b.*")) || candidate.matches(Regex(".*\\d{10,}.*"))
            val isUrl = candidate.contains("http") || candidate.contains(".com") || candidate.contains(".in")
            val isBank = INDICATOR_BANK.any { b -> candidate.split(Regex("\\s+")).any { it.equals(b, ignoreCase = true) } }

            if (!isPhone && !isUrl && !isBank && candidate.length > 2) {
                merchant = candidate
            }
        }

        // STEP 7 — DATE EXTRACTION
        var date: String? = null
        val dateMatcher = DATE_PATTERN.matcher(body)
        if (dateMatcher.find()) date = dateMatcher.group(1)

        // OTHER EXTRACTIONS
        var reference: String? = null
        val refMatcher = REF_PATTERN.matcher(body)
        if (refMatcher.find()) reference = refMatcher.group(1)

        var balance: Double? = null
        val bMatcher = BALANCE_PATTERN.matcher(body)
        if (bMatcher.find()) {
            val balanceStr = bMatcher.group(1)
            if (balanceStr != null) {
                try {
                    balance = balanceStr.replace(",", "").toDouble()
                } catch (e: Exception) {}
            }
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
