package com.synapse.social.studioasinc.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
                            color = SettingsColors.cardBackground,
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
                AccountInfoSelectionRow(
                    title = stringResource(R.string.account_information_section),
                    subtitle = stringResource(R.string.request_account_info_account_desc),
                    icon = Icons.Outlined.Description,
                    selected = uiState.isAccountInfoSelected,
                    enabled = uiState.status is RequestStatus.Idle,
                    onToggle = { viewModel.toggleAccountSelection() }
                )
                SettingsDivider()
                AccountInfoSelectionRow(
                    title = stringResource(R.string.channels_activity_section),
                    subtitle = stringResource(R.string.request_account_info_channels_desc),
                    icon = Icons.Default.Campaign,
                    selected = uiState.isChannelActivitySelected,
                    enabled = uiState.status is RequestStatus.Idle,
                    onToggle = { viewModel.toggleChannelSelection() }
                )
            }

            SettingsSection(title = stringResource(R.string.request_account_info_automation)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(SettingsSpacing.itemPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(SettingsSpacing.iconSize),
                        tint = SettingsColors.itemIcon
                    )
                    Spacer(modifier = Modifier.width(SettingsSpacing.iconTextSpacing))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.request_account_info_auto_monthly_title),
                            style = SettingsTypography.itemTitle
                        )
                        Text(
                            text = stringResource(R.string.request_account_info_auto_monthly_desc),
                            style = SettingsTypography.itemSubtitle,
                            color = SettingsColors.sectionTitle
                        )
                    }
                    Switch(
                        checked = uiState.isAutoReportEnabled,
                        onCheckedChange = { viewModel.toggleAutoReport(it) }
                    )
                }
            }

            AnimatedVisibility(visible = uiState.status is RequestStatus.Ready) {
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    Text(stringResource(R.string.action_download_report))
                }
            }

            Spacer(modifier = Modifier.height(Sizes.HeightSmall))
        }
    }
}

@Composable
private fun AccountInfoStatusBanner(status: RequestStatus) {
    data class BannerContent(val icon: ImageVector, val title: String, val desc: String)

    val content = when (status) {
        is RequestStatus.Ready -> BannerContent(
            Icons.Default.CheckCircle,
            stringResource(R.string.request_account_info_report_ready),
            stringResource(R.string.request_account_info_available_until, status.availableUntil)
        )
        else -> BannerContent(
            Icons.Default.Info,
            stringResource(R.string.request_account_info_how_it_works),
            stringResource(R.string.request_account_info_how_it_works_desc)
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SettingsColors.cardBackground),
        shape = SettingsShapes.sectionShape
    ) {
        Row(
            modifier = Modifier
                .padding(SettingsSpacing.itemPadding)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = content.icon,
                contentDescription = null,
                tint = SettingsColors.itemIcon
            )
            Spacer(modifier = Modifier.width(SettingsSpacing.sectionSpacing))
            Column {
                Text(
                    text = content.title,
                    style = SettingsTypography.itemTitle
                )
                Text(
                    text = content.desc,
                    style = SettingsTypography.itemSubtitle,
                    color = SettingsColors.sectionTitle
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
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(SettingsSpacing.itemPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(SettingsSpacing.iconSize),
            tint = if (selected) SettingsColors.categoryIconTint else SettingsColors.itemIcon
        )
        Spacer(modifier = Modifier.width(SettingsSpacing.iconTextSpacing))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = SettingsTypography.itemTitle)
            Text(
                text = subtitle,
                style = SettingsTypography.itemSubtitle,
                color = SettingsColors.sectionTitle,
                maxLines = 1
            )
        }
        Checkbox(
            checked = selected,
            onCheckedChange = { if (enabled) onToggle() },
            enabled = enabled
        )
    }
}
