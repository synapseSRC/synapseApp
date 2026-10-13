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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.components.LiveWaveformView
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.components.OdometerTimerText
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.auth.ui.util.AnimationUtil

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
    ) {
        VoiceRecordingGlowSurface(
            voiceState = voiceState,
            modifier = Modifier.matchParentSize()
        )

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
                    modifier = Modifier.size(Sizes.SendButton),
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

        AnimatedVisibility(
            visible = voiceState is VoiceState.Recording,
            modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
            enter = fadeIn(animationSpec = spring(dampingRatio = 1f, stiffness = 1600f)),
            exit = fadeOut(animationSpec = spring(dampingRatio = 1f, stiffness = 1600f))
        ) {
            (voiceState as? VoiceState.Recording)?.let { recording ->
                RecordingStatusOverlay(recording)
            }
        }
    }
}

@Composable
private fun VoiceInputBarContent(
    voiceState: VoiceState,
    onVoiceAction: (VoiceAction) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        when (voiceState) {
            is VoiceState.VoiceReady, is VoiceState.Pressing -> {
                IconButton(onClick = { onVoiceAction(VoiceAction.CloseVoiceMode) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Close, stringResource(R.string.action_cancel), tint = MaterialTheme.colorScheme.onSurface)
                }
                Text(stringResource(R.string.voice_hold_to_record), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f).padding(horizontal = Spacing.Medium))
            }
            is VoiceState.Recording -> {
                Spacer(modifier = Modifier.fillMaxWidth().height(1.dp))
            }
            is VoiceState.Locked -> {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = { onVoiceAction(VoiceAction.CancelRecording) }, modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.errorContainer, CircleShape)) {
                        Icon(Icons.Default.Delete, stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = Spacing.Small),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)
                    ) {
                        OdometerTimerText(
                            voiceState.durationMs,
                            color = if (voiceState.isPaused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(if (voiceState.isPaused) R.string.voice_recording_paused_state else R.string.voice_recording_locked_state),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                    IconButton(onClick = { onVoiceAction(VoiceAction.TogglePause) }, modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape)) {
                        Icon(imageVector = if (voiceState.isPaused) Icons.Default.Mic else Icons.Default.Pause,
                            contentDescription = stringResource(if (voiceState.isPaused) R.string.cd_play_audio else R.string.cd_pause_audio), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(Modifier.width(Spacing.ExtraSmall))
                    Surface(modifier = Modifier.size(Sizes.SendButton), shape = CircleShape, color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary, shadowElevation = 2.dp) {
                        IconButton(onClick = { onVoiceAction(VoiceAction.FinishRecording) }, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.chat_action_send), tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(Sizes.IconLarge))
                        }
                    }
                }
            }
            is VoiceState.VoiceDraft -> {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = { onVoiceAction(VoiceAction.DiscardDraft) }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Delete, stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error)
                    }
                    IconButton(onClick = { onVoiceAction(VoiceAction.TogglePlayDraft) }, modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape)) {
                        Icon(imageVector = if (voiceState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = stringResource(if (voiceState.isPlaying) R.string.cd_pause_audio else R.string.cd_play_audio), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    val progress = if (voiceState.durationMs > 0) voiceState.playbackPositionMs.toFloat() / voiceState.durationMs else 0f
                    LiveWaveformView(amplitudeHistory = voiceState.amplitudeHistory, progressFraction = progress, isDraftPlayback = true,
                        onSeek = { onVoiceAction(VoiceAction.SeekDraft((it * voiceState.durationMs).toLong())) }, modifier = Modifier.weight(1f).height(32.dp), activeColor = MaterialTheme.colorScheme.primary)
                    OdometerTimerText(voiceState.durationMs, color = MaterialTheme.colorScheme.onSurface)
                    Surface(modifier = Modifier.size(Sizes.SendButton), shape = CircleShape, color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                        IconButton(onClick = { onVoiceAction(VoiceAction.SendDraft) }, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.chat_action_send), modifier = Modifier.size(Sizes.IconLarge))
                        }
                    }
                }
            }
            is VoiceState.Sending -> {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Medium), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(Spacing.Medium))
                    Text(stringResource(R.string.voice_sending), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            is VoiceState.Failed -> {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(voiceState.error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f).padding(horizontal = Spacing.Small))
                    TextButton(onClick = { if (voiceState.draftFile != null) onVoiceAction(VoiceAction.SendDraft) else onVoiceAction(VoiceAction.OpenVoiceMode) }) { Text(stringResource(R.string.retry)) }
                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun RecordingStatusOverlay(voiceState: VoiceState.Recording) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)
    ) {
        OdometerTimerText(durationMs = voiceState.durationMs, color = MaterialTheme.colorScheme.onSurface)
        AnimatedContent(
            targetState = voiceState.isArmedForCancel,
            transitionSpec = {
                fadeIn(animationSpec = spring(dampingRatio = 1f, stiffness = 1600f)) togetherWith
                    fadeOut(animationSpec = spring(dampingRatio = 1f, stiffness = 1600f))
            },
            contentAlignment = Alignment.Center,
            label = "voiceCancelFeedback"
        ) { isArmedForCancel ->
            Text(
                text = stringResource(if (isArmedForCancel) R.string.voice_recording_cancel_release else R.string.voice_recording_cancel),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BoxScope.SlideToLockIndicator(progress: Float) {
    val reducedMotion = AnimationUtil.rememberReducedMotion()
    val arrowMotion = remember { Animatable(3f) }
    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            arrowMotion.snapTo(0f)
        } else {
            while (true) {
                arrowMotion.animateTo(
                    targetValue = -3f,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f)
                )
                arrowMotion.animateTo(
                    targetValue = 3f,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f)
                )
            }
        }
    }
    val arrowTravel = if (reducedMotion) 0f else arrowMotion.value
    val alpha = 0.55f + 0.45f * progress.coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .align(Alignment.TopCenter)
            // Project design decision (not an M3 token): position the gesture cue just above the mic.
            .offset(y = (-72).dp)
            .graphicsLayer { this.alpha = if (reducedMotion) 0.72f else alpha },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)
    ) {
        Box(modifier = Modifier.size(width = 24.dp, height = 36.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .width(2.dp)
                    .height(22.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0f)
                            )
                        ),
                        shape = RoundedCornerShape(Spacing.ExtraSmall)
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = if (reducedMotion) 0.dp else arrowTravel.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
            }
        }
        Text(
            text = stringResource(R.string.voice_recording_lock_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
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
    val isActive = isPressing || isRecording
    val dragOffsetX = (voiceState as? VoiceState.Recording)?.slideOffsetPx ?: 0f
    val dragOffsetY = (voiceState as? VoiceState.Recording)?.dragOffsetYPx ?: 0f

    // Project design decision (not an M3 token): restrained scale and halo preserve the fixed pill silhouette.
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "canonicalMicScale"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isActive) 0.42f else 0f,
        animationSpec = spring(dampingRatio = 1f, stiffness = 3800f),
        label = "canonicalMicGlow"
    )
    val glowElevation by animateDpAsState(
        targetValue = if (isActive) 5.dp else 0.dp,
        animationSpec = spring(dampingRatio = 1f, stiffness = 3800f),
        label = "canonicalMicElevation"
    )

    Box(
        modifier = modifier
            .size(48.dp)
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
            },
        contentAlignment = Alignment.Center
    ) {
        if (voiceState is VoiceState.Recording) {
            SlideToLockIndicator(progress = voiceState.lockProgress)
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = dragOffsetX
                    translationY = dragOffsetY
                }
                .shadow(
                    elevation = glowElevation,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha),
                    spotColor = MaterialTheme.colorScheme.tertiary.copy(alpha = glowAlpha * 0.8f)
                )
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = stringResource(R.string.voice_hold_to_record),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(Sizes.IconLarge)
            )
        }
    }
}
