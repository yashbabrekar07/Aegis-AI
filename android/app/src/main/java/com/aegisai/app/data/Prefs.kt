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

    fun clearSession() {
        sp.edit()
            .remove(KEY_ACCESS)
            .remove(KEY_REFRESH)
            .remove(KEY_EMAIL)
            .apply()
    }

    fun isLoggedIn(): Boolean = !accessToken.isNullOrBlank()

    companion object {
        private const val KEY_API = "api_base"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
    }
}
