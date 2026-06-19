package com.aegisai.app.call

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log

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
        val candidates = queryCandidates(context, sinceMs)
        return candidates.maxByOrNull { it.score }?.uri
    }

    fun findBestMatchDetailed(context: Context, sinceMs: Long): Candidate? {
        val candidates = queryCandidates(context, sinceMs)
        return candidates.maxByOrNull { it.score }
    }

    private fun queryCandidates(context: Context, sinceMs: Long): List<Candidate> {
        val sinceSec = (sinceMs / 1000L) - 10L
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.DATE_MODIFIED)
            add(MediaStore.Audio.Media.DURATION)
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
                    val pathHint = if (pathCol >= 0) cursor.getString(pathCol).orEmpty() else ""
                    val uri = Uri.withAppendedPath(collection, id.toString())
                    val score = scoreCandidate(name, pathHint, added, duration, sinceSec)
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
        return results
    }

    private fun scoreCandidate(
        name: String,
        path: String,
        dateAddedSec: Long,
        durationMs: Long,
        sinceSec: Long,
    ): Int {
        val ageSec = dateAddedSec - sinceSec
        val combined = "$name $path".lowercase()
        val matchedHints = PATH_HINTS.count { combined.contains(it) }

        if (matchedHints == 0) {
            // Recent audio with call-like duration — many OEMs omit "call" in filenames
            if (durationMs in 5_000..3_600_000 && ageSec in 0..300) {
                return 8
            }
            return 0
        }

        var score = 10 + matchedHints * 15
        if (combined.contains("call")) score += 20
        if (combined.contains("record")) score += 15
        if (durationMs in 3_000..3_600_000) score += 10
        if (ageSec in 0..120) score += 25
        else if (ageSec in 0..300) score += 10
        return score
    }
}
