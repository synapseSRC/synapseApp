package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.AppColors
import com.synapse.social.studioasinc.feature.shared.theme.AppShapes
import com.synapse.social.studioasinc.feature.shared.theme.AppSpacing
import com.synapse.social.studioasinc.feature.shared.theme.AppTypography
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.auth.Passkey
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasskeysScreen(
    viewModel: PasskeysViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var passkeyPendingRemoval by remember { mutableStateOf<Passkey?>(null) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = AppColors.screenBackground,
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.settings_passkeys_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_button)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = {
            if (uiState.error != null) {
                Snackbar(
                    modifier = Modifier.padding(Spacing.space16),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text(stringResource(R.string.action_dismiss))
                        }
                    }
                ) {
                    Text(uiState.error ?: "")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = AppSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionSpacing)
        ) {
            when {
                uiState.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.space64),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                uiState.passkeys.isEmpty() -> {
                    item {
                        EmptyPasskeysState()
                    }
                }

                else -> {
                    item {
                        SettingsSection(title = stringResource(R.string.settings_your_passkeys_title)) {
                            uiState.passkeys.forEachIndexed { index, passkey ->
                                PasskeyItem(
                                    passkey = passkey,
                                    isRemoving = passkey.id in uiState.removingIds,
                                    isEnabled = !uiState.isAdding && uiState.removingIds.isEmpty(),
                                    position = when {
                                        uiState.passkeys.size == 1 -> SettingsItemPosition.Single
                                        index == 0 -> SettingsItemPosition.Top
                                        index == uiState.passkeys.lastIndex -> SettingsItemPosition.Bottom
                                        else -> SettingsItemPosition.Middle
                                    },
                                    onRemoveClick = { passkeyPendingRemoval = passkey }
                                )
                            }
                        }
                    }
                }
            }

            item {
                AddPasskeyButton(
                    isAdding = uiState.isAdding,
                    enabled = !uiState.isAdding && !uiState.isLoading,
                    onClick = { viewModel.addPasskey() }
                )
            }

            item {
                Spacer(modifier = Modifier.height(Sizes.HeightSmall))
            }
        }
    }

    if (passkeyPendingRemoval != null) {
        AlertDialog(
            onDismissRequest = { passkeyPendingRemoval = null },
            title = { Text(stringResource(R.string.settings_passkey_delete_confirm_title)) },
            text = { Text(stringResource(R.string.settings_passkey_delete_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        passkeyPendingRemoval?.let { viewModel.removePasskey(it.id) }
                        passkeyPendingRemoval = null
                    }
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { passkeyPendingRemoval = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun EmptyPasskeysState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.space48),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Key,
            contentDescription = null,
            modifier = Modifier.size(Sizes.EmptyStateIcon),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(Spacing.space16))
        Text(
            text = stringResource(R.string.settings_no_passkeys_title),
            style = AppTypography.itemTitle,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(Spacing.space8))
        Text(
            text = stringResource(R.string.settings_passkeys_description),
            style = AppTypography.itemSubtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PasskeyItem(
    passkey: Passkey,
    isRemoving: Boolean,
    isEnabled: Boolean,
    position: SettingsItemPosition,
    onRemoveClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = position.getShape(),
        color = AppColors.cardBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppSpacing.itemHorizontalPadding,
                    vertical = AppSpacing.itemVerticalPadding
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsIconBadge(imageVector = Icons.Filled.Key)
            Spacer(modifier = Modifier.width(AppSpacing.iconTextSpacing))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = passkey.deviceName,
                    style = AppTypography.itemTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Spacing.space4))
                Text(
                    text = stringResource(
                        R.string.settings_passkey_added_date,
                        formatAddedDate(passkey.dateAdded)
                    ),
                    style = AppTypography.itemSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isRemoving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Sizes.IconSmall),
                    strokeWidth = Sizes.ProgressIndicatorStrokeWidth,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                IconButton(
                    onClick = onRemoveClick,
                    enabled = isEnabled
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.settings_passkey_delete_description),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun AddPasskeyButton(
    isAdding: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = AppSpacing.itemHorizontalPadding,
                vertical = AppSpacing.itemVerticalPadding
            ),
        shape = AppShapes.itemShape,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        if (isAdding) {
            CircularProgressIndicator(
                modifier = Modifier.size(Sizes.IconSmall),
                strokeWidth = Sizes.ProgressIndicatorStrokeWidth,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(Spacing.space8))
        }
        Text(
            text = stringResource(R.string.action_add_passkey),
            style = AppTypography.buttonText
        )
    }
}

private fun formatAddedDate(dateAdded: Long): String {
    val millis = if (dateAdded > 0 && dateAdded < 1_000_000_000_000L) dateAdded * 1000L else dateAdded
    return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Date(millis))
}
