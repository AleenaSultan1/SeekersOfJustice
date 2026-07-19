package com.seekersofjustice.app

import android.content.Context
import com.seekersofjustice.app.data.db.AppDatabase
import com.seekersofjustice.app.data.repo.CaseRepository
import com.seekersofjustice.app.data.repo.MemoRepository

/**
 * Manual dependency container. Transcriber and audio components are added in
 * later stages; swapping the transcription backend happens here.
 */
class AppContainer(context: Context) {
    private val db = AppDatabase.build(context)

    val caseRepository = CaseRepository(db.caseDao())
    val memoRepository = MemoRepository(db.memoDao())
}
