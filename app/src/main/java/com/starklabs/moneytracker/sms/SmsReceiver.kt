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
import com.starklabs.moneytracker.domain.ParsedSms
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
                database.categoryDao(),
                database.merchantMappingDao()
            )
            val scope = CoroutineScope(Dispatchers.IO)

            scope.launch {
                try {
                    messages?.forEach { sms ->
                        val sender = sms.originatingAddress ?: "Unknown"
                        val body = sms.messageBody ?: ""
                        val timestamp = sms.timestampMillis

                        Log.d("SmsReceiver", "Received SMS from $sender")

                        val parsed = SmsParser.parseSms(sender, body, timestamp)
                        
                        // Check if it's a valid transaction
                        if (parsed.isTransaction) {
                            // All required fields are guaranteed to be non-null when isTransaction = true
                            val amount = parsed.amount ?: run {
                                Log.w("SmsReceiver", "Transaction marked valid but amount is null")
                                return@forEach
                            }
                            
                            val merchant = parsed.merchant ?: "Unknown Merchant"
                            val transactionType = parsed.transactionType?.uppercase() ?: "DEBIT"
                            
                            // 1. Find or Create Account
                            val matchedAccount = repository.findAccountForSms(parsed.accountLast4)
                            val accountId = if (matchedAccount != null) {
                                matchedAccount.id
                            } else {
                                val last4 = parsed.accountLast4
                                val bankName = parsed.bank ?: "Unknown"
                                val accountType = when(parsed.patternUsed) {
                                    com.starklabs.moneytracker.domain.SmsPattern.CARD_SPEND -> "CARD"
                                    com.starklabs.moneytracker.domain.SmsPattern.WALLET_PAYMENT -> "WALLET"
                                    else -> "BANK"
                                }
                                val newAccount = com.starklabs.moneytracker.data.Account(
                                    name = if (last4 == null) bankName else "$bankName •••• $last4",
                                    type = accountType,
                                    balance = 0.0,
                                    last4Digits = last4,
                                    colorHex = "#FFD700"
                                )
                                repository.addAccount(newAccount).toInt()
                            }

                            // 2. Find Category
                            val categoryId = repository.identifyCategory(merchant, parsed.rawMessage)

                            val transaction = Transaction(
                                amount = amount,
                                merchant = merchant,
                                date = timestamp, // Use the SMS timestamp
                                type = transactionType, // "DEBIT" or "CREDIT"
                                smsBody = parsed.rawMessage,
                                accountId = accountId,
                                categoryId = categoryId
                            )

                            // Balance is not extracted in strict mode
                            val balanceToUpdate: Double? = null 

                            repository.addTransaction(transaction, balanceToUpdate)
                        } else {
                            Log.d("SmsReceiver", "SMS rejected: ${parsed.reason}")
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
