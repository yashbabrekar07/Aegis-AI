package com.aegisai.app.ui.sms

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.aegisai.app.R
import com.aegisai.app.databinding.FragmentSmsBinding
import com.aegisai.app.sms.SmsRecord
import com.aegisai.app.sms.SmsStore
import com.aegisai.app.util.AnimUtil

class SmsFragment : Fragment() {
    private var _binding: FragmentSmsBinding? = null
    private val binding get() = _binding!!

    private val smsPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (results.values.all { it }) {
            Toast.makeText(requireContext(), "SMS scanning enabled", Toast.LENGTH_SHORT).show()
            updateUiState()
        } else {
            Toast.makeText(requireContext(), "Permissions required for SMS scanning", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSmsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        AnimUtil.fadeInUp(binding.permissionCard)
        AnimUtil.fadeInUp(binding.smsHistoryCard)

        binding.grantPermissionBtn.setOnClickListener {
            smsPermission.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS))
        }

        updateUiState()
    }

    override fun onResume() {
        super.onResume()
        updateUiState()
    }

    private fun updateUiState() {
        if (_binding == null) return

        val hasPermissions = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

        binding.permissionCard.isVisible = !hasPermissions
        binding.smsHistoryCard.isVisible = hasPermissions

        if (hasPermissions) {
            refreshHistory()
        }
    }

    private fun refreshHistory() {
        val records = SmsStore.recentRecords(requireContext(), 10)
        
        if (records.isEmpty()) {
            binding.smsNoHistory.isVisible = true
            binding.smsHistoryList.isVisible = false
            return
        }

        binding.smsNoHistory.isVisible = false
        binding.smsHistoryList.isVisible = true
        binding.smsHistoryList.text = records.joinToString("\n\n---\n\n") { formatRecord(it) }
    }

    private fun formatRecord(record: SmsRecord): String {
        val formattedTime = DateFormat.format("MMM d, HH:mm", record.timestamp)
        
        val risk = record.result?.risk ?: "PENDING/UNKNOWN"
        val conf = record.result?.confidence?.let { " (${(it * 100).toInt()}%)" } ?: ""
        
        val bodyPreview = if (record.body.length > 100) record.body.take(100) + "..." else record.body
        
        return "[$formattedTime] From: ${record.sender}\nRisk: $risk$conf\n\n$bodyPreview"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
