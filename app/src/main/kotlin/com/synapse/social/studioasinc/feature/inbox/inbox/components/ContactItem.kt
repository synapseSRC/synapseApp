package com.synapse.social.studioasinc.feature.inbox.inbox.components
import com.synapse.social.studioasinc.R
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.shared.domain.model.UserStatus
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.StatusOnline

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactItem(
    contact: User,
    onContactClick: (User) -> Unit,
    onContactLongClick: (User) -> Unit = {},
    onCallClick: (User, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onContactClick(contact) },
                onLongClick = { onContactLongClick(contact) }
            )
            .padding(horizontal = Spacing.Medium, vertical = Spacing.SmallMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AsyncImage(
                model = contact.avatar,
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(Sizes.AvatarMedium)
                    .clip(CircleShape)
            )

            if (contact.status == UserStatus.ONLINE) {
                Box(
                    modifier = Modifier
                        .size(Sizes.IconSmall)
                        .clip(CircleShape)
                        .background(StatusOnline)
                        .align(Alignment.BottomEnd)
                )
            }
        }

        Spacer(modifier = Modifier.width(Spacing.Medium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.displayName ?: contact.username ?: "Unknown User",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = stringResource(R.string.common_at_username, contact.username ?: stringResource(R.string.common_unknown)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = { onContactClick(contact) }) {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = "Message",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        IconButton(onClick = { onCallClick(contact, false) }) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = "Audio Call",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        IconButton(onClick = { onCallClick(contact, true) }) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "Video Call",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
