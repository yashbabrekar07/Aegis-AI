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
import java.io.IOException
import java.net.SocketTimeoutException
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

@JsonClass(generateAdapter = true)
data class OtpResponse(val ok: Boolean = false, val message: String? = null, val dev_otp: String? = null, val error: String? = null)

class ApiClient(private val baseUrl: String) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    /** Audio + Whisper on Render free tier can take several minutes on cold start. */
    private val audioClient = OkHttpClient.Builder()
        .connectTimeout(180, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .build()

    private val wakeClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val scanAdapter = moshi.adapter(ScanResult::class.java)
    private val profileAdapter = moshi.adapter(ProfileResponse::class.java)
    private val otpAdapter = moshi.adapter(OtpResponse::class.java)
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    fun scanText(text: String): ScanResult {
        val body = org.json.JSONObject().put("text", text).toString().toRequestBody(jsonType)
        val req = Request.Builder().url("$baseUrl/api/scan").post(body).build()
        return executeScan(req, client)
    }

    fun scanAudio(file: File): ScanResult = scanAudioWithRetry(file, maxAttempts = 2)

    /**
     * Call Guard: transcribe first, then analyze transcript (shows what was heard even if scam scan fails).
     */
    fun analyzeCallRecording(file: File, phone: String?): ScanResult {
        wakeBackend()
        val transcriptResult = transcribeAudioWithRetry(file, maxAttempts = 2)
        val text = transcriptResult.transcription?.trim().orEmpty()
        if (text.isEmpty()) {
            val err = humanizeAudioError(transcriptResult.error)
            return ScanResult(
                error = err,
                transcription = null,
                method = transcriptResult.method
            )
        }
        val analysis = analyzeVishingTranscript(text, phone)
        return analysis.copy(
            transcription = text,
            method = transcriptResult.method ?: analysis.method
        )
    }

    fun transcribeAudioWithRetry(file: File, maxAttempts: Int = 2): ScanResult {
        wakeBackend()
        var last: ScanResult? = null
        repeat(maxAttempts) { attempt ->
            val r = postAudioMultipart(file, "$baseUrl/api/transcribe-audio")
            last = r
            val text = r.transcription?.trim().orEmpty()
            if (text.isNotEmpty()) return r
            val err = r.error?.lowercase().orEmpty()
            val retryable = err.contains("timeout") || err.contains("waking") || err.contains("502") ||
                err.contains("503") || err.contains("end of input") || err.contains("empty")
            if (attempt < maxAttempts - 1 && retryable) {
                Thread.sleep(4_000)
                wakeBackend()
            } else {
                return r
            }
        }
        return last ?: ScanResult(error = "Transcription failed")
    }

    fun scanAudioWithRetry(file: File, maxAttempts: Int = 2): ScanResult {
        wakeBackend()
        var lastError: Exception? = null
        repeat(maxAttempts) { attempt ->
            try {
                return postAudioMultipart(file, "$baseUrl/api/scan-audio")
            } catch (e: Exception) {
                lastError = e
                if (attempt < maxAttempts - 1 && isRetryable(e)) {
                    Thread.sleep(4_000)
                    wakeBackend()
                }
            }
        }
        throw lastError ?: IOException("Audio scan failed")
    }

    private fun postAudioMultipart(file: File, url: String): ScanResult {
        val mime = when {
            file.name.endsWith(".wav", ignoreCase = true) -> "audio/wav"
            file.name.endsWith(".m4a", ignoreCase = true) -> "audio/mp4"
            file.name.endsWith(".mp3", ignoreCase = true) -> "audio/mpeg"
            file.name.endsWith(".ogg", ignoreCase = true) -> "audio/ogg"
            else -> "audio/*"
        }
        val part = MultipartBody.Part.createFormData(
            "file",
            file.name,
            file.asRequestBody(mime.toMediaType())
        )
        val body = MultipartBody.Builder().setType(MultipartBody.FORM).addPart(part).build()
        val req = Request.Builder().url(url).post(body).build()
        return executeScan(req, audioClient)
    }

    fun analyzeVishingTranscript(transcript: String, phone: String?): ScanResult {
        val json = org.json.JSONObject().put("transcript", transcript)
        if (!phone.isNullOrBlank()) json.put("phone_number", phone)
        val payload = json.toString()
        val req = Request.Builder()
            .url("$baseUrl/api/vishing/analyze-transcript")
            .post(payload.toRequestBody(jsonType))
            .build()
        return executeScan(req, client)
    }

    fun fetchProfile(email: String, phone: String? = null): ProfileResponse? {
        var url = "$baseUrl/api/user/profile?email=${java.net.URLEncoder.encode(email, "UTF-8")}"
        if (!phone.isNullOrBlank()) {
            url += "&phone=${java.net.URLEncoder.encode(phone, "UTF-8")}"
        }
        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val json = resp.body?.string() ?: return null
            return profileAdapter.fromJson(json)
        }
    }

    fun sendPhoneOtp(phone: String, email: String?): OtpResponse {
        val json = org.json.JSONObject().put("phone", phone)
        if (!email.isNullOrBlank()) json.put("email", email)
        return postOtp("$baseUrl/api/auth/send-phone-otp", json.toString())
    }

    fun verifyPhoneOtp(phone: String, otp: String, email: String?): OtpResponse {
        val json = org.json.JSONObject().put("phone", phone).put("otp", otp)
        if (!email.isNullOrBlank()) json.put("email", email)
        return postOtp("$baseUrl/api/auth/verify-phone-otp", json.toString())
    }

    private fun postOtp(url: String, body: String): OtpResponse {
        val req = Request.Builder().url(url).post(body.toRequestBody(jsonType)).build()
        client.newCall(req).execute().use { resp ->
            val parsed = otpAdapter.fromJson(resp.body?.string() ?: "{}")
                ?: OtpResponse(error = "Invalid response")
            if (!resp.isSuccessful && parsed.error == null) {
                return parsed.copy(error = "HTTP ${resp.code}")
            }
            return parsed
        }
    }

    /** Ping Render so the next scan-audio request is less likely to cold-start timeout. */
    fun wakeBackend(): Boolean {
        val req = Request.Builder().url("$baseUrl/").get().build()
        return try {
            wakeClient.newCall(req).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

    fun healthCheck(): Boolean = wakeBackend()

    private fun executeScan(req: Request, http: OkHttpClient): ScanResult {
        http.newCall(req).execute().use { resp ->
            val json = resp.body?.string() ?: "{}"
            val parsed = scanAdapter.fromJson(json) ?: ScanResult(error = "Invalid response")
            if (!resp.isSuccessful && parsed.error == null) {
                return parsed.copy(error = "HTTP ${resp.code}")
            }
            return parsed
        }
    }

    companion object {
        fun isRetryable(e: Throwable): Boolean {
            if (e is SocketTimeoutException) return true
            val msg = e.message?.lowercase().orEmpty()
            return msg.contains("timeout") || msg.contains("timed out")
        }

        fun friendlyError(e: Throwable): String {
            if (isRetryable(e)) {
                return "Server took too long (Render may be waking up). Keep speakerphone on for short calls, or paste a transcript in Vishing."
            }
            return humanizeAudioError(e.message)
        }

        fun humanizeAudioError(message: String?): String {
            val m = message?.trim().orEmpty()
            if (m.isEmpty()) {
                return "Could not transcribe call. Use speakerphone and talk for at least 5 seconds."
            }
            if (m.contains("end of input", ignoreCase = true) ||
                m.contains("end of file", ignoreCase = true) ||
                m.contains("truncated", ignoreCase = true)
            ) {
                return "Recording was empty or cut off. Turn on speakerphone during calls so the mic captures speech."
            }
            if (m.contains("No speech", ignoreCase = true)) {
                return "No speech detected in the recording. Use speakerphone and speak clearly for a few seconds."
            }
            return m.removePrefix("Error:").trim()
        }
    }
}
