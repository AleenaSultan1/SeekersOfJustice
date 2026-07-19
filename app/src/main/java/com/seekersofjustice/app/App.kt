package com.seekersofjustice.app

import android.app.Application

class App : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Stage 2 adds: reset stuck PROCESSING memos + re-enqueue PENDING work.
    }
}
