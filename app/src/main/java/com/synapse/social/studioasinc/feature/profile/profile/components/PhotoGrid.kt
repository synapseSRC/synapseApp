package com.synapse.social.studioasinc.feature.profile.profile.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.synapse.social.studioasinc.ui.components.shimmer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import kotlinx.coroutines.delay
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

data class MediaItem(
    val id: String,
    val url: String,
    val isVideo: Boolean = false,
    val isMultiple: Boolean = false,
    val thumbnailUrl: String? = null
)



@Composable
fun PhotoGrid(
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
    columns: Int = 3,
    spacing: Float = 2f
) {
    if (items.isEmpty() && !isLoading) {
        PhotoGridEmptyState(modifier = modifier)
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = modifier.heightIn(max = 2000.dp),
            contentPadding = PaddingValues(spacing.dp),
            horizontalArrangement = Arrangement.spacedBy(spacing.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.dp)
        ) {
            itemsIndexed(
                items = items,
                key = { index, item -> "${item.id}_${index}" }
            ) { index, item ->
                AnimatedGridItem(
                    item = item,
                    onClick = { onItemClick(item) },
                    animationDelay = (index % 9) * 50
                )
            }


        }
    }
}



@Composable
private fun AnimatedGridItem(
    item: MediaItem,
    onClick: () -> Unit,
    animationDelay: Int = 0
) {
    var visible by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    var imageLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible && imageLoaded) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "itemAlpha"
    )

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.95f
            visible -> 1f
            else -> 0.8f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "itemScale"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .scale(scale)
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(Sizes.CornerSmall))
            .clickable { onClick() }
    ) {

        AsyncImage(
            model = item.thumbnailUrl ?: item.url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            onState = { state ->
                imageLoaded = state is AsyncImagePainter.State.Success
            }
        )




        if (item.isVideo) {
            VideoIndicator(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Spacing.Small)
            )
        }


        if (item.isMultiple) {
            MultipleMediaIndicator(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Spacing.Small)
            )
        }


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Spacing.ExtraLarge)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)
                        )
                    )
                )
        )
    }
}



@Composable
private fun VideoIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(Sizes.IconLarge)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Video",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(Sizes.IconSmall)
        )
    }
}



@Composable
private fun MultipleMediaIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(Sizes.IconLarge)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Collections,
            contentDescription = "Multiple photos",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(Sizes.IconSmall)
        )
    }
}



@Composable
private fun ShimmerGridItem(animationDelay: Int = 0) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(animationDelay.toLong())
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "shimmerAlpha"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(Sizes.CornerSmall))
            .shimmer()
    )
}



@Composable
private fun PhotoGridEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Sizes.HeightExtraLarge)
            .padding(Spacing.ExtraLarge),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            Icon(
                imageVector = Icons.Default.Collections,
                contentDescription = null,
                modifier = Modifier.size(Sizes.IconGiant),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "No photos yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Share your first photo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PhotoGridPreview() {
    MaterialTheme {
        PhotoGrid(
            items = List(9) { index ->
                MediaItem(
                    id = index.toString(),
                    url = "",
                    isVideo = index == 2 || index == 5,
                    isMultiple = index == 3 || index == 7
                )
            },
            onItemClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PhotoGridLoadingPreview() {
    MaterialTheme {
        PhotoGrid(
            items = emptyList(),
            onItemClick = {},
            isLoading = true
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PhotoGridEmptyPreview() {
    MaterialTheme {
        PhotoGrid(
            items = emptyList(),
            onItemClick = {},
            isLoading = false
        )
    }
}
