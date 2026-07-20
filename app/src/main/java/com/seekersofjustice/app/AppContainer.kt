package com.seekersofjustice.app

import android.content.Context
import com.seekersofjustice.app.audio.AudioDecoder
import com.seekersofjustice.app.audio.AudioImporter
import com.seekersofjustice.app.data.db.AppDatabase
import com.seekersofjustice.app.data.repo.CaseRepository
import com.seekersofjustice.app.data.repo.MemoRepository
import com.seekersofjustice.app.domain.Transcriber
import com.seekersofjustice.app.transcription.ExtractiveSummarizer
import com.seekersofjustice.app.transcription.IcuSentenceSplitter
import com.seekersofjustice.app.transcription.TranscriptionQueue
import com.seekersofjustice.app.transcription.WhisperTranscriber

/**
 * Manual dependency container. The transcription backend is swapped here:
 * replace FakeTranscriber with WhisperTranscriber in Stage 3.
 */
class AppContainer(context: Context) {
    private val db = AppDatabase.build(context)

    val caseRepository = CaseRepository(db.caseDao())
    val memoRepository = MemoRepository(db.memoDao())

    val transcriber: Transcriber = WhisperTranscriber(context)
    val summarizer = ExtractiveSummarizer(IcuSentenceSplitter())
    val transcriptionQueue = TranscriptionQueue(context)
    val audioDecoder = AudioDecoder()
    val audioImporter = AudioImporter(context)
}
