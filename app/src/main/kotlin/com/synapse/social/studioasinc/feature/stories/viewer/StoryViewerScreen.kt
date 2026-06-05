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
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import java.time.Duration
import java.time.Instant
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.stories.management.StoryOptionsSheet
import com.synapse.social.studioasinc.feature.stories.management.StoryReactionsSheet
import com.synapse.social.studioasinc.feature.stories.management.ViewerListSheet
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction

@Composable
fun QuickReactionsRow(
    userReaction: String?,
    onEmojiClick: (String) -> Unit
) {
    val emojis = listOf("❤️", "😂", "😮", "😢", "😡")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        emojis.forEach { emoji ->
            val isSelected = userReaction == emoji
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else Color.Transparent
                    )
                    .clickable { onEmojiClick(emoji) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    }
}

@Composable
fun StoryReplyBar(
    replyText: String,
    onReplyTextChange: (String) -> Unit,
    onSendReply: () -> Unit,
    onFocus: () -> Unit,
    onBlur: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            .padding(horizontal = Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = replyText,
            onValueChange = onReplyTextChange,
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) onFocus()
                    else onBlur()
                },
            placeholder = {
                Text(
                    text = stringResource(R.string.story_reply_placeholder),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                unfocusedTextColor = MaterialTheme.colorScheme.onPrimary
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSendReply() }),
            singleLine = true
        )

        if (replyText.isNotEmpty()) {
            IconButton(onClick = onSendReply) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = stringResource(R.string.cd_send),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun StoryViewerScreen(
    onFinished: () -> Unit,
    onClose: () -> Unit,
    viewModel: StoryViewerViewModel,
    isActive: Boolean = true
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current


    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) {
            onFinished()
        }
    }

    LaunchedEffect(isActive, uiState.stories, uiState.isLoading, uiState.currentStoryIndex) {
        if (isActive && uiState.stories.isNotEmpty() && !uiState.isLoading) {
            val currentStory = uiState.stories.getOrNull(uiState.currentStoryIndex)
            if (currentStory != null) {
                if (currentStory.mediaType != StoryMediaType.VIDEO) {
                    viewModel.startProgress()
                }
                viewModel.markAsSeen(currentStory.id)
            }
        } else {
            viewModel.stopProgress()
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
                    isPaused = uiState.isPaused || !isActive,
                    contentScale = uiState.contentScale,
                    onVideoReady = { duration -> viewModel.onVideoReady(duration) }
                )

                IconButton(
                    onClick = { viewModel.cycleContentScale() },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(Spacing.Medium)
                ) {
                    Icon(
                        imageVector = Icons.Default.Crop,
                        contentDescription = stringResource(R.string.story_cycle_scale),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                if (uiState.isOwnStory) {
                    val viewsCount = uiState.viewers.size
                    val reactionsCount = uiState.reactionsCount

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = Spacing.Medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                currentStory.id?.let { id ->
                                    viewModel.loadViewers(id)
                                    viewModel.showViewersSheet()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                            Text(
                                text = stringResource(R.string.seen_by_count, viewsCount),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        if (reactionsCount > 0) {
                            Spacer(modifier = Modifier.width(Spacing.Medium))
                            TextButton(
                                onClick = { viewModel.showReactionsSheet() }
                            ) {
                                Text(
                                    text = stringResource(R.string.story_reactions_count, reactionsCount),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(Spacing.Medium)
                    ) {
                        QuickReactionsRow(
                            userReaction = uiState.userReaction,
                            onEmojiClick = { emoji ->
                                if (uiState.userReaction == emoji) {
                                    viewModel.removeReaction()
                                } else {
                                    viewModel.reactToStory(emoji)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(Spacing.Small))

                        StoryReplyBar(
                            replyText = uiState.replyText,
                            onReplyTextChange = { viewModel.updateReplyText(it) },
                            onSendReply = { viewModel.sendReply() },
                            onFocus = { viewModel.pause() },
                            onBlur = { viewModel.resume() }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = Spacing.Medium, start = Spacing.Small, end = Spacing.Small)
                ) {

                    StoryProgressBar(
                        steps = uiState.stories.size,
                        currentStep = uiState.currentStoryIndex,
                        currentStepProgress = uiState.progress,
                        isPaused = uiState.isPaused || !isActive
                    )

                    Spacer(modifier = Modifier.height(Spacing.SmallMedium))


                    StoryUserHeader(
                        user = uiState.user,
                        storyTime = currentStory.createdAt,
                        expiresAt = currentStory.expiresAt,
                        onClose = onClose,
                        onMore = { viewModel.showOptionsSheet() }
                    )
                }

                if (uiState.showViewersSheet) {
                    ViewerListSheet(
                        viewers = uiState.viewers,
                        isLoading = uiState.isLoadingViewers,
                        onDismiss = { viewModel.hideViewersSheet() },
                        onUserClick = { /* no-op for now */ }
                    )
                }

                if (uiState.showReactionsSheet) {
                    StoryReactionsSheet(
                        reactions = uiState.reactions,
                        onDismiss = { viewModel.hideReactionsSheet() }
                    )
                }

                if (uiState.showOptionsSheet) {
                    StoryOptionsSheet(
                        isOwnStory = uiState.isOwnStory,
                        onDismiss = { viewModel.hideOptionsSheet() },
                        onDelete = {
                            viewModel.hideOptionsSheet()
                            viewModel.showDeleteConfirmation()
                        },
                        onReport = {
                            viewModel.hideOptionsSheet()
                            // Report logic
                        },
                        onMute = {
                            viewModel.hideOptionsSheet()
                            // Mute logic
                        }
                    )
                }

                if (uiState.showDeleteConfirmation) {
                    AlertDialog(
                        onDismissRequest = { viewModel.hideDeleteConfirmation() },
                        title = { Text(stringResource(R.string.story_delete_title)) },
                        text = { Text(stringResource(R.string.story_delete_body)) },
                        confirmButton = {
                            TextButton(
                                onClick = { currentStory.id?.let { viewModel.confirmDeleteStory(it) } },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text(stringResource(R.string.m_delete))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.hideDeleteConfirmation() }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
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
    contentScale: ContentScale,
    onVideoReady: (Long) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (story.mediaType == StoryMediaType.VIDEO && story.mediaUrl != null) {
            VideoPlayer(
                mediaUrl = story.mediaUrl,
                isPaused = isPaused,
                onVideoReady = onVideoReady
            )
        } else {
            AsyncImage(
                model = story.mediaUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
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
    onClose: () -> Unit,
    onMore: () -> Unit = {},
    expiresAt: String? = null
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
                    text = (user?.displayName ?: user?.username).let { name -> name?.firstOrNull { it.isLetterOrDigit() } ?: name?.firstOrNull() }?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(Spacing.Small))


        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user?.displayName ?: user?.username ?: stringResource(R.string.error_unknown_short),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )

                if (expiresAt != null) {
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    StoryExpiryCountdown(expiresAt = expiresAt)
                }
            }

            if (storyTime != null) {
                Text(
                    text = formatTimeAgo(storyTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
            }
        }


        IconButton(onClick = onMore) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.cd_more_options),
                tint = MaterialTheme.colorScheme.onPrimary
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

@Composable
fun StoryExpiryCountdown(expiresAt: String) {
    val context = LocalContext.current
    var remainingTime by remember(expiresAt) { mutableStateOf(calculateRemainingTime(expiresAt, context)) }

    LaunchedEffect(expiresAt) {
        while (true) {
            delay(60000L) // Update every minute
            remainingTime = calculateRemainingTime(expiresAt, context)
        }
    }

    Text(
        text = stringResource(R.string.story_expires_countdown, remainingTime),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
    )
}

private fun calculateRemainingTime(expiresAt: String, context: android.content.Context): String {
    return try {
        val expiry = Instant.parse(expiresAt)
        val now = Instant.now()
        val duration = Duration.between(now, expiry)
        val minutes = duration.toMinutes()

        when {
            minutes <= 0 -> "Expired"
            minutes < 5 -> context.getString(R.string.story_expiring_soon)
            minutes < 60 -> context.getString(R.string.story_expires_minutes, minutes.toInt())
            else -> {
                val hours = duration.toHours()
                val remMinutes = minutes % 60
                context.getString(R.string.story_expires_hours_minutes, hours.toInt(), remMinutes.toInt())
            }
        }
    } catch (e: Exception) {
        ""
    }
}

private fun formatTimeAgo(timestamp: String?): String {
    if (timestamp == null) return ""

    return try {
        val instant = Instant.parse(timestamp)
        val now = Instant.now()
        val duration = Duration.between(instant, now)

        when {
            duration.toMinutes() < 1 -> "Just now"
            duration.toMinutes() < 60 -> "${duration.toMinutes()}m"
            duration.toHours() < 24 -> "${duration.toHours()}h"
            else -> "${duration.toDays()}d"
        }
    } catch (e: Exception) {
        ""
    }
}
