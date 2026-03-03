package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySecurityScreen(
    viewModel: PrivacySecurityViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToBlockedUsers: () -> Unit,
    onNavigateToMutedUsers: () -> Unit,
    onNavigateToActiveSessions: () -> Unit
) {
    val privacySettings by viewModel.privacySettings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    PrivacySecurityContent(
        privacySettings = privacySettings,
        isLoading = isLoading,
        error = error,
        onNavigateBack = onNavigateBack,
        onNavigateToBlockedUsers = onNavigateToBlockedUsers,
        onNavigateToMutedUsers = onNavigateToMutedUsers,
        onNavigateToActiveSessions = onNavigateToActiveSessions,
        onClearError = { viewModel.clearError() },
        onReadReceiptsChanged = { viewModel.setReadReceiptsEnabled(it) },
        onAppLockChanged = { viewModel.setAppLockEnabled(it) },
        onChatLockChanged = { viewModel.setChatLockEnabled(it) },
        onProfileVisibilityChanged = { viewModel.setProfileVisibility(it) },
        onContentVisibilityChanged = { viewModel.setContentVisibility(it) },
        onGroupPrivacyChanged = { viewModel.setGroupPrivacy(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySecurityContent(
    privacySettings: PrivacySettings,
    isLoading: Boolean,
    error: String?,
    onNavigateBack: () -> Unit,
    onNavigateToBlockedUsers: () -> Unit,
    onNavigateToMutedUsers: () -> Unit,
    onNavigateToActiveSessions: () -> Unit,
    onClearError: () -> Unit,
    onReadReceiptsChanged: (Boolean) -> Unit,
    onAppLockChanged: (Boolean) -> Unit,
    onChatLockChanged: (Boolean) -> Unit,
    onProfileVisibilityChanged: (ProfileVisibility) -> Unit,
    onContentVisibilityChanged: (ContentVisibility) -> Unit,
    onGroupPrivacyChanged: (GroupPrivacy) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text("Privacy & Security") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = {
            if (error != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = onClearError) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(paddingValues)
                .padding(horizontal = SettingsSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)
        ) {

            item {
                PrivacyCheckupSection(isLoading = isLoading)
            }


            item {
                ProfilePrivacySection(
                    privacySettings = privacySettings,
                    isLoading = isLoading,
                    onProfileVisibilityChanged = onProfileVisibilityChanged,
                    onContentVisibilityChanged = onContentVisibilityChanged
                )
            }


            item {
                MessagePrivacySection(
                    readReceiptsEnabled = privacySettings.readReceiptsEnabled,
                    onReadReceiptsChanged = onReadReceiptsChanged,
                    isLoading = isLoading
                )
            }


            item {
                GroupPrivacySection(
                    privacySettings = privacySettings,
                    isLoading = isLoading,
                    onGroupPrivacyChanged = onGroupPrivacyChanged
                )
            }


            item {
                SecuritySection(
                    appLockEnabled = privacySettings.appLockEnabled,
                    onAppLockChanged = onAppLockChanged,
                    chatLockEnabled = privacySettings.chatLockEnabled,
                    onChatLockChanged = onChatLockChanged,
                    isLoading = isLoading
                )
            }


            item {
                ContactsSection(
                    onNavigateToBlockedUsers = onNavigateToBlockedUsers,
                    onNavigateToMutedUsers = onNavigateToMutedUsers,
                    isLoading = isLoading
                )
            }


            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
