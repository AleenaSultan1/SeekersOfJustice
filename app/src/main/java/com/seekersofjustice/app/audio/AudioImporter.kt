package com.seekersofjustice.app.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * Copies a SAF-picked audio file into app-private storage and validates it is
 * WAV or FLAC. No persistent URI permission needed because we copy immediately.
 */
class AudioImporter(private val context: Context) {

    sealed interface Result {
        data class Success(val file: File, val durationMs: Long) : Result
        data object UnsupportedFile : Result
    }

    fun import(uri: Uri): Result {
        val dir = File(context.filesDir, "audio").apply { mkdirs() }
        val temp = File(dir, "import_${UUID.randomUUID()}.tmp")
        context.contentResolver.openInputStream(uri)?.use { input ->
            temp.outputStream().use { output -> input.copyTo(output) }
        } ?: return Result.UnsupportedFile

        val magic = temp.inputStream().use { ByteArray(4).also(it::read) }.decodeToString()
        val extension = when (magic) {
            "RIFF" -> "wav"
            "fLaC" -> "flac"
            else -> {
                temp.delete()
                return Result.UnsupportedFile
            }
        }
        val file = File(dir, "memo_${UUID.randomUUID()}.$extension")
        if (!temp.renameTo(file)) {
            temp.copyTo(file, overwrite = true)
            temp.delete()
        }

        // Not .use {}: MediaMetadataRetriever.close() only exists on API 29+.
        val retriever = MediaMetadataRetriever()
        val durationMs = try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            retriever.release()
        }
        return Result.Success(file, durationMs)
    }
}
