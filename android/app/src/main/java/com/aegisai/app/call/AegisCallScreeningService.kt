package com.aegisai.app.call

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.aegisai.app.AegisApp

class AegisCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart
        Log.i(TAG, "Screening call from: $number")

        if (AegisApp.get(this).prefs.callGuardEnabled) {
            CallGuardCoordinator.onIncomingCallScreened(this, number)
        }

        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()

        respondToCall(callDetails, response)
    }

    companion object {
        private const val TAG = "AegisCallScreening"
    }
}
