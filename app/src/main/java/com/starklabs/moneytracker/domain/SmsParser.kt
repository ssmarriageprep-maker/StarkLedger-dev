package com.starklabs.moneytracker.domain

import java.security.MessageDigest
import java.util.regex.Pattern

// ─── Pattern Enum ────────────────────────────────────────────────────────────

/**
 * Formalized SMS pattern types for analytics, debugging, and future ML training.
 */
enum class SmsPattern {
    UPI_SENT,
    UPI_RECEIVED,
    BANK_DEBIT,
    BANK_CREDIT,
    CARD_SPEND,
    CARD_PAYMENT_RECEIVED,
    WALLET_PAYMENT,
    NEFT_TRANSFER,
    ACH_DEBIT,
    EMI_DEBIT,
    AUTOPAY_DEBIT,
    ATM_WITHDRAWAL,
    UNKNOWN
}

// ─── Classification Result (Phase 1 output) ────────────────────────────────

data class ClassificationResult(
    val category: String,       // "transactional" | "non-transactional"
    val confidence: Int,        // 0-100
    val patternDetected: Boolean,
    val pattern: SmsPattern,
    val reason: String
)

// ─── Parsed SMS (Final output) ──────────────────────────────────────────────

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
    val reason: String? = null,
    // ── New intelligence fields ──
    val confidence: Int = 0,
    val category: String? = null,
    val patternDetected: Boolean = false,
    val patternUsed: SmsPattern = SmsPattern.UNKNOWN,
    val messageHash: String? = null
)

// ─── Intelligence Engine ────────────────────────────────────────────────────

object SmsParser {

    // ── Rejection keywords (used for penalty, NOT hard reject) ─────────────
    private val REJECT_KEYWORDS = listOf(
        "offer", "validity", "ott", "xstream",
        "bill reminder", "bill due", "statement", "minimum due",
        "credit card offer", "cashback offer", "promo",
        "otp", "verification code"
    )

    // Telecom sender IDs — these ALONE trigger strong penalty
    private val TELECOM_SENDERS = listOf("airtel", "jio", "bsnl", "vicare", "vi-")

    private val REJECT_PATTERN = Pattern.compile(
        "(?i)\\b(?:" + REJECT_KEYWORDS.joinToString("|") { Pattern.quote(it) } + ")\\b"
    )

    // ── Transaction signal indicators ──────────────────────────────────────
    private val ACTION_WORDS = listOf(
        "sent", "paid", "debited", "spent", "transferred", "transfer",
        "credited", "received", "payment", "withdrawn", "deducted",
        "done", "made a payment", "successfully", "spent"
    )

    // Pre-compiled pattern for all action words with word boundaries
    private val ACTION_WORDS_PATTERN = Pattern.compile(
        "\\b(?:" + ACTION_WORDS.distinct().joinToString("|") { Pattern.quote(it) } + ")\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val KNOWN_BANKS = listOf(
        "HDFC", "SBI", "ICICI", "AXIS", "KOTAK", "PNB", "IDFC", "YES", "CANARA", "FEDERAL", "UNION"
    )
    private val ACCOUNT_INDICATORS = listOf("A/C", "A/c", "Acct", "Account", "card")
    private val METHOD_INDICATORS = listOf("UPI", "IMPS", "NEFT", "RTGS")
    private val REF_INDICATORS = listOf("Ref", "Txn", "UTR", "Reference")
    private val CURRENCY_INDICATORS = listOf("Rs", "INR", "₹")

    private val DEBIT_KEYWORDS = setOf(
        "sent", "paid", "debited", "spent", "transfer", "transferred",
        "withdrawn", "deducted", "made a payment", "done"
    )
    private val CREDIT_KEYWORDS = setOf("credited", "received", "refund")

    private val FUTURE_TENSE_PATTERN = Pattern.compile(
        "\\b(?:will be|due on|due by|overdue)\\b", Pattern.CASE_INSENSITIVE
    )

    private val PROMO_KEYWORDS_PATTERN = Pattern.compile(
        "\\b(?:bill|emi|loan|recharge|invoice|min payment|statement)\\b", Pattern.CASE_INSENSITIVE
    )

    private val MERCHANT_KEYWORD_PATTERN = Pattern.compile(
        "(?:to |paid to|transferred to|sent to| at )", Pattern.CASE_INSENSITIVE
    )

    // ── Compiled regex patterns ────────────────────────────────────────────
    // Strict amount: currency symbol followed by optional space then number
    // Handles: Rs.318.00, Rs 318, INR 7,316.43, ₹5,000, Rs:236.00 (Union Bank), Rs. 2.00
    private val AMOUNT_STRICT = Pattern.compile(
        "(?:Rs[.:]?|INR|₹)\\s*([0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE
    )
    // Relaxed amount: handles "INR318", "Rs318debited", "₹318"
    private val AMOUNT_RELAXED = Pattern.compile(
        "(?:Rs[.:]?|INR|₹)\\s?([0-9]+(?:,[0-9]+)*\\.?[0-9]*)", Pattern.CASE_INSENSITIVE
    )
    // Context amount: number near action keywords (fallback)
    private val AMOUNT_CONTEXT = Pattern.compile(
        "(?:(?:sent|paid|debited|credited|received|spent|withdrawn|deducted)\\s+(?:Rs[.:]?|INR|₹)?\\s*|(?:Rs[.:]?|INR|₹)\\s*)([0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)",
        Pattern.CASE_INSENSITIVE
    )

    private val ACCOUNT_MASK_PATTERN = Pattern.compile(
        "(?i)(?:[*X]{1,}|XX|xxxx|card ending\\s+|card\\s+[*X]*|card no\\.\\s*(?:xx)?)[0-9]{2,}"
    )
    private val BANK_PATTERN = Pattern.compile(
        "\\b(HDFC|SBI|ICICI|AXIS|KOTAK|PNB|IDFC|YES|CANARA|FEDERAL|UNION)\\b", Pattern.CASE_INSENSITIVE
    )
    // Handles: A/C *3263, A/c *3868, Acct XX1234, Card 6763, card ending 6763, card no.xx1322, xxxx1322
    private val ACCOUNT_PATTERN = Pattern.compile(
        "(?:A/[Cc]|Acct|Account|Card ending|Card no\\.\\s*|Card|Bank)\\s+[*Xx]*([0-9]{2,})", Pattern.CASE_INSENSITIVE
    )

    // Merchant patterns in PRIORITY ORDER (highest first)
    private val MERCHANT_PATTERNS = listOf(
        Pattern.compile("(?:Paid to)\\s+([A-Za-z0-9\\s&'.\\-/:]{2,60})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:Transferred to)\\s+([A-Za-z0-9\\s&'.\\-/:]{2,60})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:Sent to)\\s+([A-Za-z0-9\\s&'.\\-/:]{2,60})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:To)\\s+([A-Za-z0-9\\s&'.\\-/:]{2,60})", Pattern.CASE_INSENSITIVE),
        // Card spend: "At ..LULU INTERNATION_" or "At STARBUCKS"
        Pattern.compile("(?:At)\\s+\\.{0,2}([A-Za-z0-9\\s&'.\\-/:]{2,60})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:spent at|spent on)\\s+([A-Za-z0-9\\s&'.\\-/:]{2,60})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:at)\\s+([A-Za-z0-9\\s&'.\\-/:]{2,60})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:for)\\s+([A-Za-z0-9\\s&'.\\-/:]{2,60})", Pattern.CASE_INSENSITIVE),
        // HDFC card spend: "towards MERCHANT"
        Pattern.compile("(?:towards)\\s+([A-Za-z0-9\\s&'.\\-/:]{2,60})", Pattern.CASE_INSENSITIVE)
    )

    // Date patterns: DD/MM/YY, DD-MM-YYYY, DD-MMM-YY, DD/MMM/YYYY, YYYY-MM-DD:HH:MM:SS (HDFC card)
    private val DATE_PATTERN = Pattern.compile(
        "(?:On\\s+)?([0-9]{2}[/-][0-9]{2}[/-](?:[0-9]{4}|[0-9]{2})|[0-9]{2}-[A-Za-z]{3}-[0-9]{2,4}|[0-9]{2}/[A-Za-z]{3}/[0-9]{4}|[0-9]{4}-[0-9]{2}-[0-9]{2}(?::[0-9]{2}:[0-9]{2}:[0-9]{2})?|[0-9]{1,2}-[0-9]{1,2}-[0-9]{4})",
        Pattern.CASE_INSENSITIVE
    )
    private val REF_PATTERN = Pattern.compile(
        "(?:Ref|Txn ID|Txn|UTR|Reference|ref no|Ref#)[:\\s\\-]*([A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE
    )
    // Balance patterns: Avl Bal, Avl bal, Available bal, Balance, AVAILABLE LIMIT
    private val BALANCE_PATTERN = Pattern.compile(
        "(?:Avl Bal|Avl bal|Available Balance|Available bal|Balance|AVAILABLE LIMIT)[:\\s\\-]*(?:Rs[.:]?|INR|₹|RS\\.)?\\s*([0-9]+(?:,[0-9]+)*(?:\\.[0-9]{1,2})?)",
        Pattern.CASE_INSENSITIVE
    )

    // Payment gateways — excluded from merchant results
    private val PAYMENT_GATEWAYS = listOf(
        "razorpay", "payu", "paypal", "phonepe", "gpay", "google pay", "paytm"
    )

    // HDFC card spend: "Spent Rs.105 On HDFC Bank Card 6763 At ..MR DIY_ On 2026-01-10:22:17:59"
    private val HDFC_CARD_SPEND_PATTERN = Pattern.compile(
        "Spent\\s+Rs\\.?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+On\\s+HDFC Bank Card\\s+(\\d{4})\\s+At\\s+\\.{0,2}(.+?)(?:\\s+On\\s|\\s*\\.\\s*Not|$)",
        Pattern.CASE_INSENSITIVE
    )
    // Union Bank: "A/c *3868 Debited for Rs:236.00 on 30-12-2025 05:40:37 by BRANCH"
    private val UNION_BANK_PATTERN = Pattern.compile(
        "A/c\\s+[*]?(\\d{4})\\s+(?:Debited|Credited)\\s+for\\s+Rs[:]\\s*([0-9,]+(?:\\.[0-9]{1,2})?)",
        Pattern.CASE_INSENSITIVE
    )
    // Pluxee: "Rs. 850.64 spent from Pluxee Meal wallet, card no.xx1322 on ... at MERCHANT"
    private val PLUXEE_SPEND_PATTERN = Pattern.compile(
        "Rs\\.?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+(?:spent|deducted)\\s+from\\s+(?:your\\s+)?Pluxee",
        Pattern.CASE_INSENSITIVE
    )
    // HDFC Credit Alert: "Rs.2000.00 credited to HDFC Bank A/c XX3263 on 10-01-26 from VPA ..."
    private val CREDIT_ALERT_PATTERN = Pattern.compile(
        "Rs\\.?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+credited\\s+to\\s+HDFC Bank A/c\\s+XX(\\d{4})",
        Pattern.CASE_INSENSITIVE
    )
    // HDFC UPDATE debit: "INR 7,316.43 debited from HDFC Bank XX3263 on 10-JAN-26. Info: ..."
    private val HDFC_UPDATE_PATTERN = Pattern.compile(
        "INR\\s+([0-9,]+(?:\\.[0-9]{1,2})?)\\s+debited\\s+from\\s+HDFC Bank\\s+XX(\\d{4})",
        Pattern.CASE_INSENSITIVE
    )
    // HDFCBANK CARDMEMBER: "PAYMENT OF Rs. 11493.00 RECEIVED TOWARDS YOUR CREDIT CARD ENDING WITH 6763"
    private val CARDMEMBER_PAYMENT_PATTERN = Pattern.compile(
        "PAYMENT OF\\s+Rs\\.?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+RECEIVED\\s+TOWARDS\\s+YOUR\\s+CREDIT CARD\\s+ENDING\\s+WITH\\s+(\\d{4})",
        Pattern.CASE_INSENSITIVE
    )

    // ════════════════════════════════════════════════════════════════════════
    //  PUBLIC API
    // ════════════════════════════════════════════════════════════════════════

    fun parseSms(sender: String, body: String, timestamp: Long): ParsedSms {
        // Normalize: merge multi-line fragments, collapse extra spaces
        val normalizedBody = normalizeMessage(body)
        val hash = sha256(normalizedBody)

        // ── PHASE 1: Classification ─────────────────────────────────────
        val classification = classifyMessage(sender, normalizedBody)

        if (classification.category == "non-transactional") {
            return ParsedSms(
                isTransaction = false,
                reason = classification.reason,
                rawMessage = body,
                confidence = classification.confidence,
                category = classification.category,
                patternDetected = classification.patternDetected,
                patternUsed = classification.pattern,
                messageHash = hash
            )
        }

        // ── PHASE 2: Extraction (only if transactional & confidence >= 70) ─
        if (classification.confidence < 70) {
            return ParsedSms(
                isTransaction = false,
                reason = "Insufficient transaction confidence (${classification.confidence}%)",
                rawMessage = body,
                confidence = classification.confidence,
                category = classification.category,
                patternDetected = classification.patternDetected,
                patternUsed = classification.pattern,
                messageHash = hash
            )
        }

        return extractTransaction(sender, normalizedBody, body, classification, hash)
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PHASE 1 — CLASSIFICATION
    // ════════════════════════════════════════════════════════════════════════

    internal fun classifyMessage(sender: String, body: String): ClassificationResult {
        val lowerSender = sender.lowercase()

        // ── Detect signals ──────────────────────────────────────────────
        val hasAmount = AMOUNT_STRICT.matcher(body).find() || AMOUNT_RELAXED.matcher(body).find()
        val hasActionWord = ACTION_WORDS_PATTERN.matcher(body).find()
        val hasBank = KNOWN_BANKS.any { body.contains(it, ignoreCase = true) || sender.contains(it, ignoreCase = true) }
        val hasAccountMask = ACCOUNT_INDICATORS.any { body.contains(it, ignoreCase = true) } || ACCOUNT_MASK_PATTERN.matcher(body).find()
        val hasMethod = METHOD_INDICATORS.any { body.contains(it, ignoreCase = true) }
        val hasReference = REF_INDICATORS.any { body.contains(it, ignoreCase = true) } || REF_PATTERN.matcher(body).find()
        val hasCurrency = CURRENCY_INDICATORS.any { body.contains(it, ignoreCase = true) }
        val hasMerchantKeyword = MERCHANT_KEYWORD_PATTERN.matcher(body).find()

        // Higher order signals
        val isMaskedAccount = ACCOUNT_MASK_PATTERN.matcher(body).find()
        val hasTransactionSignals = hasAmount && (hasActionWord || isMaskedAccount || hasBank)

        val isFutureTense = FUTURE_TENSE_PATTERN.matcher(body).find()
        val hasRejectKeyword = REJECT_PATTERN.matcher(body).find() ||
                             (PROMO_KEYWORDS_PATTERN.matcher(body).find() && !hasActionWord)
        val isTelecomSender = TELECOM_SENDERS.any { lowerSender.contains(it) }

        // 🚨 Smart rejection: reject if future tense OR (keyword present AND no bank/mask)
        if (isFutureTense || (hasRejectKeyword && !(hasBank && isMaskedAccount))) {
            return ClassificationResult(
                category = "non-transactional",
                confidence = if (isFutureTense) 0 else 10,
                patternDetected = false,
                pattern = SmsPattern.UNKNOWN,
                reason = "Promotional or non-bank message"
            )
        }

        // ── Structured confidence scoring ───────────────────────────────
        var score = 0
        if (hasAmount)        score += 30
        if (hasActionWord)    score += 25
        if (hasBank)          score += 15
        if (hasAccountMask)   score += 10
        if (hasReference)     score += 10
        if (hasMerchantKeyword) score += 10
        if (hasCurrency)      score += 5
        if (hasMethod)        score += 5

        // Penalties
        if (hasRejectKeyword) {
            // Even if bank/mask is present, if it's a "bill" or "statement", it's usually not an expenditure
            score -= if (hasBank && isMaskedAccount) 25 else 50
        }
        if (isTelecomSender && !hasTransactionSignals) score -= 20

        // Clamp
        score = score.coerceIn(0, 100)

        // ── Pattern detection ───────────────────────────────────────────
        val pattern = detectPattern(body, hasActionWord, hasMethod, hasBank, hasAccountMask)

        val isTransactional = score >= 70
        return ClassificationResult(
            category = if (isTransactional) "transactional" else "non-transactional",
            confidence = score,
            patternDetected = pattern != SmsPattern.UNKNOWN,
            pattern = pattern,
            reason = if (isTransactional) "Transaction signals detected" else "Insufficient transaction confidence"
        )
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PHASE 2 — EXTRACTION
    // ════════════════════════════════════════════════════════════════════════

    private fun extractTransaction(
        sender: String,
        normalizedBody: String,
        rawBody: String,
        classification: ClassificationResult,
        hash: String
    ): ParsedSms {

        // ── Amount (multi-pass) ─────────────────────────────────────────
        val amount = extractAmount(normalizedBody)
        if (amount == null || amount <= 0.0) {
            return ParsedSms(
                isTransaction = false,
                reason = "Invalid or missing amount",
                rawMessage = rawBody,
                confidence = classification.confidence,
                category = classification.category,
                patternDetected = classification.patternDetected,
                patternUsed = classification.pattern,
                messageHash = hash
            )
        }

        // ── Transaction Type ────────────────────────────────────────────
        val type = extractTransactionType(normalizedBody)
        if (type == null) {
            return ParsedSms(
                isTransaction = false,
                reason = "Unclear transaction type",
                rawMessage = rawBody,
                confidence = classification.confidence,
                category = classification.category,
                patternDetected = classification.patternDetected,
                patternUsed = classification.pattern,
                messageHash = hash
            )
        }

        // ── Bank (controlled list, body first, then sender) ─────────────
        var bank: String? = null
        val bankMatcher = BANK_PATTERN.matcher(normalizedBody)
        if (bankMatcher.find()) {
            bank = bankMatcher.group(1).uppercase()
        } else {
            bank = KNOWN_BANKS.find { sender.contains(it, ignoreCase = true) }?.uppercase()
        }

        // ── Account (masked only) ──────────────────────────────────────
        var accountLast4: String? = null
        val accountMatcher = ACCOUNT_PATTERN.matcher(normalizedBody)
        if (accountMatcher.find()) {
            val fullAcc = accountMatcher.group(1)
            accountLast4 = fullAcc.takeLast(4).filter { it.isDigit() }
            if (accountLast4.length < 2) accountLast4 = null
        }

        // ── Merchant (ranked priority) ──────────────────────────────────
        val merchant = extractMerchant(normalizedBody)

        // ── Date ────────────────────────────────────────────────────────
        var date: String? = null
        val dateMatcher = DATE_PATTERN.matcher(normalizedBody)
        if (dateMatcher.find()) date = dateMatcher.group(1)

        // ── Reference ───────────────────────────────────────────────────
        var reference: String? = null
        val refMatcher = REF_PATTERN.matcher(normalizedBody)
        if (refMatcher.find()) reference = refMatcher.group(1)

        // ── Balance ─────────────────────────────────────────────────────
        var balance: Double? = null
        val bMatcher = BALANCE_PATTERN.matcher(normalizedBody)
        if (bMatcher.find()) {
            val balanceStr = bMatcher.group(1)
            if (balanceStr != null) {
                try { balance = balanceStr.replace(",", "").toDouble() } catch (_: Exception) {}
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
            rawMessage = rawBody,
            confidence = classification.confidence,
            category = classification.category,
            patternDetected = classification.patternDetected,
            patternUsed = classification.pattern,
            messageHash = hash
        )
    }

    // ════════════════════════════════════════════════════════════════════════
    //  EXTRACTION HELPERS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Multi-pass amount extraction:
     * 1. Strict regex
     * 2. Relaxed regex
     * 3. Context-based (number near action keyword)
     * Always skips amounts that fall inside balance declarations.
     */
    private fun extractAmount(body: String): Double? {
        // Build balance exclusion ranges
        val balanceMatcher = BALANCE_PATTERN.matcher(body)
        val balanceRanges = mutableListOf<IntRange>()
        while (balanceMatcher.find()) {
            balanceRanges.add(balanceMatcher.start()..balanceMatcher.end())
        }

        fun isInsideBalance(start: Int, end: Int): Boolean =
            balanceRanges.any { start >= it.first && end <= it.last }

        // Pass 1: strict
        val strictMatcher = AMOUNT_STRICT.matcher(body)
        while (strictMatcher.find()) {
            if (!isInsideBalance(strictMatcher.start(), strictMatcher.end())) {
                val parsed = parseAmountString(strictMatcher.group(1))
                if (parsed != null && parsed > 0.0) return parsed
            }
        }

        // Pass 2: relaxed
        val relaxedMatcher = AMOUNT_RELAXED.matcher(body)
        while (relaxedMatcher.find()) {
            if (!isInsideBalance(relaxedMatcher.start(), relaxedMatcher.end())) {
                val parsed = parseAmountString(relaxedMatcher.group(1))
                if (parsed != null && parsed > 0.0) return parsed
            }
        }

        // Pass 3: context-based
        val contextMatcher = AMOUNT_CONTEXT.matcher(body)
        while (contextMatcher.find()) {
            if (!isInsideBalance(contextMatcher.start(), contextMatcher.end())) {
                val parsed = parseAmountString(contextMatcher.group(1))
                if (parsed != null && parsed > 0.0) return parsed
            }
        }

        return null
    }

    private fun parseAmountString(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        return try {
            val cleaned = raw.replace(",", "").trim()
            if (cleaned.isEmpty()) null else cleaned.toDouble()
        } catch (_: Exception) { null }
    }

    /**
     * Transaction type: debit vs credit based on first-occurring keyword.
     */
    private fun extractTransactionType(body: String): String? {
        val firstCredit = CREDIT_KEYWORDS
            .map { body.indexOf(it, ignoreCase = true) }
            .filter { it != -1 }
            .minOrNull() ?: Int.MAX_VALUE

        val firstDebit = DEBIT_KEYWORDS
            .map { body.indexOf(it, ignoreCase = true) }
            .filter { it != -1 }
            .minOrNull() ?: Int.MAX_VALUE

        return when {
            firstCredit < firstDebit -> "credit"
            firstDebit < firstCredit -> "debit"
            else -> null
        }
    }

    /**
     * Merchant extraction with PRIORITY RANKING.
     * Tries patterns in order: Paid to > Transferred to > Sent to > To > at > for.
     * Returns first valid match.
     */
    private fun extractMerchant(body: String): String? {
        for (pattern in MERCHANT_PATTERNS) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                val candidate = cleanMerchant(matcher.group(1))
                if (candidate != null) return candidate
            }
        }
        return null
    }

    /**
     * Clean + validate a merchant candidate string.
     * Handles HDFC card spend truncation: "..LULU INTERNATION_" → "LULU INTERNATION"
     */
    private fun cleanMerchant(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        var candidate = raw.trim()

        // Strip HDFC card spend truncation artifacts: leading dots, trailing underscore
        candidate = candidate.replace(Regex("^\\.{1,2}"), "").trim()
        candidate = candidate.replace(Regex("_+$"), "").trim()

        // Truncate at delimiter keywords
        val delimitersRegex = Regex("(?i)\\s+(?:via|on|from|a/c|account|ref|txn|utr|to|card ending|at|for|using|with|is|not you|if not)\\b")
        val match = delimitersRegex.find(candidate)
        if (match != null) {
            candidate = candidate.substring(0, match.range.first).trim()
        }

        candidate = candidate.replace(Regex("\\s+"), " ").trim()
        candidate = candidate.replace(Regex("[.,:\\-]+$"), "").trim()

        // Reject phone numbers, URLs, bank names, payment gateways
        val isPhone = candidate.matches(Regex(".*\\d{10,}.*"))
        val isUrl = candidate.contains("http") || candidate.contains(".com") || candidate.contains(".in")
        val isBankName = KNOWN_BANKS.any { b -> candidate.split(Regex("\\s+")).any { it.equals(b, ignoreCase = true) } }
        val isGateway = PAYMENT_GATEWAYS.any { candidate.equals(it, ignoreCase = true) }

        return if (!isPhone && !isUrl && !isBankName && !isGateway && candidate.length > 2) candidate else null
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PATTERN DETECTION
    // ════════════════════════════════════════════════════════════════════════

    private fun detectPattern(
        body: String, hasAction: Boolean, hasMethod: Boolean, hasBank: Boolean, hasAccount: Boolean
    ): SmsPattern {
        val lower = body.lowercase()

        // HDFC card spend: "Spent Rs.105 On HDFC Bank Card 6763 At"
        // 1. Specially defined patterns (Strongest signals)
        if (HDFC_CARD_SPEND_PATTERN.matcher(body).find()) return SmsPattern.CARD_SPEND
        if (CARDMEMBER_PAYMENT_PATTERN.matcher(body).find()) return SmsPattern.CARD_PAYMENT_RECEIVED
        if (CREDIT_ALERT_PATTERN.matcher(body).find()) return SmsPattern.BANK_CREDIT
        if (UNION_BANK_PATTERN.matcher(body).find()) return SmsPattern.BANK_DEBIT
        if (PLUXEE_SPEND_PATTERN.matcher(body).find()) return SmsPattern.WALLET_PAYMENT

        if (HDFC_UPDATE_PATTERN.matcher(body).find()) {
            return when {
                lower.contains("autopay") || lower.contains("si-tad") -> SmsPattern.AUTOPAY_DEBIT
                lower.contains("ach") || lower.contains("umrn") -> SmsPattern.ACH_DEBIT
                lower.contains("emi") -> SmsPattern.EMI_DEBIT
                else -> SmsPattern.BANK_DEBIT
            }
        }

        // 2. Keyword-based specifics
        if (lower.contains("upi")) {
            return if (CREDIT_KEYWORDS.any { lower.contains(it) }) SmsPattern.UPI_RECEIVED
            else SmsPattern.UPI_SENT
        }

        if (lower.contains("atm")) return SmsPattern.ATM_WITHDRAWAL

        if (lower.contains("neft") || lower.contains("imps") || lower.contains("rtgs")) {
            return SmsPattern.NEFT_TRANSFER
        }

        if (lower.contains("wallet") || lower.contains("pluxee") || lower.contains("paytm") || 
            lower.contains("phonepe") || lower.contains("gpay") || lower.contains("amazon pay")) {
            return SmsPattern.WALLET_PAYMENT
        }

        if (lower.contains("autopay") || lower.contains("si-tad") || lower.contains("si-mad")) {
            return SmsPattern.AUTOPAY_DEBIT
        }
        
        if (lower.contains("umrn") || lower.contains("ach")) return SmsPattern.ACH_DEBIT

        // 3. Generic Structure matches
        if (lower.contains("card") && (lower.contains("spent") || lower.contains("at "))) {
            return SmsPattern.CARD_SPEND
        }

        if (hasAction && hasBank) {
            return if (CREDIT_KEYWORDS.any { lower.contains(it) }) SmsPattern.BANK_CREDIT
            else SmsPattern.BANK_DEBIT
        }

        if (hasAction && hasAccount) {
            return if (CREDIT_KEYWORDS.any { lower.contains(it) }) SmsPattern.BANK_CREDIT
            else SmsPattern.BANK_DEBIT
        }

        return SmsPattern.UNKNOWN
    }

    // ════════════════════════════════════════════════════════════════════════
    //  UTILITIES
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Normalize: merge multi-line fragments, collapse runs of whitespace to single space.
     */
    private fun normalizeMessage(body: String): String {
        return body
            .replace("\r\n", " ")
            .replace("\n", " ")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    }

    /**
     * SHA-256 hash for duplicate detection.
     */
    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
