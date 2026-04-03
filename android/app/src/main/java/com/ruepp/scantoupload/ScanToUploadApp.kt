package com.ruepp.scantoupload

import android.app.Application
import android.util.Log

class ScanToUploadApp : Application() {

    companion object {
        private const val TAG = "ScanToUploadApp"
    }

    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception on thread ${thread.name}", throwable)
            if (defaultHandler != null) {
                runCatching {
                    defaultHandler.uncaughtException(thread, throwable)
                }.onFailure { error ->
                    Log.e(TAG, "Default exception handler failed", error)
                }
            }
        }
    }
}
