package com.starklabs.moneytracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.starklabs.moneytracker.data.MoneyRepository
import com.starklabs.moneytracker.domain.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val pendingResult = goAsync()
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val repository = MoneyRepository.getInstance(context)
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        scope.launch {
            try {
                messages?.forEach { sms ->
                    val sender = sms.originatingAddress ?: "Unknown"
                    val body = sms.messageBody ?: ""
                    val timestamp = sms.timestampMillis

                    Log.d("SmsReceiver", "Received SMS from $sender: $body")

                    val parsed = SmsParser.parseSms(sender, body, timestamp)
                    when (val result = TransactionProcessor.process(
                        parsed = parsed,
                        timestamp = timestamp,
                        body = parsed.rawMessage,
                        repository = repository
                    )) {
                        is TransactionProcessor.Result.Created ->
                            Log.d("SmsReceiver", "Inserted transaction rowId=${result.rowId}")
                        is TransactionProcessor.Result.Skipped ->
                            Log.d("SmsReceiver", "SMS skipped: ${result.reason}")
                    }
                }
            } catch (e: Exception) {
                Log.e("SmsReceiver", "Error processing SMS", e)
            } finally {
                pendingResult.finish()
                scope.coroutineContext[Job]?.cancel()
            }
        }
    }
}
