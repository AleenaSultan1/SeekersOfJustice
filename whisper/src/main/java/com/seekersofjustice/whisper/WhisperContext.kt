package com.seekersofjustice.whisper

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

data class WhisperSegment(
    val startMs: Long,
    val endMs: Long,
    val text: String,
)

data class WhisperResult(
    val text: String,
    val detectedLanguage: String,
    val segments: List<WhisperSegment>,
)

/**
 * Owns one native whisper context. All native calls are funneled through a
 * single-thread dispatcher because the context is not thread-safe.
 */
class WhisperContext private constructor(private var ctxPtr: Long) {

    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "whisper-native")
    }
    private val dispatcher = executor.asCoroutineDispatcher()

    /**
     * @param pcm 16 kHz mono float32 samples in [-1, 1]
     * @param language ISO 639-1 hint or null for auto-detect
     */
    suspend fun transcribe(
        pcm: FloatArray,
        language: String?,
        translateToEnglish: Boolean,
        nThreads: Int,
    ): WhisperResult = withContext(dispatcher) {
        check(ctxPtr != 0L) { "WhisperContext already released" }
        val ret = WhisperLib.fullTranscribe(
            ctxPtr = ctxPtr,
            nThreads = nThreads,
            lang = language ?: "auto",
            translate = translateToEnglish,
            pcm = pcm,
        )
        check(ret == 0) { "whisper_full failed with code $ret" }

        val count = WhisperLib.getSegmentCount(ctxPtr)
        val segments = (0 until count).map { i ->
            WhisperSegment(
                startMs = WhisperLib.getSegmentT0(ctxPtr, i) * 10, // 10 ms ticks
                endMs = WhisperLib.getSegmentT1(ctxPtr, i) * 10,
                text = WhisperLib.getSegmentText(ctxPtr, i).trim(),
            )
        }.filter { it.text.isNotEmpty() }

        WhisperResult(
            text = segments.joinToString(" ") { it.text }.trim(),
            detectedLanguage = WhisperLib.detectedLanguage(ctxPtr),
            segments = segments,
        )
    }

    suspend fun release() = withContext(dispatcher) {
        if (ctxPtr != 0L) {
            WhisperLib.freeContext(ctxPtr)
            ctxPtr = 0L
        }
        executor.shutdown()
    }

    companion object {
        /** Loads the ggml model file; throws if it cannot be loaded. */
        fun createFromFile(modelPath: String): WhisperContext {
            val ptr = WhisperLib.initContext(modelPath)
            check(ptr != 0L) { "Failed to load Whisper model from $modelPath" }
            return WhisperContext(ptr)
        }
    }
}
