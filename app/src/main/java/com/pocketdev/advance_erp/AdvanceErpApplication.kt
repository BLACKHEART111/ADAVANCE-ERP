package com.pocketdev.advance_erp

import android.app.Application
import com.pocketdev.advance_erp.core.worker.FridayBackupScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AdvanceErpApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FridayBackupScheduler.schedule(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
