package com.aegisai.app.call

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aegisai.app.R
import com.aegisai.app.data.ScanResult
import java.util.concurrent.atomic.AtomicInteger

object CallAnalysisNotifier {
    private val nextId = AtomicInteger(5000)

    fun showResult(context: Context, phone: String?, result: ScanResult) {
        if (result.error != null) {
            showError(context, phone, result.error)
            return
        }

        val risk = resolveRisk(result)
        val conf = ((result.confidence ?: 0.0) * 100).toInt()
        val title = when (risk) {
            "SCAM" -> "Scam call detected"
            "SAFE" -> "Call looks safe"
            else -> "Call analysis result"
        }
        val body = buildString {
            phone?.takeIf { it.isNotBlank() }?.let { append("From: $it\n") }
            append("Risk: $risk")
            if (conf > 0) append(" · $conf% confidence")
            append("\n\n")
            append(result.reason ?: "Open Aegis AI for details.")
            result.transcription?.takeIf { it.isNotBlank() }?.let {
                append("\n\nTranscript:\n")
                append(it.take(500))
                if (it.length > 500) append("…")
            }
        }
        val priority = if (risk == "SCAM") {
            NotificationCompat.PRIORITY_HIGH
        } else {
            NotificationCompat.PRIORITY_DEFAULT
        }
        notify(context, title, body, priority)
    }

    fun showError(context: Context, phone: String?, message: String?) {
        val body = buildString {
            phone?.takeIf { it.isNotBlank() }?.let { append("From: $it\n") }
            append(com.aegisai.app.data.ApiClient.humanizeAudioError(message))
            append("\n\nTip: Enable speakerphone on calls so Call Guard can hear both sides.")
        }
        notify(context, "Call Guard — could not analyze", body, NotificationCompat.PRIORITY_DEFAULT)
    }

    private fun resolveRisk(result: ScanResult): String {
        result.risk?.trim()?.uppercase()?.takeIf { it.isNotBlank() }?.let { return it }
        val label = result.label?.lowercase().orEmpty()
        return when {
            label.contains("phishing") || label.contains("scam") -> "SCAM"
            label.contains("legit") || label.contains("safe") -> "SAFE"
            else -> "UNKNOWN"
        }
    }

    private fun notify(context: Context, title: String, body: String, priority: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val channelId = "call_guard_alerts"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Call scam alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Results after each protected phone call"
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(title)
            .setContentText(body.lines().firstOrNull() ?: title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setAutoCancel(true)
            .build()

        val id = nextId.incrementAndGet()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
