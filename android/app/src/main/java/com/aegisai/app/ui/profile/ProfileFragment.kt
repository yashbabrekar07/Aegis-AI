package com.aegisai.app.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aegisai.app.AegisApp
import com.aegisai.app.BuildConfig
import com.aegisai.app.R
import com.aegisai.app.data.ApiClient
import com.aegisai.app.data.SessionHelper
import com.aegisai.app.databinding.FragmentProfileBinding
import com.aegisai.app.ui.login.LoginActivity
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.logoutBtn.setOnClickListener { logout() }
        setupBackendUrlUi()
        loadProfile()
    }

    override fun onResume() {
        super.onResume()
        val prefs = AegisApp.get(requireContext()).prefs
        binding.profileBackendUrl.setText(prefs.apiBaseUrl)
        loadProfile()
    }

    private fun setupBackendUrlUi() {
        val prefs = AegisApp.get(requireContext()).prefs
        binding.profileBackendUrl.setText(prefs.apiBaseUrl)

        binding.saveBackendBtn.setOnClickListener {
            val raw = binding.profileBackendUrl.text?.toString()?.trim().orEmpty()
            if (!raw.startsWith("http://") && !raw.startsWith("https://")) {
                Toast.makeText(requireContext(), R.string.backend_url_invalid, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            prefs.apiBaseUrl = raw.trimEnd('/')
            Toast.makeText(requireContext(), R.string.backend_url_saved, Toast.LENGTH_SHORT).show()
            testBackendConnection()
        }

        binding.resetBackendBtn.setOnClickListener {
            val defaultUrl = BuildConfig.API_BASE_URL.trimEnd('/')
            prefs.apiBaseUrl = defaultUrl
            binding.profileBackendUrl.setText(defaultUrl)
            Toast.makeText(requireContext(), R.string.backend_url_saved, Toast.LENGTH_SHORT).show()
            testBackendConnection()
        }

        binding.testBackendBtn.setOnClickListener { testBackendConnection() }
    }

    private fun testBackendConnection() {
        val prefs = AegisApp.get(requireContext()).prefs
        val url = binding.profileBackendUrl.text?.toString()?.trim().orEmpty().ifBlank { prefs.apiBaseUrl }
        binding.profileBackendStatus.isVisible = true
        binding.profileBackendStatus.text = "Checking…"
        binding.profileBackendStatus.setTextColor(requireContext().getColor(R.color.aegis_text_muted))

        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    ApiClient(url.trimEnd('/')).wakeBackend()
                    true
                } catch (_: Exception) {
                    false
                }
            }
            binding.profileBackendStatus.text = if (ok) {
                getString(R.string.backend_connected)
            } else {
                getString(R.string.backend_unreachable)
            }
            binding.profileBackendStatus.setTextColor(
                requireContext().getColor(if (ok) R.color.aegis_green else R.color.scam_red)
            )
        }
    }

    private fun loadProfile() {
        val prefs = AegisApp.get(requireContext()).prefs
        lifecycleScope.launch {
            SessionHelper.refreshUserFromToken(requireContext())

            val username = prefs.username ?: "Guest"
            val email = prefs.email
            val emailDisplay = SessionHelper.emailLocalPart(email)?.let { "$it@…" }
                ?: "No email linked"

            binding.profileGreeting.text = "Hi $username!"
            binding.profileUsername.text = username
            binding.profileEmail.text = emailDisplay
            binding.profileAvatar.text = username.take(2).uppercase()

            val xp = requireContext()
                .getSharedPreferences("aegis_training", android.content.Context.MODE_PRIVATE)
                .getInt("xp", 0)
            binding.profileRank.text = when {
                xp >= 100 -> "Expert defender · $xp XP"
                xp >= 50 -> "Alert guardian · $xp XP"
                xp > 0 -> "Security trainee · $xp XP"
                else -> "Complete training to establish your rank."
            }

            val api = ApiClient(prefs.apiBaseUrl)
            val profile = withContext(Dispatchers.IO) {
                if (!email.isNullOrBlank() && email.contains("@")) {
                    api.fetchProfile(email, if (prefs.phoneVerified) prefs.phone else null)
                } else {
                    null
                }
            }

            if (prefs.phoneVerified && !prefs.phone.isNullOrBlank()) {
                binding.profilePhone.text = prefs.phone
            }

            if (profile != null) {
                binding.profileUserId.text = profile.user_id ?: "—"
                if (!prefs.phoneVerified) {
                    binding.profilePhone.text = profile.phone ?: "—"
                }
            } else if (!email.isNullOrBlank()) {
                binding.profileUserId.text = "—"
                binding.profilePhone.text = "—"
            } else {
                binding.profileUserId.text = "Sign in with Google or email to link"
                binding.profilePhone.text = "—"
            }
        }
    }

    private fun logout() {
        val prefs = AegisApp.get(requireContext()).prefs
        prefs.clearSession()
        lifecycleScope.launch {
            try {
                AegisApp.get(requireContext()).supabase.auth.signOut()
            } catch (_: Exception) { }
        }
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
