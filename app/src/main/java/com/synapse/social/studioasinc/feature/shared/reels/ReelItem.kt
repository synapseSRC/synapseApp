package com.synapse.social.studioasinc.feature.shared.reels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.media3.ui.PlayerView
import com.synapse.social.studioasinc.feature.shared.reels.components.ReelAction
import com.synapse.social.studioasinc.feature.shared.reels.components.HeartAnimation
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
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.8f)),
                        startY = with(density) { 300.dp.toPx() }
                    )
                )
        )


        AnimatedVisibility(
            visible = (showControls || !videoState.isPlaying) && !isLongPressing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.4f), shape = MaterialTheme.shapes.medium)
                    .padding(16.dp)
            ) {
                IconButton(
                    onClick = {
                        if (videoState.isPlaying) videoViewModel.pause() else videoViewModel.play()
                        showControls = true
                    }
                ) {
                    Icon(
                        imageVector = if (videoState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (videoState.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }


        if (showControls && !isLongPressing) {
            IconButton(
                onClick = { videoViewModel.toggleMute() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 16.dp)
            ) {
                Icon(
                    imageVector = if (videoState.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Mute toggle",
                    tint = Color.White
                )
            }
        }


        AnimatedVisibility(
            visible = !isLongPressing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 60.dp, end = 16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ReelAction(
                    icon = if (reel.isLikedByCurrentUser) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (reel.isLikedByCurrentUser) Color.Red else Color.White,
                    text = "${reel.likesCount}",
                    onClick = onLikeClick
                )

                ReelAction(
                    icon = if (reel.isOpposedByCurrentUser) Icons.Default.ThumbDown else Icons.Outlined.ThumbDown,
                    contentDescription = "Oppose",
                    tint = if (reel.isOpposedByCurrentUser) Color.Red else Color.White,
                    text = "${reel.opposeCount}",
                    onClick = onOpposeClick
                )

                ReelAction(
                    icon = Icons.AutoMirrored.Outlined.Comment,
                    contentDescription = "Comment",
                    text = "${reel.commentCount}",
                    onClick = onCommentClick
                )

                ReelAction(
                    icon = Icons.Outlined.Share,
                    contentDescription = "Share",
                    text = "${reel.shareCount}",
                    onClick = onShareClick
                )

                ReelAction(
                    icon = Icons.Default.MoreVert,
                    contentDescription = "More",
                    onClick = onMoreClick
                )
            }
        }


        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 10.dp)
                .fillMaxWidth()
        ) {
            AnimatedVisibility(
                visible = !isLongPressing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 80.dp, bottom = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularAvatar(
                            imageUrl = reel.creatorAvatarUrl,
                            contentDescription = null,
                            size = 32.dp,
                            onClick = onUserClick
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Column {
                            Text(
                                text = reel.creatorUsername ?: "Unknown",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black.copy(alpha = 0.5f),
                                        blurRadius = 4f
                                    )
                                ),
                                color = Color.White,
                                modifier = Modifier.clickable(onClick = onUserClick)
                            )
                            reel.locationName?.let { location ->
                                Text(
                                    text = location,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        shadow = androidx.compose.ui.graphics.Shadow(
                                            color = Color.Black.copy(alpha = 0.5f),
                                            blurRadius = 4f
                                        )
                                    ),
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    reel.caption?.let { caption ->
                        if (caption.isNotEmpty()) {
                            Text(
                                text = caption,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black.copy(alpha = 0.5f),
                                        blurRadius = 4f
                                    )
                                ),
                                color = Color.White,
                                maxLines = 3
                            )
                        }
                    }
                    reel.musicTrack?.let { musicTrack ->
                        if (musicTrack.isNotEmpty()) {
                            Text(
                                text = "🎵 $musicTrack",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = Color.Black.copy(alpha = 0.5f),
                                        blurRadius = 4f
                                    )
                                ),
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            if ((showControls || isLongPressing) && videoState.duration > 0) {
                LinearProgressIndicator(
                    progress = { videoState.progress },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f),
                )
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showHeartAnimation,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            HeartAnimation(
                onAnimationEnd = { showHeartAnimation = false }
            )
        }
    }
}
