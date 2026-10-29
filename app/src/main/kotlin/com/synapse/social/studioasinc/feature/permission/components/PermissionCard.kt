package com.synapse.social.studioasinc.feature.permission.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.data.permission.AppPermission
import com.synapse.social.studioasinc.data.permission.PermissionSnapshot
import com.synapse.social.studioasinc.data.permission.PermissionStatus
import com.synapse.social.studioasinc.feature.permission.benefitRes
import com.synapse.social.studioasinc.feature.permission.hintRes
import com.synapse.social.studioasinc.feature.permission.icon
import com.synapse.social.studioasinc.feature.permission.purposeRes
import com.synapse.social.studioasinc.feature.permission.titleRes
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.ui.settings.SettingsCard
import com.synapse.social.studioasinc.feature.shared.theme.AppColors
import com.synapse.social.studioasinc.ui.settings.SettingsIconBadge
import com.synapse.social.studioasinc.feature.shared.theme.AppShapes
import com.synapse.social.studioasinc.feature.shared.theme.AppSpacing
import com.synapse.social.studioasinc.feature.shared.theme.AppTypography

/**
 * One permission, its live state and the single action the state allows.
 *
 * The action is decided by the status rather than by the caller: a granted permission offers nothing, a
 * denial can be retried, and a blocked or restricted one can only be changed in system Settings.
 */
@Composable
internal fun PermissionCard(
    snapshot: PermissionSnapshot,
    isRequestPending: Boolean,
    onAction: (AppPermission) -> Unit,
    modifier: Modifier = Modifier
) {
    val permission = snapshot.permission
    val status = snapshot.status
    val permissionTitle = stringResource(permission.titleRes())

    SettingsCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.itemPadding)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                SettingsIconBadge(imageVector = permission.icon())
                Spacer(Modifier.width(AppSpacing.iconTextSpacing))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(permission.titleRes()),
                        style = AppTypography.itemTitle,
                        color = AppColors.itemContent
                    )
                    Spacer(Modifier.height(Spacing.space4))
                    Text(
                        text = stringResource(permission.purposeRes()),
                        style = AppTypography.itemSubtitle,
                        color = AppColors.supportingText
                    )
                }
            }

            Spacer(Modifier.height(Spacing.space12))

            Text(
                text = stringResource(permission.benefitRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.itemContent
            )

            status.hintRes()?.let { hint ->
                Spacer(Modifier.height(Spacing.space4))
                Text(
                    text = stringResource(hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.supportingText
                )
            }

            Spacer(Modifier.height(Spacing.space12))

            PermissionStatusChip(status = status)

            if (status != PermissionStatus.GRANTED) {
                Spacer(Modifier.height(Spacing.space8))
                PermissionActionButton(
                    status = status,
                    permissionTitle = permissionTitle,
                    enabled = !isRequestPending,
                    onClick = { onAction(permission) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PermissionActionButton(
    status: PermissionStatus,
    permissionTitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = when (status) {
        PermissionStatus.NOT_REQUESTED -> R.string.permission_action_allow
        PermissionStatus.DENIED -> R.string.permission_action_try_again
        PermissionStatus.PERMANENTLY_DENIED, PermissionStatus.RESTRICTED ->
            R.string.permission_action_open_settings
        PermissionStatus.GRANTED -> return
    }

    val labelText = stringResource(label)
    val description = stringResource(R.string.permission_action_accessibility, labelText, permissionTitle)
    val buttonModifier = modifier
        .height(Sizes.ButtonHeightLarge)
        .semantics { contentDescription = description }

    when (status) {
        PermissionStatus.NOT_REQUESTED, PermissionStatus.DENIED -> Button(
            onClick = onClick,
            enabled = enabled,
            modifier = buttonModifier,
            shape = AppShapes.chipShape
        ) {
            Text(text = labelText, style = AppTypography.buttonText)
        }
        PermissionStatus.PERMANENTLY_DENIED, PermissionStatus.RESTRICTED -> OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = buttonModifier,
            shape = AppShapes.chipShape
        ) {
            Text(text = labelText, style = AppTypography.buttonText)
        }
        PermissionStatus.GRANTED -> Unit
    }
}
