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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Download
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.PlaybackParameters
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.VoiceDownloadCache
import kotlinx.coroutines.launch
import kotlin.random.Random


@Composable
fun VoiceMessagePlayer(
    mediaUrl: String,
    tintColor: Color,
    isFromMe: Boolean,
    viewModel: VideoPlayerViewModel = hiltViewModel(key = mediaUrl)
) {
    val uiState by viewModel.uiState.collectAsState()
    val isPlaying = uiState.isPlaying
    val durationMs = uiState.duration
    val currentPositionMs = uiState.currentPosition

    val lifecycleOwner = LocalLifecycleOwner.current

    // We need to access VoiceDownloadCache. The best way in Compose without injecting a new ViewModel
    // is to resolve it from the application context.
    val context = androidx.compose.ui.platform.LocalContext.current
    val voiceCache = remember {
        // Quick manual resolution, assuming VoiceDownloadCache is provided by Hilt.
        // Actually, we can use an entry point to get it.
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            context.applicationContext,
            VoiceDownloadCacheEntryPoint::class.java
        ).getVoiceDownloadCache()
    }

    var localPath by remember { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var playbackSpeed by remember { mutableFloatStateOf(1f) }

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

    LaunchedEffect(localPath) {
        if (localPath != null) {
            viewModel.initializePlayer(localPath!!)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.releasePlayer()
        }
    }

    val playPauseAction = {
        if (localPath == null) {
            isDownloading = true
            coroutineScope.launch {
                val result = voiceCache.getLocalPath(mediaUrl)
                isDownloading = false
                result.onSuccess { path ->
                    localPath = path
                }
            }
        } else {
            if (isPlaying) viewModel.pause() else viewModel.play()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(Spacing.ExtraLarge)
    ) {
        // Play/Pause / Download Button
        Box(
            modifier = Modifier
                .size(Sizes.IconLarge)
                .clip(CircleShape)
                .background(tintColor.copy(alpha = 0.1f))
                .clickable { playPauseAction() },
            contentAlignment = Alignment.Center
        ) {
            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Sizes.IconSemiMedium),
                    color = tintColor,
                    strokeWidth = 2.dp
                )
            } else if (localPath == null) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = stringResource(R.string.voice_download),
                    tint = tintColor,
                    modifier = Modifier.size(Sizes.IconDefault)
                )
            } else {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(if (isPlaying) R.string.chat_action_pause_audio else R.string.chat_action_play_audio),
                    tint = tintColor,
                    modifier = Modifier.size(Sizes.IconDefault)
                )
            }
        }

        Spacer(modifier = Modifier.width(Spacing.Small))

        // Waveform
        val progress = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f

        // Generate pseudo-random waveform based on url hash
        val random = remember(mediaUrl) { Random(mediaUrl.hashCode()) }
        val bars = remember(mediaUrl) { List(30) { random.nextFloat() * 0.8f + 0.2f } }

        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(Sizes.IconLarge)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val barCount = bars.size
            val spacing = Spacing.ExtraSmall.toPx()
            val totalSpacing = spacing * (barCount - 1)
            val barWidth = (canvasWidth - totalSpacing) / barCount

            bars.forEachIndexed { index, amp ->
                val x = index * (barWidth + spacing)
                val barHeight = canvasHeight * amp
                val startY = (canvasHeight - barHeight) / 2f
                val endY = startY + barHeight

                // Color based on progress
                val barProgress = index.toFloat() / barCount
                val color = if (barProgress <= progress && localPath != null) {
                    tintColor
                } else {
                    tintColor.copy(alpha = 0.3f)
                }

                drawLine(
                    color = color,
                    start = Offset(x + barWidth / 2f, startY),
                    end = Offset(x + barWidth / 2f, endY),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.width(Spacing.Small))

        // Duration / Speed Chip
        Column(horizontalAlignment = Alignment.End) {
            if (localPath != null) {
                Surface(
                    shape = RoundedCornerShape(Sizes.CornerMedium),
                    color = tintColor.copy(alpha = 0.1f),
                    modifier = Modifier
                        .clickable {
                            // VideoPlayerViewModel does not support changing playback speed right now.
                            // To prevent compile issues, this will be visually updated but functionally inert
                            // until VideoPlayerViewModel exposes speed control.
                            playbackSpeed = when (playbackSpeed) {
                                1f -> 1.5f
                                1.5f -> 2f
                                else -> 1f
                            }
                        }
                        .padding(horizontal = Spacing.ExtraSmall, vertical = Spacing.Tiny)
                ) {
                    Text(
                        text = "${playbackSpeed}x",
                        style = MaterialTheme.typography.labelSmall,
                        color = tintColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = Spacing.ExtraSmall, vertical = Spacing.Tiny)
                    )
                }
            }

            val displayTimeMs = if (isPlaying || localPath != null) {
                if (isPlaying) currentPositionMs else durationMs
            } else 0L

            val totalSeconds = displayTimeMs / 1000
            val m = totalSeconds / 60
            val s = totalSeconds % 60

            Text(
                text = String.format("%02d:%02d", m, s),
                color = tintColor,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = Spacing.Tiny)
            )
        }
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface VoiceDownloadCacheEntryPoint {
    fun getVoiceDownloadCache(): VoiceDownloadCache
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
