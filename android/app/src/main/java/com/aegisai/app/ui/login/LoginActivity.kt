package com.aegisai.app.ui.login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.lifecycleScope
import com.aegisai.app.AegisApp
import com.aegisai.app.BuildConfig
import com.aegisai.app.databinding.ActivityLoginBinding
import com.aegisai.app.ui.main.MainActivity
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val app get() = AegisApp.get(this)
    private val prefs get() = app.prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (prefs.isLoggedIn()) {
            goMain()
            return
        }
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apiUrlInput.setText(prefs.apiBaseUrl)

        binding.signInBtn.setOnClickListener { signInEmail() }
        binding.googleBtn.setOnClickListener { signInGoogleBrowser() }

        handleAuthCallback(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthCallback(intent)
    }

    private fun signInEmail() {
        val email = binding.emailInput.text?.toString()?.trim().orEmpty()
        val password = binding.passwordInput.text?.toString().orEmpty()
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
            return
        }
        prefs.apiBaseUrl = binding.apiUrlInput.text?.toString()?.trim().orEmpty()

        lifecycleScope.launch {
            try {
                binding.signInBtn.isEnabled = false
                app.supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                persistSession(email)
                goMain()
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, e.message ?: "Sign in failed", Toast.LENGTH_LONG).show()
            } finally {
                binding.signInBtn.isEnabled = true
            }
        }
    }

    private fun signInGoogleBrowser() {
        prefs.apiBaseUrl = binding.apiUrlInput.text?.toString()?.trim().orEmpty()
        val redirect = URLEncoder.encode("com.aegisai.app://auth-callback", "UTF-8")
        val url =
            "${BuildConfig.SUPABASE_URL.trimEnd('/')}/auth/v1/authorize?provider=google&redirect_to=$redirect"
        CustomTabsIntent.Builder().build().launchUrl(this, Uri.parse(url))
    }

    private fun handleAuthCallback(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "com.aegisai.app" || data.host != "auth-callback") return

        val fragment = data.fragment ?: return
        val params = fragment.split("&").mapNotNull {
            val p = it.split("=")
            if (p.size == 2) p[0] to p[1] else null
        }.toMap()

        val access = params["access_token"]
        val refresh = params["refresh_token"]
        if (access.isNullOrBlank()) {
            Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_LONG).show()
            return
        }

        prefs.accessToken = access
        prefs.refreshToken = refresh

        lifecycleScope.launch {
            val user = fetchSupabaseUser(access)
            val email = user?.optString("email").orEmpty()
            if (email.isNotBlank()) prefs.email = email
            val meta = user?.optJSONObject("user_metadata")
            prefs.username = meta?.optString("username")?.takeIf { it.isNotBlank() }
                ?: meta?.optString("full_name")?.takeIf { it.isNotBlank() }
                ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }
            goMain()
        }
    }

    private suspend fun persistSession(email: String) {
        val session = app.supabase.auth.currentSessionOrNull()
        prefs.accessToken = session?.accessToken
        prefs.refreshToken = session?.refreshToken
        prefs.email = email
        val meta = app.supabase.auth.currentUserOrNull()?.userMetadata
        prefs.username = meta?.get("username")?.toString()
            ?: email.substringBefore("@")
    }

    private suspend fun fetchSupabaseUser(accessToken: String): JSONObject? {
        return try {
            val req = Request.Builder()
                .url("${BuildConfig.SUPABASE_URL.trimEnd('/')}/auth/v1/user")
                .header("Authorization", "Bearer $accessToken")
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .get()
                .build()
            OkHttpClient().newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                JSONObject(resp.body?.string() ?: return null)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun goMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
