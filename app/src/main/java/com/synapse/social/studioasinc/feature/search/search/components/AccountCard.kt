package com.synapse.social.studioasinc.feature.search.search.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.shared.domain.model.SearchAccount
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun AccountCard(
    account: SearchAccount,
    onClick: () -> Unit,
    onFollowClick: () -> Unit
) {

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
            .height(Sizes.HeightLarge)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.Medium, vertical = Spacing.SmallMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(account.avatarUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = rememberVectorPainter(Icons.Filled.Person),
            error = rememberVectorPainter(Icons.Filled.Person),
            modifier = Modifier
                .size(Sizes.IconGiant)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Spacer(modifier = Modifier.width(Spacing.Medium))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = account.displayName ?: account.handle ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (account.isVerified) {
                    Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                    Icon(
                        imageVector = Icons.Outlined.Verified,
                        contentDescription = "Verified",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Sizes.IconSemiMedium)
                    )
                }
            }
            Text(
                text = "@${account.handle ?: ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!account.bio.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                Text(
                    text = account.bio ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
            Text(
                text = "${formatCount(account.followersCount)} followers",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(Spacing.Small))


        if (account.isFollowing) {
             OutlinedButton(
                 onClick = onFollowClick,
                 modifier = Modifier.height(Sizes.AvatarSmall),
                 contentPadding = PaddingValues(horizontal = Spacing.Medium)
             ) {
                 Text(stringResource(R.string.action_following))
             }
        } else {
            Button(
                onClick = onFollowClick,
                modifier = Modifier.height(Sizes.AvatarSmall),
                contentPadding = PaddingValues(horizontal = Spacing.Medium)
            ) {
                Text(stringResource(R.string.action_follow))
            }
        }
        }
    }
}
