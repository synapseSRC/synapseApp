package com.synapse.social.studioasinc.feature.settings.backup

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.AtomicFile
import com.synapse.social.studioasinc.data.local.database.SettingsDataStore
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.time.Instant
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
    private val syncMutex = Mutex()

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

    fun lastSuccessfulSyncAt(userId: String): Long? {
        val value = syncMetadata.getLong(lastSyncKey(userId), 0L)
        return value.takeIf { it > 0L }
    }

    fun hasCloudBaseline(userId: String): Boolean = syncBaselineFile(userId).exists()

    suspend fun currentBackup(): Result<SettingsBackup> = capture {
        withContext(Dispatchers.IO) { captureCurrentBackup() }
    }

    suspend fun hasChangesSinceBaseline(userId: String): Boolean = capture {
        withContext(Dispatchers.IO) {
            val baseline = loadSyncBaseline(userId) ?: return@withContext false
            captureCurrentBackup().settings != baseline.settings
        }
    }.getOrDefault(false)

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
            SettingsBackupMetadata(backup, SettingsBackupCodec.encode(backup).toByteArray(Charsets.UTF_8).size.toLong())
        }
    }

    suspend fun restoreLocalBackup(backup: SettingsBackup): Result<Unit> = capture {
        withContext(Dispatchers.IO) {
            val mutation = SettingsBackupCodec.prepareRestore(backup)
            settingsDataStore.updatePreferencesForBackup(mutation)
            saveLocalBackup(backup)
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
            syncMutex.withLock {
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
    }

    suspend fun restoreCloudBackup(userId: String, backup: SettingsBackup, expectedRevision: String): Result<Unit> = capture {
        withContext(Dispatchers.IO) {
            syncMutex.withLock {
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
    }

    suspend fun syncCloudSettings(): Result<CloudSettingsSyncOutcome> = capture {
        withContext(Dispatchers.IO) {
            syncMutex.withLock {
                requireOnline()
                val userId = requireCurrentUserId()
                val localBackup = captureCurrentBackup()
                val cloudRecord = fetchCloudRecord(userId)
                ensureSameAccount(userId)

                if (cloudRecord == null) return@withLock CloudSettingsSyncOutcome.NoCloudBackup

                val baseline = loadSyncBaseline(userId)
                val cloudBackup = cloudRecord.backup
                if (baseline == null) {
                    if (localBackup.settings == cloudBackup.settings) {
                        saveLocalBackup(cloudBackup)
                        saveSyncBaseline(userId, cloudBackup)
                        markSuccessfulSync(userId)
                        return@withLock CloudSettingsSyncOutcome.Synchronized(cloudBackup)
                    }
                    return@withLock CloudSettingsSyncOutcome.Conflict(
                        SettingsBackupConflict(userId, localBackup, cloudBackup, cloudRecord.updatedAt)
                    )
                }

                val localChanged = localBackup.settings != baseline.settings
                val cloudChanged = cloudBackup.settings != baseline.settings
                if (localBackup.settings == cloudBackup.settings) {
                    saveLocalBackup(cloudBackup)
                    saveSyncBaseline(userId, cloudBackup)
                    markSuccessfulSync(userId)
                    return@withLock CloudSettingsSyncOutcome.Synchronized(cloudBackup)
                }

                when {
                    localChanged && !cloudChanged -> {
                        val updated = updateCloudRecordIfRevisionMatches(userId, cloudRecord.updatedAt, localBackup)
                        if (!updated) {
                            val latest = fetchCloudRecord(userId)
                            CloudSettingsSyncOutcome.Conflict(
                                SettingsBackupConflict(userId, captureCurrentBackup(), latest?.backup, latest?.updatedAt)
                            )
                        } else {
                            ensureSameAccount(userId)
                            saveLocalBackup(localBackup)
                            saveSyncBaseline(userId, localBackup)
                            markSuccessfulSync(userId)
                            CloudSettingsSyncOutcome.Synchronized(localBackup)
                        }
                    }
                    !localChanged && cloudChanged -> {
                        val confirmedCloud = fetchCloudRecord(userId)
                        ensureSameAccount(userId)
                        if (confirmedCloud == null ||
                            confirmedCloud.updatedAt != cloudRecord.updatedAt ||
                            confirmedCloud.backup.settings != cloudBackup.settings
                        ) {
                            return@withLock CloudSettingsSyncOutcome.Conflict(
                                SettingsBackupConflict(userId, localBackup, confirmedCloud?.backup, confirmedCloud?.updatedAt)
                            )
                        }
                        try {
                            applyBackupIfLocalUnchanged(localBackup, confirmedCloud.backup)
                        } catch (_: SettingsBackupLocalChangedException) {
                            val latest = fetchCloudRecord(userId)
                            return@withLock CloudSettingsSyncOutcome.Conflict(
                                SettingsBackupConflict(userId, captureCurrentBackup(), latest?.backup, latest?.updatedAt)
                            )
                        }
                        val latest = fetchCloudRecord(userId)
                        ensureSameAccount(userId)
                        if (latest == null ||
                            latest.updatedAt != confirmedCloud.updatedAt ||
                            latest.backup.settings != confirmedCloud.backup.settings
                        ) {
                            val originalLocal = revertLocalIfUnchanged(confirmedCloud.backup, localBackup)
                            CloudSettingsSyncOutcome.Conflict(
                                SettingsBackupConflict(userId, originalLocal, latest?.backup, latest?.updatedAt)
                            )
                        } else {
                            saveLocalBackup(confirmedCloud.backup)
                            saveSyncBaseline(userId, confirmedCloud.backup)
                            markSuccessfulSync(userId)
                            CloudSettingsSyncOutcome.Synchronized(confirmedCloud.backup)
                        }
                    }
                    else -> CloudSettingsSyncOutcome.Conflict(
                        SettingsBackupConflict(userId, localBackup, cloudBackup, cloudRecord.updatedAt)
                    )
                }
            }
        }
    }

    suspend fun resolveCloudConflict(
        conflict: SettingsBackupConflict,
        useCloudCopy: Boolean
    ): Result<CloudSettingsSyncOutcome> = capture {
        withContext(Dispatchers.IO) {
            syncMutex.withLock {
                requireOnline()
                ensureSameAccount(conflict.userId)
                val latestCloud = fetchCloudRecord(conflict.userId)
                ensureSameAccount(conflict.userId)
                if (latestCloud?.updatedAt != conflict.cloudRevision) {
                    val currentLocal = captureCurrentBackup()
                    return@withLock CloudSettingsSyncOutcome.Conflict(
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
                        return@withLock CloudSettingsSyncOutcome.Conflict(
                            SettingsBackupConflict(conflict.userId, captureCurrentBackup(), latest?.backup, latest?.updatedAt)
                        )
                    }
                    val latest = fetchCloudRecord(conflict.userId)
                    ensureSameAccount(conflict.userId)
                    if (latest == null || latest.updatedAt != conflict.cloudRevision || latest.backup.settings != cloud.settings) {
                        val originalLocal = revertLocalIfUnchanged(cloud, conflict.localBackup)
                        return@withLock CloudSettingsSyncOutcome.Conflict(
                            SettingsBackupConflict(conflict.userId, originalLocal, latest?.backup, latest?.updatedAt)
                        )
                    }
                    saveLocalBackup(cloud)
                    cloud
                } else {
                    val local = captureCurrentBackup()
                    if (local.settings != conflict.localBackup.settings) {
                        val latest = fetchCloudRecord(conflict.userId)
                        return@withLock CloudSettingsSyncOutcome.Conflict(
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
                        return@withLock CloudSettingsSyncOutcome.Conflict(
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

    /**
     * Restores the pre-sync snapshot when the cloud changed after it was already applied locally.
     * Returns the snapshot that now represents this device, or the current local state if a newer
     * edit landed in the meantime.
     */
    private suspend fun revertLocalIfUnchanged(applied: SettingsBackup, original: SettingsBackup): SettingsBackup =
        try {
            applyBackupIfLocalUnchanged(applied, original)
            original
        } catch (_: SettingsBackupLocalChangedException) {
            captureCurrentBackup()
        }

    private suspend fun updateCloudRecordIfRevisionMatches(
        userId: String,
        expectedRevision: String,
        backup: SettingsBackup
    ): Boolean {
        ensureSameAccount(userId)
        val result = SupabaseClient.client.from(TABLE_SETTINGS_BACKUPS).update(
            CloudBackupWrite(userId = userId, backup = SettingsBackupCodec.validate(backup))
        ) {
            filter {
                eq("user_id", userId)
                eq("updated_at", expectedRevision)
            }
            select()
        }
        ensureSameAccount(userId)
        // A conditional update that matches no rows returns an empty representation.
        if (result.data.isBlank()) return false
        return result.decodeList<CloudBackupRecord>().isNotEmpty()
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
    @SerialName("backup_json") val backup: SettingsBackup,
    @SerialName("updated_at") val updatedAt: String = Instant.now().toString()
)
