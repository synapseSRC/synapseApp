package com.synapse.social.studioasinc.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestAccountInfoScreen(
    viewModel: RequestAccountInfoViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = SettingsColors.screenBackground,
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.request_account_info_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SettingsColors.screenBackground,
                    scrolledContainerColor = SettingsColors.cardBackground
                )
            )
        },
        bottomBar = {
            if (uiState.status !is RequestStatus.Ready) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SettingsColors.screenBackground,
                    tonalElevation = Sizes.ElevationLow
                ) {
                    Button(
                        onClick = { viewModel.requestReport() },
                        enabled = (uiState.isAccountInfoSelected || uiState.isChannelActivitySelected) && uiState.status is RequestStatus.Idle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(SettingsSpacing.screenPadding)
                            .height(Sizes.HeightDefault),
                        shape = SettingsShapes.inputShape
                    ) {
                        if (uiState.status is RequestStatus.Processing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(Sizes.IconLarge),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = Sizes.BorderDefault
                            )
                            Spacer(Modifier.width(SettingsSpacing.sectionSpacing))
                            Text(stringResource(R.string.action_processing_request))
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(Modifier.width(Spacing.Small))
                            Text(stringResource(R.string.action_request_reports))
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SettingsSpacing.screenPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)
        ) {
            AccountInfoStatusBanner(uiState.status)

            SettingsSection(title = stringResource(R.string.request_account_info_select_data)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(SettingsSpacing.itemSpacing)
                ) {
                    AccountInfoSelectionRow(
                        title = stringResource(R.string.account_information_section),
                        subtitle = stringResource(R.string.request_account_info_account_desc),
                        icon = Icons.Outlined.Description,
                        selected = uiState.isAccountInfoSelected,
                        enabled = uiState.status is RequestStatus.Idle,
                        position = SettingsItemPosition.Top,
                        onToggle = { viewModel.toggleAccountSelection() }
                    )
                    AccountInfoSelectionRow(
                        title = stringResource(R.string.channels_activity_section),
                        subtitle = stringResource(R.string.request_account_info_channels_desc),
                        icon = Icons.Default.Campaign,
                        selected = uiState.isChannelActivitySelected,
                        enabled = uiState.status is RequestStatus.Idle,
                        position = SettingsItemPosition.Bottom,
                        onToggle = { viewModel.toggleChannelSelection() }
                    )
                }
            }

            SettingsSection(title = stringResource(R.string.request_account_info_automation)) {
                SettingsToggleItem(
                    title = stringResource(R.string.request_account_info_auto_monthly_title),
                    subtitle = stringResource(R.string.request_account_info_auto_monthly_desc),
                    imageVector = Icons.Outlined.Settings,
                    checked = uiState.isAutoReportEnabled,
                    onCheckedChange = { viewModel.toggleAutoReport(it) },
                    enabled = true,
                    position = SettingsItemPosition.Single
                )
            }

            AnimatedVisibility(visible = uiState.status is RequestStatus.Ready) {
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Sizes.HeightDefault),
                    shape = SettingsShapes.inputShape
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    Text(stringResource(R.string.action_download_report))
                }
            }

            Spacer(modifier = Modifier.height(Spacing.Medium))
        }
    }
}

@Composable
private fun AccountInfoStatusBanner(status: RequestStatus) {
    data class BannerContent(val icon: ImageVector, val title: String, val desc: String, val isReady: Boolean)

    val content = when (status) {
        is RequestStatus.Ready -> BannerContent(
            Icons.Default.CheckCircle,
            stringResource(R.string.request_account_info_report_ready),
            stringResource(R.string.request_account_info_available_until, status.availableUntil),
            true
        )
        else -> BannerContent(
            Icons.Default.Info,
            stringResource(R.string.request_account_info_how_it_works),
            stringResource(R.string.request_account_info_how_it_works_desc),
            false
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsShapes.cardShape,
        color = SettingsColors.cardBackgroundElevated,
        tonalElevation = Sizes.ElevationLow
    ) {
        Row(
            modifier = Modifier
                .padding(SettingsSpacing.itemPadding)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = SettingsShapes.iconBadgeShape,
                color = if (content.isReady) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(
                    imageVector = content.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(Spacing.Small)
                        .size(SettingsSpacing.iconSize),
                    tint = if (content.isReady) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.width(SettingsSpacing.iconTextSpacing))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = content.title,
                    style = SettingsTypography.itemTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                Text(
                    text = content.desc,
                    style = SettingsTypography.itemSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AccountInfoSelectionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    position: SettingsItemPosition,
    onToggle: () -> Unit
) {
    val rowDescription = "$title, $subtitle"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = position.getShape(),
        color = SettingsColors.cardBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = enabled,
                    role = Role.Checkbox,
                    onClick = onToggle
                )
                .semantics(mergeDescendants = true) {
                    role = Role.Checkbox
                    contentDescription = rowDescription
                }
                .padding(
                    horizontal = SettingsSpacing.itemHorizontalPadding,
                    vertical = SettingsSpacing.itemVerticalPadding
                )
                .heightIn(min = SettingsSpacing.minTouchTarget),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = SettingsShapes.iconBadgeShape,
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(Spacing.Small)
                        .size(SettingsSpacing.iconSize),
                    tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(SettingsSpacing.iconTextSpacing))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = SettingsTypography.itemTitle,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                Text(
                    text = subtitle,
                    style = SettingsTypography.itemSubtitle,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
            Spacer(modifier = Modifier.width(Spacing.Small))
            Checkbox(
                checked = selected,
                onCheckedChange = null,
                enabled = enabled,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
