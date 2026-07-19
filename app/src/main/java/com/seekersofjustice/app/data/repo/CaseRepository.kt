package com.seekersofjustice.app.data.repo

import android.database.sqlite.SQLiteConstraintException
import com.seekersofjustice.app.data.db.CaseDao
import com.seekersofjustice.app.data.db.toDomain
import com.seekersofjustice.app.data.db.toEntity
import com.seekersofjustice.app.domain.model.Case
import com.seekersofjustice.app.domain.model.Memo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface AddCaseResult {
    data class Success(val caseId: Long) : AddCaseResult
    data object DuplicateFir : AddCaseResult
}

data class CaseDetails(
    val case: Case,
    val memos: List<Memo>,
)

class CaseRepository(private val caseDao: CaseDao) {

    fun observeCases(): Flow<List<Case>> =
        caseDao.observeCases().map { list -> list.map { it.toDomain() } }

    fun observeCaseDetails(caseId: Long): Flow<CaseDetails?> =
        caseDao.observeCaseWithMemos(caseId).map { rel ->
            rel?.let {
                CaseDetails(
                    case = it.case.toDomain(),
                    memos = it.memos
                        .map { memo -> memo.toDomain() }
                        .sortedByDescending { memo -> memo.createdAt },
                )
            }
        }

    suspend fun addCase(case: Case): AddCaseResult =
        try {
            AddCaseResult.Success(caseDao.insert(case.toEntity()))
        } catch (e: SQLiteConstraintException) {
            AddCaseResult.DuplicateFir
        }

    suspend fun deleteCase(caseId: Long) = caseDao.delete(caseId)
}
