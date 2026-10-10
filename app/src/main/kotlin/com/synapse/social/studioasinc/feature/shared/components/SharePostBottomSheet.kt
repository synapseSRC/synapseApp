package com.synapse.social.studioasinc.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePostBottomSheet(
    onDismiss: () -> Unit,
    onCopyLink: () -> Unit,
    onShareToStory: () -> Unit,
    onShareViaMessage: () -> Unit,
    onShareExternal: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.Medium)
        ) {
            Text(
                text = stringResource(R.string.share_post),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
            )

            ShareOption(
                icon = Icons.Default.Link,
                text = stringResource(R.string.copy_link),
                onClick = {
                    onCopyLink()
                    onDismiss()
                }
            )

            ShareOption(
                icon = Icons.Default.AddCircle,
                text = stringResource(R.string.share_to_story),
                onClick = {
                    onShareToStory()
                    onDismiss()
                }
            )

            ShareOption(
                icon = Icons.AutoMirrored.Filled.Send,
                text = stringResource(R.string.share_via_message),
                onClick = {
                    onShareViaMessage()
                    onDismiss()
                }
            )

            ShareOption(
                icon = Icons.Default.Share,
                text = stringResource(R.string.share_to_other_apps),
                onClick = {
                    onShareExternal()
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(Spacing.Medium))
        }
    }
}

@Composable
private fun ShareOption(
    icon: ImageVector,
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(Sizes.IconLarge)
        )
        Spacer(modifier = Modifier.width(Spacing.Medium))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
