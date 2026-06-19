package com.aegisai.app.sms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.aegisai.app.AegisApp
import com.aegisai.app.R
import com.aegisai.app.call.CallAnalysisNotifier
import com.aegisai.app.data.ApiClient
import com.aegisai.app.data.ScanResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SmsAnalysisService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sender = intent?.getStringExtra(EXTRA_SENDER)
        val body = intent?.getStringExtra(EXTRA_BODY)
        
        if (sender.isNullOrBlank() || body.isNullOrBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }

        promoteForeground("Analyzing SMS", "Scanning incoming message from $sender...")
        
        scope.launch {
            try {
                analyzeSms(sender, body)
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        
        return START_NOT_STICKY
    }

    private suspend fun analyzeSms(sender: String, body: String) {
        val appContext = applicationContext
        val record = SmsStore.createRecord(appContext, sender, body)
        
        try {
            val api = ApiClient(AegisApp.get(appContext).prefs.apiBaseUrl)
            // Just scan text since SMS is just text
            val result = api.scanText(body)
            
            val updated = record.copy(result = result)
            SmsStore.saveRecord(appContext, updated)
            
            val risk = result.risk?.uppercase() ?: "UNKNOWN"
            if (risk == "SCAM" || risk == "PHISHING") {
                notifyScamSms(appContext, updated)
            }
        } catch (e: Exception) {
            val failed = record.copy(result = ScanResult(error = ApiClient.friendlyError(e)))
            SmsStore.saveRecord(appContext, failed)
        }
    }
    
    private fun notifyScamSms(context: Context, record: SmsRecord) {
        val result = record.result ?: return
        val risk = result.risk ?: "SCAM"
        val conf = result.confidence?.let { (it * 100).toInt() }
        
        val title = "Scam SMS detected!"
        val bodyText = buildString {
            append("From: ${record.sender}\n")
            append("Risk: $risk")
            if (conf != null) append(" · $conf% confidence\n\n")
            append(result.reason ?: "Suspicious text message detected.")
        }
        
        val channelId = "sms_alerts"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SMS Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(title)
            .setContentText(bodyText.lines().firstOrNull() ?: title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
            
        androidx.core.app.NotificationManagerCompat.from(context).notify(record.id.hashCode(), notification)
    }

    private fun promoteForeground(title: String, text: String) {
        ensureChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_shield)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()
            
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SMS Analysis",
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val EXTRA_SENDER = "sender"
        const val EXTRA_BODY = "body"
        private const val CHANNEL_ID = "sms_analysis"
        private const val NOTIFICATION_ID = 4202
        
        fun start(context: Context, sender: String, body: String) {
            val intent = Intent(context, SmsAnalysisService::class.java).apply {
                putExtra(EXTRA_SENDER, sender)
                putExtra(EXTRA_BODY, body)
            }
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }
    }
}
