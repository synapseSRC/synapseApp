package com.synapse.social.studioasinc.data.local.database

import android.content.Context
import android.content.SharedPreferences
import com.synapse.social.studioasinc.shared.domain.repository.StorageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageMigration @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storageRepository: StorageRepository
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun migrateIfNeeded() = withContext(Dispatchers.IO) {
        val currentVersion = prefs.getInt(MIGRATION_VERSION_KEY, 0)

        if (currentVersion < CURRENT_VERSION) {
            performMigration(currentVersion)
            prefs.edit().putInt(MIGRATION_VERSION_KEY, CURRENT_VERSION).apply()
        }
    }

    private suspend fun performMigration(fromVersion: Int) {

        when (fromVersion) {
            0 -> {

                storageRepository.ensureDefault()
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "migration_prefs"
        private const val MIGRATION_VERSION_KEY = "storage_migration_version"
        private const val CURRENT_VERSION = 1
    }
}
