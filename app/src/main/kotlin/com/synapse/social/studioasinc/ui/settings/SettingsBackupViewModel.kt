package com.synapse.social.studioasinc.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.settings.backup.CloudSettingsSyncOutcome
import com.synapse.social.studioasinc.feature.settings.backup.CloudNotConfiguredException
import com.synapse.social.studioasinc.feature.settings.backup.CloudSignInRequiredException
import com.synapse.social.studioasinc.feature.settings.backup.InvalidSettingsBackupException
import com.synapse.social.studioasinc.feature.settings.backup.OfflineSettingsBackupException
import com.synapse.social.studioasinc.feature.settings.backup.SettingsBackup
import com.synapse.social.studioasinc.feature.settings.backup.SettingsBackupAccountChangedException
import com.synapse.social.studioasinc.feature.settings.backup.SettingsBackupConflict
import com.synapse.social.studioasinc.feature.settings.backup.SettingsBackupCloudChangedException
import com.synapse.social.studioasinc.feature.settings.backup.SettingsBackupManager
import com.synapse.social.studioasinc.feature.settings.backup.SettingsBackupMetadata
import com.synapse.social.studioasinc.feature.settings.backup.SettingsCloudBackup
import com.synapse.social.studioasinc.feature.settings.backup.UnsupportedSettingsBackupVersionException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


enum class BackupDestinationMode { LOCAL, CLOUD }
enum class CloudBackupUiStatus { CHECKING, NOT_CONFIGURED, SIGN_IN_REQUIRED, OFFLINE, NO_BACKUP, READY, SYNCING, CONFLICT, ERROR }
enum class RestoreBackupSource { LOCAL, FILE, CLOUD }

data class PendingSettingsRestore(
    val backup: SettingsBackup,
    val source: RestoreBackupSource,
    val userId: String? = null,
    val cloudRevision: String? = null
)

data class SettingsBackupUiState(
    val mode: BackupDestinationMode = BackupDestinationMode.LOCAL,
    val isBusy: Boolean = true,
    val localBackup: SettingsBackupMetadata? = null,
    val localErrorResource: Int? = null,
    val isCloudConfigured: Boolean = false,
    val isSignedIn: Boolean = false,
    val accountId: String? = null,
    val isOnline: Boolean = false,
    val cloudStatus: CloudBackupUiStatus = CloudBackupUiStatus.CHECKING,
    val cloudBackupExists: Boolean? = null,
    val cloudBackupTimestamp: Long? = null,
    val cloudBackupVersion: Int? = null,
    val lastSuccessfulSyncAt: Long? = null,
    val cloudErrorResource: Int? = null,
    val pendingRestore: PendingSettingsRestore? = null,
    val conflict: SettingsBackupConflict? = null
) {
    val canUseCloud: Boolean
        get() = isCloudConfigured && isSignedIn && isOnline && !isBusy
}

@HiltViewModel
class SettingsBackupViewModel @Inject constructor(
    application: Application,
    private val backupManager: SettingsBackupManager
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(SettingsBackupUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<Int>(extraBufferCapacity = 8)
    val events: SharedFlow<Int> = _events.asSharedFlow()
    private var observationJob: Job? = null
    private var refreshJob: Job? = null

    init {
        refresh()
        observationJob = viewModelScope.launch {
            backupManager.observeSettings().drop(1).debounce(AUTO_SYNC_DEBOUNCE_MILLIS).collect {
                val current = _uiState.value
                if (current.mode == BackupDestinationMode.CLOUD && current.canUseCloud) {
                    syncCloud(showSuccess = false)
                }
            }
        }
    }

    fun onResume() = refresh()

    fun selectMode(mode: BackupDestinationMode) {
        _uiState.update { it.copy(mode = mode, cloudErrorResource = null) }
        if (mode == BackupDestinationMode.CLOUD) refresh()
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, cloudStatus = CloudBackupUiStatus.CHECKING, localErrorResource = null) }
            refreshLocalMetadata()

            val configured = backupManager.isCloudConfigured()
            val userId = backupManager.currentUserId()
            val online = backupManager.isNetworkAvailable()
            val status = when {
                !configured -> CloudBackupUiStatus.NOT_CONFIGURED
                userId == null -> CloudBackupUiStatus.SIGN_IN_REQUIRED
                !online -> CloudBackupUiStatus.OFFLINE
                else -> CloudBackupUiStatus.CHECKING
            }
            _uiState.update { current ->
                val accountChanged = current.accountId != userId
                current.copy(
                    isBusy = false,
                    isCloudConfigured = configured,
                    isSignedIn = userId != null,
                    accountId = userId,
                    isOnline = online,
                    cloudStatus = status,
                    lastSuccessfulSyncAt = userId?.let(backupManager::lastSuccessfulSyncAt),
                    cloudBackupExists = if (accountChanged || status != CloudBackupUiStatus.CHECKING) null else current.cloudBackupExists,
                    cloudBackupTimestamp = if (accountChanged) null else current.cloudBackupTimestamp,
                    cloudBackupVersion = if (accountChanged) null else current.cloudBackupVersion,
                    pendingRestore = if (accountChanged && current.pendingRestore?.source == RestoreBackupSource.CLOUD) null else current.pendingRestore,
                    conflict = if (accountChanged) null else current.conflict,
                    cloudErrorResource = null
                )
            }
            if (_uiState.value.mode == BackupDestinationMode.CLOUD && status == CloudBackupUiStatus.CHECKING) {
                syncCloud(showSuccess = false)
            }
        }
    }

    fun createLocalBackup() = viewModelScope.launch {
        if (_uiState.value.isBusy) return@launch
        _uiState.update { it.copy(isBusy = true, localErrorResource = null) }
        backupManager.createLocalBackup().fold(
            onSuccess = { metadata ->
                _uiState.update { it.copy(isBusy = false, localBackup = metadata, localErrorResource = null) }
                emit(R.string.backup_local_created)
            },
            onFailure = { error -> fail(error, cloud = false) }
        )
    }

    fun exportCurrentBackup(uri: Uri) = viewModelScope.launch {
        if (_uiState.value.isBusy) return@launch
        _uiState.update { it.copy(isBusy = true, localErrorResource = null) }
        backupManager.exportCurrentBackup(uri).fold(
            onSuccess = { metadata ->
                _uiState.update { it.copy(isBusy = false, localBackup = metadata, localErrorResource = null) }
                emit(R.string.backup_export_success)
            },
            onFailure = { error -> fail(error, cloud = false) }
        )
    }

    fun importBackup(uri: Uri) = viewModelScope.launch {
        if (_uiState.value.isBusy) return@launch
        _uiState.update { it.copy(isBusy = true, localErrorResource = null) }
        backupManager.importBackup(uri).fold(
            onSuccess = { metadata ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        localBackup = metadata,
                        localErrorResource = null,
                        pendingRestore = PendingSettingsRestore(metadata.backup, RestoreBackupSource.FILE)
                    )
                }
                emit(R.string.backup_import_ready)
            },
            onFailure = { error -> fail(error, cloud = false) }
        )
    }

    fun requestLocalRestore() = viewModelScope.launch {
        if (_uiState.value.isBusy) return@launch
        _uiState.update { it.copy(isBusy = true, localErrorResource = null) }
        backupManager.loadLocalBackup().fold(
            onSuccess = { metadata ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        localBackup = metadata,
                        pendingRestore = metadata?.let { PendingSettingsRestore(it.backup, RestoreBackupSource.LOCAL) }
                    )
                }
                if (metadata == null) emit(R.string.backup_no_local_backup)
            },
            onFailure = { error -> fail(error, cloud = false) }
        )
    }

    fun requestCloudRestore() = viewModelScope.launch {
        if (!prepareCloudAction()) return@launch
        _uiState.update { it.copy(isBusy = true, cloudStatus = CloudBackupUiStatus.SYNCING, cloudErrorResource = null) }
        backupManager.loadCloudBackup().fold(
            onSuccess = { remote ->
                if (remote == null) {
                    _uiState.update { it.copy(isBusy = false, cloudStatus = CloudBackupUiStatus.NO_BACKUP, cloudBackupExists = false) }
                    emit(R.string.backup_no_cloud_backup)
                } else {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            cloudStatus = CloudBackupUiStatus.READY,
                            cloudBackupExists = true,
                            cloudBackupTimestamp = remote.backup.timestamp,
                            cloudBackupVersion = remote.backup.version,
                            pendingRestore = PendingSettingsRestore(
                                backup = remote.backup,
                                source = RestoreBackupSource.CLOUD,
                                userId = remote.userId,
                                cloudRevision = remote.updatedAt
                            )
                        )
                    }
                }
            },
            onFailure = { error -> fail(error, cloud = true) }
        )
    }

    fun confirmRestore() = viewModelScope.launch {
        val pending = _uiState.value.pendingRestore ?: return@launch
        if (_uiState.value.isBusy) return@launch
        val cloudRestore = if (pending.source == RestoreBackupSource.CLOUD) {
            val userId = pending.userId
            if (userId == null) {
                fail(SettingsBackupAccountChangedException(), cloud = true)
                return@launch
            }
            val cloudRevision = pending.cloudRevision
            if (cloudRevision == null) {
                fail(SettingsBackupCloudChangedException(), cloud = true)
                return@launch
            }
            userId to cloudRevision
        } else null
        _uiState.update { it.copy(isBusy = true, localErrorResource = null) }
        val result = if (cloudRestore == null) {
            backupManager.restoreLocalBackup(pending.backup)
        } else {
            backupManager.restoreCloudBackup(cloudRestore.first, pending.backup, cloudRestore.second)
        }
        result.fold(
            onSuccess = {
                _uiState.update { it.copy(isBusy = false, pendingRestore = null, localErrorResource = null) }
                if (pending.source == RestoreBackupSource.CLOUD) refresh() else refreshLocalMetadata()
                emit(R.string.backup_restore_success)
            },
            onFailure = { error -> fail(error, cloud = pending.source == RestoreBackupSource.CLOUD) }
        )
    }

    fun dismissRestorePreview() {
        _uiState.update { it.copy(pendingRestore = null) }
    }

    fun backupNowToCloud() = viewModelScope.launch {
        if (!prepareCloudAction()) return@launch
        _uiState.update { it.copy(isBusy = true, cloudStatus = CloudBackupUiStatus.SYNCING, cloudErrorResource = null) }
        backupManager.uploadCurrentSettingsToCloud().fold(
            onSuccess = { backup ->
                val userId = backupManager.currentUserId()
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        cloudStatus = CloudBackupUiStatus.READY,
                        cloudBackupExists = true,
                        cloudBackupTimestamp = backup.timestamp,
                        cloudBackupVersion = backup.version,
                        lastSuccessfulSyncAt = userId?.let(backupManager::lastSuccessfulSyncAt),
                        conflict = null,
                        cloudErrorResource = null
                    )
                }
                refreshLocalMetadata()
                emit(R.string.backup_cloud_created)
            },
            onFailure = { error -> fail(error, cloud = true) }
        )
    }

    fun syncNow() = viewModelScope.launch { syncCloud(showSuccess = true) }

    fun dismissConflict() {
        _uiState.update {
            it.copy(
                conflict = null,
                cloudStatus = if (it.cloudBackupExists == true) CloudBackupUiStatus.READY else CloudBackupUiStatus.NO_BACKUP
            )
        }
    }

    fun resolveConflict(useCloudCopy: Boolean) = viewModelScope.launch {
        val conflict = _uiState.value.conflict ?: return@launch
        if (_uiState.value.isBusy || (useCloudCopy && conflict.cloudBackup == null)) return@launch
        _uiState.update { it.copy(isBusy = true, cloudStatus = CloudBackupUiStatus.SYNCING) }
        backupManager.resolveCloudConflict(conflict, useCloudCopy).fold(
            onSuccess = { outcome -> applySyncOutcome(outcome, showSuccess = true) },
            onFailure = { error -> fail(error, cloud = true) }
        )
    }

    private suspend fun syncCloud(showSuccess: Boolean) {
        val current = _uiState.value
        if (current.isBusy || current.mode != BackupDestinationMode.CLOUD) return
        if (!prepareCloudAction()) return
        _uiState.update { it.copy(isBusy = true, cloudStatus = CloudBackupUiStatus.SYNCING, cloudErrorResource = null) }
        backupManager.syncCloudSettings().fold(
            onSuccess = { outcome -> applySyncOutcome(outcome, showSuccess) },
            onFailure = { error -> fail(error, cloud = true) }
        )
    }

    private suspend fun applySyncOutcome(outcome: CloudSettingsSyncOutcome, showSuccess: Boolean) {
        when (outcome) {
            CloudSettingsSyncOutcome.NoCloudBackup -> {
                _uiState.update {
                    it.copy(isBusy = false, cloudStatus = CloudBackupUiStatus.NO_BACKUP, cloudBackupExists = false, cloudBackupTimestamp = null, cloudBackupVersion = null, conflict = null)
                }
                if (showSuccess) emit(R.string.backup_no_cloud_backup)
            }
            is CloudSettingsSyncOutcome.Synchronized -> {
                val userId = backupManager.currentUserId()
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        cloudStatus = CloudBackupUiStatus.READY,
                        cloudBackupExists = true,
                        cloudBackupTimestamp = outcome.backup.timestamp,
                        cloudBackupVersion = outcome.backup.version,
                        lastSuccessfulSyncAt = userId?.let(backupManager::lastSuccessfulSyncAt),
                        conflict = null,
                        cloudErrorResource = null
                    )
                }
                refreshLocalMetadata()
                if (showSuccess) emit(R.string.backup_sync_success)
            }
            is CloudSettingsSyncOutcome.Conflict -> {
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        cloudStatus = CloudBackupUiStatus.CONFLICT,
                        cloudBackupExists = outcome.details.cloudBackup != null,
                        cloudBackupTimestamp = outcome.details.cloudBackup?.timestamp,
                        cloudBackupVersion = outcome.details.cloudBackup?.version,
                        conflict = outcome.details,
                        cloudErrorResource = null
                    )
                }
                if (showSuccess) emit(R.string.backup_conflict_attention)
            }
        }
    }

    private fun prepareCloudAction(): Boolean {
        val configured = backupManager.isCloudConfigured()
        val userId = backupManager.currentUserId()
        val online = backupManager.isNetworkAvailable()
        val status = when {
            !configured -> CloudBackupUiStatus.NOT_CONFIGURED
            userId == null -> CloudBackupUiStatus.SIGN_IN_REQUIRED
            !online -> CloudBackupUiStatus.OFFLINE
            else -> null
        }
        if (status == null) {
            _uiState.update { current ->
                val accountChanged = current.accountId != userId
                current.copy(
                    isCloudConfigured = configured,
                    isSignedIn = true,
                    accountId = userId,
                    isOnline = true,
                    cloudBackupExists = if (accountChanged) null else current.cloudBackupExists,
                    cloudBackupTimestamp = if (accountChanged) null else current.cloudBackupTimestamp,
                    cloudBackupVersion = if (accountChanged) null else current.cloudBackupVersion,
                    pendingRestore = if (accountChanged && current.pendingRestore?.source == RestoreBackupSource.CLOUD) null else current.pendingRestore,
                    conflict = if (accountChanged) null else current.conflict,
                    cloudErrorResource = null
                )
            }
            return true
        }
        _uiState.update {
            it.copy(
                isBusy = false,
                isCloudConfigured = configured,
                isSignedIn = userId != null,
                isOnline = online,
                cloudStatus = status
            )
        }
        val resource = when (status) {
            CloudBackupUiStatus.NOT_CONFIGURED -> R.string.backup_cloud_not_configured
            CloudBackupUiStatus.SIGN_IN_REQUIRED -> R.string.backup_cloud_sign_in_required
            CloudBackupUiStatus.OFFLINE -> R.string.backup_error_offline
            else -> R.string.backup_error_cloud
        }
        _events.tryEmit(resource)
        return false
    }

    private fun refreshLocalMetadata() {
        viewModelScope.launch {
            backupManager.loadLocalBackup().fold(
                onSuccess = { metadata -> _uiState.update { it.copy(localBackup = metadata, localErrorResource = null) } },
                onFailure = { error ->
                    val resource = messageFor(error, cloud = false)
                    _uiState.update { it.copy(localBackup = null, localErrorResource = resource) }
                }
            )
        }
    }

    private fun fail(error: Throwable, cloud: Boolean) {
        if (error is CancellationException) throw error
        val message = messageFor(error, cloud)
        _uiState.update { current ->
            if (cloud) {
                val userId = backupManager.currentUserId()
                val configured = backupManager.isCloudConfigured()
                val online = backupManager.isNetworkAvailable()
                val accountChanged = current.accountId != userId
                current.copy(
                    isBusy = false,
                    isCloudConfigured = configured,
                    isSignedIn = userId != null,
                    accountId = userId,
                    isOnline = online,
                    cloudStatus = when {
                        !configured -> CloudBackupUiStatus.NOT_CONFIGURED
                        userId == null -> CloudBackupUiStatus.SIGN_IN_REQUIRED
                        !online || error is OfflineSettingsBackupException -> CloudBackupUiStatus.OFFLINE
                        else -> CloudBackupUiStatus.ERROR
                    },
                    cloudBackupExists = if (accountChanged) null else current.cloudBackupExists,
                    cloudBackupTimestamp = if (accountChanged) null else current.cloudBackupTimestamp,
                    cloudBackupVersion = if (accountChanged) null else current.cloudBackupVersion,
                    pendingRestore = if (accountChanged && current.pendingRestore?.source == RestoreBackupSource.CLOUD) null else current.pendingRestore,
                    conflict = if (accountChanged) null else current.conflict,
                    cloudErrorResource = message
                )
            } else {
                current.copy(isBusy = false, localErrorResource = message)
            }
        }
        _events.tryEmit(message)
    }

    private fun messageFor(error: Throwable, cloud: Boolean): Int = when (error) {
        is UnsupportedSettingsBackupVersionException -> R.string.backup_error_unsupported
        is InvalidSettingsBackupException -> R.string.backup_error_invalid
        is SecurityException -> R.string.backup_error_permission
        is OfflineSettingsBackupException -> R.string.backup_error_offline
        is CloudSignInRequiredException -> R.string.backup_cloud_sign_in_required
        is CloudNotConfiguredException -> R.string.backup_cloud_not_configured
        is SettingsBackupAccountChangedException -> R.string.backup_error_account_changed
        else -> if (cloud) R.string.backup_error_cloud else R.string.backup_error_storage
    }

    private fun emit(resource: Int) {
        _events.tryEmit(resource)
    }

    companion object {
        private const val AUTO_SYNC_DEBOUNCE_MILLIS = 1_000L
    }
}
