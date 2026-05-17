package com.aegisai.app.call

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.aegisai.app.AegisApp

/** Registers phone-state listening and keeps a lightweight foreground watcher alive. */
object CallGuardController {
    private var receiver: IncomingCallReceiver? = null

    fun sync(context: Context) {
        if (AegisApp.get(context).prefs.callGuardEnabled) enable(context) else disable(context)
    }

    fun enable(context: Context) {
        val appContext = context.applicationContext
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (receiver == null) {
            receiver = IncomingCallReceiver()
            val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                appContext.registerReceiver(receiver, filter)
            }
        }
        val watch = Intent(appContext, CallGuardWatchService::class.java)
        try {
            ContextCompat.startForegroundService(appContext, watch)
        } catch (_: Exception) { }
    }

    fun disable(context: Context) {
        val appContext = context.applicationContext
        receiver?.let {
            try {
                appContext.unregisterReceiver(it)
            } catch (_: IllegalArgumentException) { }
        }
        receiver = null
        appContext.stopService(Intent(appContext, CallGuardWatchService::class.java))
        appContext.stopService(Intent(appContext, CallRecordService::class.java))
    }
}
