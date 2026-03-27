package com.synapse.social.studioasinc.feature.profile.profile.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileMoreMenuBottomSheet(
    isOwnProfile: Boolean,
    onDismiss: () -> Unit,
    onShareProfile: () -> Unit,
    onViewAs: () -> Unit,
    onLockProfile: () -> Unit,
    onArchiveProfile: () -> Unit,
    onQrCode: () -> Unit,
    onCopyLink: () -> Unit,
    onSettings: () -> Unit,
    onActivityLog: () -> Unit,
    onBlockUser: () -> Unit,
    onReportUser: () -> Unit,
    onMuteUser: () -> Unit
) {
    val context = LocalContext.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = Spacing.Medium)) {
            MenuOption(
                icon = Icons.Default.Share,
                text = stringResource(R.string.share_profile),
                onClick = { onShareProfile(); onDismiss() }
            )

            if (isOwnProfile) {
                MenuOption(
                    icon = Icons.Default.Visibility,
                    text = stringResource(R.string.view_as),
                    onClick = { onViewAs(); onDismiss() }
                )

                MenuOption(
                    icon = Icons.Default.Lock,
                    text = stringResource(R.string.lock_profile),
                    onClick = { onLockProfile(); onDismiss() }
                )
                MenuOption(
                    icon = Icons.Default.Archive,
                    text = stringResource(R.string.archive_profile),
                    onClick = { onArchiveProfile(); onDismiss() }
                )
            }

            MenuOption(
                icon = Icons.Default.QrCode,
                text = stringResource(R.string.qr_code),
                onClick = { onQrCode(); onDismiss() }
            )
            MenuOption(
                icon = Icons.Default.ContentCopy,
                text = stringResource(R.string.copy_profile_link),
                onClick = { onCopyLink(); onDismiss() }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.Small))

            if (isOwnProfile) {
                MenuOption(
                    icon = Icons.Default.Settings,
                    text = stringResource(R.string.settings),
                    onClick = {
                        onDismiss()
                        onSettings()
                    }
                )
                MenuOption(
                    icon = Icons.Default.History,
                    text = stringResource(R.string.activity_log),
                    onClick = {
                        onDismiss()
                        onActivityLog()
                    }
                )
            } else {
                MenuOption(
                    icon = Icons.Default.Block,
                    text = stringResource(R.string.block_user_menu),
                    onClick = { onBlockUser(); onDismiss() }
                )
                MenuOption(
                    icon = Icons.Default.Report,
                    text = stringResource(R.string.report_user_menu),
                    onClick = { onReportUser(); onDismiss() }
                )
                MenuOption(
                    icon = Icons.AutoMirrored.Filled.VolumeOff,
                    text = stringResource(R.string.mute_user),
                    onClick = { onMuteUser(); onDismiss() }
                )
            }
        }
    }
}

@Composable
private fun MenuOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.Medium, vertical = Spacing.SmallMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(Sizes.IconLarge))
        Spacer(modifier = Modifier.width(Spacing.Medium))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
