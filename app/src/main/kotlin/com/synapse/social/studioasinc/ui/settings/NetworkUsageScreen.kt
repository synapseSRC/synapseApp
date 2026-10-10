package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkUsageScreen(
    viewModel: NetworkUsageViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val usageItems = uiState.usageItems
    val totalSent = uiState.totalSent
    val totalReceived = uiState.totalReceived
    val isLoading = uiState.isLoading
    val lastResetTime = uiState.lastResetTime

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resetMessage = stringResource(R.string.network_usage_statistics_reset)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SettingsColors.screenBackground,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                title = {
                    Text(
                        text = stringResource(R.string.network_usage_title),
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
        if (isLoading) {
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
                    SettingsSection(title = stringResource(R.string.network_usage_heading)) {
                        SettingsCard {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(SettingsSpacing.itemHorizontalPadding)
                            ) {
                                val subtitleText = if (lastResetTime > 0) {
                                    val dateStr = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(lastResetTime))
                                    stringResource(R.string.network_usage_last_reset_format, dateStr)
                                } else {
                                    stringResource(R.string.network_usage_since_device_boot)
                                }

                                Text(
                                    text = subtitleText,
                                    style = SettingsTypography.itemSubtitle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(Spacing.Medium))

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f).padding(end = Spacing.Medium)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Outlined.ArrowUpward,
                                                contentDescription = stringResource(R.string.cd_upload_icon),
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                                            Text(
                                                text = stringResource(R.string.network_usage_sent),
                                                style = SettingsTypography.itemSubtitle,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                                        Text(
                                            text = formatBytes(totalSent),
                                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Outlined.ArrowDownward,
                                                contentDescription = stringResource(R.string.cd_download_icon),
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.tertiary
                                            )
                                            Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                                            Text(
                                                text = stringResource(R.string.network_usage_received),
                                                style = SettingsTypography.itemSubtitle,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                                        Text(
                                            text = formatBytes(totalReceived),
                                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    SettingsSection(title = stringResource(R.string.storage_section_data)) {
                        SettingsCard {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                usageItems.forEachIndexed { index, item ->
                                    val icon = when (item.labelRes) {
                                        R.string.network_usage_mobile_data -> Icons.Filled.CellTower
                                        R.string.network_usage_wifi -> Icons.Filled.Wifi
                                        R.string.network_usage_this_app -> Icons.Filled.Settings
                                        else -> Icons.Filled.NetworkCheck
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = SettingsSpacing.itemHorizontalPadding,
                                                vertical = SettingsSpacing.itemVerticalPadding
                                            ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SettingsIconBadge(imageVector = icon)

                                        Spacer(modifier = Modifier.width(SettingsSpacing.iconTextSpacing))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = stringResource(item.labelRes),
                                                style = SettingsTypography.itemTitle,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )

                                            Spacer(modifier = Modifier.height(Spacing.ExtraSmall))

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Outlined.ArrowUpward,
                                                    contentDescription = stringResource(R.string.cd_upload_icon),
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = formatBytes(item.sentBytes),
                                                    style = SettingsTypography.itemSubtitle,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )

                                                Spacer(modifier = Modifier.width(Spacing.Medium))

                                                Icon(
                                                    imageVector = Icons.Outlined.ArrowDownward,
                                                    contentDescription = stringResource(R.string.cd_download_icon),
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.tertiary
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = formatBytes(item.receivedBytes),
                                                    style = SettingsTypography.itemSubtitle,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    if (index < usageItems.size - 1) {
                                        SettingsDivider()
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(Spacing.Small))
                    SettingsButtonItem(
                        title = stringResource(R.string.network_reset_statistics),
                        isDestructive = true,
                        onClick = {
                            viewModel.resetStats()
                            scope.launch {
                                snackbarHostState.showSnackbar(resetMessage)
                            }
                        }
                    )
                }
            }
        }
    }
}
