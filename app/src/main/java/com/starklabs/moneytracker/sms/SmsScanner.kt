package com.starklabs.moneytracker.sms

import android.content.Context
import android.provider.Telephony
import android.util.Log
import com.starklabs.moneytracker.data.Account
import com.starklabs.moneytracker.data.MoneyRepository
import com.starklabs.moneytracker.data.Transaction
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

                    if (parsed.isTransaction && parsed.accountLast4 != null) {
                        val last4 = parsed.accountLast4!!

                        var accountId = sessionAccounts[last4]
                        if (accountId == null) {
                            val existingAccount = repository.findAccountForSms(last4)
                            if (existingAccount != null) {
                                accountId = existingAccount.id
                            } else {
                                val bankName = parsed.bank ?: "Bank"
                                val newAccount = Account(
                                    name = "$bankName - $last4",
                                    type = "BANK",
                                    balance = 0.0,
                                    maskedNumber = last4,
                                    colorHex = "#FFD700"
                                )
                                val id = repository.addAccount(newAccount)
                                accountId = id.toInt()
                                newAccountsCreated++
                            }
                            sessionAccounts[last4] = accountId
                        }

                        val amount = parsed.amount ?: 0.0
                        if (amount > 0.0) {
                            val merchant = parsed.merchant ?: "Unknown Merchant"
                            val transactionType = parsed.transactionType?.uppercase() ?: "DEBIT"

                            val transaction = Transaction(
                                amount = amount,
                                merchant = merchant,
                                date = timestamp,
                                type = transactionType,
                                smsBody = body,
                                accountId = accountId,
                                categoryId = repository.identifyCategory(merchant, body, categories)
                            )

                            repository.addTransaction(transaction)
                            transactionsCreated++
                        }
                    } else {
                        messagesRejected++
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
