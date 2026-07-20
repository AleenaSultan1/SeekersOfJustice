package com.seekersofjustice.whisper

internal object WhisperLib {
    init {
        System.loadLibrary("whisper_jni")
    }

    external fun initContext(modelPath: String): Long
    external fun freeContext(ctxPtr: Long)

    /** lang: ISO 639-1 code, or "auto". Returns 0 on success. */
    external fun fullTranscribe(
        ctxPtr: Long,
        nThreads: Int,
        lang: String,
        translate: Boolean,
        pcm: FloatArray,
    ): Int

    external fun getSegmentCount(ctxPtr: Long): Int
    external fun getSegmentText(ctxPtr: Long, index: Int): String
    external fun getSegmentT0(ctxPtr: Long, index: Int): Long
    external fun getSegmentT1(ctxPtr: Long, index: Int): Long
    external fun detectedLanguage(ctxPtr: Long): String
}
