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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.components.LiveWaveformView
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.components.OdometerTimerText
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

internal fun voiceCancelTint(normal: Color, cancel: Color, progress: Float): Color =
    lerp(normal, cancel, progress.coerceIn(0f, 1f))

private const val CANCEL_SHIMMER_BAND_HALF_WIDTH = 0.10f
private const val ACTIVE_MIC_SCALE = 1.08f
private const val ACTIVE_MIC_GLOW_ALPHA = 0.42f
private const val MIC_SCALE_DAMPING_RATIO = 0.6f
private const val LOCK_ARROW_START_OFFSET_DP = 3f
private const val LOCK_ARROW_END_OFFSET_DP = -3f
private const val LOCK_ARROW_BOUNCE_DURATION_MILLIS = 1_800
private const val LOCK_ARROW_GRADIENT_CLEAR_ALPHA = 0f
private const val LOCK_ARROW_GRADIENT_CORE_ALPHA = 0.75f
private val MIC_TOUCH_TARGET_SIZE = 48.dp
private val MIC_VISUAL_SIZE = 44.dp
private val LOCK_INDICATOR_GAP = Spacing.Small
private val LOCK_INDICATOR_WIDTH = 144.dp
private val VOICE_BAR_MIN_HEIGHT = 52.dp + Spacing.ExtraSmall + Spacing.Small
// 6.dp follows the Material 3 level-3 elevation step; the halo color remains a theme surface color.
private val ACTIVE_MIC_GLOW_ELEVATION = 6.dp
private val LOCK_ARROW_WIDTH = 24.dp
private val LOCK_ARROW_HEIGHT = 36.dp
private val LOCK_ARROW_STEM_WIDTH = 2.dp
private val LOCK_ARROW_STEM_HEIGHT = 22.dp
private val LOCK_ARROW_ICON_SIZE = 20.dp

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
    reducedMotionOverride: Boolean,
    modifier: Modifier = Modifier,
    normalContent: @Composable () -> Unit
) {
    val reducedMotion = reducedMotionOverride
    val density = LocalDensity.current
    var lockIndicatorHeightPx by remember { mutableIntStateOf(0) }
    var hostBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
    var micBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
    var lastMicBoundsInHost by remember { mutableStateOf<Rect?>(null) }
    var lastLockGestureOffsetYPx by remember { mutableFloatStateOf(0f) }
    var lockReturnStarted by remember { mutableStateOf(false) }
    var lockReturnStartYPx by remember { mutableFloatStateOf(0f) }
    val lockReturnYPx = remember { Animatable(0f) }
    val isLocked = voiceState is VoiceState.Locked

    val micBoundsInHost = remember(hostBoundsInRoot, micBoundsInRoot) {
        val host = hostBoundsInRoot
        val mic = micBoundsInRoot
        if (host == null || mic == null) null else Rect(
            left = mic.left - host.left,
            top = mic.top - host.top,
            right = mic.right - host.left,
            bottom = mic.bottom - host.top
        )
    }
    SideEffect {
        micBoundsInHost?.let { lastMicBoundsInHost = it }
    }

    // A lock changes the recording UI immediately, but the mic's displaced visual completes its spring return.
    LaunchedEffect(isLocked, reducedMotion) {
        if (isLocked && lastLockGestureOffsetYPx < 0f && !reducedMotion) {
            lockReturnStartYPx = lastLockGestureOffsetYPx
            lockReturnYPx.snapTo(lastLockGestureOffsetYPx)
            lockReturnStarted = true
            lockReturnYPx.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
            )
            lastLockGestureOffsetYPx = 0f
            lockReturnStarted = false
        } else {
            lockReturnStarted = false
            lastLockGestureOffsetYPx = 0f
            lockReturnStartYPx = 0f
            lockReturnYPx.snapTo(0f)
        }
    }

    val lockLaneIsNeeded = voiceState is VoiceState.Recording || (isLocked && lastLockGestureOffsetYPx < 0f && !reducedMotion)
    val lockLaneHeight = if (lockLaneIsNeeded) {
        with(density) { lockIndicatorHeightPx.toDp() } +
            LOCK_INDICATOR_GAP + VoiceGestureConfig().lockThresholdDp.dp
    } else {
        0.dp
    }
    val animatedLockLaneHeight = animateDpForMotion(
        targetValue = lockLaneHeight,
        reducedMotion = reducedMotion,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow),
        label = "voiceLockHintHeadroom"
    )

    // Measured headroom follows the actual localized/font-scaled cue and the configured upward gesture travel.
    // The pill stays at the bottom while the chat list can reserve the host's full measured height.
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(Modifier.height(animatedLockLaneHeight))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = VOICE_BAR_MIN_HEIGHT)
                .padding(horizontal = Spacing.ExtraSmall)
                .padding(top = Spacing.ExtraSmall, bottom = Spacing.Small)
                .onGloballyPositioned { hostBoundsInRoot = it.boundsInRoot() }
                .testTag("voice-recording-bar")
                .shadow(
                    elevation = Spacing.ExtraSmall,
                    shape = RoundedCornerShape(Sizes.CornerMassive),
                    clip = false
                )
        ) {
            VoiceRecordingGlowSurface(
                voiceState = voiceState,
                reducedMotionOverride = reducedMotion,
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
                            if (reducedMotion) {
                                EnterTransition.None togetherWith ExitTransition.None
                            } else {
                                (fadeIn(animationSpec = spring(dampingRatio = 1f, stiffness = 1600f)) +
                                    slideInVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f)) { it / 8 }) togetherWith
                                    (fadeOut(animationSpec = spring(dampingRatio = 1f, stiffness = 1600f)) +
                                        slideOutVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f)) { -it / 8 })
                            }
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

                // Keep the mic recognizer outside AnimatedContent across Normal/Pressing/Recording.
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
                        onVoiceAction = onVoiceAction,
                        reducedMotionOverride = reducedMotion,
                        onMicBoundsChanged = { micBoundsInRoot = it },
                        onGestureOffsetChanged = { lastLockGestureOffsetYPx = it }
                    )
                }
            }

            // This overlay shares the pill's bounds; its cue is laid out from the measured mic anchor.
            Box(Modifier.matchParentSize()) {
                VoiceRecordingContentOverlay(
                    voiceState = voiceState,
                    reducedMotion = reducedMotion,
                    micBoundsInHost = micBoundsInHost,
                    lockIndicatorHeightPx = lockIndicatorHeightPx,
                    onLockIndicatorHeightChanged = { lockIndicatorHeightPx = it }
                )
            }

            if (lockReturnStarted) {
                lastMicBoundsInHost?.let { micBounds ->
                    val returnOffset = lockReturnYPx.value
                    val returnAlpha = if (lockReturnStartYPx == 0f) 0f else
                        (kotlin.math.abs(returnOffset / lockReturnStartYPx)).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .absoluteOffset {
                                IntOffset(
                                    x = micBounds.left.roundToInt(),
                                    y = (micBounds.top + returnOffset).roundToInt()
                                )
                            }
                            .size(MIC_TOUCH_TARGET_SIZE)
                            .graphicsLayer { alpha = returnAlpha }
                            .testTag("voice-lock-returning-mic"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(MIC_VISUAL_SIZE)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                .testTag("voice-lock-returning-mic-visual"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(Sizes.IconLarge)
                            )
                        }
                    }
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
                // The single, full-width status layer is drawn over this row in VoiceInputHost so it stays centered across the mic slot.
                Spacer(modifier = Modifier.fillMaxWidth().height(1.dp))
            }
            is VoiceState.Locked -> {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = { onVoiceAction(VoiceAction.CancelRecording) }, modifier = Modifier.size(MIC_TOUCH_TARGET_SIZE).testTag("voice-locked-cancel")) {
                        Icon(Icons.Default.Delete, stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error)
                    }
                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = Spacing.Small),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)
                    ) {
                        OdometerTimerText(
                            voiceState.durationMs,
                            modifier = Modifier.testTag("voice-locked-timer"),
                            color = if (voiceState.isPaused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(if (voiceState.isPaused) R.string.voice_recording_paused_state else R.string.voice_recording_locked_state),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("voice-locked-status"),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                    IconButton(onClick = { onVoiceAction(VoiceAction.TogglePause) }, modifier = Modifier.size(MIC_TOUCH_TARGET_SIZE).testTag("voice-locked-pause")) {
                        Icon(
                            imageVector = if (voiceState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = stringResource(if (voiceState.isPaused) R.string.cd_play_audio else R.string.cd_pause_audio),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.width(Spacing.Small))
                    Surface(modifier = Modifier.size(Sizes.SendButton), shape = CircleShape, color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary, shadowElevation = 2.dp) {
                        IconButton(onClick = { onVoiceAction(VoiceAction.FinishRecording) }, modifier = Modifier.fillMaxSize().testTag("voice-locked-finish")) {
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
private fun VoiceRecordingContentOverlay(
    voiceState: VoiceState,
    reducedMotion: Boolean,
    micBoundsInHost: Rect?,
    lockIndicatorHeightPx: Int,
    onLockIndicatorHeightChanged: (Int) -> Unit
) {
    AnimatedVisibility(
        visible = voiceState is VoiceState.Recording,
        modifier = Modifier.fillMaxSize(),
        enter = if (reducedMotion) EnterTransition.None else fadeIn(animationSpec = spring(dampingRatio = 1f, stiffness = 1600f)),
        exit = if (reducedMotion) ExitTransition.None else fadeOut(animationSpec = spring(dampingRatio = 1f, stiffness = 1600f))
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val recording = voiceState as? VoiceState.Recording
            if (recording != null) {
                RecordingStatusOverlay(recording, reducedMotion, Modifier.align(Alignment.Center))
                micBoundsInHost?.let { micBounds ->
                    val safeWidth = (maxWidth - Spacing.ExtraSmall - Spacing.ExtraSmall).coerceAtLeast(0.dp)
                    val indicatorWidth = minOf(LOCK_INDICATOR_WIDTH, safeWidth)
                    val parentWidthPx = with(density) { maxWidth.toPx() }
                    val indicatorWidthPx = with(density) { indicatorWidth.toPx() }
                    val arrowWidthPx = with(density) { LOCK_ARROW_WIDTH.toPx() }
                    val gapPx = with(density) { LOCK_INDICATOR_GAP.toPx() }
                    val micCenterX = (micBounds.left + micBounds.right) / 2f
                    val micTop = micBounds.top + recording.dragOffsetYPx
                    val leftPx = (micCenterX - indicatorWidthPx / 2f)
                        .coerceIn(0f, (parentWidthPx - indicatorWidthPx).coerceAtLeast(0f))
                    val arrowLeftPx = (micCenterX - leftPx - arrowWidthPx / 2f)
                        .coerceIn(0f, (indicatorWidthPx - arrowWidthPx).coerceAtLeast(0f))
                    val topPx = micTop - lockIndicatorHeightPx - gapPx

                    SlideToLockIndicator(
                        progress = recording.lockProgress,
                        reducedMotion = reducedMotion,
                        columnWidth = indicatorWidth,
                        arrowOffsetX = with(density) { arrowLeftPx.toDp() },
                        measuredHeightPx = lockIndicatorHeightPx,
                        onHeightChanged = onLockIndicatorHeightChanged,
                        modifier = Modifier
                            .absoluteOffset { IntOffset(leftPx.roundToInt(), topPx.roundToInt()) }
                            .width(indicatorWidth)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingStatusOverlay(voiceState: VoiceState.Recording, reducedMotion: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.Medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)
    ) {
        OdometerTimerText(
            durationMs = voiceState.durationMs,
            modifier = Modifier.testTag("voice-recording-timer"),
            color = MaterialTheme.colorScheme.onSurface
        )
        ShimmerCancelHint(reducedMotion)
    }
}

@Composable
private fun ShimmerCancelHint(reducedMotion: Boolean) {
    val baseColor = MaterialTheme.colorScheme.onSurfaceVariant
    val highlightColor = lerp(baseColor, MaterialTheme.colorScheme.onSurface, 0.68f)
    val phase = rememberVoiceShimmerPhase(reducedMotion, "voiceCancelShimmer")
    val center = 0.15f + phase * 0.70f
    val start = (center - CANCEL_SHIMMER_BAND_HALF_WIDTH).coerceIn(0f, 1f)
    val end = (center + CANCEL_SHIMMER_BAND_HALF_WIDTH).coerceIn(0f, 1f)
    val hintBrush = if (reducedMotion) null else Brush.horizontalGradient(
        colorStops = arrayOf(
            0f to baseColor,
            start to baseColor,
            center.coerceIn(0f, 1f) to highlightColor,
            end to baseColor,
            1f to baseColor
        )
    )
    Text(
        text = stringResource(R.string.voice_recording_cancel),
        modifier = Modifier.fillMaxWidth().testTag("voice-cancel-instruction"),
        style = if (hintBrush == null) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodySmall.copy(brush = hintBrush),
        color = if (hintBrush == null) baseColor else Color.Unspecified,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Clip,
        softWrap = true
    )
}

@Composable
private fun SlideToLockIndicator(
    progress: Float,
    reducedMotion: Boolean,
    columnWidth: androidx.compose.ui.unit.Dp,
    arrowOffsetX: androidx.compose.ui.unit.Dp,
    measuredHeightPx: Int,
    onHeightChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val arrowTravel = rememberLockArrowTravel(reducedMotion)
    val alpha = when {
        measuredHeightPx == 0 -> 0f
        reducedMotion -> 0.72f
        else -> 0.55f + 0.45f * progress.coerceIn(0f, 1f)
    }

    Column(
        modifier = modifier
            .width(columnWidth)
            .onSizeChanged { onHeightChanged(it.height) }
            .graphicsLayer { this.alpha = alpha },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(LOCK_ARROW_HEIGHT)) {
            Box(
                modifier = Modifier
                    .absoluteOffset(x = arrowOffsetX)
                    .size(width = LOCK_ARROW_WIDTH, height = LOCK_ARROW_HEIGHT)
                    .testTag("voice-lock-arrow"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .width(LOCK_ARROW_STEM_WIDTH)
                        .height(LOCK_ARROW_STEM_HEIGHT)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = LOCK_ARROW_GRADIENT_CLEAR_ALPHA),
                                    MaterialTheme.colorScheme.primary.copy(alpha = LOCK_ARROW_GRADIENT_CORE_ALPHA),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = LOCK_ARROW_GRADIENT_CLEAR_ALPHA)
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
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(LOCK_ARROW_ICON_SIZE))
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(LOCK_ARROW_ICON_SIZE))
                }
            }
        }
        Text(
            text = stringResource(R.string.voice_recording_lock_hint),
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.ExtraSmall).testTag("voice-slide-lock-hint"),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            softWrap = true
        )
    }
}

@Composable
private fun rememberLockArrowTravel(reducedMotion: Boolean): Float {
    if (reducedMotion) return 0f
    val transition = rememberInfiniteTransition(label = "voiceLockArrow")
    val arrowTravel by transition.animateFloat(
        initialValue = LOCK_ARROW_START_OFFSET_DP,
        targetValue = LOCK_ARROW_END_OFFSET_DP,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = LOCK_ARROW_BOUNCE_DURATION_MILLIS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voiceLockArrowTravel"
    )
    return arrowTravel
}

@Composable
private fun animateFloatForMotion(
    targetValue: Float,
    reducedMotion: Boolean,
    animationSpec: AnimationSpec<Float>,
    label: String
): Float = if (reducedMotion) targetValue else animateFloatAsState(targetValue, animationSpec, label = label).value

@Composable
private fun animateDpForMotion(
    targetValue: androidx.compose.ui.unit.Dp,
    reducedMotion: Boolean,
    animationSpec: AnimationSpec<androidx.compose.ui.unit.Dp>,
    label: String
): androidx.compose.ui.unit.Dp = if (reducedMotion) targetValue else animateDpAsState(targetValue, animationSpec, label = label).value

@Composable
fun CanonicalMicButton(
    voiceState: VoiceState,
    onVoiceAction: (VoiceAction) -> Unit,
    reducedMotionOverride: Boolean,
    modifier: Modifier = Modifier,
    onMicBoundsChanged: (Rect) -> Unit = {},
    onGestureOffsetChanged: (Float) -> Unit = {}
) {
    val reducedMotion = reducedMotionOverride
    val isPressing = voiceState is VoiceState.Pressing
    val isRecording = voiceState is VoiceState.Recording
    val isActive = isPressing || isRecording
    val recordingState = voiceState as? VoiceState.Recording
    val dragOffsetX = recordingState?.slideOffsetPx ?: 0f
    val dragOffsetY = recordingState?.dragOffsetYPx ?: 0f
    val cancelProgress = recordingState?.cancelProgress?.coerceIn(0f, 1f) ?: 0f
    val latestDragOffsetX by rememberUpdatedState(dragOffsetX)
    val latestDragOffsetY by rememberUpdatedState(dragOffsetY)
    val latestVoiceAction by rememberUpdatedState(onVoiceAction)
    val latestMicBoundsChanged by rememberUpdatedState(onMicBoundsChanged)
    val latestGestureOffsetChanged by rememberUpdatedState(onGestureOffsetChanged)
    val lockTravelThresholdPx = with(LocalDensity.current) {
        VoiceGestureConfig().lockThresholdDp.dp.toPx()
    }
    var isReturning by remember { mutableStateOf(false) }
    var returnStartX by remember { mutableFloatStateOf(0f) }
    var returnStartY by remember { mutableFloatStateOf(0f) }
    val returnX = remember { Animatable(0f) }
    val returnY = remember { Animatable(0f) }

    // Return offsets and animation lifetime are UI state; the recording model only stores live gesture progress.
    LaunchedEffect(isReturning, reducedMotion) {
        if (!isReturning) return@LaunchedEffect
        if (reducedMotion) {
            returnX.snapTo(0f)
            returnY.snapTo(0f)
        } else {
            returnX.snapTo(returnStartX)
            returnY.snapTo(returnStartY)
            coroutineScope {
                launch { returnX.animateTo(0f, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)) }
                launch { returnY.animateTo(0f, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)) }
            }
        }
        isReturning = false
    }

    val targetScale = if (isActive) ACTIVE_MIC_SCALE else 1f
    val scale = animateFloatForMotion(
        targetValue = targetScale,
        reducedMotion = reducedMotion,
        animationSpec = spring(dampingRatio = MIC_SCALE_DAMPING_RATIO, stiffness = Spring.StiffnessMedium),
        label = "canonicalMicScale"
    )
    val targetTint = voiceCancelTint(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.error, cancelProgress)
    val micTint by animateColorAsState(
        targetValue = targetTint,
        animationSpec = if (reducedMotion) snap() else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 520f),
        label = "voiceCancelDragTint"
    )

    val targetGlowAlpha = if (isActive) ACTIVE_MIC_GLOW_ALPHA else 0f
    val glowAlpha = animateFloatForMotion(
        targetValue = targetGlowAlpha,
        reducedMotion = reducedMotion,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "canonicalMicGlow"
    )
    val targetGlowElevation = if (isActive) ACTIVE_MIC_GLOW_ELEVATION else 0.dp
    val glowElevation = animateDpForMotion(
        targetValue = targetGlowElevation,
        reducedMotion = reducedMotion,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "canonicalMicElevation"
    )

    Box(
        modifier = modifier
            .size(MIC_TOUCH_TARGET_SIZE)
            .onGloballyPositioned { latestMicBoundsChanged(it.boundsInRoot()) }
            .testTag("voice-recording-mic")
            .pointerInput(lockTravelThresholdPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isReturning = false
                    latestGestureOffsetChanged(0f)
                    var gestureStarted = false
                    var gestureFinished = false
                    var gestureOffsetY = 0f
                    try {
                        latestVoiceAction(VoiceAction.StartRecording)
                        gestureStarted = true
                        var gestureActive = true
                        while (gestureActive) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) {
                                change?.consume()
                                val returnXOffset = latestDragOffsetX
                                val returnYOffset = latestDragOffsetY
                                gestureFinished = true
                                latestVoiceAction(VoiceAction.ReleaseRecording)
                                if (returnXOffset != 0f || returnYOffset != 0f) {
                                    returnStartX = returnXOffset
                                    returnStartY = returnYOffset
                                    isReturning = true
                                }
                                gestureActive = false
                            } else {
                                val posChange = change.positionChange()
                                if (posChange.x != 0f || posChange.y != 0f) {
                                    change.consume()
                                    gestureOffsetY = (gestureOffsetY + posChange.y).coerceAtLeast(-lockTravelThresholdPx)
                                    latestGestureOffsetChanged(gestureOffsetY)
                                    latestVoiceAction(VoiceAction.Dragged(posChange.x, posChange.y))
                                }
                            }
                        }
                    } finally {
                        // Normal lift is handled above; cancellation must not strand a Pressing/Recording recorder.
                        // The controller intentionally ignores this action after the gesture successfully locks.
                        if (gestureStarted && !gestureFinished) {
                            val returnXOffset = latestDragOffsetX
                            val returnYOffset = latestDragOffsetY
                            if (returnXOffset != 0f || returnYOffset != 0f) {
                                returnStartX = returnXOffset
                                returnStartY = returnYOffset
                                isReturning = true
                            }
                            latestVoiceAction(VoiceAction.PointerGestureInterrupted)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(MIC_VISUAL_SIZE)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = if (isReturning) returnX.value else dragOffsetX
                    translationY = if (isReturning) returnY.value else dragOffsetY
                }
                .shadow(
                    elevation = glowElevation,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = glowAlpha),
                    spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = glowAlpha * 0.8f)
                )
                .background(micTint, CircleShape)
                .testTag("voice-recording-mic-visual"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = stringResource(if (isRecording) R.string.voice_recording_cancel else R.string.voice_hold_to_record),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(Sizes.IconLarge)
            )
        }
    }
}
