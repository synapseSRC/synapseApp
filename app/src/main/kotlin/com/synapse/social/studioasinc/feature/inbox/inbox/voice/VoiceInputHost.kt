package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.components.LiveWaveformView
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.components.OdometerTimerText
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

private enum class VoiceVisualStage { Normal, VoiceReady, Pressing, Recording, Locked, VoiceDraft, Sending, Failed }

private fun VoiceState.visualStage(): VoiceVisualStage = when (this) {
    is VoiceState.Normal -> VoiceVisualStage.Normal
    is VoiceState.VoiceReady -> VoiceVisualStage.VoiceReady
    is VoiceState.Pressing -> VoiceVisualStage.Pressing
    is VoiceState.Recording -> VoiceVisualStage.Recording
    is VoiceState.Locked -> VoiceVisualStage.Locked
    is VoiceState.VoiceDraft -> VoiceVisualStage.VoiceDraft
    is VoiceState.Sending -> VoiceVisualStage.Sending
    is VoiceState.Failed -> VoiceVisualStage.Failed
}

private fun VoiceState.contentStage(): VoiceVisualStage = when (this) {
    is VoiceState.Pressing -> VoiceVisualStage.Normal
    else -> visualStage()
}

@Composable
fun VoiceInputHost(
    voiceState: VoiceState,
    onVoiceAction: (VoiceAction) -> Unit,
    showSendButton: Boolean,
    onSendMessage: () -> Unit,
    modifier: Modifier = Modifier,
    normalContent: @Composable () -> Unit
) {
    // Container without clipping so dragging the mic or lock target upward remains visible with headroom
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = Spacing.ExtraSmall)
            .padding(top = Spacing.ExtraSmall, bottom = Spacing.Small)
            .shadow(
                elevation = Spacing.ExtraSmall,
                shape = RoundedCornerShape(Sizes.CornerMassive),
                clip = false
            )
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(Sizes.CornerMassive)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(horizontal = Spacing.Small, vertical = Spacing.ExtraSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Morphing left/middle composer content area
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                AnimatedContent(
                    targetState = voiceState,
                    contentKey = { it.contentStage() },
                    transitionSpec = {
                        (fadeIn(animationSpec = spring(dampingRatio = 1f, stiffness = 1600f)) +
                            slideInVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f)) { it / 8 }) togetherWith
                            (fadeOut(animationSpec = spring(dampingRatio = 1f, stiffness = 1600f)) +
                                slideOutVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f)) { -it / 8 })
                    },
                    contentAlignment = Alignment.CenterStart,
                    label = "voiceStageMorph"
                ) { animatedState ->
                    if (animatedState.contentStage() == VoiceVisualStage.Normal) {
                        Box(modifier = Modifier.fillMaxWidth()) { normalContent() }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            VoiceInputBarContent(voiceState = animatedState, onVoiceAction = onVoiceAction)
                        }
                    }
                }
            }

            // Persistent Right Action Slot OUTSIDE AnimatedContent
            // Ensures pointerInput node on CanonicalMicButton is never recreated or disposed during Normal -> Pressing -> Recording
            val isNormal = voiceState is VoiceState.Normal
            if (isNormal && showSendButton) {
                Surface(
                    modifier = Modifier.size(Sizes.InputButtonCompact),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        IconButton(onClick = onSendMessage) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = stringResource(R.string.chat_action_send),
                                modifier = Modifier.size(Sizes.IconLarge),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            } else if (voiceState is VoiceState.Locked || voiceState is VoiceState.VoiceDraft || voiceState is VoiceState.Sending || voiceState is VoiceState.Failed) {
                // In Locked, VoiceDraft, Sending, and Failed states, VoiceInputBarContent renders its own right action buttons (Send/Retry)
                Spacer(modifier = Modifier.width(0.dp))
            } else {
                // Canonical Mic Button persistent across Normal (empty text), VoiceReady, Pressing, Recording
                CanonicalMicButton(
                    voiceState = voiceState,
                    onVoiceAction = onVoiceAction
                )
            }
        }
    }
}

@Composable
private fun VoiceInputBarContent(
    voiceState: VoiceState,
    onVoiceAction: (VoiceAction) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        when (voiceState) {
            is VoiceState.VoiceReady, is VoiceState.Pressing -> {
                IconButton(
                    onClick = { onVoiceAction(VoiceAction.CloseVoiceMode) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.action_cancel),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = stringResource(R.string.voice_hold_to_record),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Spacing.Medium)
                )
            }

            is VoiceState.Recording -> {
                val pulseInfinite = rememberInfiniteTransition(label = "recordingPulse")
                val pulseAlpha by pulseInfinite.animateFloat(
                    initialValue = 1f,
                    targetValue = 0.3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseAlpha"
                )
                val cancelProgress = voiceState.cancelProgress.coerceIn(0f, 1f)
                val cancelColor = lerp(MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.error, cancelProgress)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                ) {
                    IconButton(
                        onClick = { onVoiceAction(VoiceAction.CancelRecording) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.action_cancel),
                            tint = if (voiceState.isArmedForCancel) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                            Box(
                                modifier = Modifier.size(10.dp).graphicsLayer { alpha = pulseAlpha }
                                    .background(MaterialTheme.colorScheme.error, CircleShape)
                            )
                            LiveWaveformView(
                                amplitudeHistory = voiceState.amplitudeHistory,
                                modifier = Modifier.weight(1f).height(24.dp),
                                activeColor = if (voiceState.isArmedForCancel) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            OdometerTimerText(
                                durationMs = voiceState.durationMs,
                                color = if (voiceState.isArmedForCancel) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = cancelColor,
                                modifier = Modifier.size(12.dp).graphicsLayer { rotationZ = 180f }
                            )
                            Text(
                                text = stringResource(R.string.voice_cancel_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = cancelColor
                            )
                            LinearProgressIndicator(
                                progress = { cancelProgress },
                                modifier = Modifier.weight(1f).height(2.dp),
                                color = MaterialTheme.colorScheme.error,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }

            is VoiceState.Locked -> {
                // Pulsing recording dot indicator when active
                val pulseInfinite = rememberInfiniteTransition(label = "recordingPulseLocked")
                val pulseAlpha by pulseInfinite.animateFloat(
                    initialValue = 1f,
                    targetValue = 0.3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseAlphaLocked"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Clearly Destructive Delete Action
                    IconButton(
                        onClick = { onVoiceAction(VoiceAction.CancelRecording) },
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = Spacing.Small),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                    ) {
                        // Mic capture status indicator (Red pulse if recording, static gray if paused)
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .graphicsLayer { alpha = if (voiceState.isPaused) 0.5f else pulseAlpha }
                                .background(
                                    color = if (voiceState.isPaused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                                    shape = CircleShape
                                )
                        )

                        LiveWaveformView(
                            amplitudeHistory = voiceState.amplitudeHistory,
                            isPaused = voiceState.isPaused,
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp),
                            activeColor = MaterialTheme.colorScheme.primary
                        )

                        OdometerTimerText(
                            durationMs = voiceState.durationMs,
                            color = if (voiceState.isPaused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Visually Secondary / Tonal Pause / Resume Action
                    IconButton(
                        onClick = { onVoiceAction(VoiceAction.TogglePause) },
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (voiceState.isPaused) Icons.Default.Mic else Icons.Default.Pause,
                            contentDescription = stringResource(if (voiceState.isPaused) R.string.cd_play_audio else R.string.cd_pause_audio),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(Spacing.ExtraSmall))

                    // Dominant Primary Send Action
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shadowElevation = 2.dp
                    ) {
                        IconButton(
                            onClick = { onVoiceAction(VoiceAction.FinishRecording) },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(R.string.chat_action_send),
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            is VoiceState.VoiceDraft -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { onVoiceAction(VoiceAction.DiscardDraft) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    IconButton(
                        onClick = { onVoiceAction(VoiceAction.TogglePlayDraft) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (voiceState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = stringResource(if (voiceState.isPlaying) R.string.cd_pause_audio else R.string.cd_play_audio),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    val progress = if (voiceState.durationMs > 0) {
                        voiceState.playbackPositionMs.toFloat() / voiceState.durationMs
                    } else 0f

                    LiveWaveformView(
                        amplitudeHistory = voiceState.amplitudeHistory,
                        progressFraction = progress,
                        isDraftPlayback = true,
                        onSeek = { seekFrac ->
                            val posMs = (seekFrac * voiceState.durationMs).toLong()
                            onVoiceAction(VoiceAction.SeekDraft(posMs))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .padding(horizontal = Spacing.Small)
                    )

                    OdometerTimerText(durationMs = voiceState.durationMs)

                    Spacer(modifier = Modifier.width(Spacing.ExtraSmall))

                    IconButton(
                        onClick = { onVoiceAction(VoiceAction.SendDraft) },
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.chat_action_send),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            is VoiceState.Sending -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.Medium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(Spacing.Medium))
                    Text(
                        text = stringResource(R.string.voice_sending),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is VoiceState.Failed -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = voiceState.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = Spacing.Small)
                    )
                    TextButton(onClick = {
                        if (voiceState.draftFile != null) {
                            onVoiceAction(VoiceAction.SendDraft)
                        } else {
                            onVoiceAction(VoiceAction.OpenVoiceMode)
                        }
                    }) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }

            else -> {}
        }
    }
}

@Composable
fun CanonicalMicButton(
    voiceState: VoiceState,
    onVoiceAction: (VoiceAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPressing = voiceState is VoiceState.Pressing
    val isRecording = voiceState is VoiceState.Recording
    val dragOffsetX = (voiceState as? VoiceState.Recording)?.slideOffsetPx ?: 0f
    val dragOffsetY = (voiceState as? VoiceState.Recording)?.dragOffsetYPx ?: 0f

    val scale by animateFloatAsState(
        targetValue = if (isPressing || isRecording) 1.25f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "canonicalMicScale"
    )

    Box(
        modifier = modifier.size(44.dp),
        contentAlignment = Alignment.Center
    ) {
        if (voiceState is VoiceState.Recording) {
            val lockProgress = voiceState.lockProgress

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-68).dp)
                    .graphicsLayer {
                        alpha = 0.25f + 0.75f * lockProgress.coerceIn(0f, 1f)
                        scaleX = 0.9f + 0.1f * lockProgress.coerceIn(0f, 1f)
                        scaleY = 0.9f + 0.1f * lockProgress.coerceIn(0f, 1f)
                    }
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            color = if (lockProgress >= 0.9f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (lockProgress >= 0.9f) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = stringResource(R.string.voice_lock_hint),
                        tint = if (lockProgress >= 0.9f) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = dragOffsetX
                    translationY = dragOffsetY
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        onVoiceAction(VoiceAction.StartRecording)

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null) {
                                onVoiceAction(VoiceAction.ReleaseRecording)
                                break
                            }
                            if (!change.pressed) {
                                change.consume()
                                onVoiceAction(VoiceAction.ReleaseRecording)
                                break
                            }

                            val posChange = change.positionChange()
                            if (posChange.x != 0f || posChange.y != 0f) {
                                change.consume()
                                onVoiceAction(VoiceAction.Dragged(posChange.x, posChange.y))
                            }
                        }
                    }
                }
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = stringResource(R.string.voice_hold_to_record),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
