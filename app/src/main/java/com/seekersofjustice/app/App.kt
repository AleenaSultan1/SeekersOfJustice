package com.seekersofjustice.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class App : Application() {
    lateinit var container: AppContainer
        private set

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Recovery after process death: memos stuck in PROCESSING go back to
        // PENDING, then everything pending is re-enqueued. The worker exits
        // early for memos that already completed, so double-enqueue is harmless.
        applicationScope.launch {
            container.memoRepository.resetStuckProcessing()
            container.memoRepository.pendingMemoIds().forEach {
                container.transcriptionQueue.enqueue(it)
            }
        }
    }
}
