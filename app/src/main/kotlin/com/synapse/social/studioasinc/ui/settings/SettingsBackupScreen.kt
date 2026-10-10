package com.synapse.social.studioasinc.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import java.util.Date
import android.text.format.DateFormat as AndroidDateFormat
import android.text.format.Formatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBackupScreen(
    viewModel: SettingsBackupViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) viewModel.exportCurrentBackup(uri) }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) viewModel.importBackup(uri) }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(context.getString(message))
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = SettingsColors.screenBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.backup_restore_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_settings_hub_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SettingsColors.screenBackground,
                    scrolledContainerColor = SettingsColors.cardBackground
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = SettingsSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing),
            contentPadding = PaddingValues(vertical = Spacing.Small)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SettingsColors.cardBackground)
                ) {
                    Column(Modifier.padding(SettingsSpacing.itemPadding)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Backup,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(SettingsSpacing.iconSize)
                            )
                            Spacer(Modifier.size(Spacing.Medium))
                            Text(
                                text = stringResource(R.string.backup_restore_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(Modifier.height(Spacing.SmallMedium))
                        Text(
                            text = stringResource(R.string.backup_restore_intro),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    Text(
                        text = stringResource(R.string.backup_destination_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    PrimaryTabRow(
                        selectedTabIndex = if (state.mode == BackupDestinationMode.LOCAL) 0 else 1,
                        containerColor = SettingsColors.screenBackground,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = state.mode == BackupDestinationMode.LOCAL,
                            onClick = { viewModel.selectMode(BackupDestinationMode.LOCAL) },
                            text = { Text(stringResource(R.string.backup_mode_on_device)) }
                        )
                        Tab(
                            selected = state.mode == BackupDestinationMode.CLOUD,
                            onClick = { viewModel.selectMode(BackupDestinationMode.CLOUD) },
                            text = { Text(stringResource(R.string.backup_mode_cloud)) }
                        )
                    }
                }
            }

            if (state.isBusy) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = context.getString(R.string.backup_working) }
                    )
                }
            }

            item {
                if (state.mode == BackupDestinationMode.LOCAL) {
                    LocalBackupSection(
                        state = state,
                        onCreate = { viewModel.createLocalBackup() },
                        onRestore = { viewModel.requestLocalRestore() },
                        onExport = { exportLauncher.launch("synapse_settings_backup.json") },
                        onImport = { importLauncher.launch(arrayOf("application/json", "text/json", "text/plain")) }
                    )
                } else {
                    CloudBackupSection(
                        state = state,
                        onBackup = { viewModel.backupNowToCloud() },
                        onRestore = { viewModel.requestCloudRestore() },
                        onSync = { viewModel.syncNow() }
                    )
                }
            }

            item {
                IncludedSettingsSection()
            }

            item {
                PrivacyAndRestoreNote()
            }
        }
    }

    state.pendingRestore?.let { pending ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissRestorePreview() },
            title = {
                Text(
                    when (pending.source) {
                        RestoreBackupSource.LOCAL -> stringResource(R.string.backup_restore_local_title)
                        RestoreBackupSource.FILE -> stringResource(R.string.backup_restore_file_title)
                        RestoreBackupSource.CLOUD -> stringResource(R.string.backup_restore_cloud_title)
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    Text(
                        stringResource(
                            R.string.backup_restore_preview_message,
                            formatTimestamp(context, pending.backup.timestamp),
                            pending.backup.settings.size,
                            pending.backup.version
                        )
                    )
                    Text(stringResource(R.string.backup_restore_warning))
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmRestore() }, enabled = !state.isBusy) {
                    Text(stringResource(R.string.backup_restore_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRestorePreview() }, enabled = !state.isBusy) {
                    Text(stringResource(R.string.backup_cancel))
                }
            }
        )
    }

    state.conflict?.let { conflict ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissConflict() },
            title = { Text(stringResource(R.string.backup_conflict_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    Text(stringResource(R.string.backup_conflict_message))
                    Text(
                        stringResource(
                            R.string.backup_conflict_device_time,
                            formatTimestamp(context, conflict.localBackup.timestamp)
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    conflict.cloudBackup?.let { cloud ->
                        Text(
                            stringResource(
                                R.string.backup_conflict_cloud_time,
                                formatTimestamp(context, cloud.timestamp)
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                    } ?: Text(stringResource(R.string.backup_conflict_cloud_missing))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.resolveConflict(useCloudCopy = true) },
                    enabled = !state.isBusy && conflict.cloudBackup != null
                ) {
                    Text(stringResource(R.string.backup_use_cloud))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.resolveConflict(useCloudCopy = false) },
                    enabled = !state.isBusy
                ) {
                    Text(stringResource(R.string.backup_use_device))
                }
            }
        )
    }
}

@Composable
private fun LocalBackupSection(
    state: SettingsBackupUiState,
    onCreate: () -> Unit,
    onRestore: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SettingsColors.cardBackground)
    ) {
        Column(
            modifier = Modifier.padding(SettingsSpacing.itemPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            Text(stringResource(R.string.backup_local_status_title), style = MaterialTheme.typography.titleMedium)
            val metadata = state.localBackup
            if (metadata == null) {
                Text(
                    stringResource(R.string.backup_local_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    stringResource(R.string.backup_local_available),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(stringResource(R.string.backup_created_at, formatTimestamp(LocalContext.current, metadata.backup.timestamp)))
                Text(stringResource(R.string.backup_size_version, Formatter.formatFileSize(LocalContext.current, metadata.sizeBytes), metadata.backup.version))
            }
            state.localErrorResource?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            Button(onClick = onCreate, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Backup, contentDescription = null)
                Spacer(Modifier.size(Spacing.Small))
                Text(stringResource(R.string.backup_create_local))
            }
            OutlinedButton(onClick = onRestore, enabled = !state.isBusy && metadata != null, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Restore, contentDescription = null)
                Spacer(Modifier.size(Spacing.Small))
                Text(stringResource(R.string.backup_restore))
            }
            OutlinedButton(onClick = onExport, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.FileDownload, contentDescription = null)
                Spacer(Modifier.size(Spacing.Small))
                Text(stringResource(R.string.backup_export_file))
            }
            OutlinedButton(onClick = onImport, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.FileUpload, contentDescription = null)
                Spacer(Modifier.size(Spacing.Small))
                Text(stringResource(R.string.backup_import_file))
            }
        }
    }
}

@Composable
private fun CloudBackupSection(
    state: SettingsBackupUiState,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onSync: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SettingsColors.cardBackground)
    ) {
        Column(
            modifier = Modifier.padding(SettingsSpacing.itemPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(Spacing.Small))
                Text(stringResource(R.string.backup_cloud_status_title), style = MaterialTheme.typography.titleMedium)
            }
            Text(
                text = cloudStatusText(state.cloudStatus),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.cloudStatus == CloudBackupUiStatus.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
            state.cloudErrorResource?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            if (state.cloudBackupExists == true && state.cloudBackupTimestamp != null && state.cloudBackupVersion != null) {
                Text(stringResource(R.string.backup_cloud_created_at, formatTimestamp(context, state.cloudBackupTimestamp), state.cloudBackupVersion))
            }
            state.lastSuccessfulSyncAt?.let {
                Text(
                    stringResource(R.string.backup_last_successful_sync, formatTimestamp(context, it)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                stringResource(R.string.backup_cloud_overwrite_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onBackup, enabled = state.canUseCloud, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Backup, contentDescription = null)
                Spacer(Modifier.size(Spacing.Small))
                Text(stringResource(R.string.backup_cloud_backup_now))
            }
            OutlinedButton(
                onClick = onRestore,
                enabled = state.canUseCloud && state.cloudBackupExists == true,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Restore, contentDescription = null)
                Spacer(Modifier.size(Spacing.Small))
                Text(stringResource(R.string.backup_cloud_restore))
            }
            OutlinedButton(onClick = onSync, enabled = state.canUseCloud, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Sync, contentDescription = null)
                Spacer(Modifier.size(Spacing.Small))
                Text(stringResource(R.string.backup_sync_now))
            }
        }
    }
}

@Composable
private fun IncludedSettingsSection() {
    val categories = listOf(
        R.string.backup_category_appearance,
        R.string.backup_category_chat,
        R.string.backup_category_privacy,
        R.string.backup_category_notifications,
        R.string.backup_category_language_accessibility,
        R.string.backup_category_navigation,
        R.string.backup_category_data_media
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SettingsColors.cardBackground)
    ) {
        Column(
            modifier = Modifier.padding(SettingsSpacing.itemPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            Text(stringResource(R.string.backup_scope_title), style = MaterialTheme.typography.titleMedium)
            categories.forEach { category ->
                Text(
                    text = "• " + stringResource(category),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PrivacyAndRestoreNote() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(SettingsSpacing.itemPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            Text(stringResource(R.string.backup_privacy_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.backup_privacy_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                stringResource(R.string.backup_restore_warning),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun cloudStatusText(status: CloudBackupUiStatus): String = when (status) {
    CloudBackupUiStatus.CHECKING -> stringResource(R.string.backup_cloud_checking)
    CloudBackupUiStatus.NOT_CONFIGURED -> stringResource(R.string.backup_cloud_not_configured)
    CloudBackupUiStatus.SIGN_IN_REQUIRED -> stringResource(R.string.backup_cloud_sign_in_required)
    CloudBackupUiStatus.OFFLINE -> stringResource(R.string.backup_cloud_offline)
    CloudBackupUiStatus.NO_BACKUP -> stringResource(R.string.backup_cloud_empty)
    CloudBackupUiStatus.READY -> stringResource(R.string.backup_cloud_ready)
    CloudBackupUiStatus.SYNCING -> stringResource(R.string.backup_cloud_syncing)
    CloudBackupUiStatus.CONFLICT -> stringResource(R.string.backup_cloud_conflict)
    CloudBackupUiStatus.ERROR -> stringResource(R.string.backup_cloud_error)
}

private fun formatTimestamp(context: android.content.Context, timestamp: Long): String {
    val date = Date(timestamp)
    return AndroidDateFormat.getMediumDateFormat(context).format(date) + " " +
        AndroidDateFormat.getTimeFormat(context).format(date)
}
