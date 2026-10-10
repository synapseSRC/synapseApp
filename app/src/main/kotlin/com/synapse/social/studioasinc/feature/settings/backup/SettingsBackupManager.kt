package com.synapse.social.studioasinc.feature.settings.backup

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.AtomicFile
import androidx.datastore.preferences.core.Preferences
import com.synapse.social.studioasinc.data.local.database.SettingsDataStore
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Serializable
data class SettingsBackup(
    val version: Int,
    val timestamp: Long,
    val settings: Map<String, SettingsBackupValue>
)

@Serializable
data class SettingsBackupValue(
    val type: String,
    val value: JsonElement
)

data class SettingsBackupMetadata(
    val backup: SettingsBackup,
    val sizeBytes: Long
)

data class SettingsCloudBackup(
    val userId: String,
    val backup: SettingsBackup,
    val updatedAt: String
)

data class SettingsBackupConflict(
    val userId: String,
    val localBackup: SettingsBackup,
    val cloudBackup: SettingsBackup?,
    val cloudRevision: String?
)

sealed interface CloudSettingsSyncOutcome {
    data object NoCloudBackup : CloudSettingsSyncOutcome
    data class Synchronized(val backup: SettingsBackup) : CloudSettingsSyncOutcome
    data class Conflict(val details: SettingsBackupConflict) : CloudSettingsSyncOutcome
}

class InvalidSettingsBackupException(cause: Throwable? = null) : IOException("The selected backup is invalid or corrupt", cause)
class UnsupportedSettingsBackupVersionException : IOException("This settings backup version is not supported")
class OfflineSettingsBackupException : IOException("No network connection is available")
class CloudSignInRequiredException : IOException("Sign in is required for cloud backup")
class CloudNotConfiguredException : IOException("Cloud backup is not configured")
class SettingsBackupAccountChangedException : IOException("The signed-in account changed during the backup operation")
class SettingsBackupLocalChangedException : IOException("Local preferences changed during cloud sync")
class SettingsBackupCloudChangedException : IOException("The cloud backup changed after it was previewed")
class SettingsBackupStorageException(cause: Throwable? = null) : IOException("The backup could not be read or saved", cause)

@Singleton
class SettingsBackupManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val settingsDataStore by lazy { SettingsDataStore.getInstance(context) }
    private val syncMetadata by lazy {
        context.getSharedPreferences("settings_backup_sync_metadata", Context.MODE_PRIVATE)
    }

    fun isCloudConfigured(): Boolean = SupabaseClient.isConfigured()

    fun currentUserId(): String? {
        if (!isCloudConfigured()) return null
        return try {
            SupabaseClient.client.auth.currentUserOrNull()?.id
        } catch (_: Exception) {
            null
        }
    }

    fun isNetworkAvailable(): Boolean {
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = connectivity.activeNetwork ?: return false
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun observeSettings(): kotlinx.coroutines.flow.Flow<Preferences> =
        settingsDataStore.observePreferencesForBackup()

    fun lastSuccessfulSyncAt(userId: String): Long? {
        val value = syncMetadata.getLong(lastSyncKey(userId), 0L)
        return value.takeIf { it > 0L }
    }

    suspend fun createLocalBackup(): Result<SettingsBackupMetadata> = capture {
        withContext(Dispatchers.IO) {
            val backup = captureCurrentBackup()
            saveLocalBackup(backup)
            SettingsBackupMetadata(backup, localBackupFile().length())
        }
    }

    suspend fun loadLocalBackup(): Result<SettingsBackupMetadata?> = capture {
        withContext(Dispatchers.IO) {
            val file = localBackupFile()
            if (!file.exists()) return@withContext null
            val backup = SettingsBackupCodec.decode(readLimited(file.inputStream()))
            SettingsBackupMetadata(backup, file.length())
        }
    }

    suspend fun exportCurrentBackup(uri: Uri): Result<SettingsBackupMetadata> = capture {
        withContext(Dispatchers.IO) {
            val backup = captureCurrentBackup()
            val contents = SettingsBackupCodec.encode(backup).toByteArray(Charsets.UTF_8)
            saveLocalBackup(backup)
            val output = context.contentResolver.openOutputStream(uri, "w")
                ?: throw SettingsBackupStorageException()
            output.use { it.write(contents) }
            SettingsBackupMetadata(backup, contents.size.toLong())
        }
    }

    suspend fun importBackup(uri: Uri): Result<SettingsBackupMetadata> = capture {
        withContext(Dispatchers.IO) {
            val input = context.contentResolver.openInputStream(uri)
                ?: throw SettingsBackupStorageException()
            val backup = SettingsBackupCodec.decode(input.use(::readLimited))
            saveLocalBackup(backup)
            SettingsBackupMetadata(backup, SettingsBackupCodec.encode(backup).toByteArray(Charsets.UTF_8).size.toLong())
        }
    }

    suspend fun restoreLocalBackup(backup: SettingsBackup): Result<Unit> = capture {
        withContext(Dispatchers.IO) {
            val mutation = SettingsBackupCodec.prepareRestore(backup)
            settingsDataStore.updatePreferencesForBackup(mutation)
        }
    }

    suspend fun loadCloudBackup(): Result<SettingsCloudBackup?> = capture {
        withContext(Dispatchers.IO) {
            requireOnline()
            val userId = requireCurrentUserId()
            val record = fetchCloudRecord(userId)
            ensureSameAccount(userId)
            record?.let { SettingsCloudBackup(it.userId, it.backup, it.updatedAt) }
        }
    }

    suspend fun uploadCurrentSettingsToCloud(): Result<SettingsBackup> = capture {
        withContext(Dispatchers.IO) {
            requireOnline()
            val userId = requireCurrentUserId()
            val backup = captureCurrentBackup()
            saveLocalBackup(backup)
            ensureSameAccount(userId)
            writeCloudRecord(userId, backup)
            ensureSameAccount(userId)
            saveSyncBaseline(userId, backup)
            markSuccessfulSync(userId)
            backup
        }
    }

    suspend fun restoreCloudBackup(userId: String, backup: SettingsBackup, expectedRevision: String): Result<Unit> = capture {
        withContext(Dispatchers.IO) {
            requireOnline()
            ensureSameAccount(userId)
            val localBeforeRestore = captureCurrentBackup()
            val currentCloud = fetchCloudRecord(userId)
            if (currentCloud?.updatedAt != expectedRevision || currentCloud.backup.settings != backup.settings) {
                throw SettingsBackupCloudChangedException()
            }
            ensureSameAccount(userId)
            applyBackupIfLocalUnchanged(localBeforeRestore, backup)
            ensureSameAccount(userId)
            saveLocalBackup(backup)
            saveSyncBaseline(userId, backup)
            markSuccessfulSync(userId)
        }
    }

    suspend fun syncCloudSettings(): Result<CloudSettingsSyncOutcome> = capture {
        withContext(Dispatchers.IO) {
            requireOnline()
            val userId = requireCurrentUserId()
            val localBackup = captureCurrentBackup()
            val cloudRecord = fetchCloudRecord(userId)
            ensureSameAccount(userId)

            if (cloudRecord == null) return@withContext CloudSettingsSyncOutcome.NoCloudBackup

            val baseline = loadSyncBaseline(userId)
            val cloudBackup = cloudRecord.backup
            if (baseline == null) {
                if (localBackup.settings == cloudBackup.settings) {
                    saveSyncBaseline(userId, cloudBackup)
                    markSuccessfulSync(userId)
                    return@withContext CloudSettingsSyncOutcome.Synchronized(cloudBackup)
                }
                return@withContext CloudSettingsSyncOutcome.Conflict(
                    SettingsBackupConflict(userId, localBackup, cloudBackup, cloudRecord.updatedAt)
                )
            }

            val localChanged = localBackup.settings != baseline.settings
            val cloudChanged = cloudBackup.settings != baseline.settings
            if (localBackup.settings == cloudBackup.settings) {
                saveSyncBaseline(userId, cloudBackup)
                markSuccessfulSync(userId)
                return@withContext CloudSettingsSyncOutcome.Synchronized(cloudBackup)
            }

            when {
                localChanged && !cloudChanged -> {
                    val updated = updateCloudRecordIfRevisionMatches(userId, cloudRecord.updatedAt, localBackup)
                    if (!updated) {
                        val latest = fetchCloudRecord(userId)
                        val currentLocal = captureCurrentBackup()
                        CloudSettingsSyncOutcome.Conflict(
                            SettingsBackupConflict(userId, currentLocal, latest?.backup, latest?.updatedAt)
                        )
                    } else {
                        ensureSameAccount(userId)
                        saveSyncBaseline(userId, localBackup)
                        markSuccessfulSync(userId)
                        CloudSettingsSyncOutcome.Synchronized(localBackup)
                    }
                }
                !localChanged && cloudChanged -> {
                    try {
                        applyBackupIfLocalUnchanged(localBackup, cloudBackup)
                    } catch (_: SettingsBackupLocalChangedException) {
                        val latest = fetchCloudRecord(userId)
                        val currentLocal = captureCurrentBackup()
                        return@withContext CloudSettingsSyncOutcome.Conflict(
                            SettingsBackupConflict(userId, currentLocal, latest?.backup, latest?.updatedAt)
                        )
                    }
                    val latest = fetchCloudRecord(userId)
                    ensureSameAccount(userId)
                    if (latest == null || latest.updatedAt != cloudRecord.updatedAt || latest.backup.settings != cloudBackup.settings) {
                        val currentLocal = captureCurrentBackup()
                        CloudSettingsSyncOutcome.Conflict(
                            SettingsBackupConflict(userId, currentLocal, latest?.backup, latest?.updatedAt)
                        )
                    } else {
                        saveLocalBackup(cloudBackup)
                        saveSyncBaseline(userId, cloudBackup)
                        markSuccessfulSync(userId)
                        CloudSettingsSyncOutcome.Synchronized(cloudBackup)
                    }
                }
                else -> CloudSettingsSyncOutcome.Conflict(
                    SettingsBackupConflict(userId, localBackup, cloudBackup, cloudRecord.updatedAt)
                )
            }
        }
    }

    suspend fun resolveCloudConflict(
        conflict: SettingsBackupConflict,
        useCloudCopy: Boolean
    ): Result<CloudSettingsSyncOutcome> = capture {
        withContext(Dispatchers.IO) {
            requireOnline()
            ensureSameAccount(conflict.userId)
            val latestCloud = fetchCloudRecord(conflict.userId)
            ensureSameAccount(conflict.userId)
            if (latestCloud?.updatedAt != conflict.cloudRevision) {
                val currentLocal = captureCurrentBackup()
                return@withContext CloudSettingsSyncOutcome.Conflict(
                    SettingsBackupConflict(
                        conflict.userId,
                        currentLocal,
                        latestCloud?.backup,
                        latestCloud?.updatedAt
                    )
                )
            }

            val selected = if (useCloudCopy) {
                val cloud = conflict.cloudBackup ?: throw InvalidSettingsBackupException()
                try {
                    applyBackupIfLocalUnchanged(conflict.localBackup, cloud)
                } catch (_: SettingsBackupLocalChangedException) {
                    val latest = fetchCloudRecord(conflict.userId)
                    val currentLocal = captureCurrentBackup()
                    return@withContext CloudSettingsSyncOutcome.Conflict(
                        SettingsBackupConflict(conflict.userId, currentLocal, latest?.backup, latest?.updatedAt)
                    )
                }
                val latest = fetchCloudRecord(conflict.userId)
                ensureSameAccount(conflict.userId)
                if (latest == null || latest.updatedAt != conflict.cloudRevision || latest.backup.settings != cloud.settings) {
                    val currentLocal = captureCurrentBackup()
                    return@withContext CloudSettingsSyncOutcome.Conflict(
                        SettingsBackupConflict(conflict.userId, currentLocal, latest?.backup, latest?.updatedAt)
                    )
                }
                saveLocalBackup(cloud)
                cloud
            } else {
                val local = captureCurrentBackup()
                if (local.settings != conflict.localBackup.settings) {
                    val latest = fetchCloudRecord(conflict.userId)
                    return@withContext CloudSettingsSyncOutcome.Conflict(
                        SettingsBackupConflict(conflict.userId, local, latest?.backup, latest?.updatedAt)
                    )
                }
                val updated = if (latestCloud == null) {
                    insertCloudRecordIfAbsent(conflict.userId, conflict.localBackup)
                } else {
                    updateCloudRecordIfRevisionMatches(conflict.userId, latestCloud.updatedAt, conflict.localBackup)
                }
                if (!updated) {
                    val latest = fetchCloudRecord(conflict.userId)
                    val currentLocal = captureCurrentBackup()
                    return@withContext CloudSettingsSyncOutcome.Conflict(
                        SettingsBackupConflict(conflict.userId, currentLocal, latest?.backup, latest?.updatedAt)
                    )
                }
                saveLocalBackup(conflict.localBackup)
                conflict.localBackup
            }
            ensureSameAccount(conflict.userId)
            saveSyncBaseline(conflict.userId, selected)
            markSuccessfulSync(conflict.userId)
            CloudSettingsSyncOutcome.Synchronized(selected)
        }
    }

    private suspend fun captureCurrentBackup(): SettingsBackup =
        SettingsBackupCodec.fromPreferences(settingsDataStore.readPreferencesForBackup(), System.currentTimeMillis())

    private suspend fun applyBackupIfLocalUnchanged(expectedLocal: SettingsBackup, incoming: SettingsBackup) {
        val mutation = SettingsBackupCodec.prepareRestore(incoming)
        settingsDataStore.updatePreferencesForBackup { preferences ->
            val current = SettingsBackupCodec.fromPreferences(preferences, expectedLocal.timestamp)
            if (current.settings != expectedLocal.settings) throw SettingsBackupLocalChangedException()
            mutation(preferences)
        }
    }

    private suspend fun updateCloudRecordIfRevisionMatches(
        userId: String,
        expectedRevision: String,
        backup: SettingsBackup
    ): Boolean {
        ensureSameAccount(userId)
        val updated = SupabaseClient.client.from(TABLE_SETTINGS_BACKUPS).update(
            CloudBackupWrite(userId = userId, backup = SettingsBackupCodec.validate(backup))
        ) {
            filter {
                eq("user_id", userId)
                eq("updated_at", expectedRevision)
            }
        }.decodeSingleOrNull<CloudBackupRecord>()
        ensureSameAccount(userId)
        return updated != null
    }

    private suspend fun insertCloudRecordIfAbsent(userId: String, backup: SettingsBackup): Boolean {
        ensureSameAccount(userId)
        try {
            SupabaseClient.client.from(TABLE_SETTINGS_BACKUPS).insert(
                CloudBackupWrite(userId = userId, backup = SettingsBackupCodec.validate(backup))
            )
            ensureSameAccount(userId)
            return true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            val latest = fetchCloudRecord(userId)
            if (latest != null) return false
            throw error
        }
    }

    private suspend fun fetchCloudRecord(userId: String): CloudBackupRecord? {
        ensureSameAccount(userId)
        return SupabaseClient.client.from(TABLE_SETTINGS_BACKUPS).select {
            filter { eq("user_id", userId) }
        }.decodeSingleOrNull<CloudBackupRecord>()?.let { record ->
            val safeBackup = SettingsBackupCodec.validate(record.backup)
            record.copy(backup = safeBackup)
        }
    }

    private suspend fun writeCloudRecord(userId: String, backup: SettingsBackup) {
        ensureSameAccount(userId)
        val validated = SettingsBackupCodec.validate(backup)
        SupabaseClient.client.from(TABLE_SETTINGS_BACKUPS).upsert(
            CloudBackupWrite(userId = userId, backup = validated)
        ) {
            onConflict = "user_id"
        }
    }

    private fun requireCurrentUserId(): String {
        if (!isCloudConfigured()) throw CloudNotConfiguredException()
        return SupabaseClient.client.auth.currentUserOrNull()?.id ?: throw CloudSignInRequiredException()
    }

    private fun ensureSameAccount(userId: String) {
        if (currentUserId() != userId) throw SettingsBackupAccountChangedException()
    }

    private fun requireOnline() {
        if (!isNetworkAvailable()) throw OfflineSettingsBackupException()
    }

    private fun localBackupFile(): File = File(File(context.filesDir, BACKUP_DIRECTORY), LOCAL_BACKUP_FILE)

    private fun syncBaselineFile(userId: String): File =
        File(File(context.filesDir, BACKUP_DIRECTORY), "sync_" + stableUserHash(userId) + ".json")

    private fun saveLocalBackup(backup: SettingsBackup) {
        writeAtomic(localBackupFile(), SettingsBackupCodec.encode(backup))
    }

    private fun saveSyncBaseline(userId: String, backup: SettingsBackup) {
        writeAtomic(syncBaselineFile(userId), SettingsBackupCodec.encode(backup))
    }

    private fun loadSyncBaseline(userId: String): SettingsBackup? {
        val file = syncBaselineFile(userId)
        if (!file.exists()) return null
        return SettingsBackupCodec.decode(readLimited(file.inputStream()))
    }

    private fun markSuccessfulSync(userId: String) {
        syncMetadata.edit().putLong(lastSyncKey(userId), System.currentTimeMillis()).apply()
    }

    private fun lastSyncKey(userId: String): String = "last_sync_" + stableUserHash(userId)

    private fun stableUserHash(userId: String): String = MessageDigest.getInstance("SHA-256")
        .digest(userId.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private fun writeAtomic(file: File, contents: String) {
        val parent = file.parentFile
        if (parent != null && !parent.exists() && !parent.mkdirs()) throw SettingsBackupStorageException()
        val atomicFile = AtomicFile(file)
        val stream = atomicFile.startWrite()
        try {
            stream.write(contents.toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(stream)
        } catch (error: Exception) {
            atomicFile.failWrite(stream)
            throw SettingsBackupStorageException(error)
        }
    }

    private fun readLimited(input: InputStream): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > MAX_BACKUP_BYTES) throw InvalidSettingsBackupException()
            output.write(buffer, 0, read)
        }
        return String(output.toByteArray(), Charsets.UTF_8)
    }

    private suspend fun <T> capture(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        Result.failure(error)
    }

    companion object {
        const val TABLE_SETTINGS_BACKUPS = "settings_backups"
        private const val BACKUP_DIRECTORY = "settings_backups"
        private const val LOCAL_BACKUP_FILE = "latest.json"
        private const val MAX_BACKUP_BYTES = 1_048_576
        private const val BUFFER_SIZE = 8_192
    }
}

@Serializable
private data class CloudBackupRecord(
    @SerialName("user_id") val userId: String,
    @SerialName("backup_json") val backup: SettingsBackup,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
private data class CloudBackupWrite(
    @SerialName("user_id") val userId: String,
    @SerialName("backup_json") val backup: SettingsBackup
)
