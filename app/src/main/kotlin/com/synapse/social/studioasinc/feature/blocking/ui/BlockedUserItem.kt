package com.synapse.social.studioasinc.feature.blocking.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.shared.domain.model.BlockedUser
import kotlinx.datetime.Clock

/**
 * Composable that displays a single blocked user item.
 * Shows user avatar, username, and an unblock button.
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = Spacing.Medium,
                vertical = Spacing.Small
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User Avatar
        AsyncImage(
            model = blockedUser.blockedUserAvatar,
            contentDescription = blockedUser.blockedUsername ?: stringResource(R.string.no_user_data_found),
            modifier = Modifier
                .size(Sizes.AvatarDefault)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(Spacing.Medium))
        
        // Username
        Text(
            text = blockedUser.blockedUsername ?: stringResource(R.string.no_user_data_found),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        
        Spacer(modifier = Modifier.width(Spacing.Small))
        
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
                blockedAt = Clock.System.now()
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
                blockedAt = Clock.System.now()
            ),
            onUnblockClick = {}
        )
    }
}
