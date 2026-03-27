package com.synapse.social.studioasinc.feature.stories.management

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.domain.model.StoryViewWithUser
import com.synapse.social.studioasinc.domain.model.User
import java.time.Duration
import java.time.Instant
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerListSheet(
    viewers: List<StoryViewWithUser>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onUserClick: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.ExtraLarge)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(Sizes.IconLarge)
                )
                Spacer(modifier = Modifier.width(Spacing.SmallMedium))
                Text(
                    text = "Seen by ${viewers.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.Small))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Sizes.HeightExtraLarge),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                viewers.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Sizes.HeightExtraLarge),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = null,
                                modifier = Modifier.size(Sizes.IconGiant),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(Spacing.SmallMedium))
                            Text(
                                text = "No views yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = Spacing.Medium)
                    ) {
                        items(viewers) { viewWithUser ->
                            ViewerListItem(
                                viewer = viewWithUser.viewer,
                                viewedAt = viewWithUser.storyView.viewedAt,
                                onClick = { viewWithUser.viewer?.let { onUserClick(it) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewerListItem(
    viewer: User?,
    viewedAt: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = Spacing.SmallMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {

        if (viewer?.avatar != null) {
            AsyncImage(
                model = viewer.avatar,
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .then(
                        Modifier.padding(0.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = viewer?.displayName?.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.width(Spacing.SmallMedium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = viewer?.displayName ?: viewer?.username ?: "User",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            viewer?.username?.let { username ->
                Text(
                    text = "@$username",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }


        Text(
            text = formatTimeAgo(viewedAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryOptionsSheet(
    isOwnStory: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit,
    onMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.ExtraLarge)
        ) {
            if (isOwnStory) {

                ListItem(
                    headlineContent = { Text(stringResource(R.string.story_delete)) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier.clickable { onDelete() }
                )
            } else {

                ListItem(
                    headlineContent = { Text(stringResource(R.string.story_mute)) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable { onMute() }
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.story_report)) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier.clickable { onReport() }
                )
            }
        }
    }
}

private fun formatTimeAgo(timestamp: String?): String {
    if (timestamp == null) return ""

    return try {
        val instant = Instant.parse(timestamp)
        val now = Instant.now()
        val duration = Duration.between(instant, now)

        when {
            duration.toMinutes() < 1 -> "Just now"
            duration.toMinutes() < 60 -> "${duration.toMinutes()}m"
            duration.toHours() < 24 -> "${duration.toHours()}h"
            else -> "${duration.toDays()}d"
        }
    } catch (e: Exception) {
        ""
    }
}
