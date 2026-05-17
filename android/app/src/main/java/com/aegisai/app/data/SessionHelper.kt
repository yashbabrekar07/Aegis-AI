package com.aegisai.app.data

import com.aegisai.app.AegisApp
import com.aegisai.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object SessionHelper {
    private val http = OkHttpClient()

    suspend fun refreshUserFromToken(context: android.content.Context): Boolean = withContext(Dispatchers.IO) {
        val app = AegisApp.get(context)
        val prefs = app.prefs
        val token = prefs.accessToken?.takeIf { it.isNotBlank() } ?: return@withContext false
        val user = fetchUser(token) ?: return@withContext false

        val email = user.optString("email").takeIf { it.isNotBlank() }
        if (email != null) prefs.email = email

        val meta = user.optJSONObject("user_metadata")
        val name = meta?.optString("username")?.takeIf { it.isNotBlank() }
            ?: meta?.optString("full_name")?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
        if (name != null) prefs.username = name

        !prefs.email.isNullOrBlank()
    }

    fun fetchUser(accessToken: String): JSONObject? {
        return try {
            val req = Request.Builder()
                .url("${BuildConfig.SUPABASE_URL.trimEnd('/')}/auth/v1/user")
                .header("Authorization", "Bearer $accessToken")
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .get()
                .build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                JSONObject(resp.body?.string() ?: return null)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun emailLocalPart(email: String?): String? {
        if (email.isNullOrBlank() || !email.contains("@")) return null
        return email.substringBefore("@")
    }
}
