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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
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
    val isRecordingMode = voiceState is VoiceState.Recording || voiceState is VoiceState.Locked || voiceState is VoiceState.Pressing

    val currentAmplitude = when (voiceState) {
        is VoiceState.Recording -> voiceState.amplitudeHistory.lastOrNull() ?: 0.1f
        is VoiceState.Locked -> voiceState.amplitudeHistory.lastOrNull() ?: 0.1f
        else -> 0f
    }

    val smoothedAmp by animateFloatAsState(
        targetValue = currentAmplitude.coerceIn(0.08f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "smoothedAmp"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "gradientFlow")
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradientShift"
    )

    val fixedPillShape = RoundedCornerShape(Sizes.CornerMassive)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = Spacing.ExtraSmall)
            .padding(top = Spacing.ExtraSmall, bottom = Spacing.Small)
    ) {
        // Ambient Glow / Halo effect spilling onto surrounding environment
        if (isRecordingMode) {
            val glowAlpha = (0.30f + smoothedAmp * 0.50f).coerceIn(0f, 0.85f)
            val glowSpreadRadius = (12 + smoothedAmp * 24)

            Box(
                modifier = Modifier
                    .matchParentSize()

                    .drawBehind {
                        val colorCyan = Color(0xFF00F0FF).copy(alpha = glowAlpha)
                        val colorPurple = Color(0xFFA855F7).copy(alpha = glowAlpha)
                        val colorBlue = Color(0xFF2979FF).copy(alpha = glowAlpha)

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(colorCyan, colorBlue, Color.Transparent),
                                center = Offset(size.width * 0.3f, size.height * 0.5f),
                                radius = size.width * 0.6f + glowSpreadRadius
                            )
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(colorPurple, colorCyan, Color.Transparent),
                                center = Offset(size.width * 0.7f, size.height * 0.5f),
                                radius = size.width * 0.6f + glowSpreadRadius
                            )
                        )
                    }
            )
        }

        // Fixed Shape Recording / Input Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = Spacing.ExtraSmall,
                    shape = fixedPillShape,
                    clip = false
                )
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = fixedPillShape
                )
                .then(
                    if (isRecordingMode) {
                        Modifier.background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF00F0FF).copy(alpha = 0.22f + smoothedAmp * 0.28f),
                                    Color(0xFF2979FF).copy(alpha = 0.25f + smoothedAmp * 0.30f),
                                    Color(0xFFA855F7).copy(alpha = 0.22f + smoothedAmp * 0.28f),
                                    Color(0xFF00F0FF).copy(alpha = 0.22f + smoothedAmp * 0.28f)
                                ),
                                start = Offset(gradientShift, 0f),
                                end = Offset(gradientShift + 600f, 250f)
                            ),
                            shape = fixedPillShape
                        )
                    } else Modifier
                )
                .clip(fixedPillShape)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = Spacing.Small, vertical = Spacing.ExtraSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Morphing left/middle composer content area
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
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
                        contentAlignment = Alignment.Center,
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
                    Spacer(modifier = Modifier.width(0.dp))
                } else {
                    CanonicalMicButton(
                        voiceState = voiceState,
                        onVoiceAction = onVoiceAction
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceInputBarContent(
    voiceState: VoiceState,
    onVoiceAction: (VoiceAction) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        when (voiceState) {
            is VoiceState.VoiceReady, is VoiceState.Pressing -> {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = { onVoiceAction(VoiceAction.CloseVoiceMode) }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Close, stringResource(R.string.action_cancel), tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Text(stringResource(R.string.voice_hold_to_record), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f).padding(horizontal = Spacing.Medium))
                }
            }
            is VoiceState.Recording -> {
                // Timer centered horizontally, cancellation text directly beneath timer
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OdometerTimerText(
                        durationMs = voiceState.durationMs,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val cancelText = if (voiceState.isArmedForCancel) {
                        stringResource(R.string.voice_release_to_cancel)
                    } else {
                        stringResource(R.string.voice_cancel_hint)
                    }

                    val cancelTextColor = if (voiceState.isArmedForCancel) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    AnimatedContent(
                        targetState = cancelText,
                        transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                        label = "cancelTextTransition"
                    ) { text ->
                        ShimmerHintText(
                            text = text,
                            baseColor = cancelTextColor,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
            is VoiceState.Locked -> {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = { onVoiceAction(VoiceAction.CancelRecording) }, modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.errorContainer, CircleShape)) {
                        Icon(Icons.Default.Delete, stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                    Row(modifier = Modifier.weight(1f).padding(horizontal = Spacing.Small), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                        VoiceWaveformBars(voiceState.amplitudeHistory, Modifier.weight(1f).height(28.dp), voiceState.isPaused)
                        OdometerTimerText(voiceState.durationMs, color = if (voiceState.isPaused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
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
private fun VoiceWaveformBars(amplitudeHistory: List<Float>, modifier: Modifier = Modifier, isPaused: Boolean = false) {
    val recentSamples = remember(amplitudeHistory) { amplitudeHistory.takeLast(7) }
    val barColor = MaterialTheme.colorScheme.primary
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(7) { index ->
            val sample = if (isPaused) 0.12f else recentSamples.getOrNull(index) ?: 0.08f
            val targetHeight = (4f + sample.coerceIn(0f, 1f) * 18f).dp
            val barHeight by animateDpAsState(targetValue = targetHeight, animationSpec = spring(dampingRatio = 0.8f, stiffness = 800f), label = "voiceWaveBar$index")
            Box(Modifier.width(3.dp).height(barHeight).clip(CircleShape).background(barColor))
        }
    }
}

@Composable
private fun ShimmerHintText(
    text: String,
    modifier: Modifier = Modifier,
    baseColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val density = LocalDensity.current
    val sweepWidth = with(density) { 112.dp.toPx() }
    val sweepOffset = remember(sweepWidth) { Animatable(-sweepWidth) }
    LaunchedEffect(sweepWidth) {
        while (true) {
            sweepOffset.animateTo(
                targetValue = sweepWidth,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f)
            )
            sweepOffset.animateTo(
                targetValue = -sweepWidth,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f)
            )
        }
    }
    val shimmerBrush = Brush.linearGradient(
        listOf(baseColor.copy(alpha = 0.40f), baseColor, baseColor.copy(alpha = 0.40f)),
        start = Offset(sweepOffset.value, 0f), end = Offset(sweepOffset.value + sweepWidth, 0f)
    )
    Text(text, modifier = modifier, style = MaterialTheme.typography.labelSmall.copy(brush = shimmerBrush))
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

            // Straight vertical slide-up gesture hint indicator aligned near mic button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-80).dp)
                    .graphicsLayer {
                        alpha = (0.35f + 0.65f * lockProgress.coerceIn(0f, 1f))
                    }
            ) {
                // Straight upward chevron motion
                val arrowMotion = remember { Animatable(4f) }
                LaunchedEffect(arrowMotion) {
                    while (true) {
                        arrowMotion.animateTo(
                            targetValue = -4f,
                            animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f)
                        )
                        arrowMotion.animateTo(
                            targetValue = 4f,
                            animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = arrowMotion.value.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = if (lockProgress >= 0.8f) Color(0xFF00F0FF) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                        modifier = Modifier.size(18.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = if (lockProgress >= 0.8f) Color(0xFF00F0FF).copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp).offset(y = (-6).dp)
                    )
                }

                Text(
                    text = stringResource(R.string.voice_slide_up_to_lock),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (lockProgress >= 0.8f) Color(0xFF00F0FF) else MaterialTheme.colorScheme.onSurfaceVariant
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
                .shadow(
                    elevation = if (isPressing || isRecording) 8.dp else 2.dp,
                    shape = CircleShape,
                    ambientColor = Color(0xFF00F0FF),
                    spotColor = Color(0xFFA855F7)
                )
                .background(
                    brush = if (isPressing || isRecording) {
                        Brush.linearGradient(
                            listOf(Color(0xFF00F0FF), Color(0xFF2979FF), Color(0xFFA855F7))
                        )
                    } else {
                        SolidColor(MaterialTheme.colorScheme.primary)
                    },
                    shape = CircleShape
                ),
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
