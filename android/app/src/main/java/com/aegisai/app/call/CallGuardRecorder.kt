package com.aegisai.app.call

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/** Mic capture during an active call (speakerphone works best on most devices). */
class CallGuardRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    val isActive: Boolean get() = recorder != null

    fun start(): File? {
        stop()
        val file = File(context.cacheDir, "call_${System.currentTimeMillis()}.m4a")
        val sources = intArrayOf(
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.VOICE_RECOGNITION
        )
        for (source in sources) {
            try {
                val r = newRecorder()
                configure(r, file, source)
                recorder = r
                outputFile = file
                return file
            } catch (_: Exception) {
                try {
                    recorder?.release()
                } catch (_: Exception) { }
                recorder = null
            }
        }
        return null
    }

    fun stop(): File? {
        val file = outputFile
        try {
            recorder?.stop()
        } catch (_: Exception) { }
        try {
            recorder?.release()
        } catch (_: Exception) { }
        recorder = null
        outputFile = null
        return file
    }

    private fun newRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }

    private fun configure(recorder: MediaRecorder, file: File, source: Int) {
        recorder.setAudioSource(source)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        recorder.setOutputFile(file.absolutePath)
        recorder.prepare()
        recorder.start()
    }
}
