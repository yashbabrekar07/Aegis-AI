package com.aegisai.app.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.aegisai.app.AegisApp

/** Fallback for API 26–28 or when call screening role is not granted. */
class IncomingCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        if (!AegisApp.get(context).prefs.callGuardEnabled) return
        if (!CallGuardPermissions.canRunCallGuard(context)) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        val appContext = context.applicationContext

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                CallGuardState.callPhase = CallGuardState.PHASE_RINGING
                CallGuardState.lastCaller = number?.takeIf { it.isNotBlank() }
                if (CallGuardState.activeSessionId == null) {
                    CallGuardCoordinator.onIncomingCallScreened(
                        appContext,
                        number ?: CallGuardState.lastCaller,
                    )
                }
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                CallGuardState.callPhase = CallGuardState.PHASE_OFFHOOK
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                if (CallGuardState.callPhase == CallGuardState.PHASE_OFFHOOK ||
                    CallGuardState.callPhase == CallGuardState.PHASE_RINGING
                ) {
                    CallGuardState.activeSessionId?.let { sessionId ->
                        CallGuardCoordinator.onCallEnded(appContext, sessionId)
                    }
                }
                CallGuardState.callPhase = CallGuardState.PHASE_IDLE
                CallGuardState.activeSessionId = null
                CallGuardState.lastCaller = null
            }
        }
    }
}

object CallGuardState {
    const val PHASE_IDLE = 0
    const val PHASE_RINGING = 1
    const val PHASE_OFFHOOK = 2

    @Volatile
    var lastCaller: String? = null

    @Volatile
    var callPhase: Int = PHASE_IDLE

    @Volatile
    var activeSessionId: String? = null

    @Volatile
    var watchServiceRunning: Boolean = false
}
