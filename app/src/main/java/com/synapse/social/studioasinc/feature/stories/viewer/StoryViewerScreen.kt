package com.synapse.social.studioasinc.feature.stories.viewer

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.synapse.social.studioasinc.R
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.domain.model.Story
import com.synapse.social.studioasinc.domain.model.StoryMediaType
import com.synapse.social.studioasinc.domain.model.User
import kotlinx.coroutines.delay
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@OptIn(UnstableApi::class)
@Composable
fun StoryViewerScreen(
    onClose: () -> Unit,
    viewModel: StoryViewerViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current


    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) {
            onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Intentional: story viewer requires black background
            .pointerInput(Unit) {
                val screenWidth = size.width
                detectTapGestures(
                    onPress = {
                        viewModel.pause()
                        tryAwaitRelease()
                        viewModel.resume()
                    },
                    onTap = { offset ->
                        if (offset.x < screenWidth * 0.3f) {
                            viewModel.previousStory()
                        } else {
                            viewModel.nextStory()
                        }
                    }
                )
            }
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else if (uiState.error != null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = uiState.error ?: stringResource(R.string.error_unknown), color = MaterialTheme.colorScheme.onPrimary)
                Button(onClick = onClose) {
                    Text(stringResource(R.string.cd_close))
                }
            }
        } else if (uiState.stories.isNotEmpty()) {
            val currentStory = uiState.stories.getOrNull(uiState.currentStoryIndex)

            if (currentStory != null) {

                StoryMediaContent(
                    story = currentStory,
                    isPaused = uiState.isPaused,
                    onVideoReady = { duration -> viewModel.onVideoReady(duration) }
                )


                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = Spacing.Medium, start = Spacing.Small, end = Spacing.Small)
                ) {

                    StoryProgressBar(
                        steps = uiState.stories.size,
                        currentStep = uiState.currentStoryIndex,
                        currentStepProgress = uiState.progress,
                        isPaused = uiState.isPaused
                    )

                    Spacer(modifier = Modifier.height(Spacing.SmallMedium))


                    StoryUserHeader(
                        user = uiState.user,
                        storyTime = currentStory.createdAt,
                        onClose = onClose
                    )
                }
            }
        }
    }
}

@Composable
fun StoryMediaContent(
    story: Story,
    isPaused: Boolean,
    onVideoReady: (Long) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (story.mediaType == StoryMediaType.VIDEO && story.mediaUrl != null) {
            VideoPlayer(
                mediaUrl = story.mediaUrl ?: "",
                isPaused = isPaused,
                onVideoReady = onVideoReady
            )
        } else {
            AsyncImage(
                model = story.mediaUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    mediaUrl: String,
    isPaused: Boolean,
    onVideoReady: (Long) -> Unit
) {
    val context = LocalContext.current


    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }


    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                     if (exoPlayer.duration > 0) {
                        onVideoReady(exoPlayer.duration)
                     }
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }


    LaunchedEffect(mediaUrl) {
        val mediaItem = MediaItem.fromUri(mediaUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = !isPaused
    }


    LaunchedEffect(isPaused) {
        if (isPaused) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun StoryProgressBar(
    steps: Int,
    currentStep: Int,
    currentStepProgress: Float,
    isPaused: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Spacing.ExtraSmall),
        horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)
    ) {
        for (i in 0 until steps) {
            val progress = when {
                i < currentStep -> 1f
                i == currentStep -> currentStepProgress
                else -> 0f
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.onPrimary,
                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
            )
        }
    }
}

@Composable
fun StoryUserHeader(
    user: User?,
    storyTime: String?,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(Sizes.IconHuge)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (user?.avatar != null) {
                AsyncImage(
                    model = user.avatar,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = (user?.displayName ?: user?.username ?: "?").take(1).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(Spacing.Small))


        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user?.displayName ?: user?.username ?: stringResource(R.string.error_unknown_short),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )

        }


        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.cd_close),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
