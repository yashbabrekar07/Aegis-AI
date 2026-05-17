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
                CallGuardState.lastCaller = number?.takeIf { it.isNotBlank() }
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                val phone = number?.takeIf { it.isNotBlank() } ?: CallGuardState.lastCaller
                val start = Intent(context, CallRecordService::class.java).apply {
                    action = CallRecordService.ACTION_START
                    putExtra(CallRecordService.EXTRA_PHONE, phone)
                }
                ContextCompat.startForegroundService(context, start)
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                val analyze = Intent(context, CallRecordService::class.java).apply {
                    action = CallRecordService.ACTION_ANALYZE
                }
                ContextCompat.startForegroundService(context, analyze)
            }
        }
    }
}

object CallGuardState {
    @Volatile
    var lastCaller: String? = null
}
