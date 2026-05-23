package com.aegisai.app.call

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log

class AegisCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val handle = callDetails.handle
        val number = handle?.schemeSpecificPart
        Log.i(TAG, "Screening call from: $number")

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
