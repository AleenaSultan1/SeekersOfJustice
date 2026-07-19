package com.seekersofjustice.app.transcription

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.seekersofjustice.app.App
import com.seekersofjustice.app.R
import com.seekersofjustice.app.domain.model.MemoLanguage
import com.seekersofjustice.app.domain.model.MemoStatus
import java.io.File

/**
 * Transcribes one memo: decode audio → transcribe → (Urdu) translate →
 * summarize → write results. Runs as a long-running foreground worker so
 * whisper.cpp is not killed by the 10-minute JobScheduler cap.
 */
class TranscriptionWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    companion object {
        private const val CHANNEL_ID = "transcription"
        private const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        val memoId = inputData.getLong(TranscriptionQueue.KEY_MEMO_ID, -1)
        if (memoId < 0) return Result.success()

        val container = (applicationContext as App).container
        val memoRepository = container.memoRepository

        val memo = memoRepository.getMemo(memoId) ?: return Result.success()
        // Already handled (e.g. re-enqueued after process death, then completed).
        if (memo.status == MemoStatus.DONE) return Result.success()

        try {
            setForeground(createForegroundInfo())
        } catch (_: IllegalStateException) {
            // Foreground start restricted; continue as a regular worker.
        }

        memoRepository.markProcessing(memoId)
        try {
            val pcm = container.audioDecoder.decode(File(memo.audioPath))

            val result = container.transcriber.transcribe(
                pcm = pcm,
                language = memo.language.whisperCode,
                translateToEnglish = false,
            )
            val effectiveLanguage = memo.language.whisperCode ?: result.detectedLanguage

            val translationEn = if (effectiveLanguage == "ur") {
                container.transcriber.transcribe(
                    pcm = pcm,
                    language = effectiveLanguage,
                    translateToEnglish = true,
                ).text
            } else {
                null
            }

            val keyPoints = container.summarizer.summarize(
                text = result.text,
                segments = result.segments,
                languageCode = effectiveLanguage,
            )

            memoRepository.setResult(
                memoId = memoId,
                transcript = result.text,
                translationEn = translationEn,
                detectedLanguage = if (memo.language == MemoLanguage.AUTO) result.detectedLanguage else null,
                keyPoints = keyPoints,
            )
        } catch (e: Exception) {
            memoRepository.setFailed(memoId, e.message ?: e.javaClass.simpleName)
        }
        // Always success: failures are surfaced in the UI with a Retry button.
        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = createForegroundInfo()

    private fun createForegroundInfo(): ForegroundInfo {
        val manager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.notification_channel_transcription),
                NotificationManager.IMPORTANCE_LOW,
            )
        )
        val notification: Notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_transcribe)
            .setContentTitle(applicationContext.getString(R.string.notification_transcribing))
            .setOngoing(true)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }
}
