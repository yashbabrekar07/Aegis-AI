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
import com.aegisai.app.data.ScanResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CallRecordService : Service() {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var currentPhone: String? = null
    private var isRecording = false
    private var isAnalyzing = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            ACTION_ANALYZE -> handleAnalyze()
        }
        return START_NOT_STICKY
    }

    private fun handleStart(intent: Intent) {
        if (isAnalyzing) return
        if (isRecording) {
            stopRecorderQuietly()
        }
        currentPhone = intent.getStringExtra(EXTRA_PHONE) ?: CallGuardState.lastCaller
        startForeground(RECORDING_NOTIFICATION_ID, buildRecordingNotification())
        try {
            val file = File(cacheDir, "call_${System.currentTimeMillis()}.m4a")
            outputFile = file
            recorder = createRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            isRecording = true
        } catch (e: Exception) {
            isRecording = false
            CallAnalysisNotifier.showError(
                applicationContext,
                currentPhone,
                "Could not start recording: ${e.message ?: "mic busy"}"
            )
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun handleAnalyze() {
        if (isAnalyzing) return
        isAnalyzing = true
        isRecording = false

        val file = stopRecorderQuietly()
        val phone = currentPhone

        startForeground(ANALYZE_NOTIFICATION_ID, buildAnalyzingNotification())

        scope.launch {
            try {
                if (file == null || !file.exists() || file.length() < MIN_BYTES) {
                    CallAnalysisNotifier.showError(
                        applicationContext,
                        phone,
                        "Not enough call audio captured. Use speakerphone or scan a transcript in Vishing."
                    )
                    return@launch
                }

                val prefs = AegisApp.get(applicationContext).prefs
                val result = ApiClient(prefs.apiBaseUrl).scanAudio(file)
                CallAnalysisNotifier.showResult(applicationContext, phone, result)
            } catch (e: Exception) {
                CallAnalysisNotifier.showError(
                    applicationContext,
                    phone,
                    e.message ?: "Analysis failed — check internet and backend."
                )
            } finally {
                file?.delete()
                outputFile = null
                isAnalyzing = false
                withContext(Dispatchers.Main) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    private fun stopRecorderQuietly(): File? {
        val file = outputFile
        try {
            recorder?.stop()
        } catch (_: Exception) { }
        try {
            recorder?.release()
        } catch (_: Exception) { }
        recorder = null
        isRecording = false
        return file
    }

    private fun createRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildRecordingNotification(): Notification {
        ensureChannel("call_guard_record", "Call recording")
        return NotificationCompat.Builder(this, "call_guard_record")
            .setContentTitle("Aegis — recording call")
            .setContentText("Analyzing for scams when the call ends…")
            .setSmallIcon(R.drawable.ic_shield)
            .setOngoing(true)
            .build()
    }

    private fun buildAnalyzingNotification(): Notification {
        ensureChannel("call_guard_analyze", "Call analysis")
        return NotificationCompat.Builder(this, "call_guard_analyze")
            .setContentTitle("Aegis — analyzing call")
            .setContentText("Checking for voice phishing…")
            .setSmallIcon(R.drawable.ic_shield)
            .setOngoing(true)
            .build()
    }

    private fun ensureChannel(id: String, name: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_START = "com.aegisai.app.call.START"
        const val ACTION_ANALYZE = "com.aegisai.app.call.ANALYZE"
        const val EXTRA_PHONE = "phone"
        private const val RECORDING_NOTIFICATION_ID = 4102
        private const val ANALYZE_NOTIFICATION_ID = 4103
        private const val MIN_BYTES = 8_000L
    }
}
