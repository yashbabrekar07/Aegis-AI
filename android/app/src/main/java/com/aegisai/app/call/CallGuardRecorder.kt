package com.aegisai.app.call

import android.content.Context
import java.io.File

/**
 * Call Guard recording — prefers WAV via [CallGuardWavRecorder] for reliable transcription.
 */
class CallGuardRecorder(private val context: Context) {
    private val wavRecorder = CallGuardWavRecorder(context)

    val isActive: Boolean get() = wavRecorder.isActive

    fun start(): File? = wavRecorder.start()

    fun stop(): File? = wavRecorder.stop()

    fun recordedDurationMs(): Long = wavRecorder.recordedDurationMs()

    fun hasAudibleSignal(): Boolean = wavRecorder.hasAudibleSignal()

    fun peakLevel(): Int = wavRecorder.peakLevel()
}
