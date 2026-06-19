package com.aegisai.app.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages.firstOrNull()?.originatingAddress ?: "Unknown"
        val bodyBuilder = StringBuilder()
        for (msg in messages) {
            bodyBuilder.append(msg.messageBody)
        }
        val body = bodyBuilder.toString()
        
        if (body.isNotBlank()) {
            SmsAnalysisService.start(context, sender, body)
        }
    }
}
