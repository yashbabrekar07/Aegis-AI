package com.aegisai.app.call

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Records call audio as 16 kHz mono WAV (Whisper-friendly).
 * More reliable than M4A from MediaRecorder when calls end abruptly.
 */
class CallGuardWavRecorder(private val context: Context) {
    private var audioRecord: AudioRecord? = null
    private var worker: Thread? = null
    private val running = AtomicBoolean(false)
    private var outputFile: File? = null
    @Volatile
    private var pcmBytesWritten: Long = 0

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
        val sources = intArrayOf(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC,
        )

        var record: AudioRecord? = null
        for (source in sources) {
            try {
                val candidate = AudioRecord(source, sampleRate, channelConfig, encoding, bufferSize)
                if (candidate.state == AudioRecord.STATE_INITIALIZED) {
                    record = candidate
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
        outputFile = file
        audioRecord = record
        running.set(true)

        worker = Thread {
            val buffer = ByteArray(bufferSize)
            val raf = RandomAccessFile(file, "rw")
            try {
                raf.seek(WAV_HEADER_SIZE.toLong())
                while (running.get()) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        raf.write(buffer, 0, read)
                        pcmBytesWritten += read
                    } else if (read < 0) {
                        break
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

        try {
            record.startRecording()
        } catch (e: Exception) {
            Log.e(TAG, "startRecording failed", e)
            stop()
            return null
        }
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
        return file
    }

    fun recordedDurationMs(): Long {
        // 16-bit mono = 2 bytes per sample, 16000 samples/sec
        return if (pcmBytesWritten <= 0) 0L else (pcmBytesWritten * 1000L) / (16_000L * 2L)
    }

    companion object {
        private const val TAG = "CallGuardWav"
        private const val WAV_HEADER_SIZE = 44

        private fun writeEmptyWavHeader(file: File) {
            RandomAccessFile(file, "rw").use { it.write(ByteArray(WAV_HEADER_SIZE)) }
        }

        private fun patchWavHeader(file: File, pcmBytes: Long) {
            val totalDataLen = pcmBytes + 36
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
