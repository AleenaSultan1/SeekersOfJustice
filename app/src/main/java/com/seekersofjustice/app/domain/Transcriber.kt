package com.seekersofjustice.app.domain

/**
 * Speech-to-text backend. Implementations must be swappable without touching
 * callers (see FakeTranscriber and WhisperTranscriber).
 */
interface Transcriber {
    /**
     * @param pcm audio as 16 kHz mono float32 samples in [-1, 1]
     * @param language ISO 639-1 hint ("ur", "en"); null lets the backend auto-detect
     * @param translateToEnglish when true, output text is an English translation
     *   of the speech instead of a same-language transcript
     */
    suspend fun transcribe(
        pcm: FloatArray,
        language: String?,
        translateToEnglish: Boolean,
    ): TranscriptionResult
}

data class TranscriptionResult(
    val text: String,
    val detectedLanguage: String,
    val segments: List<Segment>,
)

data class Segment(
    val startMs: Long,
    val endMs: Long,
    val text: String,
)
