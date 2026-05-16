package com.aegisai.app.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aegisai.app.AegisApp
import com.aegisai.app.data.ApiClient
import com.aegisai.app.databinding.FragmentProfileBinding
import com.aegisai.app.ui.login.LoginActivity
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
        val prefs = AegisApp.get(requireContext()).prefs
        val username = prefs.username ?: "Guest"
        val email = prefs.email ?: "Not linked"
        binding.profileUsername.text = username
        binding.profileEmail.text = if (email.contains("@")) {
            email.substringBefore("@") + "@…"
        } else {
            email
        }

        binding.logoutBtn.setOnClickListener {
            prefs.clearSession()
            lifecycleScope.launch {
                try {
                    AegisApp.get(requireContext()).supabase.auth.signOut()
                } catch (_: Exception) { }
            }
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        lifecycleScope.launch {
            val api = ApiClient(prefs.apiBaseUrl)
            val profile = withContext(Dispatchers.IO) {
                if (email.contains("@")) api.fetchProfile(email) else null
            }
            profile?.let {
                binding.profileUserId.text = "User ID: ${it.user_id ?: "—"}"
                binding.profilePhone.text = "Phone: ${it.phone ?: "—"}"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
