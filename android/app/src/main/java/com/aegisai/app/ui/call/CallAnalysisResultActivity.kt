package com.aegisai.app.ui.call

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.aegisai.app.call.CallAnalysisNotifier
import com.aegisai.app.call.CallGuardCoordinator
import com.aegisai.app.call.CallSession
import com.aegisai.app.call.CallSessionStore
import com.aegisai.app.databinding.ActivityCallAnalysisResultBinding

class CallAnalysisResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCallAnalysisResultBinding
    private var sessionId: String? = null
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            renderSession()
            if (shouldKeepPolling()) {
                refreshHandler.postDelayed(this, 2000L)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallAnalysisResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionId = intent.getStringExtra(CallAnalysisNotifier.EXTRA_SESSION_ID)
            ?: intent.getStringExtra(EXTRA_SESSION_ID)

        binding.pickRecordingBtn.setOnClickListener {
            sessionId?.let { id ->
                startActivity(CallRecordingPickerActivity.intent(this, id))
            }
        }
        binding.retryDiscoveryBtn.setOnClickListener {
            sessionId?.let { id ->
                CallGuardCoordinator.retryDiscovery(this, id)
                finish()
            }
        }

        renderSession()
    }

    override fun onResume() {
        super.onResume()
        renderSession()
        refreshHandler.removeCallbacks(refreshRunnable)
        if (shouldKeepPolling()) {
            refreshHandler.postDelayed(refreshRunnable, 2000L)
        }
    }

    override fun onPause() {
        refreshHandler.removeCallbacks(refreshRunnable)
        super.onPause()
    }

    private fun shouldKeepPolling(): Boolean {
        val id = sessionId ?: return false
        val session = CallSessionStore.getSession(this, id) ?: return false
        return session.status in listOf(
            CallSession.STATUS_ACTIVE,
            CallSession.STATUS_DISCOVERING,
            CallSession.STATUS_ANALYZING,
        )
    }

    private fun renderSession() {
        val id = sessionId ?: run {
            binding.resultBody.text = getString(com.aegisai.app.R.string.call_guard_session_missing)
            return
        }
        val session = CallSessionStore.getSession(this, id) ?: run {
            binding.resultBody.text = getString(com.aegisai.app.R.string.call_guard_session_missing)
            return
        }

        binding.resultMeta.text = buildString {
            session.phoneNumber?.let { append("From: $it\n") }
            append("Status: ${session.status}\n")
            session.detectionMethod?.let { append("Source: $it\n") }
            session.recordingDisplayName?.let { append("File: $it\n") }
            session.endedAt?.let { append("Ended: ${android.text.format.DateFormat.format("MMM d, HH:mm", it)}") }
        }

        binding.resultBody.text = formatSessionBody(session)

        val needsManual = session.status == CallSession.STATUS_NO_RECORDING ||
            (session.status == CallSession.STATUS_FAILED && session.recordingUri == null)
        binding.pickRecordingBtn.isVisible = needsManual
        binding.retryDiscoveryBtn.isVisible = session.status == CallSession.STATUS_NO_RECORDING

        binding.resultTitle.text = when (session.status) {
            CallSession.STATUS_DONE -> {
                val risk = session.result?.risk ?: "Result"
                getString(com.aegisai.app.R.string.call_guard_result_title_risk, risk)
            }
            CallSession.STATUS_ACTIVE ->
                getString(com.aegisai.app.R.string.call_guard_result_waiting_call)
            CallSession.STATUS_ANALYZING, CallSession.STATUS_DISCOVERING ->
                getString(com.aegisai.app.R.string.call_guard_result_analyzing)
            CallSession.STATUS_NO_RECORDING ->
                getString(com.aegisai.app.R.string.call_guard_no_recording_title)
            else -> getString(com.aegisai.app.R.string.call_guard_result_title)
        }
    }

    private fun formatSessionBody(session: CallSession): String {
        when (session.status) {
            CallSession.STATUS_ACTIVE ->
                return getString(com.aegisai.app.R.string.call_guard_result_active_hint)
            CallSession.STATUS_DISCOVERING ->
                return getString(com.aegisai.app.R.string.call_guard_discovering_body)
            CallSession.STATUS_ANALYZING ->
                return getString(com.aegisai.app.R.string.call_guard_analyzing_body)
        }
        session.errorMessage?.takeIf { session.result == null }?.let { return it }
        val result = session.result ?: return getString(com.aegisai.app.R.string.call_guard_result_pending)
        if (result.error != null) return result.error

        val conf = ((result.confidence ?: 0.0) * 100).toInt()
        return buildString {
            append(getString(com.aegisai.app.R.string.vishing_risk, result.risk ?: "UNKNOWN"))
            append("\n")
            append(getString(com.aegisai.app.R.string.vishing_confidence, conf))
            append("\n\n")
            append(result.reason ?: "")
            result.transcription?.takeIf { it.isNotBlank() }?.let {
                append("\n\n")
                append(getString(com.aegisai.app.R.string.vishing_transcription_label))
                append("\n")
                append(it)
            }
            result.detected_keywords?.takeIf { it.isNotEmpty() }?.let {
                append("\n\nKeywords: ")
                append(it.joinToString())
            }
        }
    }

    companion object {
        const val EXTRA_SESSION_ID = CallAnalysisNotifier.EXTRA_SESSION_ID

        fun intent(context: Context, sessionId: String): Intent =
            Intent(context, CallAnalysisResultActivity::class.java).apply {
                putExtra(EXTRA_SESSION_ID, sessionId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
    }
}
