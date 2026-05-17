package com.aegisai.app.call

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.aegisai.app.AegisApp

/**
 * Listens for call state changes. When Call Guard is enabled, starts mic-based
 * recording during active calls (see [CallRecordService]).
 */
class IncomingCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        if (!AegisApp.get(context).prefs.callGuardEnabled) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                CallRecordService.lastCaller = number
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                val svc = Intent(context, CallRecordService::class.java)
                    .putExtra(CallRecordService.EXTRA_PHONE, number ?: CallRecordService.lastCaller)
                context.startForegroundService(svc)
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                context.stopService(Intent(context, CallRecordService::class.java))
            }
        }
    }
}
