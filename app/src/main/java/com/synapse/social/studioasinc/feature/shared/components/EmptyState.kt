package com.synapse.social.studioasinc.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.ExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(Sizes.AvatarExtraLarge),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(Spacing.Medium))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.Small))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(Spacing.Large))
            Button(onClick = onActionClick) {
                Text(actionText)
            }
        }
    }
}

@Composable
fun EmptyPostsState(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Default.PostAdd,
        title = stringResource(R.string.empty_posts_title),
        message = stringResource(R.string.empty_posts_message),
        modifier = modifier
    )
}

@Composable
fun EmptyPhotosState(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Default.PhotoLibrary,
        title = stringResource(R.string.empty_photos_title),
        message = stringResource(R.string.empty_photos_message),
        modifier = modifier
    )
}

@Composable
fun EmptyReelsState(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Default.VideoLibrary,
        title = stringResource(R.string.empty_reels_title),
        message = stringResource(R.string.empty_reels_message),
        modifier = modifier
    )
}

@Composable
fun EmptyFollowingState(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Default.PersonAdd,
        title = stringResource(R.string.empty_following_title),
        message = stringResource(R.string.empty_following_message),
        modifier = modifier
    )
}
