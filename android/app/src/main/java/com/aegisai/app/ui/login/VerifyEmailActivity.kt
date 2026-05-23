package com.aegisai.app.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aegisai.app.AegisApp
import com.aegisai.app.databinding.ActivityVerifyEmailBinding
import com.aegisai.app.util.AnimUtil
import io.github.jan.supabase.gotrue.OtpType
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.launch

class VerifyEmailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVerifyEmailBinding
    private val app get() = AegisApp.get(this)
    private val email get() = intent.getStringExtra(EXTRA_EMAIL).orEmpty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyEmailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.verifyEmailHint.text = "Enter the 6-digit code sent to $email"
        AnimUtil.fadeInUp(binding.root)

        binding.verifyBtn.setOnClickListener { verify() }
        binding.resendBtn.setOnClickListener { resend() }
    }

    private fun verify() {
        val token = binding.otpInput.text?.toString()?.trim().orEmpty()
        if (token.length != 6) {
            Toast.makeText(this, "Enter the 6-digit code", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            try {
                binding.verifyBtn.isEnabled = false
                app.supabase.auth.verifyEmailOtp(
                    type = OtpType.Email.SIGNUP,
                    email = email,
                    token = token
                )
                val session = app.supabase.auth.currentSessionOrNull()
                app.prefs.accessToken = session?.accessToken
                app.prefs.refreshToken = session?.refreshToken
                app.prefs.email = email
                intent.getStringExtra(EXTRA_USERNAME)?.let { app.prefs.username = it }
                startActivity(Intent(this@VerifyEmailActivity, VerifyPhoneActivity::class.java))
                AnimUtil.activityOpen(this@VerifyEmailActivity)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@VerifyEmailActivity, e.message ?: "Invalid code", Toast.LENGTH_LONG).show()
            } finally {
                binding.verifyBtn.isEnabled = true
            }
        }
    }

    private fun resend() {
        lifecycleScope.launch {
            try {
                app.supabase.auth.resendEmail(OtpType.Email.SIGNUP, email)
                Toast.makeText(this@VerifyEmailActivity, "Code resent", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@VerifyEmailActivity, e.message ?: "Could not resend", Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        const val EXTRA_EMAIL = "email"
        const val EXTRA_USERNAME = "username"
    }
}
