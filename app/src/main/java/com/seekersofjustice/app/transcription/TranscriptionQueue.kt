package com.seekersofjustice.app.transcription

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

/**
 * Enqueues memos for transcription as a single unique-work chain, so memos
 * are processed strictly one at a time (the Whisper context is not shareable).
 */
class TranscriptionQueue(private val context: Context) {

    companion object {
        private const val UNIQUE_WORK_NAME = "transcription"
        const val KEY_MEMO_ID = "memoId"
    }

    fun enqueue(memoId: Long) {
        val request = OneTimeWorkRequestBuilder<TranscriptionWorker>()
            .setInputData(workDataOf(KEY_MEMO_ID to memoId))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            request,
        )
    }
}
