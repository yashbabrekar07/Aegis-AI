package com.aegisai.app.sms

import com.aegisai.app.data.ScanResult
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SmsRecord(
    val id: String,
    val sender: String,
    val body: String,
    val timestamp: Long,
    val result: ScanResult? = null
)
