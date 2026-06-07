package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageDataScreen(
    viewModel: StorageDataViewModel,
    onBackClick: () -> Unit,
    navController: NavController? = null
) {
    val mediaUploadQuality by viewModel.mediaUploadQuality.collectAsState()
    val autoDownloadRules by viewModel.autoDownloadRules.collectAsState()
    val useLessDataCalls by viewModel.useLessDataCalls.collectAsState()
    val showMediaQualitySheet by viewModel.showMediaQualitySheet.collectAsState()


    var showMobileDialog by remember { mutableStateOf(false) }
    var showWifiDialog by remember { mutableStateOf(false) }
    var showRoamingDialog by remember { mutableStateOf(false) }

    StorageDataContent(
        mediaUploadQuality = mediaUploadQuality,
        autoDownloadRules = autoDownloadRules,
        useLessDataCalls = useLessDataCalls,
        onBackClick = onBackClick,
        onNavigateToStorageManage = { navController?.navigate("settings_storage_manage") },
        onNavigateToNetworkUsage = { navController?.navigate("settings_network_usage") },
        onNavigateToProxy = {  },
        onUseLessDataCallsChanged = { viewModel.setUseLessDataCalls(it) },
        onOpenMobileDialog = { showMobileDialog = true },
        onOpenWifiDialog = { showWifiDialog = true },
        onOpenRoamingDialog = { showRoamingDialog = true },
        onOpenMediaQualitySheet = { viewModel.openMediaQualitySheet() }
    )

    StorageDataScreenDialogs(
        showMediaQualitySheet = showMediaQualitySheet,
        mediaUploadQuality = mediaUploadQuality,
        autoDownloadRules = autoDownloadRules,
        showMobileDialog = showMobileDialog,
        showWifiDialog = showWifiDialog,
        showRoamingDialog = showRoamingDialog,
        onCloseMediaQualitySheet = { viewModel.closeMediaQualitySheet() },
        onSetMediaUploadQuality = { viewModel.setMediaUploadQuality(it) },
        onSetAutoDownloadRule = { type, rules -> viewModel.setAutoDownloadRule(type, rules) },
        onDismissMobileDialog = { showMobileDialog = false },
        onDismissWifiDialog = { showWifiDialog = false },
        onDismissRoamingDialog = { showRoamingDialog = false }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StorageDataContent(
    mediaUploadQuality: MediaUploadQuality,
    autoDownloadRules: AutoDownloadRules,
    useLessDataCalls: Boolean,
    onBackClick: () -> Unit,
    onNavigateToStorageManage: () -> Unit,
    onNavigateToNetworkUsage: () -> Unit,
    onNavigateToProxy: () -> Unit,
    onUseLessDataCallsChanged: (Boolean) -> Unit,
    onOpenMobileDialog: () -> Unit,
    onOpenWifiDialog: () -> Unit,
    onOpenRoamingDialog: () -> Unit,
    onOpenMediaQualitySheet: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings_storage_data_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_button)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
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
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
        ) {

            item {
                StorageManagementSection(
                    onNavigateToStorageManage = onNavigateToStorageManage,
                    onNavigateToNetworkUsage = onNavigateToNetworkUsage
                )
            }


            item {
                CallSettingsSection(
                    useLessDataCalls = useLessDataCalls,
                    onUseLessDataCallsChanged = onUseLessDataCallsChanged
                )
            }


            item {
                NetworkSection(
                    onNavigateToProxy = onNavigateToProxy
                )
            }


            item {
                MediaAutoDownloadSection(
                    autoDownloadRules = autoDownloadRules,
                    onOpenMobileDialog = onOpenMobileDialog,
                    onOpenWifiDialog = onOpenWifiDialog,
                    onOpenRoamingDialog = onOpenRoamingDialog
                )
            }


            item {
                MediaUploadQualitySection(
                    mediaUploadQuality = mediaUploadQuality,
                    onOpenMediaQualitySheet = onOpenMediaQualitySheet
                )
            }


            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun getAutoDownloadSummary(selectedTypes: Set<MediaType>): String {
    if (selectedTypes.isEmpty()) return stringResource(R.string.settings_no_media)
    if (selectedTypes.size == MediaType.values().size) return stringResource(R.string.settings_all_media)
    return selectedTypes.joinToString(", ") { it.displayName() }
}

@Composable
private fun StorageDataScreenDialogs(
    showMediaQualitySheet: Boolean,
    mediaUploadQuality: MediaUploadQuality,
    autoDownloadRules: AutoDownloadRules,
    showMobileDialog: Boolean,
    showWifiDialog: Boolean,
    showRoamingDialog: Boolean,
    onCloseMediaQualitySheet: () -> Unit,
    onSetMediaUploadQuality: (MediaUploadQuality) -> Unit,
    onSetAutoDownloadRule: (String, Set<MediaType>) -> Unit,
    onDismissMobileDialog: () -> Unit,
    onDismissWifiDialog: () -> Unit,
    onDismissRoamingDialog: () -> Unit
) {
    if (showMediaQualitySheet) {
        MediaQualityBottomSheet(
            onDismissRequest = onCloseMediaQualitySheet,
            currentQuality = mediaUploadQuality,
            onQualitySelected = {
                onSetMediaUploadQuality(it)
                onCloseMediaQualitySheet()
            }
        )
    }

    if (showMobileDialog) {
        AutoDownloadDialog(
            title = stringResource(R.string.settings_when_using_mobile_data_title),
            selectedTypes = autoDownloadRules.mobileData,
            onConfirm = {
                onSetAutoDownloadRule("mobile", it)
                onDismissMobileDialog()
            },
            onDismiss = onDismissMobileDialog
        )
    }

    if (showWifiDialog) {
        AutoDownloadDialog(
            title = stringResource(R.string.settings_when_connected_on_wifi_title),
            selectedTypes = autoDownloadRules.wifi,
            onConfirm = {
                onSetAutoDownloadRule("wifi", it)
                onDismissWifiDialog()
            },
            onDismiss = onDismissWifiDialog
        )
    }

    if (showRoamingDialog) {
        AutoDownloadDialog(
            title = stringResource(R.string.settings_when_roaming_title),
            selectedTypes = autoDownloadRules.roaming,
            onConfirm = {
                onSetAutoDownloadRule("roaming", it)
                onDismissRoamingDialog()
            },
            onDismiss = onDismissRoamingDialog
        )
    }
}

@Composable
private fun StorageManagementSection(
    onNavigateToStorageManage: () -> Unit,
    onNavigateToNetworkUsage: () -> Unit
) {
    SettingsSection(title = stringResource(R.string.settings_storage_management_section)) {
        SettingsNavigationItem(
            title = stringResource(R.string.settings_storage_manage_title),
            subtitle = stringResource(R.string.settings_storage_manage_subtitle),
            imageVector = Icons.Filled.Archive,
            onClick = onNavigateToStorageManage
        )
        SettingsDivider()
        SettingsNavigationItem(
            title = stringResource(R.string.settings_network_usage_title),
            subtitle = stringResource(R.string.settings_network_usage_subtitle),
            imageVector = Icons.Filled.NetworkCheck,
            onClick = onNavigateToNetworkUsage
        )
    }
}

@Composable
private fun CallSettingsSection(
    useLessDataCalls: Boolean,
    onUseLessDataCallsChanged: (Boolean) -> Unit
) {
    SettingsSection(title = stringResource(R.string.settings_call_settings_section)) {
        SettingsToggleItem(
            title = stringResource(R.string.settings_use_less_data_calls_title),
            subtitle = stringResource(R.string.settings_use_less_data_calls_subtitle),
            imageVector = Icons.Filled.Call,
            checked = useLessDataCalls,
            onCheckedChange = onUseLessDataCallsChanged
        )
    }
}

@Composable
private fun NetworkSection(onNavigateToProxy: () -> Unit) {
    SettingsSection(title = stringResource(R.string.settings_network_section)) {
        SettingsNavigationItem(
            title = stringResource(R.string.settings_proxy_title),
            subtitle = stringResource(R.string.settings_proxy_subtitle),
            imageVector = Icons.Filled.NetworkCheck,
            onClick = onNavigateToProxy
        )
    }
}

@Composable
private fun MediaAutoDownloadSection(
    autoDownloadRules: AutoDownloadRules,
    onOpenMobileDialog: () -> Unit,
    onOpenWifiDialog: () -> Unit,
    onOpenRoamingDialog: () -> Unit
) {
    SettingsSection(title = stringResource(R.string.settings_media_auto_download_section)) {
        SettingsNavigationItem(
            title = stringResource(R.string.settings_when_using_mobile_data_title),
            subtitle = getAutoDownloadSummary(autoDownloadRules.mobileData),
            imageVector = Icons.Filled.Phone,
            onClick = onOpenMobileDialog
        )
        SettingsDivider()
        SettingsNavigationItem(
            title = stringResource(R.string.settings_when_connected_on_wifi_title),
            subtitle = getAutoDownloadSummary(autoDownloadRules.wifi),
            imageVector = Icons.Filled.Wifi,
            onClick = onOpenWifiDialog
        )
        SettingsDivider()
        SettingsNavigationItem(
            title = stringResource(R.string.settings_when_roaming_title),
            subtitle = getAutoDownloadSummary(autoDownloadRules.roaming),
            imageVector = Icons.Filled.SignalCellularAlt,
            onClick = onOpenRoamingDialog
        )
    }
}

@Composable
private fun MediaUploadQualitySection(
    mediaUploadQuality: MediaUploadQuality,
    onOpenMediaQualitySheet: () -> Unit
) {
    SettingsSection(title = stringResource(R.string.settings_media_upload_quality_section)) {
        SettingsNavigationItem(
            title = stringResource(R.string.settings_photo_upload_quality_title),
            subtitle = mediaUploadQuality.displayName(),
            imageVector = Icons.Filled.Image,
            onClick = onOpenMediaQualitySheet
        )
    }
}
