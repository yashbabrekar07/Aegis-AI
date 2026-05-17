package com.aegisai.app.call

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aegisai.app.R

object CallAnalysisNotifier {
    fun show(context: Context, phone: String?, risk: String?, reason: String?) {
        val channelId = "call_guard_alerts"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Call scam alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val title = when (risk?.uppercase()) {
            "SCAM" -> "Possible scam call detected"
            "SAFE" -> "Call looks safe"
            else -> "Call analysis complete"
        }
        val body = buildString {
            phone?.let { append("From: $it\n") }
            append(reason ?: "Review the result in Aegis AI.")
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify((phone ?: "call").hashCode(), notification)
    }
}
