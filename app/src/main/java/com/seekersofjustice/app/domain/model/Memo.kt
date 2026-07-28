package com.seekersofjustice.app.domain.model

/** Language the user selected for this memo before transcription. */
enum class MemoLanguage(val whisperCode: String?) {
    URDU("ur"),
    ENGLISH("en"),
    AUTO(null),
}

enum class MemoStatus {
    PENDING, PROCESSING, DONE, FAILED
}

data class Memo(
    val id: Long = 0,
    val caseId: Long,
    val audioPath: String,
    val durationMs: Long,
    val language: MemoLanguage,
    val status: MemoStatus,
    /** Person giving this statement; null means "same as the case's primary person". */
    val speakerName: String? = null,
    val speakerRelation: RelationToCrime? = null,
    /** Officer who recorded the statement, for audit. */
    val recordedByUserId: Long? = null,
    val transcript: String? = null,
    val translationEn: String? = null,
    /** ISO code Whisper detected; filled when [language] is [MemoLanguage.AUTO]. */
    val detectedLanguage: String? = null,
    val keyPoints: List<String> = emptyList(),
    val errorMessage: String? = null,
    val createdAt: Long,
)
