package com.synapse.social.studioasinc.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun PostHeader(
    username: String,
    avatarUrl: String?,
    isVerified: Boolean,
    timestamp: String,
    onUserClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.SmallMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(Sizes.AvatarMedium)
                .clickable(onClick = onUserClick)
        )

        Spacer(modifier = Modifier.width(Spacing.SmallMedium))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = username,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (isVerified) {
                    Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = stringResource(R.string.cd_selected),
                        modifier = Modifier.size(Sizes.IconSemiMedium),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = timestamp,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.cd_more_options)
            )
        }
    }
}

@Preview
@Composable
private fun PostHeaderPreview() {
    MaterialTheme {
        PostHeader(
            username = "john_doe",
            avatarUrl = null,
            isVerified = true,
            timestamp = "2 hours ago",
            onUserClick = {},
            onMenuClick = {}
        )
    }
}
