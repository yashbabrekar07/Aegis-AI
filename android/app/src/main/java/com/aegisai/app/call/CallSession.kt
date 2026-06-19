package com.aegisai.app.call

import com.aegisai.app.data.ScanResult
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CallSession(
    val id: String,
    val phoneNumber: String? = null,
    val startedAt: Long,
    val endedAt: Long? = null,
    val recordingUri: String? = null,
    val recordingDisplayName: String? = null,
    val detectionMethod: String? = null,
    val status: String = STATUS_ACTIVE,
    val errorMessage: String? = null,
    val result: ScanResult? = null,
) {
    companion object {
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_DISCOVERING = "DISCOVERING"
        const val STATUS_ANALYZING = "ANALYZING"
        const val STATUS_DONE = "DONE"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_NO_RECORDING = "NO_RECORDING"

        const val DETECTION_AUTO = "AUTO"
        const val DETECTION_MANUAL = "MANUAL"
    }
}
