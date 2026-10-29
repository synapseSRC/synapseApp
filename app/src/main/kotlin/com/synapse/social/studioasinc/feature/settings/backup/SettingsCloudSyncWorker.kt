package com.synapse.social.studioasinc.feature.settings.backup

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

/**
 * Keeps the cloud settings snapshot aligned independently of the Backup & Restore screen.
 * Conflict outcomes are intentionally left untouched: the screen surfaces them when opened.
 */
class SettingsCloudSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BackupEntryPoint {
        fun backupManager(): SettingsBackupManager
    }

    override suspend fun doWork(): Result {
        val manager = EntryPointAccessors.fromApplication(
            applicationContext,
            BackupEntryPoint::class.java
        ).backupManager()

        if (!manager.isCloudConfigured() || !manager.isNetworkAvailable()) return Result.success()
        val userId = manager.currentUserId() ?: return Result.success()
        if (!manager.hasCloudBaseline(userId)) return Result.success()

        val outcome = manager.syncCloudSettings()
        if (outcome.isSuccess) return Result.success()

        return when (outcome.exceptionOrNull()) {
            is OfflineSettingsBackupException,
            is CloudSignInRequiredException,
            is CloudNotConfiguredException,
            is SettingsBackupAccountChangedException -> Result.success()
            else -> if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()
        }
    }

    companion object {
        private const val TAG = "SettingsCloudSyncWorker"
        private const val PERIODIC_WORK_NAME = "settings_cloud_sync_periodic"
        private const val IMMEDIATE_WORK_NAME = "settings_cloud_sync_now"
        private const val SYNC_INTERVAL_HOURS = 6L
        private const val MAX_ATTEMPTS = 3

        fun ensurePeriodicSync(context: Context) {
            val request = PeriodicWorkRequestBuilder<SettingsCloudSyncWorker>(
                SYNC_INTERVAL_HOURS,
                TimeUnit.HOURS
            )
                .setConstraints(networkConstraints())
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.i(TAG, "Scheduled periodic settings sync every $SYNC_INTERVAL_HOURS hours")
        }

        fun requestImmediateSync(context: Context) {
            val request = OneTimeWorkRequestBuilder<SettingsCloudSyncWorker>()
                .setConstraints(networkConstraints())
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        private fun networkConstraints(): Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}
