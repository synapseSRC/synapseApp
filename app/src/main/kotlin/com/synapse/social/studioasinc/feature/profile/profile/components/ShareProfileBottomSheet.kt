package com.synapse.social.studioasinc.feature.profile.profile.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareProfileBottomSheet(
    onDismiss: () -> Unit,
    onCopyLink: () -> Unit,
    onShareToStory: () -> Unit,
    onShareViaMessage: () -> Unit,
    onShareExternal: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = Spacing.Medium)) {
            Text(
                stringResource(R.string.share_profile),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
            )

            ShareOption(
                icon = Icons.Default.ContentCopy,
                text = stringResource(R.string.copy_link),
                onClick = { onCopyLink(); onDismiss() }
            )
            ShareOption(
                icon = Icons.Default.AutoStories,
                text = stringResource(R.string.share_to_story),
                onClick = { onShareToStory(); onDismiss() }
            )
            ShareOption(
                icon = Icons.AutoMirrored.Filled.Message,
                text = stringResource(R.string.share_via_message),
                onClick = { onShareViaMessage(); onDismiss() }
            )
            ShareOption(
                icon = Icons.Default.Share,
                text = stringResource(R.string.share_to_external_apps),
                onClick = { onShareExternal(); onDismiss() }
            )
        }
    }
}

@Composable
private fun ShareOption(
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
