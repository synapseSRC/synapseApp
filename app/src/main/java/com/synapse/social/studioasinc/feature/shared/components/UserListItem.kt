package com.synapse.social.studioasinc.feature.shared.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.shared.domain.model.User
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

/**
 * Example usage of UserAvatarWithStatus in a list item
 */
@Composable
fun UserListItem(
    user: User,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatarWithStatus(
            userId = user.uid,
            avatarUrl = user.avatar,
            size = Sizes.AvatarDefault,
            showActiveStatus = true
        )
        
        Spacer(modifier = Modifier.width(Spacing.SmallMedium))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName ?: user.username ?: "Unknown",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            user.bio?.let { bioText ->
                Text(
                    text = bioText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
