package com.synapse.social.studioasinc.feature.shared.components.picker

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun MediaGridContent(
    mediaItems: List<PickedFile>,
    selectedUris: Set<Uri>,
    isLoading: Boolean,
    maxSelection: Int,
    onFileClicked: (PickedFile) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = modifier.fillMaxSize()
        ) {
            items(mediaItems, key = { it.uri.toString() }) { file ->
                val isSelected = selectedUris.contains(file.uri)
                MediaGridItem(
                    file = file,
                    isSelected = isSelected,
                    maxSelection = maxSelection,
                    onClick = { onFileClicked(file) }
                )
            }
        }
    }
}

@Composable
fun MediaGridItem(
    file: PickedFile,
    isSelected: Boolean,
    maxSelection: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = file.uri,
            contentDescription = file.fileName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (file.mimeType.startsWith("video/")) {
            Icon(
                imageVector = Icons.Default.PlayCircleOutline,
                contentDescription = "Video",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(Spacing.Small)
            )
        }

        if (maxSelection > 1) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Spacing.Small)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.3f))
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            )
        }
    }
}
