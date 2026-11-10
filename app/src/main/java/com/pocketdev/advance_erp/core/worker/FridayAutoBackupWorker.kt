package com.pocketdev.advance_erp.core.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.pocketdev.advance_erp.core.datastore.UserPreferencesRepository
import com.pocketdev.advance_erp.domain.repository.BackupRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BackupWorkerEntryPoint {
    fun backupRepository(): BackupRepository
    fun userPreferencesRepository(): UserPreferencesRepository
}

class FridayAutoBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                BackupWorkerEntryPoint::class.java
            )
            val preferences = entryPoint.userPreferencesRepository()
            val backupRepo = entryPoint.backupRepository()

            val isEnabled = preferences.isFridayAutoBackupEnabled.first()
            if (!isEnabled) {
                return Result.success()
            }

            // Create full ZIP backup with database and photos
            val record = backupRepo.createBackup(
                isAutomatic = true,
                notes = "Automatic Friday Weekly Backup (স্বয়ংক্রিয় সাপ্তাহিক ব্যাকআপ)"
            )

            preferences.setLastAutoBackupTime(record.createdAt)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

object FridayBackupScheduler {

    private const val WORK_NAME = "friday_auto_backup_periodic_work"

    fun calculateInitialDelayToFriday(targetHour: Int = 22, targetMinute: Int = 0): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Advance to the upcoming Friday
        while (target.get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY || target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        return target.timeInMillis - now.timeInMillis
    }

    fun schedule(context: Context) {
        val initialDelayMs = calculateInitialDelayToFriday(targetHour = 22, targetMinute = 0)
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<FridayAutoBackupWorker>(
            7, TimeUnit.DAYS,
            1, TimeUnit.HOURS // 1 hour flex interval
        )
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
