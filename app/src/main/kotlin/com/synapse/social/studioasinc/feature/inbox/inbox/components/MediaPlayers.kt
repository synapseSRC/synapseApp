package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.reels.VideoPlayerViewModel
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.media3.common.PlaybackParameters
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.VoicePlayerViewModel
import androidx.compose.ui.unit.dp

@Composable
fun VoiceMessagePlayer(
    mediaUrl: String,
    tintColor: Color,
    isFromMe: Boolean = false,
    voiceViewModel: VoicePlayerViewModel = hiltViewModel(key = "voice_$mediaUrl"),
    videoViewModel: VideoPlayerViewModel = hiltViewModel(key = "video_$mediaUrl")
) {
    val localFilePath by voiceViewModel.localFilePath.collectAsState()
    val isDownloading by voiceViewModel.isDownloading.collectAsState()
    val playbackSpeed by voiceViewModel.playbackSpeed.collectAsState()

    val uiState by videoViewModel.uiState.collectAsState()
    val isPlaying = uiState.isPlaying
    val durationMs = uiState.duration
    val currentPositionMs = uiState.currentPosition

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                videoViewModel.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(localFilePath) {
        localFilePath?.let {
            videoViewModel.initializePlayer(it)
            videoViewModel.getPlayerInstance()?.playbackParameters = PlaybackParameters(playbackSpeed)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoViewModel.releasePlayer()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(Spacing.Huge)
            .padding(horizontal = Spacing.Small)
    ) {
        if (localFilePath == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(Sizes.IconExtraLarge)
                    .clip(CircleShape)
                    .background(tintColor.copy(alpha = 0.1f))
                    .clickable { voiceViewModel.prepareVoiceMessage(mediaUrl) }
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Sizes.IconMedium),
                        color = tintColor,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Download Voice Message",
                        tint = tintColor
                    )
                }
            }
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(Sizes.IconExtraLarge)
                    .clip(CircleShape)
                    .background(tintColor.copy(alpha = 0.1f))
                    .clickable {
                        if (isPlaying) videoViewModel.pause() else videoViewModel.play()
                    }
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(if (isPlaying) R.string.chat_action_pause_audio else R.string.chat_action_play_audio),
                    tint = tintColor
                )
            }
        }

        Spacer(modifier = Modifier.width(Spacing.Small))

        val progress = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f

        val bars = 30
        val seededHeights = remember(mediaUrl) {
            val random = java.util.Random(mediaUrl.hashCode().toLong())
            FloatArray(bars) { 0.2f + random.nextFloat() * 0.8f }
        }

        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(Spacing.Large)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val barWidth = canvasWidth / bars
            val gap = barWidth * 0.3f
            val actualBarWidth = barWidth - gap

            for (i in 0 until bars) {
                val isActive = if (isFromMe) {
                    // For sent messages, you might want it to fill normally left to right
                    (i.toFloat() / bars) <= progress
                } else {
                    // Left to right fill
                    (i.toFloat() / bars) <= progress
                }

                val color = if (isActive) tintColor else tintColor.copy(alpha = 0.3f)
                val barHeight = canvasHeight * seededHeights[i]

                val startY = (canvasHeight - barHeight) / 2

                drawRoundRect(
                    color = color,
                    topLeft = Offset(x = i * barWidth, y = startY),
                    size = Size(width = actualBarWidth, height = barHeight),
                    cornerRadius = CornerRadius(actualBarWidth / 2, actualBarWidth / 2)
                )
            }
        }

        Spacer(modifier = Modifier.width(Spacing.Small))

        val displayTimeMs = if (isPlaying || currentPositionMs > 0) currentPositionMs else durationMs
        val totalSeconds = displayTimeMs / 1000
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        Text(
            text = String.format("%02d:%02d", m, s),
            color = tintColor,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.width(Spacing.Tiny))

        if (localFilePath != null) {
            Surface(
                modifier = Modifier.clickable {
                    val newSpeed = voiceViewModel.cyclePlaybackSpeed()
                    videoViewModel.getPlayerInstance()?.playbackParameters = PlaybackParameters(newSpeed)
                },
                shape = CircleShape,
                color = tintColor.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "${if (playbackSpeed == 1.0f) "1" else playbackSpeed}x",
                    color = tintColor,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = Spacing.Small, vertical = Spacing.Tiny)
                )
            }
        }
    }
}

@Composable
fun VideoPlayerBox(
    mediaUrl: String,
    viewModel: VideoPlayerViewModel = hiltViewModel(key = mediaUrl)
) {
    val lifecycleOwner = LocalLifecycleOwner.current

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

    LaunchedEffect(mediaUrl) {
        viewModel.initializePlayer(mediaUrl)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.releasePlayer()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = Sizes.ChatMaxWidth)
            .clip(RoundedCornerShape(Sizes.CornerMedium))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val player = viewModel.getPlayerInstance()
        if (player != null) {
            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        this.player = player
                        useController = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                update = { view ->
                    view.player = player
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
