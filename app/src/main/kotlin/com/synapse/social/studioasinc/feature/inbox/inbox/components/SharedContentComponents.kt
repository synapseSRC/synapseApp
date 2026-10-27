package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.chat.FileItem
import com.synapse.social.studioasinc.shared.domain.model.chat.FileType
import com.synapse.social.studioasinc.shared.domain.model.chat.LinkItem
import com.synapse.social.studioasinc.shared.domain.model.chat.MediaItem
import com.synapse.social.studioasinc.shared.domain.model.chat.MediaType
import java.text.DecimalFormat

@Composable
fun MediaGridItem(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(Sizes.CornerSmall))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = item.thumbnailUrl ?: item.url,
            contentDescription = stringResource(R.string.cd_media_attachment),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Video indicator
        if (item.mediaType == MediaType.VIDEO) {
            VideoIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(Sizes.IconExtraLarge)
            )

            // Duration badge
            item.duration?.let { duration ->
                DurationBadge(
                    duration = duration,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(Spacing.space8)
                )
            }
        }

        // Audio indicator
        if (item.mediaType == MediaType.AUDIO) {
            AudioIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(Sizes.IconExtraLarge)
            )
        }
    }
}

@Composable
private fun VideoIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = stringResource(R.string.chat_action_play_video),
            tint = Color.White,
            modifier = Modifier.size(Sizes.IconMedium)
        )
    }
}

@Composable
private fun AudioIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Headphones,
            contentDescription = stringResource(R.string.chat_action_play_audio),
            tint = Color.White,
            modifier = Modifier.size(Sizes.IconMedium)
        )
    }
}

@Composable
private fun DurationBadge(
    duration: Int,
    modifier: Modifier = Modifier
) {
    val minutes = duration / 60
    val seconds = duration % 60
    Surface(
        color = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(Sizes.CornerSmall),
        modifier = modifier
    ) {
        Text(
            text = String.format("%02d:%02d", minutes, seconds),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = Spacing.space4, vertical = Spacing.space4)
        )
    }
}

@Composable
fun LinkPreviewCard(
    item: LinkItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Sizes.CornerMedium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.space16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Link image thumbnail
            if (item.imageUrl != null) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(Sizes.CornerSmall))
                )
                Spacer(modifier = Modifier.width(Spacing.space12))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.space4)
            ) {
                val title = item.title
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val description = item.description
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.space4)
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(Sizes.IconSmall)
                    )
                    Text(
                        text = item.domain,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun FileListItem(
    item: FileItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Sizes.CornerMedium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.space16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File icon
            Surface(
                shape = RoundedCornerShape(Sizes.CornerSmall),
                color = getFileIconColor(item.fileType).copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getFileIcon(item.fileType),
                        contentDescription = null,
                        tint = getFileIconColor(item.fileType),
                        modifier = Modifier.size(Sizes.IconMedium)
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.space12))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.space4)
            ) {
                Text(
                    text = item.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.space8)
                ) {
                    Text(
                        text = formatFileSize(item.fileSize),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val duration = item.duration
                    if (duration != null) {
                        Text(
                            text = "• ${formatDuration(duration)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SharedContentEmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.space32),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.space8)
        ) {
            Icon(
                imageVector = Icons.Default.Collections,
                contentDescription = null,
                modifier = Modifier.size(Sizes.IconGiant),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SharedContentErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.space32),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.space16)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(Sizes.IconGiant),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRetry) {
                Text(stringResource(R.string.chat_info_retry))
            }
        }
    }
}

private fun getFileIcon(fileType: FileType): ImageVector {
    return when (fileType) {
        FileType.DOCUMENT -> Icons.Default.Description
        FileType.AUDIO, FileType.VOICE -> Icons.Default.AudioFile
        FileType.VIDEO -> Icons.Default.VideoFile
        FileType.IMAGE -> Icons.Default.Image
        FileType.OTHER -> Icons.Default.InsertDriveFile
    }
}

private fun getFileIconColor(fileType: FileType): Color {
    return when (fileType) {
        FileType.DOCUMENT -> Color(0xFF2196F3)
        FileType.AUDIO, FileType.VOICE -> Color(0xFF9C27B0)
        FileType.VIDEO -> Color(0xFFF44336)
        FileType.IMAGE -> Color(0xFF4CAF50)
        FileType.OTHER -> Color(0xFF9E9E9E)
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", minutes, secs)
}
