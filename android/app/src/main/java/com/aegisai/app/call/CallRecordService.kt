package com.aegisai.app.call

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.aegisai.app.AegisApp
import com.aegisai.app.R
import com.aegisai.app.data.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/**
 * Records ambient audio during a phone call using the microphone.
 * Note: Android blocks third-party apps from recording both sides of a call on most devices.
 */
class CallRecordService : Service() {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val phone = intent?.getStringExtra(EXTRA_PHONE)
        startForeground(NOTIFICATION_ID, buildNotification())
        startRecording(phone)
        return START_STICKY
    }

    private fun startRecording(phone: String?) {
        try {
            val file = File(cacheDir, "call_${System.currentTimeMillis()}.m4a")
            outputFile = file
            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            lastCaller = phone
        } catch (_: Exception) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        try {
            recorder?.stop()
        } catch (_: Exception) { }
        recorder?.release()
        recorder = null

        val file = outputFile
        val phone = lastCaller
        if (file != null && file.exists() && file.length() > 0) {
            scope.launch {
                try {
                    val prefs = AegisApp.get(applicationContext).prefs
                    val result = ApiClient(prefs.apiBaseUrl).scanAudio(file)
                    CallAnalysisNotifier.show(applicationContext, phone, result.risk, result.reason)
                } catch (_: Exception) { }
            }
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val channelId = "call_guard"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Call Guard",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Aegis Call Guard")
            .setContentText("Analyzing call audio for scams…")
            .setSmallIcon(R.drawable.ic_shield)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val EXTRA_PHONE = "phone"
        var lastCaller: String? = null
        private const val NOTIFICATION_ID = 4102
    }
}
