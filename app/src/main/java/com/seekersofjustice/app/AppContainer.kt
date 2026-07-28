package com.seekersofjustice.app

import android.content.Context
import com.seekersofjustice.app.audio.AudioDecoder
import com.seekersofjustice.app.audio.AudioImporter
import com.seekersofjustice.app.data.db.AppDatabase
import com.seekersofjustice.app.data.repo.AuthRepository
import com.seekersofjustice.app.data.repo.CaseRepository
import com.seekersofjustice.app.data.repo.MemoRepository
import com.seekersofjustice.app.domain.Transcriber
import com.seekersofjustice.app.domain.model.Officer
import com.seekersofjustice.app.transcription.ExtractiveSummarizer
import com.seekersofjustice.app.transcription.IcuSentenceSplitter
import com.seekersofjustice.app.transcription.TranscriptionQueue
import com.seekersofjustice.app.transcription.WhisperTranscriber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manual dependency container. The transcription backend is swapped here:
 * replace WhisperTranscriber with FakeTranscriber to run without the model.
 */
class AppContainer(context: Context) {
    private val db = AppDatabase.build(context)

    val caseRepository = CaseRepository(db.caseDao())
    val memoRepository = MemoRepository(db.memoDao())
    val authRepository = AuthRepository(db.userDao())

    val transcriber: Transcriber = WhisperTranscriber(context)
    val summarizer = ExtractiveSummarizer(IcuSentenceSplitter())
    val transcriptionQueue = TranscriptionQueue(context)
    val audioDecoder = AudioDecoder()
    val audioImporter = AudioImporter(context)

    // In-memory session: who is signed in. Cleared on process death, so the
    // sign-in gate is shown again on every cold start.
    private val _currentOfficer = MutableStateFlow<Officer?>(null)
    val currentOfficer: StateFlow<Officer?> = _currentOfficer

    fun signIn(officer: Officer) { _currentOfficer.value = officer }
    fun signOut() { _currentOfficer.value = null }
}
