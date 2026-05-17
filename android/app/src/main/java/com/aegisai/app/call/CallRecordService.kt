package com.aegisai.app.call

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.aegisai.app.AegisApp
import com.aegisai.app.R
import com.aegisai.app.data.ApiClient
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
            else -> finishService()
        }
        return START_NOT_STICKY
    }

    private fun handleStart(intent: Intent) {
        promoteForeground(
            RECORDING_NOTIFICATION_ID,
            buildRecordingNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        )
        if (isAnalyzing) {
            finishService()
            return
        }
        if (isRecording) {
            stopRecorderQuietly()
        }
        currentPhone = intent.getStringExtra(EXTRA_PHONE) ?: CallGuardState.lastCaller
        try {
            val file = File(cacheDir, "call_${System.currentTimeMillis()}.m4a")
            outputFile = file
            recorder = createRecorder()
            try {
                configureRecorder(recorder!!, file, MediaRecorder.AudioSource.VOICE_RECOGNITION)
            } catch (_: Exception) {
                recorder?.release()
                recorder = createRecorder()
                configureRecorder(recorder!!, file, MediaRecorder.AudioSource.MIC)
            }
            isRecording = true
        } catch (e: Exception) {
            isRecording = false
            CallAnalysisNotifier.showError(
                applicationContext,
                currentPhone,
                "Could not start recording: ${e.message ?: "mic busy"}"
            )
            finishService()
        }
    }

    private fun handleAnalyze() {
        promoteForeground(
            ANALYZE_NOTIFICATION_ID,
            buildAnalyzingNotification("Waking server…"),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
        if (isAnalyzing) {
            finishService()
            return
        }
        isAnalyzing = true
        isRecording = false

        val file = stopRecorderQuietly()
        val phone = currentPhone

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
                if (file.length() > MAX_BYTES) {
                    CallAnalysisNotifier.showError(
                        applicationContext,
                        phone,
                        "Recording is very long — server may time out. Try a shorter call or paste transcript in Vishing."
                    )
                    return@launch
                }

                val api = ApiClient(AegisApp.get(applicationContext).prefs.apiBaseUrl)
                withContext(Dispatchers.Main) {
                    updateAnalyzingNotification("Transcribing call audio… (can take 1–3 min)")
                }
                val result = api.scanAudio(file)
                CallAnalysisNotifier.showResult(applicationContext, phone, result)
            } catch (e: Exception) {
                CallAnalysisNotifier.showError(
                    applicationContext,
                    phone,
                    ApiClient.friendlyError(e)
                )
            } finally {
                file?.delete()
                outputFile = null
                isAnalyzing = false
                withContext(Dispatchers.Main) {
                    finishService()
                }
            }
        }
    }

    private fun finishService() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun promoteForeground(notificationId: Int, notification: Notification, fgsType: Int) {
        ServiceCompat.startForeground(this, notificationId, notification, fgsType)
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

    private fun configureRecorder(recorder: MediaRecorder, file: File, source: Int) {
        recorder.apply {
            setAudioSource(source)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            try {
                setAudioSamplingRate(16_000)
                setAudioEncodingBitRate(48_000)
            } catch (_: Exception) { }
            setOutputFile(file.absolutePath)
            prepare()
            start()
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

    private fun buildAnalyzingNotification(text: String = "Checking for voice phishing…"): Notification {
        ensureChannel("call_guard_analyze", "Call analysis")
        return NotificationCompat.Builder(this, "call_guard_analyze")
            .setContentTitle("Aegis — analyzing call")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_shield)
            .setOngoing(true)
            .build()
    }

    private fun updateAnalyzingNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(ANALYZE_NOTIFICATION_ID, buildAnalyzingNotification(text))
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
        private const val MAX_BYTES = 12 * 1024 * 1024L
    }
}
