package com.synapse.social.studioasinc.feature.stories.creator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.domain.model.StoryMediaType
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import kotlin.math.roundToInt

@Composable
internal fun StoryDrawingCanvas(
    isDrawingMode: Boolean,
    currentPath: List<Offset>,
    currentColor: Color,
    currentStrokeWidth: Float,
    cachedPaths: List<Pair<Path, DrawingPath>>,
    activePath: Path,
    onPathChange: (List<Offset>) -> Unit,
    onDrawingComplete: (DrawingPath) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isDrawingMode) {
                if (isDrawingMode) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            onPathChange(listOf(offset))
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            onPathChange(currentPath + change.position)
                        },
                        onDragEnd = {
                            if (currentPath.isNotEmpty()) {
                                onDrawingComplete(
                                    DrawingPath(
                                        points = currentPath,
                                        color = currentColor,
                                        strokeWidth = currentStrokeWidth
                                    )
                                )
                                onPathChange(emptyList())
                            }
                        }
                    )
                }
            }
    ) {
        cachedPaths.forEach { (path, drawing) ->
            if (drawing.points.size > 1) {
                drawPath(
                    path = path,
                    color = drawing.color,
                    style = Stroke(
                        width = drawing.strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        if (currentPath.size > 1) {
            drawPath(
                path = activePath,
                color = currentColor,
                style = Stroke(
                    width = currentStrokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

@Composable
internal fun StoryTextOverlays(
    textOverlays: List<TextOverlay>,
    onPositionChange: (Int, Offset) -> Unit,
    onContentChange: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    textOverlays.forEachIndexed { index, overlay ->
        Box(
            modifier = modifier
                .offset {
                    IntOffset(overlay.position.x.roundToInt(), overlay.position.y.roundToInt())
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onPositionChange(
                            index,
                            Offset(
                                overlay.position.x + dragAmount.x,
                                overlay.position.y + dragAmount.y
                            )
                        )
                    }
                }
        ) {
            TextField(
                value = overlay.text,
                onValueChange = { onContentChange(index, it) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = overlay.color,
                    unfocusedTextColor = overlay.color,
                    cursorColor = overlay.color,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = (24 * overlay.scale).sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
internal fun StoryStickerOverlays(
    stickers: List<StickerOverlay>,
    onPositionChange: (Int, Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    stickers.forEachIndexed { index, sticker ->
        Text(
            text = sticker.emoji,
            fontSize = (48 * sticker.scale).sp,
            modifier = modifier
                .offset {
                    IntOffset(sticker.position.x.roundToInt(), sticker.position.y.roundToInt())
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onPositionChange(
                            index,
                            Offset(
                                sticker.position.x + dragAmount.x,
                                sticker.position.y + dragAmount.y
                            )
                        )
                    }
                }
        )
    }
}

@Composable
internal fun StoryTopControls(
    isDrawingMode: Boolean,
    userProfile: com.synapse.social.studioasinc.domain.model.User?,
    onClose: () -> Unit,
    onToggleDrawingMode: () -> Unit,
    onAddText: () -> Unit,
    onAddSticker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.Medium),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.story_discard), tint = MaterialTheme.colorScheme.onPrimary)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onAddText) {
                Text(
                    stringResource(R.string.story_text_button_label),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            IconButton(onClick = onAddSticker) {
                Icon(Icons.Default.Face, contentDescription = stringResource(R.string.story_add_sticker), tint = MaterialTheme.colorScheme.onPrimary)
            }
            IconButton(onClick = onToggleDrawingMode) {
                Icon(
                    if (isDrawingMode) Icons.Default.Edit else Icons.Default.Edit,
                    contentDescription = stringResource(R.string.story_draw),
                    tint = if (isDrawingMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        if (userProfile?.avatar != null) {
            AsyncImage(
                model = userProfile.avatar,
                contentDescription = null,
                modifier = Modifier
                    .size(Sizes.IconMassive)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(Sizes.IconMassive)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(Sizes.IconDefault)
                )
            }
        }
    }
}

@Composable
internal fun StoryBottomControls(
    isPosting: Boolean,
    privacyName: String,
    onPost: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onPost,
        modifier = modifier.padding(Spacing.Medium)
    ) {
        if (isPosting) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(Sizes.IconLarge))
        } else {
            Text(stringResource(R.string.post_to_privacy, privacyName))
        }
    }
}

@Composable
internal fun StoryBackground(state: StoryCreatorState) {
    val mediaUri = state.capturedMediaUri
    if (mediaUri != null) {
        if (state.mediaType == StoryMediaType.VIDEO) {
            val context = LocalContext.current
            val exoPlayer = remember(mediaUri) {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(mediaUri))
                    repeatMode = ExoPlayer.REPEAT_MODE_ONE
                    playWhenReady = true
                    prepare()
                }
            }
            DisposableEffect(exoPlayer) { onDispose { exoPlayer.release() } }
            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = mediaUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    } else if (state.sharedPost != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.story_shared_post_editor), color = Color.White)
        }
    }
}