package com.seekersofjustice.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {

    @Query("SELECT * FROM memos WHERE id = :memoId")
    fun observeMemo(memoId: Long): Flow<MemoEntity?>

    @Query("SELECT * FROM memos WHERE id = :memoId")
    suspend fun getMemo(memoId: Long): MemoEntity?

    @Insert
    suspend fun insert(memo: MemoEntity): Long

    @Query("UPDATE memos SET status = :status WHERE id = :memoId")
    suspend fun updateStatus(memoId: Long, status: String)

    @Query(
        """UPDATE memos SET
            transcript = :transcript,
            translationEn = :translationEn,
            detectedLanguage = :detectedLanguage,
            keyPoints = :keyPoints,
            status = :status,
            errorMessage = NULL
        WHERE id = :memoId"""
    )
    suspend fun setResult(
        memoId: Long,
        transcript: String,
        translationEn: String?,
        detectedLanguage: String?,
        keyPoints: List<String>,
        status: String,
    )

    @Query("UPDATE memos SET status = :status, errorMessage = :errorMessage WHERE id = :memoId")
    suspend fun setFailed(memoId: Long, errorMessage: String?, status: String = "FAILED")

    /** Recovery after process death: anything stuck in PROCESSING goes back to PENDING. */
    @Query("UPDATE memos SET status = 'PENDING' WHERE status = 'PROCESSING'")
    suspend fun resetStuckProcessing()

    @Query("SELECT id FROM memos WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun pendingMemoIds(): List<Long>

    @Query("DELETE FROM memos WHERE id = :memoId")
    suspend fun delete(memoId: Long)
}
