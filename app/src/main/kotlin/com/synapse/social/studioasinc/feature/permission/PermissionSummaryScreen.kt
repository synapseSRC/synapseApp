package com.synapse.social.studioasinc.feature.permission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.data.permission.PermissionSnapshot
import com.synapse.social.studioasinc.data.permission.PermissionStatus
import com.synapse.social.studioasinc.feature.permission.components.PermissionStatusChip
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.ui.settings.SettingsCard
import com.synapse.social.studioasinc.feature.shared.theme.AppColors
import com.synapse.social.studioasinc.ui.settings.SettingsIconBadge
import com.synapse.social.studioasinc.feature.shared.theme.AppSpacing
import com.synapse.social.studioasinc.feature.shared.theme.AppTypography

/**
 * The last step of the onboarding: what the user chose, in plain terms, before the app opens.
 *
 * It requires no grants, re-reads every status when shown, and always leaves a way into the permission
 * manager, which stays available in Settings afterwards.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PermissionSummaryScreen(
    state: PermissionOnboardingUiState,
    onBack: () -> Unit,
    onManagePermissions: () -> Unit,
    onContinue: () -> Unit
) {
    val grantedCount = state.permissions.count { it.status == PermissionStatus.GRANTED }

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.permission_summary_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_button)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = AppSpacing.screenPadding,
                end = AppSpacing.screenPadding,
                top = Spacing.space8,
                bottom = Spacing.space24
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.space12)
        ) {
            item {
                Column {
                    Text(
                        text = stringResource(R.string.permission_summary_headline),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.height(Spacing.space8))
                    Text(
                        text = stringResource(R.string.permission_summary_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(Spacing.space12))
                    Text(
                        text = stringResource(
                            R.string.permission_summary_count,
                            grantedCount,
                            state.permissions.size
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            items(state.permissions, key = { it.permission.name }) { snapshot ->
                PermissionSummaryRow(snapshot = snapshot)
            }

            item {
                Spacer(Modifier.height(Spacing.space4))
                OutlinedButton(
                    onClick = onManagePermissions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Sizes.ButtonHeightLarge)
                ) {
                    Text(
                        text = stringResource(R.string.permission_summary_manage),
                        style = AppTypography.buttonText
                    )
                }
                Spacer(Modifier.height(Spacing.space8))
                Button(
                    onClick = onContinue,
                    enabled = !state.isCompleting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Sizes.ButtonHeightLarge)
                ) {
                    Text(
                        text = stringResource(R.string.permission_summary_continue),
                        style = AppTypography.buttonText
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionSummaryRow(snapshot: PermissionSnapshot) {
    val permission = snapshot.permission
    val status = snapshot.status

    SettingsCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.itemPadding),
            verticalAlignment = Alignment.Top
        ) {
            SettingsIconBadge(imageVector = permission.icon())
            Spacer(Modifier.width(AppSpacing.iconTextSpacing))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(permission.titleRes()),
                    style = AppTypography.itemTitle,
                    color = AppColors.itemContent
                )
                Spacer(Modifier.height(Spacing.space8))
                PermissionStatusChip(status = status)
                if (status != PermissionStatus.GRANTED) {
                    Spacer(Modifier.height(Spacing.space8))
                    Text(
                        text = stringResource(permission.impactRes()),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.supportingText
                    )
                }
            }
        }
    }
}
