package com.starklabs.moneytracker.sms

import com.starklabs.moneytracker.data.Account
import com.starklabs.moneytracker.data.Category
import com.starklabs.moneytracker.data.MoneyRepository
import com.starklabs.moneytracker.data.Transaction
import com.starklabs.moneytracker.domain.ParsedSms

/**
 * Single source of truth for converting a [ParsedSms] into a stored [Transaction].
 *
 * Both [SmsReceiver] (real-time, single message) and [SmsScanner] (batch scan over
 * the inbox) delegate here so account resolution, category lookup, balance updates,
 * and per-session deduplication behave identically across both code paths.
 */
object TransactionProcessor {

    sealed class Result {
        data class Created(val rowId: Long, val newAccountCreated: Boolean) : Result()
        data class Skipped(val reason: String) : Result()
    }

    /**
     * @param parsed result of [com.starklabs.moneytracker.domain.SmsParser.parseSms]
     * @param timestamp epoch millis when the SMS was received
     * @param body raw SMS body, persisted on the transaction for traceability
     * @param sessionAccounts per-scan cache from accountLast4 -> accountId, so a batch
     *        scan does not re-hit the DB for the same masked number repeatedly
     * @param categories optionally pre-fetched categories to avoid N+1 lookups inside
     *        a tight loop; pass null for receiver-style one-off processing
     * @param seenHashes per-scan dedup set of [ParsedSms.messageHash] values; identical
     *        SMS bodies in the same batch are skipped
     */
    suspend fun process(
        parsed: ParsedSms,
        timestamp: Long,
        body: String,
        repository: MoneyRepository,
        sessionAccounts: MutableMap<String, Int>? = null,
        categories: List<Category>? = null,
        seenHashes: MutableSet<String>? = null
    ): Result {
        if (!parsed.isTransaction) {
            return Result.Skipped(parsed.reason ?: "non-transactional")
        }

        if (seenHashes != null && parsed.messageHash != null) {
            if (!seenHashes.add(parsed.messageHash)) {
                return Result.Skipped("duplicate message")
            }
        }

        val amount = parsed.amount?.takeIf { it > 0.0 }
            ?: return Result.Skipped("invalid or missing amount")

        var newAccountCreated = false
        val accountId: Int = resolveAccountId(parsed, repository, sessionAccounts) { created ->
            newAccountCreated = created
        } ?: return Result.Skipped("no resolvable account")

        val merchant = parsed.merchant ?: "Unknown Merchant"
        val transactionType = parsed.transactionType?.uppercase() ?: "DEBIT"
        val categoryId = repository.identifyCategory(merchant, body, categories)

        val transaction = Transaction(
            amount = amount,
            merchant = merchant,
            date = timestamp,
            type = transactionType,
            smsBody = body,
            accountId = accountId,
            categoryId = categoryId,
            smsHash = parsed.messageHash
        )

        val rowId = repository.addTransaction(transaction, parsed.balance)
        if (rowId == -1L) return Result.Skipped("conflict on insert")
        return Result.Created(rowId, newAccountCreated)
    }

    private suspend inline fun resolveAccountId(
        parsed: ParsedSms,
        repository: MoneyRepository,
        sessionAccounts: MutableMap<String, Int>?,
        onAccountCreated: (Boolean) -> Unit
    ): Int? {
        val last4 = parsed.accountLast4
        if (last4 == null) {
            return repository.getDefaultAccount()?.id
        }

        sessionAccounts?.get(last4)?.let { return it }

        val existing = repository.findAccountForSms(last4)
        val resolvedId = if (existing != null) {
            existing.id
        } else {
            val bankName = parsed.bank ?: "Bank"
            val newId = repository.addAccount(
                Account(
                    name = "$bankName - $last4",
                    type = "BANK",
                    balance = 0.0,
                    last4Digits = last4,
                    colorHex = "#FFD700"
                )
            ).toInt()
            onAccountCreated(true)
            newId
        }
        sessionAccounts?.put(last4, resolvedId)
        return resolvedId
    }
}
