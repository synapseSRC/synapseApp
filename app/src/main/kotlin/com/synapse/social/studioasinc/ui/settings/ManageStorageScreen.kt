package com.synapse.social.studioasinc.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

private const val KEEP_MEDIA_FOREVER_DAYS = 365
private const val MAX_CACHE_NO_LIMIT_GB = 999

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageStorageScreen(
    viewModel: ManageStorageViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val storageUsage by viewModel.storageUsage.collectAsState()
    val largeFiles by viewModel.largeFiles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val keepMediaDays by viewModel.keepMediaDays.collectAsState()
    val maxCacheSizeGB by viewModel.maxCacheSizeGB.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SettingsColors.screenBackground,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                title = {
                    Text(
                        text = stringResource(R.string.storage_manage_title),
                        style = SettingsTypography.screenTitle
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back_button)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = SettingsColors.screenBackground
    ) { padding ->
        if (isLoading && storageUsage == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = SettingsSpacing.screenPadding),
                contentPadding = PaddingValues(top = Spacing.Small, bottom = Spacing.ExtraLarge),
                verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)
            ) {
                item {
                    storageUsage?.let { StorageUsageSection(storageUsage = it) }
                }

                item {
                    AutomatedCleanupSection(
                        keepMediaDays = keepMediaDays,
                        maxCacheSizeGB = maxCacheSizeGB,
                        onKeepMediaChanged = { viewModel.setKeepMediaDays(it) },
                        onMaxCacheChanged = { viewModel.setMaxCacheSizeGB(it) }
                    )
                }

                item {
                    storageUsage?.let { usage ->
                        ClearCacheSection(
                            storageUsage = usage,
                            onClearCacheClick = { viewModel.clearEntireCache() }
                        )
                    }
                }

                if (largeFiles.isNotEmpty()) {
                    item {
                        LargeFilesSection(
                            largeFiles = largeFiles,
                            onDeleteClick = { viewModel.deleteLargeFile(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AutomatedCleanupSection(
    keepMediaDays: Int,
    maxCacheSizeGB: Int,
    onKeepMediaChanged: (Int) -> Unit,
    onMaxCacheChanged: (Int) -> Unit
) {
    val keepMediaOptions = listOf(3, 7, 30, KEEP_MEDIA_FOREVER_DAYS)
    val keepMediaLabels = listOf("3 Days", "1 Week", "1 Month", "Forever")
    val keepMediaIndex = keepMediaOptions.indexOf(keepMediaDays).takeIf { it >= 0 } ?: 1

    val maxCacheOptions = listOf(5, 10, 20, MAX_CACHE_NO_LIMIT_GB)
    val maxCacheLabels = listOf("5 GB", "10 GB", "20 GB", "No Limit")
    val maxCacheIndex = maxCacheOptions.indexOf(maxCacheSizeGB).takeIf { it >= 0 } ?: 0

    SettingsSection(title = stringResource(R.string.storage_automated_cleanup)) {
        SettingsSliderItem(
            title = stringResource(R.string.storage_keep_media),
            value = keepMediaIndex.toFloat(),
            valueRange = 0f..3f,
            steps = 2,
            onValueChange = { onKeepMediaChanged(keepMediaOptions[it.toInt()]) },
            valueLabel = { keepMediaLabels[it.toInt().coerceIn(0, 3)] },
            position = SettingsItemPosition.Top
        )

        SettingsSliderItem(
            title = stringResource(R.string.storage_max_cache),
            value = maxCacheIndex.toFloat(),
            valueRange = 0f..3f,
            steps = 2,
            onValueChange = { onMaxCacheChanged(maxCacheOptions[it.toInt()]) },
            valueLabel = { maxCacheLabels[it.toInt().coerceIn(0, 3)] },
            position = SettingsItemPosition.Bottom
        )
    }
}

@Composable
fun ClearCacheSection(
    storageUsage: StorageUsageBreakdown,
    onClearCacheClick: () -> Unit
) {
    val totalCacheSize = formatBytes(storageUsage.synapseSize)

    SettingsSection(title = stringResource(R.string.storage_clear_cache)) {
        SettingsCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SettingsSpacing.itemVerticalPadding)
            ) {
                CacheCategoryRow(stringResource(R.string.storage_photos), formatBytes(storageUsage.photoSize))
                SettingsDivider()
                CacheCategoryRow(stringResource(R.string.storage_videos), formatBytes(storageUsage.videoSize))
                SettingsDivider()
                CacheCategoryRow(stringResource(R.string.storage_documents), formatBytes(storageUsage.documentSize))
                SettingsDivider()
                CacheCategoryRow(stringResource(R.string.storage_chat), formatBytes(storageUsage.chatSize))
                SettingsDivider()
                CacheCategoryRow(stringResource(R.string.storage_temp_data), formatBytes(storageUsage.otherSize))

                Spacer(modifier = Modifier.height(Spacing.Medium))

                Button(
                    onClick = onClearCacheClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SettingsSpacing.itemHorizontalPadding),
                    shape = SettingsShapes.itemShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SettingsColors.destructiveButton,
                        contentColor = SettingsColors.destructiveText
                    )
                ) {
                    Text(
                        text = stringResource(R.string.storage_clear_entire_cache, totalCacheSize),
                        style = SettingsTypography.buttonText
                    )
                }
            }
        }
    }
}

@Composable
private fun CacheCategoryRow(name: String, size: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = SettingsSpacing.itemHorizontalPadding,
                vertical = Spacing.Small
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = SettingsTypography.itemTitle,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = size,
            style = SettingsTypography.itemSubtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LargeFilesSection(
    largeFiles: List<LargeFileInfo>,
    onDeleteClick: (String) -> Unit
) {
    SettingsSection(title = stringResource(R.string.storage_review_large_files)) {
        SettingsCard {
            Column(modifier = Modifier.fillMaxWidth()) {
                largeFiles.forEachIndexed { index, file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = SettingsSpacing.itemHorizontalPadding,
                                vertical = SettingsSpacing.itemVerticalPadding
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.fileName,
                                style = SettingsTypography.itemTitle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                            Text(
                                text = formatBytes(file.size),
                                style = SettingsTypography.itemSubtitle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { onDeleteClick(file.fileId) }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete file",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    if (index < largeFiles.size - 1) {
                        SettingsDivider()
                    }
                }
            }
        }
    }
}
