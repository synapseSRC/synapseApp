package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFramePercent
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.VoiceMessagePlayerViewModel
import com.synapse.social.studioasinc.feature.shared.reels.VideoPlayerViewModel
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

private fun formatSpeed(speed: Float): String {
    return when {
        speed == 1.5f -> "1.5x"
        speed >= 2.0f -> "2x"
        else -> "1x"
    }
}

@Composable
fun VoiceMessagePlayer(
    mediaUrl: String,
    tintColor: Color,
    isFromMe: Boolean,
    modifier: Modifier = Modifier,
    initialDurationMs: Long = 0L,
    viewModel: VoiceMessagePlayerViewModel = hiltViewModel(key = mediaUrl)
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(mediaUrl, initialDurationMs) {
        viewModel.initialize(mediaUrl, initialDurationMs)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isPlaying = uiState.isPlaying
    val isDownloading = uiState.isDownloading
    val isBuffering = uiState.isBuffering
    val hasError = uiState.downloadError != null
    val currentPositionMs = uiState.currentPositionMs
    val durationMs = uiState.durationMs
    val speed = uiState.playbackSpeed
    val amplitudes = uiState.waveformAmplitudes

    val progress = if (durationMs > 0) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val buttonContainerColor = if (isFromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    val buttonContentColor = if (isFromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary
    val activeWaveformColor = if (isFromMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
    val inactiveWaveformColor = activeWaveformColor.copy(alpha = 0.38f)
    val textColor = if (isFromMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer

    val playPauseInteractionSource = remember { MutableInteractionSource() }
    val isPlayPausePressed by playPauseInteractionSource.collectIsPressedAsState()
    val playPauseScale by animateFloatAsState(
        targetValue = if (isPlayPausePressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "playPauseScale"
    )

    val speedInteractionSource = remember { MutableInteractionSource() }
    val isSpeedPressed by speedInteractionSource.collectIsPressedAsState()
    val speedScale by animateFloatAsState(
        targetValue = if (isSpeedPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "speedScale"
    )

    Surface(
        color = Color.Transparent,
        modifier = modifier
            .widthIn(min = Sizes.ChatMaxWidth / 2, max = Sizes.ChatMaxWidth)
            .padding(vertical = Spacing.ExtraSmall)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.Small, vertical = Spacing.ExtraSmall)
        ) {
            // Material 3 Expressive Substantial Play/Pause Button
            Box(
                modifier = Modifier
                    .size(Sizes.AvatarLarge),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isDownloading || isBuffering -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Sizes.IconLarge),
                            color = activeWaveformColor,
                            strokeWidth = Sizes.BorderDefault
                        )
                    }
                    hasError -> {
                        IconButton(
                            onClick = { viewModel.onRetryClicked() },
                            modifier = Modifier.size(Sizes.AvatarLarge)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.retry),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(Sizes.IconLarge)
                            )
                        }
                    }
                    else -> {
                        FilledTonalIconButton(
                            onClick = { viewModel.onPlayPauseClicked() },
                            interactionSource = playPauseInteractionSource,
                            modifier = Modifier
                                .size(Sizes.AvatarLarge)
                                .graphicsLayer {
                                    scaleX = playPauseScale
                                    scaleY = playPauseScale
                                },
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = buttonContainerColor,
                                contentColor = buttonContentColor
                            )
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = stringResource(
                                    if (isPlaying) R.string.chat_action_pause_audio else R.string.chat_action_play_audio
                                ),
                                modifier = Modifier.size(Sizes.IconLarge)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(Spacing.Medium))

            // Waveform and Controls Column
            Column(
                modifier = Modifier
                    .weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Waveform Canvas (Seekable by tap or drag)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Sizes.IconLarge)
                        .pointerInput(durationMs) {
                            detectTapGestures { offset ->
                                if (durationMs > 0 && size.width > 0) {
                                    val fraction = offset.x / size.width.toFloat()
                                    viewModel.onSeek(fraction)
                                }
                            }
                        }
                        .pointerInput(durationMs) {
                            detectHorizontalDragGestures { change, dragAmount ->
                                change.consume()
                                if (durationMs > 0 && size.width > 0) {
                                    val fraction = change.position.x / size.width.toFloat()
                                    viewModel.onSeek(fraction)
                                }
                            }
                        }
                ) {
                    val density = LocalDensity.current
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val bars = if (amplitudes.isNotEmpty()) amplitudes else List(36) { 0.3f }
                        val barCount = bars.size
                        val spacingPx = with(density) { Spacing.ExtraSmall.toPx() }
                        val totalSpacing = spacingPx * (barCount - 1)
                        val barWidth = ((canvasWidth - totalSpacing) / barCount).coerceAtLeast(with(density) { Sizes.BorderDefault.toPx() })

                        bars.forEachIndexed { index, amp ->
                            val x = index * (barWidth + spacingPx) + (barWidth / 2f)
                            val barHeight = (canvasHeight * amp.coerceIn(0.15f, 1.0f)).coerceAtLeast(with(density) { Sizes.BorderThin.toPx() } * 4)
                            val startY = (canvasHeight - barHeight) / 2f
                            val endY = startY + barHeight

                            val barFraction = index.toFloat() / barCount.toFloat()
                            val color = if (barFraction <= progress) {
                                activeWaveformColor
                            } else {
                                inactiveWaveformColor
                            }

                            drawLine(
                                color = color,
                                start = Offset(x, startY),
                                end = Offset(x, endY),
                                strokeWidth = barWidth,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))

                // Time Display and Speed Chip Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time position / duration string
                    val formattedPosition = formatTime(currentPositionMs)
                    val formattedDuration = formatTime(durationMs)
                    val timeText = if (durationMs > 0) "$formattedPosition / $formattedDuration" else formattedPosition

                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor,
                        fontWeight = FontWeight.Medium
                    )

                    // Playback Speed Toggle Chip (1x -> 1.5x -> 2x -> 1x)
                    Surface(
                        shape = RoundedCornerShape(Sizes.CornerMedium),
                        color = buttonContainerColor,
                        contentColor = buttonContentColor,
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = speedScale
                                scaleY = speedScale
                            }
                            .clip(RoundedCornerShape(Sizes.CornerMedium))
                            .clickable(
                                interactionSource = speedInteractionSource,
                                indication = ripple(),
                                onClick = { viewModel.onSpeedToggle() }
                            )
                    ) {
                        Text(
                            text = formatSpeed(speed),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = Spacing.Small, vertical = Spacing.Tiny)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPlayerBox(
    mediaUrl: String,
    viewModel: VideoPlayerViewModel = hiltViewModel(key = mediaUrl),
    onPlayClick: (() -> Unit)? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    var activePlayer by remember(viewModel, mediaUrl) { mutableStateOf<ExoPlayer?>(null) }
    val thumbnailRequest = remember(mediaUrl, context) {
        ImageRequest.Builder(context)
            .data(mediaUrl)
            .videoFramePercent(0.1)
            .crossfade(true)
            .build()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(mediaUrl, viewModel) {
        viewModel.initializePlayer(mediaUrl)
        activePlayer = viewModel.getPlayerInstance()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.releasePlayer()
        }
    }

    val state = uiState.value

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = Sizes.ChatMaxWidth)
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(Sizes.CornerMedium))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // Thumbnail background (always present as fallback)
        AsyncImage(
            model = thumbnailRequest,
            contentDescription = stringResource(R.string.cd_media_attachment),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        // Player view on top of thumbnail
        activePlayer?.let { player ->
            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        this.player = player
                        useController = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                },
                update = { view ->
                    view.player = player
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Loading / Buffering overlay
        if (state.isBuffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Sizes.IconExtraLarge),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Play button overlay (show when not playing and not buffering)
        if (!state.isPlaying && !state.isBuffering) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .combinedClickable(
                        onClick = {
                            onPlayClick?.invoke()
                            viewModel.play()
                        },
                        onLongClick = {}
                    ),
                contentAlignment = Alignment.Center
            ) {
                val size = Sizes.IconLarge
                Box(
                    modifier = Modifier
                        .size(size)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            shape = CircleShape
                        )
                        .padding(Spacing.Small)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.chat_action_play_video),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(Sizes.IconLarge)
                    )
                }
            }
        }

        // Duration badge (bottom-right)
        if (state.duration > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.Small)
                    .align(Alignment.BottomEnd)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(Sizes.CornerSmall)
                ) {
                    Text(
                        text = formatDuration(state.duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = Spacing.Small, vertical = Spacing.Tiny)
                    )
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return if (minutes > 0) {
        "$minutes:${"%02d".format(remainingSeconds)}"
    } else {
        "0:${"%02d".format(remainingSeconds)}"
    }
}
