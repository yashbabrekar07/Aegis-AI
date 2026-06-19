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

        val intent = Intent(appContext, CallAnalysisService::class.java).apply {
            action = CallAnalysisService.ACTION_DISCOVER_AND_ANALYZE
            putExtra(CallAnalysisService.EXTRA_SESSION_ID, sessionId)
        }
        try {
            ContextCompat.startForegroundService(appContext, intent)
        } catch (_: Exception) {
            appContext.startService(intent)
        }
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
            session.copy(status = CallSession.STATUS_DISCOVERING, errorMessage = null),
        )
        val intent = Intent(appContext, CallAnalysisService::class.java).apply {
            action = CallAnalysisService.ACTION_DISCOVER_AND_ANALYZE
            putExtra(CallAnalysisService.EXTRA_SESSION_ID, sessionId)
        }
        ContextCompat.startForegroundService(appContext, intent)
    }

    internal fun startAnalysisService(context: Context, sessionId: String) {
        val intent = Intent(context, CallAnalysisService::class.java).apply {
            action = CallAnalysisService.ACTION_ANALYZE_URI
            putExtra(CallAnalysisService.EXTRA_SESSION_ID, sessionId)
        }
        ContextCompat.startForegroundService(context.applicationContext, intent)
    }
}
