package com.starklabs.moneytracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.starklabs.moneytracker.data.AppDatabase
import com.starklabs.moneytracker.data.MoneyRepository
import com.starklabs.moneytracker.data.Transaction
import com.starklabs.moneytracker.domain.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val pendingResult = goAsync()
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val database = AppDatabase.getDatabase(context)
            // Manual construction since Hilt isn't set up
            val repository = MoneyRepository(
                database.transactionDao(),
                database.accountDao(),
                database.categoryDao()
            )
            val scope = CoroutineScope(Dispatchers.IO)

            scope.launch {
                try {
                    messages?.forEach { sms ->
                        val sender = sms.originatingAddress ?: "Unknown"
                        val body = sms.messageBody ?: ""
                        val timestamp = sms.timestampMillis

                        Log.d("SmsReceiver", "Received SMS from $sender: $body")

                        val parsed = SmsParser.parseSms(sender, body, timestamp)
                        if (parsed != null) {
                            // 1. Find Account
                            val matchedAccount = repository.findAccountForSms(parsed.accountLast4)
                            val accountId = matchedAccount?.id ?: repository.getDefaultAccount()?.id ?: 1 // Fallback

                            // 2. Find Category
                            val categoryId = repository.identifyCategory(parsed.merchant, parsed.smsBody)

                            val transaction = Transaction(
                                amount = parsed.amount,
                                merchant = parsed.merchant,
                                date = parsed.date,
                                type = parsed.type,
                                smsBody = parsed.smsBody,
                                accountId = accountId,
                                categoryId = categoryId
                            )

                            // Only update balance if we are sure about the account
                            val balanceToUpdate = if (matchedAccount != null) parsed.balance else null
                            Log.d("SmsReceiver", "Inserted Transaction: $transaction with balance: $balanceToUpdate")

                            repository.addTransaction(transaction, balanceToUpdate)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Error processing SMS", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
