package com.synapse.social.studioasinc.feature.shared.components.picker

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun MediaGridContent(
    mediaItems: List<PickedFile>,
    selectedUris: LinkedHashSet<Uri>,
    isLoading: Boolean,
    maxSelection: Int,
    mediaFilter: MediaFilter,
    onFilterChanged: (MediaFilter) -> Unit,
    onFileClicked: (PickedFile) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(3) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.Small, vertical = Spacing.ExtraSmall),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                MediaFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = mediaFilter == filter,
                        onClick = { onFilterChanged(filter) },
                        label = {
                            Text(
                                text = when (filter) {
                                    MediaFilter.ALL -> stringResource(R.string.picker_filter_all)
                                    MediaFilter.IMAGES -> stringResource(R.string.picker_filter_images)
                                    MediaFilter.VIDEOS -> stringResource(R.string.picker_filter_videos)
                                },
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }
        }

        if (mediaItems.isEmpty()) {
            item(span = { GridItemSpan(3) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.Huge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.Small)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(Sizes.IconGiant),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.picker_empty_media),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(mediaItems, key = { it.uri.toString() }) { file ->
                MediaGridItem(
                    file = file,
                    selectionIndex = selectedUris.indexOf(file.uri),
                    maxSelection = maxSelection,
                    onClick = { onFileClicked(file) }
                )
            }
        }
    }
}

private fun LinkedHashSet<Uri>.indexOf(uri: Uri): Int {
    var index = 0
    for (item in this) {
        if (item == uri) return index
        index++
    }
    return -1
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
fun MediaGridItem(
    file: PickedFile,
    selectionIndex: Int,
    maxSelection: Int,
    onClick: () -> Unit
) {
    val isSelected = selectionIndex >= 0

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(Sizes.BorderThin)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = file.uri,
            contentDescription = file.fileName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Video play icon (bottom-start)
        if (file.mimeType.startsWith("video/")) {
            Icon(
                imageVector = Icons.Default.PlayCircleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(Spacing.Small)
                    .size(Sizes.IconLarge)
            )
        }

        // Video duration badge (bottom-end)
        file.duration?.let { ms ->
            Text(
                text = formatDuration(ms),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Spacing.ExtraSmall)
                    .background(
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(Sizes.CornerSmall)
                    )
                    .padding(horizontal = Spacing.ExtraSmall, vertical = Spacing.Tiny)
            )
        }

        // Selection badge (top-end)
        if (maxSelection > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Spacing.Small)
                    .size(Sizes.IconLarge)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)
                    )
                    .border(Sizes.BorderThin, MaterialTheme.colorScheme.onPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text(
                        text = (selectionIndex + 1).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        } else if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
            )
        }
    }
}
