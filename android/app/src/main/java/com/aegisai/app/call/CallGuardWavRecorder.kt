package com.aegisai.app.call

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.aegisai.app.AegisApp
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Records call audio as 16 kHz mono WAV (Whisper-friendly).
 * Automatically adapts and falls back if silence or read errors are detected.
 */
class CallGuardWavRecorder(private val context: Context) {
    private var audioRecord: AudioRecord? = null
    private var worker: Thread? = null
    private val running = AtomicBoolean(false)
    private var outputFile: File? = null
    @Volatile
    private var pcmBytesWritten: Long = 0
    private val peakAmplitude = AtomicInteger(0)

    val isActive: Boolean get() = running.get()

    fun start(): File? {
        stop()
        val file = File(context.cacheDir, "call_${System.currentTimeMillis()}.wav")
        val sampleRate = 16_000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelConfig, encoding)
        if (minBuffer <= 0) return null

        val bufferSize = minBuffer * 4

        // Get primary source from preferences
        val prefs = AegisApp.get(context).prefs
        val primarySource = prefs.bestAudioSource

        // Build fallback chain
        val sourcesList = mutableListOf(primarySource)
        val allPossible = listOf(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT
        )
        for (s in allPossible) {
            if (!sourcesList.contains(s)) {
                sourcesList.add(s)
            }
        }

        var record: AudioRecord? = null
        var chosenSource = primarySource
        var chosenIndex = 0

        for (i in sourcesList.indices) {
            val source = sourcesList[i]
            try {
                val candidate = AudioRecord(source, sampleRate, channelConfig, encoding, bufferSize)
                if (candidate.state == AudioRecord.STATE_INITIALIZED) {
                    record = candidate
                    chosenSource = source
                    chosenIndex = i
                    break
                }
                candidate.release()
            } catch (e: Exception) {
                Log.w(TAG, "Audio source $source failed: ${e.message}")
            }
        }

        if (record == null) return null

        writeEmptyWavHeader(file)
        pcmBytesWritten = 0
        peakAmplitude.set(0)
        outputFile = file
        audioRecord = record
        running.set(true)

        try {
            record.startRecording()
        } catch (e: Exception) {
            Log.e(TAG, "startRecording failed", e)
            stop()
            return null
        }

        val activeRecord = record
        val initialSourceIndex = chosenIndex

        worker = Thread {
            val buffer = ByteArray(bufferSize)
            val raf = RandomAccessFile(file, "rw")
            var currentSourceIdx = initialSourceIndex
            var currentRecord: AudioRecord? = activeRecord
            var lastSourceSwitchTime = System.currentTimeMillis()

            try {
                raf.seek(WAV_HEADER_SIZE.toLong())
                while (running.get()) {
                    var read = -1
                    try {
                        read = currentRecord?.read(buffer, 0, buffer.size) ?: -1
                    } catch (e: Exception) {
                        Log.e(TAG, "Read error: ${e.message}")
                    }

                    if (read > 0) {
                        raf.write(buffer, 0, read)
                        pcmBytesWritten += read
                        var i = 0
                        var localPeak = 0
                        while (i < read - 1) {
                            val sample = (buffer[i].toInt() and 0xff) or (buffer[i + 1].toInt() shl 8)
                            val amp = kotlin.math.abs(sample.toShort().toInt())
                            if (amp > localPeak) {
                                localPeak = amp
                            }
                            i += 2
                        }
                        if (localPeak > peakAmplitude.get()) {
                            peakAmplitude.set(localPeak)
                        }
                    }

                    // Fallback Trigger: if silence is detected (peak amplitude remains below 100) after 3.5 seconds
                    val now = System.currentTimeMillis()
                    if (now - lastSourceSwitchTime > 3500 && peakAmplitude.get() < 100) {
                        if (currentSourceIdx < sourcesList.size - 1) {
                            currentSourceIdx++
                            val nextSource = sourcesList[currentSourceIdx]
                            Log.w(TAG, "Silence detected with source ${sourcesList[currentSourceIdx - 1]}. Falling back to $nextSource")

                            // Release old recorder
                            try {
                                currentRecord?.stop()
                                currentRecord?.release()
                            } catch (_: Exception) {}

                            // Initialize new recorder
                            try {
                                val newRecord = AudioRecord(nextSource, sampleRate, channelConfig, encoding, bufferSize)
                                if (newRecord.state == AudioRecord.STATE_INITIALIZED) {
                                    newRecord.startRecording()
                                    currentRecord = newRecord
                                    audioRecord = newRecord // Update main reference
                                    lastSourceSwitchTime = now
                                    peakAmplitude.set(0)

                                    // Force speakerphone if we fallback to MIC or DEFAULT to capture incoming call audio
                                    if (nextSource == MediaRecorder.AudioSource.MIC || nextSource == MediaRecorder.AudioSource.DEFAULT) {
                                        triggerSpeakerphoneForce(context)
                                    }
                                } else {
                                    newRecord.release()
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to switch to source $nextSource", e)
                            }
                        }
                    }

                    if (read < 0) {
                        if (currentSourceIdx < sourcesList.size - 1) {
                            currentSourceIdx++
                            val nextSource = sourcesList[currentSourceIdx]
                            Log.w(TAG, "Read error, switching to source $nextSource")
                            try {
                                currentRecord?.stop()
                                currentRecord?.release()
                            } catch (_: Exception) {}

                            try {
                                val newRecord = AudioRecord(nextSource, sampleRate, channelConfig, encoding, bufferSize)
                                if (newRecord.state == AudioRecord.STATE_INITIALIZED) {
                                    newRecord.startRecording()
                                    currentRecord = newRecord
                                    audioRecord = newRecord
                                    lastSourceSwitchTime = System.currentTimeMillis()
                                    peakAmplitude.set(0)
                                } else {
                                    newRecord.release()
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to switch to source $nextSource on read error", e)
                            }
                        } else {
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Recording loop error", e)
            } finally {
                try {
                    raf.close()
                } catch (_: Exception) { }
            }
        }.apply { name = "CallGuardWav"; start() }

        Log.i(TAG, "Recording started source=$chosenSource file=${file.name}")
        return file
    }

    fun stop(): File? {
        running.set(false)
        try {
            audioRecord?.stop()
        } catch (_: Exception) { }
        try {
            worker?.join(4_000)
        } catch (_: Exception) { }
        try {
            audioRecord?.release()
        } catch (_: Exception) { }
        audioRecord = null
        worker = null

        val file = outputFile
        outputFile = null
        if (file != null && file.exists() && pcmBytesWritten > 0) {
            patchWavHeader(file, pcmBytesWritten)
        }
        Log.i(TAG, "Recording stopped bytes=$pcmBytesWritten peak=${peakAmplitude.get()}")
        return file
    }

    fun recordedDurationMs(): Long {
        return if (pcmBytesWritten <= 0) 0L else (pcmBytesWritten * 1000L) / (16_000L * 2L)
    }

    fun hasAudibleSignal(): Boolean = peakAmplitude.get() >= MIN_PEAK_AMPLITUDE

    fun peakLevel(): Int = peakAmplitude.get()

    companion object {
        private const val TAG = "CallGuardWav"
        private const val WAV_HEADER_SIZE = 44
        /** ~ -40 dB on 16-bit PCM; below this the clip is effectively silence. */
        private const val MIN_PEAK_AMPLITUDE = 250

        private fun triggerSpeakerphoneForce(context: Context) {
            try {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val devices = am.availableCommunicationDevices
                    val speakerDevice = devices.find { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                    if (speakerDevice != null) {
                        am.setCommunicationDevice(speakerDevice)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    am.isSpeakerphoneOn = true
                }
            } catch (_: Exception) {}
        }

        private fun writeEmptyWavHeader(file: File) {
            RandomAccessFile(file, "rw").use { it.write(ByteArray(WAV_HEADER_SIZE)) }
        }

        private fun patchWavHeader(file: File, pcmBytes: Long) {
            val totalLen = pcmBytes + WAV_HEADER_SIZE - 8
            val sampleRate = 16_000
            val channels = 1
            val byteRate = sampleRate * channels * 2

            RandomAccessFile(file, "rw").use { raf ->
                raf.seek(0)
                raf.writeBytes("RIFF")
                raf.writeIntLE(totalLen.toInt())
                raf.writeBytes("WAVE")
                raf.writeBytes("fmt ")
                raf.writeIntLE(16)
                raf.writeShortLE(1)
                raf.writeShortLE(channels.toShort())
                raf.writeIntLE(sampleRate)
                raf.writeIntLE(byteRate)
                raf.writeShortLE((channels * 2).toShort())
                raf.writeShortLE(16)
                raf.writeBytes("data")
                raf.writeIntLE(pcmBytes.toInt())
            }
        }

        private fun RandomAccessFile.writeIntLE(value: Int) {
            write(value and 0xff)
            write((value shr 8) and 0xff)
            write((value shr 16) and 0xff)
            write((value shr 24) and 0xff)
        }

        private fun RandomAccessFile.writeShortLE(value: Short) {
            val v = value.toInt()
            write(v and 0xff)
            write((v shr 8) and 0xff)
        }
    }
}
