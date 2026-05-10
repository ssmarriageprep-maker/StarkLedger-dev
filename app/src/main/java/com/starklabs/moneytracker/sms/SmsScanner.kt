package com.starklabs.moneytracker.sms

import android.content.Context
import android.provider.Telephony
import android.util.Log
import com.starklabs.moneytracker.data.MoneyRepository
import com.starklabs.moneytracker.domain.SmsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

object SmsScanner {

    suspend fun scan(context: Context, repository: MoneyRepository): ScanResult = withContext(Dispatchers.IO) {
        val projection = arrayOf("address", "body", "date")
        val cursor = context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            null,
            null,
            "date DESC LIMIT 1000"
        )

        val sessionAccounts = mutableMapOf<String, Int>()
        val seenHashes = mutableSetOf<String>()
        var transactionsCreated = 0
        var messagesRejected = 0
        var newAccountsCreated = 0

        // Pre-fetch categories once to avoid redundant DB queries in the loop
        val categories = repository.allCategories.firstOrNull() ?: emptyList()

        cursor?.use {
            val addressIdx = it.getColumnIndex("address")
            val bodyIdx = it.getColumnIndex("body")
            val dateIdx = it.getColumnIndex("date")

            while (it.moveToNext()) {
                try {
                    val sender = it.getString(addressIdx) ?: "Unknown"
                    val body = it.getString(bodyIdx) ?: continue
                    val timestamp = it.getLong(dateIdx)

                    val parsed = SmsParser.parseSms(sender, body, timestamp)
                    when (val result = TransactionProcessor.process(
                        parsed = parsed,
                        timestamp = timestamp,
                        body = body,
                        repository = repository,
                        sessionAccounts = sessionAccounts,
                        categories = categories,
                        seenHashes = seenHashes
                    )) {
                        is TransactionProcessor.Result.Created -> {
                            transactionsCreated++
                            if (result.newAccountCreated) newAccountsCreated++
                        }
                        is TransactionProcessor.Result.Skipped -> messagesRejected++
                    }
                } catch (e: Exception) {
                    Log.e("SmsScanner", "Error processing message", e)
                }
            }
        }

        ScanResult(newAccountsCreated, transactionsCreated, messagesRejected)
    }

    data class ScanResult(
        val accountsAdded: Int,
        val transactionsCreated: Int,
        val messagesFiltered: Int
    )
}
