package com.synapse.social.studioasinc.ui.settings

import com.synapse.social.studioasinc.ui.settings.formatBytes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                title = { Text(text = stringResource(R.string.storage_manage_title), style = MaterialTheme.typography.titleLarge) },
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
        }
    ) { padding ->
        if (isLoading || storageUsage == null) {
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
                    .padding(padding),
                contentPadding = PaddingValues(bottom = Spacing.Medium)
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
                        LargeFilesSectionHeader()
                    }
                    items(largeFiles) { file ->
                        LargeFileItem(
                            fileInfo = file,
                            onDeleteClick = { viewModel.deleteLargeFile(file.fileId) }
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.Medium)
    ) {
        Text(
            text = stringResource(R.string.storage_automated_cleanup),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = Spacing.Medium)
        )

        // Keep Media Slider (0=3 Days, 1=1 Week, 2=1 Month, 3=Forever)
        val keepMediaOptions = listOf(3, 7, 30, KEEP_MEDIA_FOREVER_DAYS)
        val keepMediaLabels = listOf("3 Days", "1 Week", "1 Month", "Forever")
        val keepMediaIndex = keepMediaOptions.indexOf(keepMediaDays).takeIf { it >= 0 } ?: 1

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.storage_keep_media), style = MaterialTheme.typography.bodyLarge)
            Text(keepMediaLabels[keepMediaIndex], color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = keepMediaIndex.toFloat(),
            onValueChange = { onKeepMediaChanged(keepMediaOptions[it.toInt()]) },
            valueRange = 0f..3f,
            steps = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Spacing.Medium))

        // Max Cache Size Slider (0=5 GB, 1=10 GB, 2=20 GB, 3=No Limit)
        val maxCacheOptions = listOf(5, 10, 20, MAX_CACHE_NO_LIMIT_GB)
        val maxCacheLabels = listOf("5 GB", "10 GB", "20 GB", "No Limit")
        val maxCacheIndex = maxCacheOptions.indexOf(maxCacheSizeGB).takeIf { it >= 0 } ?: 0

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.storage_max_cache), style = MaterialTheme.typography.bodyLarge)
            Text(maxCacheLabels[maxCacheIndex], color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = maxCacheIndex.toFloat(),
            onValueChange = { onMaxCacheChanged(maxCacheOptions[it.toInt()]) },
            valueRange = 0f..3f,
            steps = 2,
            modifier = Modifier.fillMaxWidth()
        )
    }
    HorizontalDivider(thickness = Spacing.Small, color = MaterialTheme.colorScheme.surfaceContainerLowest)
}


@Composable
fun ClearCacheSection(
    storageUsage: StorageUsageBreakdown,
    onClearCacheClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.Medium)
    ) {
        val totalCacheSize = formatBytes(storageUsage.synapseSize)

        Text(
            text = stringResource(R.string.storage_clear_cache),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = Spacing.Medium)
        )

        // Using calculated sizes for categorized breakdown
        CacheCategoryItem(stringResource(R.string.storage_photos), formatBytes(storageUsage.photoSize))
        CacheCategoryItem(stringResource(R.string.storage_videos), formatBytes(storageUsage.videoSize))
        CacheCategoryItem(stringResource(R.string.storage_documents), formatBytes(storageUsage.documentSize))
        CacheCategoryItem(stringResource(R.string.storage_chat), formatBytes(storageUsage.chatSize))
        CacheCategoryItem(stringResource(R.string.storage_temp_data), formatBytes(storageUsage.otherSize))

        Spacer(modifier = Modifier.height(Spacing.Medium))

        Button(
            onClick = onClearCacheClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(stringResource(R.string.storage_clear_entire_cache, totalCacheSize), fontWeight = FontWeight.Bold)
        }
    }
    HorizontalDivider(thickness = Spacing.Small, color = MaterialTheme.colorScheme.surfaceContainerLowest)
}

@Composable
fun CacheCategoryItem(name: String, size: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.ExtraSmall),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, style = MaterialTheme.typography.bodyLarge)
        Text(size, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun LargeFilesSectionHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.Medium)
    ) {
        Text(
            text = stringResource(R.string.storage_review_large_files),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = Spacing.Small)
        )
    }
}

@Composable
fun LargeFileItem(fileInfo: LargeFileInfo, onDeleteClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(text = fileInfo.fileName, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
        },
        supportingContent = {
            Text(text = formatBytes(fileInfo.size), style = MaterialTheme.typography.bodyMedium)
        },
        trailingContent = {
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete File",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        modifier = Modifier.clickable { /* Could open preview */ }
    )
    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
}
