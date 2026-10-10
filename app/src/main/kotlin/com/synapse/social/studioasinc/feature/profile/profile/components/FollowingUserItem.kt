package com.synapse.social.studioasinc.feature.profile.profile.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

data class FollowingUser(
    val id: String,
    val username: String,
    val name: String,
    val avatarUrl: String?,
    val isMutual: Boolean = false
)

@Composable
fun FollowingUserItem(
    user: FollowingUser,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(Sizes.WidthLarge)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = user.name,
                modifier = Modifier
                    .size(Sizes.AvatarSemiLarge)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            if (user.isMutual) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Mutual",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(Sizes.IconDefault)
                        .align(Alignment.BottomEnd)
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.ExtraSmall))

        Text(
            text = user.username,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview
@Composable
private fun FollowingUserItemPreview() {
    MaterialTheme {
        FollowingUserItem(
            user = FollowingUser(
                id = "1",
                username = "johndoe",
                name = "John Doe",
                avatarUrl = null,
                isMutual = true
            ),
            onClick = {}
        )
    }
}
