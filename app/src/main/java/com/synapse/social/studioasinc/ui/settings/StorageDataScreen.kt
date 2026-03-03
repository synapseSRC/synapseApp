package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
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
                title = { Text("Storage and data") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back"
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

private fun getAutoDownloadSummary(selectedTypes: Set<MediaType>): String {
    if (selectedTypes.isEmpty()) return "No media"
    if (selectedTypes.size == MediaType.values().size) return "All media"
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
            title = "When using mobile data",
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
            title = "When connected on Wi-Fi",
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
            title = "When roaming",
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
    SettingsSection(title = "Storage Management") {
        SettingsNavigationItem(
            title = "Manage storage",
            subtitle = "Free up space",
            icon = R.drawable.ic_archive,
            onClick = onNavigateToStorageManage
        )
        SettingsDivider()
        SettingsNavigationItem(
            title = "Network usage",
            subtitle = "View data usage",
            icon = R.drawable.ic_network_check,
            onClick = onNavigateToNetworkUsage
        )
    }
}

@Composable
private fun CallSettingsSection(
    useLessDataCalls: Boolean,
    onUseLessDataCallsChanged: (Boolean) -> Unit
) {
    SettingsSection(title = "Call Settings") {
        SettingsToggleItem(
            title = "Use less data for calls",
            subtitle = "Lower call quality to use less data",
            icon = R.drawable.ic_call,
            checked = useLessDataCalls,
            onCheckedChange = onUseLessDataCallsChanged
        )
    }
}

@Composable
private fun NetworkSection(onNavigateToProxy: () -> Unit) {
    SettingsSection(title = "Network") {
        SettingsNavigationItem(
            title = "Proxy",
            subtitle = "Configure proxy settings",
            icon = R.drawable.ic_network_check,
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
    SettingsSection(title = "Media auto-download") {
        SettingsNavigationItem(
            title = "When using mobile data",
            subtitle = getAutoDownloadSummary(autoDownloadRules.mobileData),
            icon = R.drawable.ic_phone,
            onClick = onOpenMobileDialog
        )
        SettingsDivider()
        SettingsNavigationItem(
            title = "When connected on Wi-Fi",
            subtitle = getAutoDownloadSummary(autoDownloadRules.wifi),
            icon = R.drawable.ic_network_check,
            onClick = onOpenWifiDialog
        )
        SettingsDivider()
        SettingsNavigationItem(
            title = "When roaming",
            subtitle = getAutoDownloadSummary(autoDownloadRules.roaming),
            icon = R.drawable.ic_phone,
            onClick = onOpenRoamingDialog
        )
    }
}

@Composable
private fun MediaUploadQualitySection(
    mediaUploadQuality: MediaUploadQuality,
    onOpenMediaQualitySheet: () -> Unit
) {
    SettingsSection(title = "Media upload quality") {
        SettingsNavigationItem(
            title = "Photo upload quality",
            subtitle = mediaUploadQuality.displayName(),
            icon = R.drawable.ic_image,
            onClick = onOpenMediaQualitySheet
        )
    }
}
