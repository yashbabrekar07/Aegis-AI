package com.aegisai.app.call

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.io.File

/**
 * Finds call recordings saved by the system dialer after a call ends.
 * Uses MediaStore queries with OEM path heuristics (Samsung, Pixel, Xiaomi, etc.).
 */
object DialerRecordingFinder {
    private const val TAG = "DialerRecordingFinder"

    private val PATH_HINTS = listOf(
        "call", "record", "soundrecorder", "sound recorder", "miui",
        "samsung", "dialer", "phone", "voice", "recorder", "calls",
        "realme", "oppo", "vivo", "oneplus", "coloros", "oxygen",
        "recording", "callrecord", "call_rec", "telephony", "audio",
    )

    data class Candidate(
        val uri: Uri,
        val displayName: String,
        val dateAddedSec: Long,
        val durationMs: Long,
        val pathHint: String,
        val score: Int,
    )

    fun findBestMatch(context: Context, sinceMs: Long): Uri? {
        return findBestMatch(context, sinceMs, System.currentTimeMillis())
    }

    fun findBestMatchDetailed(context: Context, sinceMs: Long): Candidate? {
        return findBestMatchDetailed(context, sinceMs, System.currentTimeMillis())
    }

    fun findBestMatch(
        context: Context,
        startedAtMs: Long,
        endedAtMs: Long,
        learnedPath: String? = null,
        learnedExt: String? = null
    ): Uri? {
        val candidates = queryCandidates(context, startedAtMs, endedAtMs, learnedPath, learnedExt)
        return candidates.maxByOrNull { it.score }?.uri
    }

    fun findBestMatchDetailed(
        context: Context,
        startedAtMs: Long,
        endedAtMs: Long,
        learnedPath: String? = null,
        learnedExt: String? = null
    ): Candidate? {
        val candidates = queryCandidates(context, startedAtMs, endedAtMs, learnedPath, learnedExt)
        return candidates.maxByOrNull { it.score }
    }

    private fun queryCandidates(context: Context, sinceMs: Long): List<Candidate> {
        return queryCandidates(context, sinceMs, System.currentTimeMillis())
    }

    private fun queryCandidates(
        context: Context,
        startedAtMs: Long,
        endedAtMs: Long,
        learnedPath: String? = null,
        learnedExt: String? = null
    ): List<Candidate> {
        val sinceSec = (startedAtMs / 1000L) - 60L
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.DATE_MODIFIED)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.SIZE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Audio.Media.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                add(MediaStore.Audio.Media.DATA)
            }
        }.toTypedArray()

        val selection = "${MediaStore.Audio.Media.DATE_ADDED} >= ?"
        val selectionArgs = arrayOf(sinceSec.toString())
        val sort = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        val results = mutableListOf<Candidate>()
        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sort,
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                } else {
                    @Suppress("DEPRECATION")
                    cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                }

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol).orEmpty()
                    val added = cursor.getLong(addedCol)
                    val duration = cursor.getLong(durationCol)
                    val size = cursor.getLong(sizeCol)
                    val pathHint = if (pathCol >= 0) cursor.getString(pathCol).orEmpty() else ""
                    val uri = Uri.withAppendedPath(collection, id.toString())

                    // Skip empty files
                    if (size <= 0) {
                        continue
                    }

                    val score = scoreCandidate(
                        name = name,
                        path = pathHint,
                        dateAddedSec = added,
                        durationMs = duration,
                        startedAtMs = startedAtMs,
                        endedAtMs = endedAtMs,
                        learnedPath = learnedPath,
                        learnedExt = learnedExt
                    )
                    if (score > 0) {
                        results.add(
                            Candidate(uri, name, added, duration, pathHint, score)
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore query failed", e)
        }

        // Run fallback scanning of OEM folders if no highly matching candidate is found
        val hasGoodCandidate = results.any { it.score >= 20 }
        if (!hasGoodCandidate) {
            val fallbackCandidates = fallbackScanOEMFolders(
                context, startedAtMs, endedAtMs, learnedPath, learnedExt
            )
            results.addAll(fallbackCandidates)
        }

        return results
    }

    private fun scoreCandidate(
        name: String,
        path: String,
        dateAddedSec: Long,
        durationMs: Long,
        startedAtMs: Long,
        endedAtMs: Long,
        learnedPath: String?,
        learnedExt: String?
    ): Int {
        val fileCreatedMs = dateAddedSec * 1000L
        val callDurationMs = endedAtMs - startedAtMs
        val combined = "$name $path".lowercase()

        // 1. Check if matches learned path/ext behavior
        val isLearnedPath = !learnedPath.isNullOrBlank() && path.lowercase().contains(learnedPath.lowercase())
        val isLearnedExt = !learnedExt.isNullOrBlank() && name.lowercase().endsWith(learnedExt.lowercase())

        // 2. File Path Contains keywords (Score: +30)
        var pathScore = 0
        val pathKeywords = listOf("call", "callrecordings", "recordings", "phonerecord", "recorder")
        if (pathKeywords.any { combined.contains(it) }) {
            pathScore = 30
        }

        // 3. File Name Contains keywords (Score: +25)
        var nameScore = 0
        val nameKeywords = listOf("call", "record", "incoming", "outgoing")
        if (nameKeywords.any { name.lowercase().contains(it) }) {
            nameScore = 25
        }

        // 4. Duration Matches Call Duration (Score: up to +40)
        var durationScore = 0
        if (callDurationMs > 0 && durationMs > 0) {
            val diffMs = Math.abs(durationMs - callDurationMs)
            durationScore = when {
                diffMs <= 5_000 -> 40
                diffMs <= 15_000 -> 25
                diffMs <= 30_000 -> 10
                else -> 0
            }
        }

        // 5. Created Immediately After Call (Score: up to +20)
        var recencyScore = 0
        val diffEndMs = Math.abs(fileCreatedMs - endedAtMs)
        recencyScore = when {
            diffEndMs <= 10_000 -> 20
            diffEndMs <= 30_000 -> 10
            else -> 0
        }

        // Calculate base score
        var totalScore = pathScore + nameScore + durationScore + recencyScore

        // Boost if matches learned behavior
        if (isLearnedPath && isLearnedExt) {
            totalScore += 60
        } else if (isLearnedPath || isLearnedExt) {
            totalScore += 20
        }

        // Filter: it must have some minimal relevance to be a candidate
        val ageSec = dateAddedSec - (startedAtMs / 1000L)
        if (pathScore == 0 && nameScore == 0 && !isLearnedPath) {
            if (durationMs in 5_000..3_600_000 && ageSec in 0..300) {
                return totalScore + 8
            }
            return 0
        }

        return totalScore
    }

    private fun scoreCandidate(
        name: String,
        path: String,
        dateAddedSec: Long,
        durationMs: Long,
        sinceSec: Long
    ): Int {
        return scoreCandidate(
            name = name,
            path = path,
            dateAddedSec = dateAddedSec,
            durationMs = durationMs,
            startedAtMs = sinceSec * 1000L,
            endedAtMs = System.currentTimeMillis(),
            learnedPath = null,
            learnedExt = null
        )
    }

    private fun fallbackScanOEMFolders(
        context: Context,
        startedAtMs: Long,
        endedAtMs: Long,
        learnedPath: String?,
        learnedExt: String?
    ): List<Candidate> {
        val results = mutableListOf<Candidate>()
        val storageDir = android.os.Environment.getExternalStorageDirectory() ?: return emptyList()

        val oemFolders = listOf(
            "Recordings/Call",
            "CallRecordings",
            "MIUI/sound_recorder/call_rec",
            "Android/media/com.google.android.dialer",
            "Recordings",
            "PhoneRecord",
            "Recorder"
        )

        for (folderName in oemFolders) {
            val folder = File(storageDir, folderName)
            if (folder.exists() && folder.isDirectory) {
                val files = folder.listFiles() ?: continue
                for (file in files) {
                    if (file.isFile && file.length() > 0) {
                        val name = file.name
                        val ext = file.extension.lowercase()
                        if (ext in listOf("m4a", "wav", "mp3", "amr", "3gp", "ogg", "aac")) {
                            val lastModified = file.lastModified()
                            val dateAddedSec = lastModified / 1000L

                            // Check if file was modified around the call time
                            if (lastModified >= startedAtMs - 60_000L && lastModified <= endedAtMs + 120_000L) {
                                val uri = Uri.fromFile(file)
                                val durationMs = getAudioDurationMs(context, uri)
                                val pathHint = file.absolutePath
                                val score = scoreCandidate(
                                    name = name,
                                    path = pathHint,
                                    dateAddedSec = dateAddedSec,
                                    durationMs = durationMs,
                                    startedAtMs = startedAtMs,
                                    endedAtMs = endedAtMs,
                                    learnedPath = learnedPath,
                                    learnedExt = learnedExt
                                )
                                if (score > 0) {
                                    results.add(
                                        Candidate(
                                            uri = uri,
                                            displayName = name,
                                            dateAddedSec = dateAddedSec,
                                            durationMs = durationMs,
                                            pathHint = pathHint,
                                            score = score
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        return results
    }

    private fun getAudioDurationMs(context: Context, uri: Uri): Long {
        var retriever: android.media.MediaMetadataRetriever? = null
        try {
            retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val timeStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            return timeStr?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            return 0L
        } finally {
            try {
                retriever?.release()
            } catch (_: Exception) {}
        }
    }
}
