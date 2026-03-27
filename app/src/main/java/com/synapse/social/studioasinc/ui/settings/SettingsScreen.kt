package com.synapse.social.studioasinc.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

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
        containerColor = SettingsColors.screenBackground,
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
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
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
                .padding(horizontal = Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
            contentPadding = PaddingValues(vertical = Spacing.Small)
        ) {

            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    Text(
                        text = "Account",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = Spacing.SmallMedium, bottom = Spacing.ExtraSmall)
                    )
                    SettingsGroup {
                        SettingRow(
                            icon = Icons.Filled.Person,
                            title = stringResource(R.string.settings_account),
                            subtitle = stringResource(R.string.settings_account_subtitle),
                            imageUrl = currentUser?.avatar,
                            onClick = onAccountClick,
                            position = SettingsItemPosition.Top
                        )
                        SettingsDivider()
                        SettingRow(
                            icon = Icons.Filled.Shield,
                            title = stringResource(R.string.settings_privacy),
                            subtitle = stringResource(R.string.settings_privacy_subtitle),
                            onClick = onPrivacyClick,
                            position = SettingsItemPosition.Middle
                        )
                        SettingsDivider()
                        SettingRow(
                            icon = Icons.Filled.Notifications,
                            title = stringResource(R.string.settings_notifications),
                            subtitle = stringResource(R.string.settings_notifications_subtitle),
                            onClick = onNotificationsClick,
                            position = SettingsItemPosition.Bottom
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    Text(
                        text = "Security",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = Spacing.SmallMedium, bottom = Spacing.ExtraSmall)
                    )
                    SettingsGroup {
                        SettingRow(
                            icon = Icons.Filled.VpnKey,
                            title = "Reset Encryption Keys",
                            subtitle = if (isResettingE2EE) "Resetting..." else "Fix issues with encrypted messages",
                            showChevron = !isResettingE2EE,
                            onClick = { if (!isResettingE2EE) viewModel.resetE2EE() },
                            position = SettingsItemPosition.Single
                        )
                    }
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
                    shape = RoundedCornerShape(Sizes.CornerLarge)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(Sizes.IconMedium)
                    )
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    Text(stringResource(R.string.action_log_out))
                }
            }

            item {
                Spacer(modifier = Modifier.height(Spacing.Medium))
            }
        }
    }
}



@Composable
fun SecureTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    tint = if (passwordVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Sizes.CornerDefault),
        singleLine = true
    )
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    imageUrl: String? = null,
    showChevron: Boolean = true,
    position: SettingsItemPosition = SettingsItemPosition.Single,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = position.getShape(),
        color = SettingsColors.cardBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = Spacing.Medium, vertical = Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(Sizes.IconLarge)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(Sizes.IconLarge),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Spacing.Medium)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(Sizes.IconLarge),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}
