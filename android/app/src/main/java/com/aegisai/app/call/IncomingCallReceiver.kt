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

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                CallGuardState.callPhase = CallGuardState.PHASE_RINGING
                CallGuardState.lastCaller = number?.takeIf { it.isNotBlank() }
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                if (CallGuardState.callPhase == CallGuardState.PHASE_OFFHOOK) return
                CallGuardState.callPhase = CallGuardState.PHASE_OFFHOOK
                val phone = number?.takeIf { it.isNotBlank() } ?: CallGuardState.lastCaller
                val start = Intent(context, CallRecordService::class.java).apply {
                    action = CallRecordService.ACTION_START
                    putExtra(CallRecordService.EXTRA_PHONE, phone)
                }
                startCallService(context, start)
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                if (CallGuardState.callPhase != CallGuardState.PHASE_OFFHOOK) return
                CallGuardState.callPhase = CallGuardState.PHASE_IDLE
                val analyze = Intent(context, CallRecordService::class.java).apply {
                    action = CallRecordService.ACTION_ANALYZE
                }
                startCallService(context, analyze)
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
}

private fun startCallService(context: Context, intent: Intent) {
    try {
        ContextCompat.startForegroundService(context, intent)
    } catch (_: Exception) {
        // Background FGS restrictions on some Android 12+ builds
    }
}
