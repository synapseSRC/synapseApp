package com.synapse.social.studioasinc.feature.settings.backup

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Application-level trigger for cloud settings sync. Local preference edits schedule a debounced
 * one-time sync; periodic sync covers devices that only receive remote changes.
 */
@Singleton
class SettingsCloudSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupManager: SettingsBackupManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var started = false

    fun start() {
        if (started) return
        started = true

        SettingsCloudSyncWorker.ensurePeriodicSync(context)
        scope.launch {
            val dataStore = com.synapse.social.studioasinc.data.local.database.SettingsDataStore.getInstance(context)
            dataStore.observePreferencesForBackup()
                .drop(1)
                .debounce(SYNC_DEBOUNCE_MILLIS)
                .collect { requestSyncIfEligible() }
        }
    }

    fun requestSyncIfEligible() {
        val userId = backupManager.currentUserId() ?: return
        if (!backupManager.hasCloudBaseline(userId)) return
        SettingsCloudSyncWorker.requestImmediateSync(context)
    }

    companion object {
        private const val SYNC_DEBOUNCE_MILLIS = 4_000L
    }
}
