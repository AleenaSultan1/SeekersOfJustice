package com.seekersofjustice.app.data.repo

import com.seekersofjustice.app.data.db.MemoDao
import com.seekersofjustice.app.data.db.toDomain
import com.seekersofjustice.app.data.db.toEntity
import com.seekersofjustice.app.domain.model.Memo
import com.seekersofjustice.app.domain.model.MemoStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MemoRepository(private val memoDao: MemoDao) {

    fun observeMemo(memoId: Long): Flow<Memo?> =
        memoDao.observeMemo(memoId).map { it?.toDomain() }

    suspend fun getMemo(memoId: Long): Memo? = memoDao.getMemo(memoId)?.toDomain()

    suspend fun addMemo(memo: Memo): Long = memoDao.insert(memo.toEntity())

    suspend fun markProcessing(memoId: Long) =
        memoDao.updateStatus(memoId, MemoStatus.PROCESSING.name)

    suspend fun markPending(memoId: Long) =
        memoDao.updateStatus(memoId, MemoStatus.PENDING.name)

    suspend fun setResult(
        memoId: Long,
        transcript: String,
        translationEn: String?,
        detectedLanguage: String?,
        keyPoints: List<String>,
    ) = memoDao.setResult(
        memoId = memoId,
        transcript = transcript,
        translationEn = translationEn,
        detectedLanguage = detectedLanguage,
        keyPoints = keyPoints,
        status = MemoStatus.DONE.name,
    )

    suspend fun setFailed(memoId: Long, errorMessage: String?) =
        memoDao.setFailed(memoId, errorMessage)

    suspend fun resetStuckProcessing() = memoDao.resetStuckProcessing()

    suspend fun pendingMemoIds(): List<Long> = memoDao.pendingMemoIds()

    suspend fun deleteMemo(memoId: Long) = memoDao.delete(memoId)
}
