package com.synapse.social.studioasinc.feature.blocking.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.shared.core.network.SupabaseClient
import com.synapse.social.studioasinc.shared.domain.model.BlockedUser
import kotlin.time.Clock

/**
 * Composable that displays a single blocked user item.
 * Shows the blocked account's avatar, name, and an unblock button.
 * 
 * The name and the picture come from the profile that is loaded together with the block, so the
 * row identifies the real account. The generic placeholder is a deliberate fallback for the case
 * where the profile genuinely could not be retrieved.
 * 
 * Follows Material Theme guidelines with no hardcoded values.
 * 
 * @param blockedUser The blocked user to display
 * @param onUnblockClick Callback when unblock button is clicked
 * @param modifier Optional modifier for the composable
 */
@Composable
fun BlockedUserItem(
    blockedUser: BlockedUser,
    onUnblockClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = blockedUser.blockedDisplayName?.takeIf { it.isNotBlank() }
        ?: blockedUser.blockedUsername?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.no_user_data_found)

    // Stored avatars are often a storage path rather than a full URL, so resolve them the same way
    // the conversation header does before handing them to the image loader.
    val avatarUrl = remember(blockedUser.blockedUserAvatar) {
        blockedUser.blockedUserAvatar?.takeIf { it.isNotBlank() }?.let { avatar ->
            if (avatar.startsWith("http")) avatar else SupabaseClient.constructAvatarUrl(avatar)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = Spacing.space16,
                vertical = Spacing.space8
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User Avatar
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = displayName,
                modifier = Modifier
                    .size(Sizes.AvatarDefault)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(Sizes.AvatarDefault)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Sizes.IconDefault)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(Spacing.space16))
        
        // Display name
        Text(
            text = displayName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        
        Spacer(modifier = Modifier.width(Spacing.space8))
        
        // Unblock Button
        OutlinedButton(
            onClick = onUnblockClick,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            ),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                width = Sizes.BorderThin
            )
        ) {
            Text(
                text = stringResource(R.string.unblock),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BlockedUserItemPreview() {
    MaterialTheme {
        BlockedUserItem(
            blockedUser = BlockedUser(
                id = "1",
                blockedUserId = "user123",
                blockedUsername = "johndoe",
                blockedUserAvatar = null,
                blockedAt = Clock.System.now(),
                blockedDisplayName = "John Doe"
            ),
            onUnblockClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BlockedUserItemLongUsernamePreview() {
    MaterialTheme {
        BlockedUserItem(
            blockedUser = BlockedUser(
                id = "2",
                blockedUserId = "user456",
                blockedUsername = "verylongusernamethatshouldbetrimmed",
                blockedUserAvatar = null,
                blockedAt = Clock.System.now(),
                blockedDisplayName = "Very Long Username That Should Be Trimmed"
            ),
            onUnblockClick = {}
        )
    }
}
