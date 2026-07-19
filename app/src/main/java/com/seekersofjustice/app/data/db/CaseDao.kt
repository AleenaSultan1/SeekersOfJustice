package com.seekersofjustice.app.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class CaseWithMemos(
    @Embedded val case: CaseEntity,
    @Relation(parentColumn = "id", entityColumn = "caseId")
    val memos: List<MemoEntity>,
)

@Dao
interface CaseDao {

    @Query("SELECT * FROM cases ORDER BY createdAt DESC")
    fun observeCases(): Flow<List<CaseEntity>>

    @Query("SELECT * FROM cases WHERE id = :caseId")
    fun observeCase(caseId: Long): Flow<CaseEntity?>

    @Transaction
    @Query("SELECT * FROM cases WHERE id = :caseId")
    fun observeCaseWithMemos(caseId: Long): Flow<CaseWithMemos?>

    /** Aborts with SQLiteConstraintException on duplicate FIR number. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(case: CaseEntity): Long

    @Query("DELETE FROM cases WHERE id = :caseId")
    suspend fun delete(caseId: Long)
}
