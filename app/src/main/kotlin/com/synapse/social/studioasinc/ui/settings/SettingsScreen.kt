package com.synapse.social.studioasinc.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import coil3.compose.AsyncImage
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.AppColors
import com.synapse.social.studioasinc.feature.shared.theme.AppShapes
import com.synapse.social.studioasinc.feature.shared.theme.AppSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    onAccountClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val storageConfig by viewModel.storageConfig.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isResettingE2EE by viewModel.isResettingE2EE.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = AppColors.screenBackground,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.screenBackground,
                    scrolledContainerColor = AppColors.cardBackground,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.space16),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionSpacing),
            contentPadding = PaddingValues(vertical = Spacing.space8)
        ) {
            item {
                SettingsSection(title = stringResource(R.string.settings_account_short)) {
                    SettingRow(
                        icon = Icons.Filled.Person,
                        title = stringResource(R.string.settings_account),
                        subtitle = stringResource(R.string.settings_account_subtitle),
                        imageUrl = currentUser?.avatar,
                        onClick = onAccountClick,
                        position = SettingsItemPosition.Top
                    )
                    SettingRow(
                        icon = Icons.Filled.Shield,
                        title = stringResource(R.string.settings_privacy),
                        subtitle = stringResource(R.string.settings_privacy_subtitle),
                        onClick = onPrivacyClick,
                        position = SettingsItemPosition.Middle
                    )
                    SettingRow(
                        icon = Icons.Filled.Notifications,
                        title = stringResource(R.string.settings_notifications),
                        subtitle = stringResource(R.string.settings_notifications_subtitle),
                        onClick = onNotificationsClick,
                        position = SettingsItemPosition.Bottom
                    )
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.settings_security_short)) {
                    SettingRow(
                        icon = Icons.Filled.VpnKey,
                        title = stringResource(R.string.settings_reset_encryption_keys_title),
                        subtitle = if (isResettingE2EE) stringResource(R.string.settings_resetting) else stringResource(R.string.settings_reset_encryption_keys_subtitle),
                        showChevron = !isResettingE2EE,
                        enabled = !isResettingE2EE,
                        onClick = { viewModel.resetE2EE() },
                        position = SettingsItemPosition.Single
                    )
                }
            }

            item {
                Button(
                    onClick = onLogoutClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = AppShapes.itemShape
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(Sizes.IconMedium)
                    )
                    Spacer(modifier = Modifier.width(Spacing.space8))
                    Text(stringResource(R.string.action_log_out))
                }
            }

            item {
                Spacer(modifier = Modifier.height(Spacing.space16))
            }
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector?,
    title: String,
    subtitle: String?,
    imageUrl: String? = null,
    showChevron: Boolean = true,
    enabled: Boolean = true,
    position: SettingsItemPosition = SettingsItemPosition.Single,
    onClick: () -> Unit
) {
    SettingsListItem(
        title = title,
        subtitle = subtitle,
        position = position,
        enabled = enabled,
        onClick = onClick,
        leadingContent = {
            if (imageUrl != null) {
                Surface(
                    modifier = Modifier.size(AppSpacing.iconBadgeSize),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            } else if (icon != null) {
                SettingsIconBadge(imageVector = icon)
            }
        },
        trailingContent = if (showChevron) {
            { SettingsNavigationChevron() }
        } else {
            null
        }
    )
}
