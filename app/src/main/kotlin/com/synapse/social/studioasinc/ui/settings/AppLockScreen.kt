package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockScreen(
    viewModel: PrivacySecurityViewModel,
    onNavigateBack: () -> Unit
) {
    val privacySettings by viewModel.privacySettings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    AppLockContent(
        appLockEnabled = privacySettings.appLockEnabled,
        isLoading = isLoading,
        error = error,
        onNavigateBack = onNavigateBack,
        onClearError = { viewModel.clearError() },
        onToggleAppLock = { targetState ->
            val activity = context as? FragmentActivity
            if (activity != null) {
                viewModel.toggleAppLock(activity, targetState)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockContent(
    appLockEnabled: Boolean,
    isLoading: Boolean,
    error: String?,
    onNavigateBack: () -> Unit,
    onClearError: () -> Unit,
    onToggleAppLock: (Boolean) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.settings_app_lock_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_description)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = {
            if (error != null) {
                Snackbar(
                    modifier = Modifier.padding(Spacing.Medium),
                    action = {
                        TextButton(onClick = onClearError) {
                            Text(stringResource(R.string.action_dismiss))
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
                SettingsSection(title = stringResource(R.string.settings_security_section)) {
                    SettingsToggleItem(
                        title = stringResource(R.string.settings_app_lock_toggle_title),
                        subtitle = stringResource(R.string.settings_app_lock_description),
                        imageVector = Icons.Filled.Lock,
                        checked = appLockEnabled,
                        onCheckedChange = { targetState -> onToggleAppLock(targetState) },
                        enabled = !isLoading,
                        position = SettingsItemPosition.Single
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(Spacing.Large))
            }
        }
    }
}
