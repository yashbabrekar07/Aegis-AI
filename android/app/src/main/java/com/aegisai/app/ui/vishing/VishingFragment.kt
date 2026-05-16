package com.aegisai.app.ui.vishing

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aegisai.app.AegisApp
import com.aegisai.app.data.ApiClient
import com.aegisai.app.data.ScanResult
import com.aegisai.app.databinding.FragmentVishingBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class VishingFragment : Fragment() {
    private var _binding: FragmentVishingBinding? = null
    private val binding get() = _binding!!
    private lateinit var api: ApiClient
    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null

    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording() else Toast.makeText(requireContext(), "Microphone permission required", Toast.LENGTH_LONG).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVishingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        api = ApiClient(AegisApp.get(requireContext()).prefs.apiBaseUrl)

        binding.analyzeTranscriptBtn.setOnClickListener {
            val t = binding.transcriptInput.text?.toString()?.trim().orEmpty()
            if (t.length < 3) {
                Toast.makeText(requireContext(), "Transcript too short", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val phone = binding.phoneInput.text?.toString()?.trim()
            runJob { api.analyzeVishingTranscript(t, phone) }
        }

        binding.recordBtn.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startRecording()
            } else {
                micPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        binding.stopRecordBtn.setOnClickListener { stopAndScan() }
    }

    private fun startRecording() {
        val file = File(requireContext().cacheDir, "vishing_${System.currentTimeMillis()}.m4a")
        recordingFile = file
        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(requireContext()) else @Suppress("DEPRECATION") MediaRecorder()
        recorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        binding.recordBtn.isEnabled = false
        binding.stopRecordBtn.isVisible = true
        Toast.makeText(requireContext(), "Recording…", Toast.LENGTH_SHORT).show()
    }

    private fun stopAndScan() {
        try {
            recorder?.stop()
        } catch (_: Exception) { }
        recorder?.release()
        recorder = null
        binding.recordBtn.isEnabled = true
        binding.stopRecordBtn.isVisible = false
        val file = recordingFile ?: return
        runJob { api.scanAudio(file) }
    }

    private fun runJob(block: suspend () -> ScanResult) {
        binding.vishingProgress.isVisible = true
        binding.vishingResult.isVisible = false
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) { block() }
                showResult(result)
            } catch (e: Exception) {
                showResult(ScanResult(error = e.message))
            } finally {
                binding.vishingProgress.isVisible = false
            }
        }
    }

    private fun showResult(result: ScanResult) {
        binding.vishingResult.isVisible = true
        if (result.error != null) {
            binding.vishingResult.text = "Error: ${result.error}"
            return
        }
        val conf = ((result.confidence ?: 0.0) * 100).toInt()
        val sb = StringBuilder()
        sb.append("Risk: ${result.risk}\nConfidence: $conf%\n\n${result.reason}")
        result.transcription?.let {
            sb.append("\n\nTranscription:\n$it")
            binding.transcriptInput.setText(it)
        }
        binding.vishingResult.text = sb.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recorder?.release()
        _binding = null
    }
}
