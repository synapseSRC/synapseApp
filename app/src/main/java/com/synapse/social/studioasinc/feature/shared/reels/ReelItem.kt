package com.synapse.social.studioasinc.feature.shared.reels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.synapse.social.studioasinc.feature.shared.reels.components.HeartAnimation
import com.synapse.social.studioasinc.feature.shared.theme.InteractionLikeActive
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.Reel
import com.synapse.social.studioasinc.ui.components.CircularAvatar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun ReelItem(
    reel: Reel,
    isActive: Boolean,
    onLikeClick: () -> Unit,
    onOpposeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onMoreClick: () -> Unit,
    onUserClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val videoViewModel: VideoPlayerViewModel = hiltViewModel(key = reel.id)
    val videoState by videoViewModel.uiState.collectAsState()
    var showControls by remember { mutableStateOf(false) }
    var isLongPressing by remember { mutableStateOf(false) }
    var showHeartAnimation by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val swipeThreshold = with(density) { 100.dp.toPx() }


    val lifecycleOwner = LocalLifecycleOwner.current
    var isResumed by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                isResumed = false
            } else if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
                isResumed = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isActive, isResumed) {
        if (isActive && isResumed) {
            videoViewModel.initializePlayer(reel.videoUrl)
            videoViewModel.play()
        } else {
            videoViewModel.pause()
        }
    }


    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3000)
            showControls = false
        }
    }


    DisposableEffect(Unit) {
        onDispose {
            videoViewModel.releasePlayer()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        onLikeClick()
                        showHeartAnimation = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onTap = {
                        videoViewModel.toggleMute()
                        showControls = true
                    },
                    onPress = {
                        val job = scope.launch {
                            delay(500)
                            isLongPressing = true
                            videoViewModel.pause()
                        }
                        try {
                            awaitRelease()
                        } finally {
                            job.cancel()
                            if (isLongPressing) {
                                isLongPressing = false
                                if (isActive) videoViewModel.play()
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                var offsetX = 0f
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX > swipeThreshold) {
                            onUserClick()
                        } else if (offsetX < -swipeThreshold) {
                            onBackClick()
                        }
                        offsetX = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount
                    }
                )
            }
    ) {
        val player = videoViewModel.getPlayerInstance()

        ReelMediaPlayer(player = player, isActive = isActive)

        ReelCenterControls(
            showControls = showControls,
            isPlaying = videoState.isPlaying,
            isLongPressing = isLongPressing,
            onTogglePlayPause = {
                if (videoState.isPlaying) videoViewModel.pause() else videoViewModel.play()
                showControls = true
            },
            modifier = Modifier.align(Alignment.Center)
        )

        ReelMuteControl(
            showControls = showControls,
            isLongPressing = isLongPressing,
            isMuted = videoState.isMuted,
            onToggleMute = { videoViewModel.toggleMute() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = Spacing.Huge, end = Spacing.Medium)
        )

        ReelActionButtons(
            reel = reel,
            isLongPressing = isLongPressing,
            onLikeClick = onLikeClick,
            onOpposeClick = onOpposeClick,
            onCommentClick = onCommentClick,
            onShareClick = onShareClick,
            onMoreClick = onMoreClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = Spacing.NavBarHeight, end = Spacing.Medium)
        )

        ReelUserInfo(
            reel = reel,
            isLongPressing = isLongPressing,
            showControls = showControls,
            videoDuration = videoState.duration,
            videoProgress = videoState.progress,
            onUserClick = onUserClick,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = Spacing.SmallPlus)
        )
        if (showHeartAnimation) {
            HeartAnimation(
                onAnimationEnd = { showHeartAnimation = false }
            )
        }
    }
}

@Composable
private fun ReelMediaPlayer(
    player: Player?,
    isActive: Boolean
) {
    if (isActive || player != null) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    this.player = player
                    useController = false
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            update = { view ->
                view.player = player
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                    startY = 500f
                )
            )
    )
}

@Composable
private fun ReelCenterControls(
    showControls: Boolean,
    isPlaying: Boolean,
    isLongPressing: Boolean,
    onTogglePlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = (showControls || !isPlaying) && !isLongPressing,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f), shape = MaterialTheme.shapes.medium)
                .padding(Spacing.Medium)
        ) {
            IconButton(
                onClick = onTogglePlayPause
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(Sizes.IconGiant)
                )
            }
        }
    }
}

@Composable
private fun ReelMuteControl(
    showControls: Boolean,
    isLongPressing: Boolean,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (showControls && !isLongPressing) {
        IconButton(
            onClick = onToggleMute,
            modifier = modifier
        ) {
            Icon(
                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Mute toggle",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun ReelActionButtons(
    reel: Reel,
    isLongPressing: Boolean,
    onLikeClick: () -> Unit,
    onOpposeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !isLongPressing,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = onLikeClick) {
                Icon(
                    imageVector = if (reel.isLikedByCurrentUser) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (reel.isLikedByCurrentUser) InteractionLikeActive else MaterialTheme.colorScheme.onPrimary
                )
            }
            Text(text = "${reel.likesCount}", color = MaterialTheme.colorScheme.onPrimary)
            Spacer(modifier = Modifier.size(Spacing.Medium))

            IconButton(onClick = onOpposeClick) {
                Icon(
                    imageVector = if (reel.isOpposedByCurrentUser) Icons.Default.ThumbDown else Icons.Outlined.ThumbDown,
                    contentDescription = "Oppose",
                    tint = if (reel.isOpposedByCurrentUser) InteractionLikeActive else MaterialTheme.colorScheme.onPrimary
                )
            }
            Text(text = "${reel.opposeCount}", color = MaterialTheme.colorScheme.onPrimary)
            Spacer(modifier = Modifier.size(Spacing.Medium))

            IconButton(onClick = onCommentClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Comment,
                    contentDescription = "Comment",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Text(text = "${reel.commentCount}", color = MaterialTheme.colorScheme.onPrimary)
            Spacer(modifier = Modifier.size(Spacing.Medium))

            IconButton(onClick = onShareClick) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Share",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Text(text = "${reel.shareCount}", color = MaterialTheme.colorScheme.onPrimary)
            Spacer(modifier = Modifier.size(Spacing.Medium))

            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun ReelUserInfo(
    reel: Reel,
    isLongPressing: Boolean,
    showControls: Boolean,
    videoDuration: Long,
    videoProgress: Float,
    onUserClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        AnimatedVisibility(
            visible = !isLongPressing,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .padding(start = Spacing.Medium, end = Sizes.WidthLarge, bottom = Spacing.SmallPlus)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularAvatar(
                        imageUrl = reel.creatorAvatarUrl,
                        contentDescription = null,
                        size = Spacing.ExtraLarge,
                        onClick = onUserClick
                    )
                    Spacer(modifier = Modifier.size(Spacing.Small))
                    Column {
                        Text(
                            text = reel.creatorUsername ?: "Unknown",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.clickable(onClick = onUserClick)
                        )
                        reel.locationName?.let { location ->
                            Text(
                                text = location,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.size(Spacing.Small))
                reel.caption?.let { caption ->
                    if (caption.isNotEmpty()) {
                        Text(
                            text = caption,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 3
                        )
                    }
                }
                reel.musicTrack?.let { musicTrack ->
                    if (musicTrack.isNotEmpty()) {
                        Text(
                            text = "🎵 ${musicTrack}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        if ((showControls || isLongPressing) && videoDuration > 0) {
            LinearProgressIndicator(
                progress = { videoProgress },
                modifier = Modifier.fillMaxWidth().height(Sizes.BorderDefault),
                color = MaterialTheme.colorScheme.onPrimary,
                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
            )
        }
    }
}
