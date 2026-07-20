package com.seekersofjustice.app.transcription

import android.content.Context
import com.seekersofjustice.app.domain.Segment
import com.seekersofjustice.app.domain.Transcriber
import com.seekersofjustice.app.domain.TranscriptionResult
import com.seekersofjustice.whisper.WhisperContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * On-device transcription via whisper.cpp with the bundled quantized
 * Whisper small model. The model asset is copied to the filesystem on first
 * use (whisper reads a plain file path) and loaded once per process.
 */
class WhisperTranscriber(private val context: Context) : Transcriber {

    companion object {
        private const val MODEL_ASSET = "models/ggml-small-q5_1.bin"
    }

    private val initMutex = Mutex()
    private var whisper: WhisperContext? = null

    private val nThreads =
        (Runtime.getRuntime().availableProcessors() - 2).coerceIn(2, 6)

    override suspend fun transcribe(
        pcm: FloatArray,
        language: String?,
        translateToEnglish: Boolean,
    ): TranscriptionResult {
        val ctx = ensureLoaded()
        val result = ctx.transcribe(
            pcm = pcm,
            language = language,
            translateToEnglish = translateToEnglish,
            nThreads = nThreads,
        )
        return TranscriptionResult(
            text = result.text,
            detectedLanguage = result.detectedLanguage,
            segments = result.segments.map { Segment(it.startMs, it.endMs, it.text) },
        )
    }

    private suspend fun ensureLoaded(): WhisperContext = initMutex.withLock {
        whisper ?: withContext(Dispatchers.IO) {
            WhisperContext.createFromFile(ensureModelFile().absolutePath)
        }.also { whisper = it }
    }

    /** Copies the bundled model out of assets on first run (exists + size check). */
    private fun ensureModelFile(): File {
        val modelFile = File(File(context.filesDir, "models"), "ggml-small-q5_1.bin")
        val assetSize = context.assets.openFd(MODEL_ASSET).use { it.length }
        if (modelFile.exists() && modelFile.length() == assetSize) return modelFile

        modelFile.parentFile?.mkdirs()
        val temp = File(modelFile.parentFile, "${modelFile.name}.tmp")
        context.assets.open(MODEL_ASSET).use { input ->
            temp.outputStream().use { output -> input.copyTo(output, bufferSize = 1 shl 20) }
        }
        check(temp.length() == assetSize) { "Model copy incomplete" }
        temp.renameTo(modelFile)
        return modelFile
    }
}
