package com.aegisai.app.call

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
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

/**
 * Single foreground service for Call Guard — records and analyzes without spawning
 * a second microphone FGS when a call is answered (avoids Android 12–14 crashes).
 */
class CallGuardWatchService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val callRecorder by lazy { CallGuardRecorder(this) }
    private var currentPhone: String? = null
    private var isAnalyzing = false

    override fun onCreate() {
        super.onCreate()
        CallGuardState.watchServiceRunning = true
    }

    override fun onDestroy() {
        CallGuardState.watchServiceRunning = false
        callRecorder.stop()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if (!AegisApp.get(this).prefs.callGuardEnabled) {
                stopGuard()
                return START_NOT_STICKY
            }

            when (intent?.action) {
                ACTION_START_RECORDING -> handleStartRecording(intent)
                ACTION_ANALYZE -> handleAnalyze()
                else -> promoteIdle()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Call Guard failed", t)
            try {
                promoteIdle()
            } catch (_: Throwable) { }
        }
        return START_STICKY
    }

    private fun handleStartRecording(intent: Intent) {
        promoteIdle()
        if (!CallGuardPermissions.canRunCallGuard(this)) {
            CallAnalysisNotifier.showError(
                applicationContext,
                null,
                "Call Guard needs microphone and phone permissions. Open Aegis → Vishing to enable."
            )
            return
        }
        if (isAnalyzing || callRecorder.isActive) return

        currentPhone = intent.getStringExtra(EXTRA_PHONE) ?: CallGuardState.lastCaller
        val file = callRecorder.start()
        if (file == null) {
            CallAnalysisNotifier.showError(
                applicationContext,
                currentPhone,
                "Could not access the mic during this call. Try speakerphone."
            )
            return
        }
        scope.launch {
            try {
                ApiClient(AegisApp.get(applicationContext).prefs.apiBaseUrl).wakeBackend()
            } catch (_: Exception) { }
        }
        promoteRecording()
    }

    private fun handleAnalyze() {
        if (isAnalyzing) return
        isAnalyzing = true
        promoteAnalyzing("Preparing analysis…")

        val phone = currentPhone
        // Let AudioRecord flush the last buffers before closing the WAV file
        Thread.sleep(350)
        val file = callRecorder.stop()
        val durationMs = callRecorder.recordedDurationMs()
        promoteIdle()

        scope.launch {
            try {
                if (file == null || !file.exists()) {
                    CallAnalysisNotifier.showError(
                        applicationContext,
                        phone,
                        "No recording file. Enable mic permission and try again."
                    )
                    return@launch
                }
                if (durationMs < 800 || file.length() < MIN_BYTES) {
                    CallAnalysisNotifier.showError(
                        applicationContext,
                        phone,
                        "Call too short or no speech captured (${durationMs / 1000}s). " +
                            "Use speakerphone during the call so the mic can hear both sides."
                    )
                    return@launch
                }
                if (file.length() > MAX_BYTES) {
                    CallAnalysisNotifier.showError(
                        applicationContext,
                        phone,
                        "Recording is very long — try a shorter call or paste transcript in Vishing."
                    )
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    promoteAnalyzing("Transcribing call… (usually 15–45 sec)")
                }
                val api = ApiClient(AegisApp.get(applicationContext).prefs.apiBaseUrl)
                val result = api.analyzeCallRecording(file, phone)
                CallAnalysisNotifier.showResult(applicationContext, phone, result)
            } catch (e: Exception) {
                CallAnalysisNotifier.showError(
                    applicationContext,
                    phone,
                    ApiClient.friendlyError(e)
                )
            } finally {
                file?.delete()
                isAnalyzing = false
                withContext(Dispatchers.Main) { promoteIdle() }
            }
        }
    }

    private fun promoteIdle() {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(
                "Aegis Call Guard is on",
                "We will analyze each call when it ends and alert you."
            ),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun promoteRecording() {
        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(
                "Aegis — recording call",
                "Analyzing for scams when the call ends…"
            ),
            fgsType
        )
    }

    private fun promoteAnalyzing(status: String) {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification("Aegis — analyzing call", status),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun stopGuard() {
        callRecorder.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(title: String, text: String): Notification {
        ensureChannel()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_shield)
            .setOngoing(true)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Call Guard",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "CallGuardWatch"
        const val ACTION_START_RECORDING = "com.aegisai.app.call.START_RECORDING"
        const val ACTION_ANALYZE = "com.aegisai.app.call.ANALYZE"
        const val EXTRA_PHONE = "phone"
        private const val CHANNEL_ID = "call_guard_watch"
        private const val NOTIFICATION_ID = 4101
        /** ~0.25s of 16 kHz mono WAV PCM */
        private const val MIN_BYTES = 8_000L
        private const val MAX_BYTES = 12 * 1024 * 1024L
    }
}
