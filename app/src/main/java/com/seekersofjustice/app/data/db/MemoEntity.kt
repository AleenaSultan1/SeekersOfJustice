package com.seekersofjustice.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.seekersofjustice.app.domain.model.Memo
import com.seekersofjustice.app.domain.model.MemoLanguage
import com.seekersofjustice.app.domain.model.MemoStatus

@Entity(
    tableName = "memos",
    foreignKeys = [
        ForeignKey(
            entity = CaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["caseId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("caseId")],
)
data class MemoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val caseId: Long,
    val audioPath: String,
    val durationMs: Long,
    val language: String,
    val status: String,
    val transcript: String? = null,
    val translationEn: String? = null,
    val detectedLanguage: String? = null,
    val keyPoints: List<String> = emptyList(),
    val errorMessage: String? = null,
    val createdAt: Long,
)

fun MemoEntity.toDomain() = Memo(
    id = id,
    caseId = caseId,
    audioPath = audioPath,
    durationMs = durationMs,
    language = MemoLanguage.valueOf(language),
    status = MemoStatus.valueOf(status),
    transcript = transcript,
    translationEn = translationEn,
    detectedLanguage = detectedLanguage,
    keyPoints = keyPoints,
    errorMessage = errorMessage,
    createdAt = createdAt,
)

fun Memo.toEntity() = MemoEntity(
    id = id,
    caseId = caseId,
    audioPath = audioPath,
    durationMs = durationMs,
    language = language.name,
    status = status.name,
    transcript = transcript,
    translationEn = translationEn,
    detectedLanguage = detectedLanguage,
    keyPoints = keyPoints,
    errorMessage = errorMessage,
    createdAt = createdAt,
)
