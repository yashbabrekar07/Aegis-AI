package com.aegisai.app.call

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.aegisai.app.AegisApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CallGuardDiagnostics {
    private const val TAG = "CallGuardDiag"

    suspend fun runCompatibilityTest(context: Context): Boolean = withContext(Dispatchers.IO) {
        val prefs = AegisApp.get(context).prefs
        val sampleRate = 16_000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelConfig, encoding)
        if (minBuffer <= 0) {
            prefs.hasCheckedCompatibility = true
            return@withContext false
        }

        val bufferSize = minBuffer * 4
        val candidateSources = intArrayOf(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT
        )

        var selectedSource = MediaRecorder.AudioSource.MIC
        var speakerNeeded = true
        var foundWorking = false

        for (source in candidateSources) {
            Log.d(TAG, "Testing audio source: $source")
            var recorder: AudioRecord? = null
            try {
                recorder = AudioRecord(source, sampleRate, channelConfig, encoding, bufferSize)
                if (recorder.state == AudioRecord.STATE_INITIALIZED) {
                    recorder.startRecording()
                    val tempBuffer = ByteArray(bufferSize)
                    var totalRead = 0
                    var maxAmplitude = 0
                    val startTime = System.currentTimeMillis()

                    // Read for 500ms to see if we get actual audio or pure silence (zeros)
                    while (System.currentTimeMillis() - startTime < 500) {
                        val read = recorder.read(tempBuffer, 0, tempBuffer.size)
                        if (read > 0) {
                            totalRead += read
                            for (i in 0 until read - 1 step 2) {
                                val sample = (tempBuffer[i].toInt() and 0xff) or (tempBuffer[i + 1].toInt() shl 8)
                                val amp = kotlin.math.abs(sample.toShort().toInt())
                                if (amp > maxAmplitude) {
                                    maxAmplitude = amp
                                }
                            }
                        }
                        Thread.sleep(50)
                    }

                    recorder.stop()
                    Log.d(TAG, "Source $source: read $totalRead bytes, max amplitude $maxAmplitude")

                    // If we read data and it's not silent (amplitude > 50)
                    if (totalRead > 0 && maxAmplitude > 50) {
                        selectedSource = source
                        speakerNeeded = (source == MediaRecorder.AudioSource.MIC || source == MediaRecorder.AudioSource.DEFAULT)
                        foundWorking = true
                        break
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Source $source failed: ${e.message}")
            } finally {
                try {
                    recorder?.release()
                } catch (_: Exception) {}
            }
        }

        prefs.bestAudioSource = selectedSource
        prefs.speakerRequired = speakerNeeded
        prefs.hasCheckedCompatibility = true
        Log.i(TAG, "Selected source: $selectedSource, speakerRequired: $speakerNeeded")
        return@withContext foundWorking
    }
}
