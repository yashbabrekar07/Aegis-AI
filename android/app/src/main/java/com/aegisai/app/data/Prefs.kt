package com.aegisai.app.data

import android.content.Context
import com.aegisai.app.BuildConfig

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("aegis_prefs", Context.MODE_PRIVATE)

    var apiBaseUrl: String
        get() = sp.getString(KEY_API, BuildConfig.API_BASE_URL) ?: BuildConfig.API_BASE_URL
        set(v) = sp.edit().putString(KEY_API, v.trimEnd('/')).apply()

    var username: String?
        get() = sp.getString(KEY_USERNAME, null)
        set(v) = sp.edit().putString(KEY_USERNAME, v).apply()

    var email: String?
        get() = sp.getString(KEY_EMAIL, null)
        set(v) = sp.edit().putString(KEY_EMAIL, v).apply()

    var accessToken: String?
        get() = sp.getString(KEY_ACCESS, null)
        set(v) = sp.edit().putString(KEY_ACCESS, v).apply()

    var refreshToken: String?
        get() = sp.getString(KEY_REFRESH, null)
        set(v) = sp.edit().putString(KEY_REFRESH, v).apply()

    var phone: String?
        get() = sp.getString(KEY_PHONE, null)
        set(v) = sp.edit().putString(KEY_PHONE, v).apply()

    var phoneVerified: Boolean
        get() = sp.getBoolean(KEY_PHONE_VERIFIED, false)
        set(v) = sp.edit().putBoolean(KEY_PHONE_VERIFIED, v).apply()

    /** User chose "Skip for now" on the phone verification screen. */
    var phoneVerificationSkipped: Boolean
        get() = sp.getBoolean(KEY_PHONE_SKIPPED, false)
        set(v) = sp.edit().putBoolean(KEY_PHONE_SKIPPED, v).apply()

    var callGuardEnabled: Boolean
        get() = sp.getBoolean(KEY_CALL_GUARD, false)
        set(v) = sp.edit().putBoolean(KEY_CALL_GUARD, v).apply()

    var bestAudioSource: Int
        get() = sp.getInt(KEY_BEST_SOURCE, 1) // default: MediaRecorder.AudioSource.MIC (1)
        set(v) = sp.edit().putInt(KEY_BEST_SOURCE, v).apply()

    var speakerRequired: Boolean
        get() = sp.getBoolean(KEY_SPEAKER_REQUIRED, true)
        set(v) = sp.edit().putBoolean(KEY_SPEAKER_REQUIRED, v).apply()

    var hasCheckedCompatibility: Boolean
        get() = sp.getBoolean(KEY_CHECKED_COMPATIBILITY, false)
        set(v) = sp.edit().putBoolean(KEY_CHECKED_COMPATIBILITY, v).apply()

    fun clearSession() {
        sp.edit()
            .remove(KEY_ACCESS)
            .remove(KEY_REFRESH)
            .remove(KEY_EMAIL)
            .remove(KEY_USERNAME)
            .remove(KEY_PHONE)
            .remove(KEY_PHONE_VERIFIED)
            .remove(KEY_PHONE_SKIPPED)
            .apply()
    }

    fun isLoggedIn(): Boolean = !accessToken.isNullOrBlank()

    fun needsPhoneVerification(): Boolean =
        isLoggedIn() && !phoneVerified && !phoneVerificationSkipped

    companion object {
        private const val KEY_API = "api_base"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
        private const val KEY_PHONE = "phone"
        private const val KEY_PHONE_VERIFIED = "phone_verified"
        private const val KEY_PHONE_SKIPPED = "phone_verification_skipped"
        private const val KEY_CALL_GUARD = "call_guard_enabled"
        private const val KEY_BEST_SOURCE = "best_audio_source"
        private const val KEY_SPEAKER_REQUIRED = "speaker_required"
        private const val KEY_CHECKED_COMPATIBILITY = "has_checked_compatibility"
    }
}
