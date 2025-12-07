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
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            val database = AppDatabase.getDatabase(context)
            // Manual construction since Hilt isn't set up
            val repository = MoneyRepository(
                database.transactionDao(),
                database.accountDao(),
                database.categoryDao()
            )
            val scope = CoroutineScope(Dispatchers.IO)

            messages?.forEach { sms ->
                val sender = sms.originatingAddress ?: "Unknown"
                val body = sms.messageBody ?: ""
                val timestamp = sms.timestampMillis

                Log.d("SmsReceiver", "Received SMS from $sender: $body")

                val parsed = SmsParser.parseSms(sender, body, timestamp)
                if (parsed != null) {
                    scope.launch {
                        // 1. Find Account
                        val account = repository.findAccountForSms(parsed.accountLast4)
                        val accountId = account?.id ?: 1 // Default to first account (Cash) if not found
                        
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

                        Log.d("SmsReceiver", "Inserted Transaction: $transaction")
                        repository.addTransaction(transaction)
                    }
                }
            }
        }
    }
}
