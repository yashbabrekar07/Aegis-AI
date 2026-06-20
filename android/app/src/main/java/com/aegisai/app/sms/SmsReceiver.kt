package com.aegisai.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "AegisSmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            val sender = messages.firstOrNull()?.originatingAddress ?: "Unknown"
            val bodyBuilder = StringBuilder()
            for (msg in messages) {
                bodyBuilder.append(msg.messageBody ?: "")
            }
            val body = bodyBuilder.toString().trim()

            if (body.isBlank()) return

            Log.d(TAG, "SMS received from: $sender, length: ${body.length}")

            // Use background thread instead of foreground service to avoid
            // Android 12+ background launch restrictions
            SmsScanWorker.enqueue(context, sender, body)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing incoming SMS", e)
        }
    }
}
