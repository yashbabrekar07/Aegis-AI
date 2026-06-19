package com.aegisai.app.call

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import com.aegisai.app.AegisApp

/** Orchestrates call sessions: screening alert → discovery → analysis. */
object CallGuardCoordinator {
    fun onIncomingCallScreened(context: Context, phoneNumber: String?): CallSession? {
        val appContext = context.applicationContext
        if (!AegisApp.get(appContext).prefs.callGuardEnabled) return null
        if (!CallGuardPermissions.canRunCallGuard(appContext)) return null

        val session = CallSessionStore.createSession(appContext, phoneNumber)
        CallGuardState.activeSessionId = session.id
        CallAnalysisNotifier.showIncomingCallAlert(appContext, session)
        CallSessionTracker.start(appContext, session.id)
        return session
    }

    fun onCallEnded(context: Context, sessionId: String) {
        val appContext = context.applicationContext
        CallGuardState.activeSessionId = null
        val session = CallSessionStore.getSession(appContext, sessionId) ?: return
        val updated = session.copy(
            endedAt = System.currentTimeMillis(),
            status = CallSession.STATUS_DISCOVERING,
        )
        CallSessionStore.saveSession(appContext, updated)

        startDiscoveryService(appContext, sessionId)
    }

    fun analyzeManualRecording(context: Context, sessionId: String, uri: Uri) {
        val appContext = context.applicationContext
        val session = CallSessionStore.getSession(appContext, sessionId) ?: return
        CallSessionStore.saveSession(
            appContext,
            session.copy(
                recordingUri = uri.toString(),
                detectionMethod = CallSession.DETECTION_MANUAL,
                status = CallSession.STATUS_ANALYZING,
            ),
        )
        startAnalysisService(appContext, sessionId)
    }

    fun retryDiscovery(context: Context, sessionId: String) {
        val appContext = context.applicationContext
        val session = CallSessionStore.getSession(appContext, sessionId) ?: return
        CallSessionStore.saveSession(
            appContext,
            session.copy(
                status = CallSession.STATUS_DISCOVERING,
                errorMessage = null,
                recordingUri = null,
                recordingDisplayName = null,
                result = null,
                endedAt = System.currentTimeMillis(),
            ),
        )
        startDiscoveryService(appContext, sessionId)
    }

    /** Restart discovery if the UI is open but the background service died. */
    fun ensureDiscoveryRunning(context: Context, sessionId: String) {
        if (CallAnalysisService.isRunning(sessionId)) return
        val session = CallSessionStore.getSession(context.applicationContext, sessionId) ?: return
        if (session.status != CallSession.STATUS_DISCOVERING) return
        startDiscoveryService(context.applicationContext, sessionId)
    }

    private fun startDiscoveryService(appContext: Context, sessionId: String) {
        val intent = Intent(appContext, CallAnalysisService::class.java).apply {
            action = CallAnalysisService.ACTION_DISCOVER_AND_ANALYZE
            putExtra(CallAnalysisService.EXTRA_SESSION_ID, sessionId)
        }
        try {
            ContextCompat.startForegroundService(appContext, intent)
        } catch (e: Exception) {
            try {
                appContext.startService(intent)
            } catch (_: Exception) {
                CallSessionStore.getSession(appContext, sessionId)?.let { session ->
                    CallSessionStore.saveSession(
                        appContext,
                        session.copy(
                            status = CallSession.STATUS_FAILED,
                            errorMessage = "Could not start analysis service. Open the app and try again.",
                        ),
                    )
                }
            }
        }
    }

    internal fun startAnalysisService(context: Context, sessionId: String) {
        val intent = Intent(context, CallAnalysisService::class.java).apply {
            action = CallAnalysisService.ACTION_ANALYZE_URI
            putExtra(CallAnalysisService.EXTRA_SESSION_ID, sessionId)
        }
        ContextCompat.startForegroundService(context.applicationContext, intent)
    }
}
