package com.aegisai.app.sms

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aegisai.app.AegisApp
import com.aegisai.app.R
import com.aegisai.app.data.ApiClient
import com.aegisai.app.data.ScanResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Lightweight background worker that scans SMS text via the API.
 * Does NOT require a foreground service, so it works even when the
 * app is in the background on Android 12+.
 */
object SmsScanWorker {
    private const val TAG = "SmsScanWorker"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun enqueue(context: Context, sender: String, body: String) {
        val appContext = context.applicationContext
        scope.launch {
            try {
                analyzeSms(appContext, sender, body)
            } catch (e: Exception) {
                Log.e(TAG, "SMS analysis failed for sender=$sender", e)
                // Still save the record so the user sees it in the history
                val record = SmsStore.createRecord(appContext, sender, body)
                val failed = record.copy(result = ScanResult(error = "Analysis failed: ${e.message}"))
                SmsStore.saveRecord(appContext, failed)
            }
        }
    }

    private suspend fun analyzeSms(context: Context, sender: String, body: String) {
        val record = SmsStore.createRecord(context, sender, body)

        try {
            val api = ApiClient(AegisApp.get(context).prefs.apiBaseUrl)
            val result = api.scanText(body, sender)

            val updated = record.copy(result = result)
            SmsStore.saveRecord(context, updated)

            Log.d(TAG, "SMS from $sender analyzed: risk=${result.risk}, confidence=${result.confidence}")

            // Notify user if it's a scam
            val risk = result.risk?.uppercase() ?: "UNKNOWN"
            if (risk == "SCAM" || risk == "PHISHING" || risk == "HIGH") {
                notifyScamSms(context, updated)
            }
        } catch (e: Exception) {
            Log.e(TAG, "API scan failed", e)
            val failed = record.copy(result = ScanResult(error = ApiClient.friendlyError(e)))
            SmsStore.saveRecord(context, failed)
        }
    }

    private fun notifyScamSms(context: Context, record: SmsRecord) {
        // Check notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "POST_NOTIFICATIONS not granted, skipping scam notification")
                return
            }
        }

        val result = record.result ?: return
        val risk = result.risk ?: "SCAM"
        val conf = result.confidence?.let { (it * 100).toInt() }

        val title = "\u26a0\ufe0f Scam SMS detected!"
        val bodyText = buildString {
            append("From: ${record.sender}\n")
            append("Risk: $risk")
            if (conf != null) append(" · $conf% confidence")
            append("\n\n")
            append(result.reason ?: "Suspicious text message detected.")
        }

        val channelId = "sms_alerts"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SMS Scam Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when Aegis detects a scam SMS"
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(title)
            .setContentText("Suspicious SMS from ${record.sender}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(record.id.hashCode(), notification)
    }
}
