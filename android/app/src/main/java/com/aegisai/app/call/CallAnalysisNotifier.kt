package com.aegisai.app.call

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aegisai.app.R
import com.aegisai.app.data.ScanResult
import com.aegisai.app.ui.call.CallAnalysisResultActivity
import com.aegisai.app.ui.call.CallRecordingPickerActivity
import com.aegisai.app.ui.main.MainActivity
import java.util.concurrent.atomic.AtomicInteger

object CallAnalysisNotifier {
    private val nextId = AtomicInteger(5000)

    const val EXTRA_SESSION_ID = "session_id"

    fun showIncomingCallAlert(context: Context, session: CallSession) {
        val phone = session.phoneNumber ?: "Unknown"
        val body = context.getString(R.string.call_guard_incoming_body)
        val openApp = pendingMain(context, session.id, REQUEST_OPEN_APP)
        notify(
            context,
            channelId = CHANNEL_INCOMING,
            channelName = context.getString(R.string.call_guard_channel_incoming),
            importance = NotificationManager.IMPORTANCE_HIGH,
            notificationId = session.id.hashCode(),
            title = context.getString(R.string.call_guard_incoming_title, phone),
            body = body,
            priority = NotificationCompat.PRIORITY_HIGH,
            contentIntent = openApp,
            ongoing = true,
        )
    }

    fun showAnalyzingProgress(context: Context, phone: String?, sessionId: String? = null) {
        val title = context.getString(R.string.call_guard_analyzing_title)
        val body = phone?.let {
            context.getString(R.string.call_guard_analyzing_from, it)
        } ?: context.getString(R.string.call_guard_analyzing_body)
        val contentIntent = sessionId?.let {
            pendingActivity(context, CallAnalysisResultActivity.intent(context, it), REQUEST_VIEW_RESULT)
        }
        notify(
            context,
            channelId = CHANNEL_ALERTS,
            channelName = context.getString(R.string.call_guard_channel_alerts),
            importance = NotificationManager.IMPORTANCE_DEFAULT,
            notificationId = sessionId?.hashCode() ?: nextId.incrementAndGet(),
            title = title,
            body = body,
            priority = NotificationCompat.PRIORITY_DEFAULT,
            contentIntent = contentIntent,
            ongoing = false,
        )
    }

    /** Opens result screen directly — used when notifications are blocked or analysis completes. */
    fun openResultActivity(context: Context, sessionId: String) {
        try {
            val intent = CallAnalysisResultActivity.intent(context, sessionId)
            context.startActivity(intent)
        } catch (_: Exception) {
            // Background activity start may be blocked on some OEMs — notification is fallback
        }
    }

    fun showNoRecordingFound(context: Context, session: CallSession) {
        val pickIntent = pendingActivity(
            context,
            CallRecordingPickerActivity.intent(context, session.id),
            REQUEST_PICK_FILE,
        )
        val body = context.getString(R.string.call_guard_no_recording_body)
        notify(
            context,
            channelId = CHANNEL_ALERTS,
            channelName = context.getString(R.string.call_guard_channel_alerts),
            importance = NotificationManager.IMPORTANCE_DEFAULT,
            notificationId = session.id.hashCode(),
            title = context.getString(R.string.call_guard_no_recording_title),
            body = body,
            priority = NotificationCompat.PRIORITY_DEFAULT,
            contentIntent = pickIntent,
            ongoing = false,
            actionTitle = context.getString(R.string.call_guard_pick_recording),
            actionIntent = pickIntent,
        )
    }

    fun showResult(context: Context, session: CallSession) {
        val result = session.result ?: return
        showResult(context, session.phoneNumber, result, session.id)
    }

    fun showResult(context: Context, phone: String?, result: ScanResult, sessionId: String? = null) {
        if (result.error != null) {
            showError(context, phone, result.error, sessionId)
            return
        }

        val risk = resolveRisk(result)
        val conf = result.confidence?.let { (it * 100).toInt() }
        val title = when (risk) {
            "SCAM" -> "Scam call detected"
            "SAFE" -> "Call looks safe"
            else -> "Call analysis result"
        }
        val body = buildString {
            phone?.takeIf { it.isNotBlank() }?.let { append("From: $it\n") }
            result.detected_language?.takeIf { it.isNotBlank() }?.let {
                append("Language: ${it.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }}\n")
            }
            append("Risk: $risk")
            if (conf != null) append(" · $conf% confidence")
            append("\n\n")
            append(result.reason ?: "Open Aegis AI for details.")
            result.transcription?.takeIf { it.isNotBlank() }?.let {
                append("\n\nTranscript:\n")
                append(it.take(500))
                if (it.length > 500) append("…")
            }
        }
        val priority = if (risk == "SCAM") NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT
        val contentIntent = sessionId?.let {
            pendingActivity(context, CallAnalysisResultActivity.intent(context, it), REQUEST_VIEW_RESULT)
        }
        notify(
            context,
            channelId = CHANNEL_ALERTS,
            channelName = context.getString(R.string.call_guard_channel_alerts),
            importance = NotificationManager.IMPORTANCE_HIGH,
            notificationId = sessionId?.hashCode() ?: nextId.incrementAndGet(),
            title = title,
            body = body,
            priority = priority,
            contentIntent = contentIntent,
            ongoing = false,
        )
    }

    fun showError(context: Context, phone: String?, message: String?, sessionId: String? = null) {
        val body = buildString {
            phone?.takeIf { it.isNotBlank() }?.let { append("From: $it\n") }
            append(com.aegisai.app.data.ApiClient.humanizeAudioError(message))
            append("\n\n")
            append(context.getString(R.string.call_guard_error_tip))
        }
        val contentIntent = sessionId?.let {
            pendingActivity(context, CallAnalysisResultActivity.intent(context, it), REQUEST_VIEW_RESULT)
        }
        notify(
            context,
            channelId = CHANNEL_ALERTS,
            channelName = context.getString(R.string.call_guard_channel_alerts),
            importance = NotificationManager.IMPORTANCE_DEFAULT,
            notificationId = sessionId?.hashCode() ?: nextId.incrementAndGet(),
            title = context.getString(R.string.call_guard_error_title),
            body = body,
            priority = NotificationCompat.PRIORITY_DEFAULT,
            contentIntent = contentIntent,
            ongoing = false,
        )
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

    private fun pendingMain(context: Context, sessionId: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_SESSION_ID, sessionId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return pendingActivity(context, intent, requestCode)
    }

    private fun pendingActivity(context: Context, intent: Intent, requestCode: Int): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getActivity(context, requestCode, intent, flags)
    }

    private fun notify(
        context: Context,
        channelId: String,
        channelName: String,
        importance: Int,
        notificationId: Int,
        title: String,
        body: String,
        priority: Int,
        contentIntent: PendingIntent?,
        ongoing: Boolean,
        actionTitle: String? = null,
        actionIntent: PendingIntent? = null,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                // Notifications blocked — caller should use openResultActivity() as fallback
                return
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, importance)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(title)
            .setContentText(body.lines().firstOrNull() ?: title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setAutoCancel(!ongoing)
            .setOngoing(ongoing)

        contentIntent?.let { builder.setContentIntent(it) }
        if (actionTitle != null && actionIntent != null) {
            builder.addAction(0, actionTitle, actionIntent)
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    private const val CHANNEL_INCOMING = "call_guard_incoming"
    private const val CHANNEL_ALERTS = "call_guard_alerts"
    private const val REQUEST_OPEN_APP = 6001
    private const val REQUEST_VIEW_RESULT = 6002
    private const val REQUEST_PICK_FILE = 6003
}
