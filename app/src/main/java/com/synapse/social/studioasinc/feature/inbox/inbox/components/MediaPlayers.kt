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

@Composable
fun AudioPlayer(
    mediaUrl: String,
    tintColor: Color,
    viewModel: VideoPlayerViewModel = hiltViewModel(key = mediaUrl)
) {
    val uiState by viewModel.uiState.collectAsState()
    val isPlaying = uiState.isPlaying
    val durationMs = uiState.duration
    val currentPositionMs = uiState.currentPosition

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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(Spacing.Huge)
            .padding(horizontal = Spacing.Small)
    ) {
        IconButton(
            onClick = {
                if (isPlaying) {
                    viewModel.pause()
                } else {
                    viewModel.play()
                }
            }
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = stringResource(if (isPlaying) R.string.chat_action_pause_audio else R.string.chat_action_play_audio),
                tint = tintColor
            )
        }

        Spacer(modifier = Modifier.width(Spacing.Small))

        // Simple progress bar
        val progress = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(Spacing.ExtraSmall)
                .clip(RoundedCornerShape(Sizes.CornerSharp)),
            color = tintColor,
            trackColor = tintColor.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.width(Spacing.Small))

        // Time display
        val displayTimeMs = if (isPlaying) currentPositionMs else durationMs
        val totalSeconds = displayTimeMs / 1000
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        Text(
            text = String.format("%02d:%02d", m, s),
            color = tintColor,
            style = MaterialTheme.typography.bodySmall
        )
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
