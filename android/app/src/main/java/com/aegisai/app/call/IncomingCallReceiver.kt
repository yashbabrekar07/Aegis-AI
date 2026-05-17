package com.aegisai.app.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.aegisai.app.AegisApp

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
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                if (CallGuardState.callPhase == CallGuardState.PHASE_OFFHOOK) return
                CallGuardState.callPhase = CallGuardState.PHASE_OFFHOOK
                val phone = number?.takeIf { it.isNotBlank() } ?: CallGuardState.lastCaller
                val start = Intent(appContext, CallGuardWatchService::class.java).apply {
                    action = CallGuardWatchService.ACTION_START_RECORDING
                    putExtra(CallGuardWatchService.EXTRA_PHONE, phone)
                }
                dispatchToWatch(appContext, start)
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                if (CallGuardState.callPhase != CallGuardState.PHASE_OFFHOOK) return
                CallGuardState.callPhase = CallGuardState.PHASE_IDLE
                val analyze = Intent(appContext, CallGuardWatchService::class.java).apply {
                    action = CallGuardWatchService.ACTION_ANALYZE
                }
                dispatchToWatch(appContext, analyze)
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
    var watchServiceRunning: Boolean = false
}

private fun dispatchToWatch(context: Context, intent: Intent) {
    try {
        if (CallGuardState.watchServiceRunning) {
            context.startService(intent)
        } else {
            ContextCompat.startForegroundService(context, intent)
        }
    } catch (_: Exception) {
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (_: Exception) { }
    }
}
