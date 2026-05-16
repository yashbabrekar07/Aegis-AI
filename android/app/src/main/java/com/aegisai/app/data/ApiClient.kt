package com.aegisai.app.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ScanResult(
    val label: String? = null,
    val risk: String? = null,
    val confidence: Double? = null,
    val reason: String? = null,
    val transcription: String? = null,
    val method: String? = null,
    val error: String? = null,
    val detected_keywords: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class ProfileResponse(val user_id: String?, val phone: String?)

class ApiClient(private val baseUrl: String) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val scanAdapter = moshi.adapter(ScanResult::class.java)
    private val profileAdapter = moshi.adapter(ProfileResponse::class.java)
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    fun scanText(text: String): ScanResult {
        val body = org.json.JSONObject().put("text", text).toString().toRequestBody(jsonType)
        val req = Request.Builder().url("$baseUrl/api/scan").post(body).build()
        return executeScan(req)
    }

    fun scanAudio(file: File): ScanResult {
        val part = MultipartBody.Part.createFormData(
            "file",
            file.name,
            file.asRequestBody("audio/*".toMediaType())
        )
        val body = MultipartBody.Builder().setType(MultipartBody.FORM).addPart(part).build()
        val req = Request.Builder().url("$baseUrl/api/scan-audio").post(body).build()
        return executeScan(req)
    }

    fun analyzeVishingTranscript(transcript: String, phone: String?): ScanResult {
        val json = org.json.JSONObject().put("transcript", transcript)
        if (!phone.isNullOrBlank()) json.put("phone_number", phone)
        val payload = json.toString()
        val req = Request.Builder()
            .url("$baseUrl/api/vishing/analyze-transcript")
            .post(payload.toRequestBody(jsonType))
            .build()
        return executeScan(req)
    }

    fun fetchProfile(email: String): ProfileResponse? {
        val req = Request.Builder()
            .url("$baseUrl/api/user/profile?email=${java.net.URLEncoder.encode(email, "UTF-8")}")
            .get()
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val json = resp.body?.string() ?: return null
            return profileAdapter.fromJson(json)
        }
    }

    fun healthCheck(): Boolean {
        val req = Request.Builder().url("$baseUrl/").get().build()
        return try {
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    private fun executeScan(req: Request): ScanResult {
        client.newCall(req).execute().use { resp ->
            val json = resp.body?.string() ?: "{}"
            val parsed = scanAdapter.fromJson(json) ?: ScanResult(error = "Invalid response")
            if (!resp.isSuccessful && parsed.error == null) {
                return parsed.copy(error = "HTTP ${resp.code}")
            }
            return parsed
        }
    }
}
