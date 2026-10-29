package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.lerp as lerpDp
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
private const val LOCK_PILL_CHEVRON_START_OFFSET_DP = 2f
private const val LOCK_PILL_CHEVRON_END_OFFSET_DP = -2f
private const val LOCK_PILL_CHEVRON_BOUNCE_DURATION_MILLIS = 1_400
private val MIC_TOUCH_TARGET_SIZE = 48.dp
private val MIC_VISUAL_SIZE = 44.dp
private val VOICE_BAR_MIN_HEIGHT = 52.dp + Spacing.space4 + Spacing.space8
// 6.dp follows the Material 3 level-3 elevation step; the halo color remains a theme surface color.
private val ACTIVE_MIC_GLOW_ELEVATION = 6.dp
// WhatsApp-style vertical lock pill capsule dimensions.
// The pill rides a FIXED track above the mic: it starts just above the mic button and
// slides up to a lock target as the user drags. Its height is NON-MONOTONIC — it grows
// slightly as the drag begins (the pill "inflates" toward the user's finger), then shrinks
// toward a circle as it approaches the lock target. Width stays constant until the lock
// commit, where it settles into a circle.
private val LOCK_PILL_WIDTH = 48.dp
private val LOCK_PILL_BASE_HEIGHT = 120.dp
private val LOCK_PILL_ICON_SIZE = 22.dp
private val LOCK_PILL_CHEVRON_SIZE = 16.dp
// Fixed track: the pill's resting top sits this far above the mic button's top.
private val LOCK_PILL_TRACK_GAP = Spacing.space8
// Travel distance the pill slides up from rest to the lock target (≈ lockThreshold).
private val LOCK_PILL_TRACK_TRAVEL = 110.dp
// Non-monotonic height curve: height grows to this peak (relative to base) mid-drag, then
// shrinks to a circle at the lock target.
private val LOCK_PILL_HEIGHT_GROWTH = 0.12f
// At the lock target the pill settles into a circle of this diameter.
private val LOCK_PILL_LOCK_DIAMETER = 48.dp

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
    var hostBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
    var micBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
    var lastMicBoundsInHost by remember { mutableStateOf<Rect?>(null) }
    var lastLockGestureOffsetYPx by remember { mutableFloatStateOf(0f) }

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
    val effectiveMicBounds = micBoundsInHost ?: lastMicBoundsInHost

    // The lock indicator is now an overlay (rendered outside the measured layout) to avoid
    // pushing the chat message list upward when the mic is held — fixing the layout glitch.
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = VOICE_BAR_MIN_HEIGHT)
                .padding(horizontal = Spacing.space4)
                .padding(top = Spacing.space4, bottom = Spacing.space8)
                .onGloballyPositioned { hostBoundsInRoot = it.boundsInRoot() }
                .testTag("voice-recording-bar")
                .shadow(
                    elevation = Sizes.ElevationMedium,
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
                    .padding(horizontal = Spacing.space8, vertical = Spacing.space4),
                verticalAlignment = Alignment.Bottom,
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
                                VoiceInputBarContent(
                                    voiceState = animatedState,
                                    onVoiceAction = onVoiceAction,
                                    onSendBoundsChanged = { micBoundsInRoot = it }
                                )
                            }
                        }
                    }
                }

                // Keep the mic recognizer outside AnimatedContent across Normal/Pressing/Recording.
                val isNormal = voiceState is VoiceState.Normal
                if (isNormal && showSendButton) {
                    // Fixed 48dp slot matching the mic's layout box so typing doesn't shrink the composer.
                    Box(
                        modifier = Modifier.size(MIC_TOUCH_TARGET_SIZE),
                        contentAlignment = Alignment.Center
                    ) {
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
                    }
                } else if (voiceState is VoiceState.Locked || voiceState is VoiceState.VoiceDraft || voiceState is VoiceState.Sending || voiceState is VoiceState.Failed) {
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
                    micBoundsInHost = effectiveMicBounds,
                    onVoiceAction = onVoiceAction
                )
            }
        }
    }
}

@Composable
private fun VoiceInputBarContent(
    voiceState: VoiceState,
    onVoiceAction: (VoiceAction) -> Unit,
    onSendBoundsChanged: (Rect) -> Unit = {}
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        when (voiceState) {
            is VoiceState.VoiceReady, is VoiceState.Pressing -> {
                IconButton(onClick = { onVoiceAction(VoiceAction.CloseVoiceMode) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Close, stringResource(R.string.action_cancel), tint = MaterialTheme.colorScheme.onSurface)
                }
                Text(stringResource(R.string.voice_hold_to_record), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f).padding(horizontal = Spacing.space16))
            }
            is VoiceState.Recording -> {
                // The single, full-width status layer is drawn over this row in VoiceInputHost so it stays centered across the mic slot.
                Spacer(modifier = Modifier.fillMaxWidth().height(1.dp)) // 1dp row reserve is component-specific, not a foundation spacing step
            }
            is VoiceState.Locked -> {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = { onVoiceAction(VoiceAction.CancelRecording) }, modifier = Modifier.size(MIC_TOUCH_TARGET_SIZE).testTag("voice-locked-cancel")) {
                        Icon(Icons.Default.Delete, stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error)
                    }
                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = Spacing.space8),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.space4)
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
                    Spacer(Modifier.width(Spacing.space8))
                    Surface(
                        modifier = Modifier
                            .size(Sizes.SendButton)
                            .onGloballyPositioned { onSendBoundsChanged(it.boundsInRoot()) },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shadowElevation = 2.dp
                    ) {
                        IconButton(onClick = { onVoiceAction(VoiceAction.FinishRecording) }, modifier = Modifier.fillMaxSize().testTag("voice-locked-finish")) {
                            Icon(Icons.Default.ArrowUpward, stringResource(R.string.chat_action_send), tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(Sizes.IconLarge))
                        }
                    }
                }
            }
            is VoiceState.VoiceDraft -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.space8),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.space8)
                ) {
                    IconButton(
                        onClick = { onVoiceAction(VoiceAction.DiscardDraft) },
                        modifier = Modifier.size(MIC_TOUCH_TARGET_SIZE)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    IconButton(
                        onClick = { onVoiceAction(VoiceAction.TogglePlayDraft) },
                        modifier = Modifier.size(MIC_TOUCH_TARGET_SIZE)
                    ) {
                        Icon(
                            imageVector = if (voiceState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = stringResource(if (voiceState.isPlaying) R.string.cd_pause_audio else R.string.cd_play_audio),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    val progress = if (voiceState.durationMs > 0) voiceState.playbackPositionMs.toFloat() / voiceState.durationMs else 0f
                    LiveWaveformView(amplitudeHistory = voiceState.amplitudeHistory, progressFraction = progress, isDraftPlayback = true,
                        onSeek = { onVoiceAction(VoiceAction.SeekDraft((it * voiceState.durationMs).toLong())) }, modifier = Modifier.weight(1f).height(32.dp), activeColor = MaterialTheme.colorScheme.primary)
                    OdometerTimerText(voiceState.durationMs, color = MaterialTheme.colorScheme.onSurface)
                    Surface(
                        modifier = Modifier.size(MIC_TOUCH_TARGET_SIZE),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        IconButton(onClick = { onVoiceAction(VoiceAction.SendDraft) }, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Default.ArrowUpward, stringResource(R.string.chat_action_send), modifier = Modifier.size(Sizes.IconLarge))
                        }
                    }
                }
            }
            is VoiceState.Sending -> {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.space16), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(Spacing.space16))
                    Text(stringResource(R.string.voice_sending), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            is VoiceState.Failed -> {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(voiceState.error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f).padding(horizontal = Spacing.space8))
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
    onVoiceAction: (VoiceAction) -> Unit
) {
    val isRecording = voiceState is VoiceState.Recording
    val isLocked = voiceState is VoiceState.Locked

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val recording = voiceState as? VoiceState.Recording
        val showLockIndicator = isLocked || (recording?.lockProgress?.let { it > 0f } ?: false)

        // Timer and slide-to-cancel shimmer shown in center during active holding/recording
        AnimatedVisibility(
            visible = isRecording,
            modifier = Modifier.align(Alignment.Center),
            enter = if (reducedMotion) EnterTransition.None else fadeIn(animationSpec = spring(dampingRatio = 1f, stiffness = 1600f)),
            exit = if (reducedMotion) ExitTransition.None else fadeOut(animationSpec = spring(dampingRatio = 1f, stiffness = 1600f))
        ) {
            if (recording != null) {
                RecordingStatusOverlay(recording, reducedMotion)
            }
        }

        // Lock indicator pill/circle overlay (visible in both Recording and Locked)
        AnimatedVisibility(
            visible = showLockIndicator,
            enter = if (reducedMotion) EnterTransition.None else (
                fadeIn(animationSpec = spring(dampingRatio = 1f, stiffness = 1200f)) +
                scaleIn(initialScale = 0.82f, animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium))
            ),
            exit = if (reducedMotion) ExitTransition.None else (
                fadeOut(animationSpec = spring(dampingRatio = 1f, stiffness = 1200f)) +
                scaleOut(targetScale = 0.82f, animationSpec = spring(dampingRatio = 1f, stiffness = 1200f))
            )
        ) {
            micBoundsInHost?.let { micBounds ->
                val parentWidthPx = with(density) { maxWidth.toPx() }
                val micCenterX = (micBounds.left + micBounds.right) / 2f
                val micCenterY = (micBounds.top + micBounds.bottom) / 2f

                val lockProgress = when {
                    isLocked -> 1f
                    recording != null -> recording.lockProgress
                    else -> 0f
                }
                val cancelProgress = when {
                    recording != null -> recording.cancelProgress
                    else -> 0f
                }
                val p = lockProgress.coerceIn(0f, 1f)

                // Fixed-track slide: pill rides upward from just above the mic to the lock target.
                // Its center moves linearly with lockProgress; height morphs around the center so
                // the capsule inflates mid-drag then deflates into a circle at lock.
                val gapPx = with(density) { LOCK_PILL_TRACK_GAP.toPx() }
                val travelPx = with(density) { LOCK_PILL_TRACK_TRAVEL.toPx() }
                val pillCenterY = micCenterY - gapPx - travelPx * p

                val baseHeightPx = with(density) { LOCK_PILL_BASE_HEIGHT.toPx() }
                val lockDiameterPx = with(density) { LOCK_PILL_LOCK_DIAMETER.toPx() }
                // Height is non-monotonic: grows toward base+(base*growth) at p=0.5, then
                // shrinks toward lockDiameter at p=1. Growth curve peaks at the middle of the drag.
                val growthCurve = 4f * p * (1f - p) // 0 at p=0, 1 at p=0.5, 0 at p=1
                val growFactor = 1f + LOCK_PILL_HEIGHT_GROWTH * growthCurve
                val pillHeightPx = baseHeightPx * growFactor * (1f - p) + lockDiameterPx * p

                val pillWidthPx = with(density) { LOCK_PILL_WIDTH.toPx() }
                val pillLeftPx = (micCenterX - pillWidthPx / 2f)
                    .coerceIn(0f, (parentWidthPx - pillWidthPx).coerceAtLeast(0f))
                val pillTopPx = pillCenterY - pillHeightPx / 2f

                // Tooltip "Slide up to lock" bubble to the left of the pill (Image 4)
                val tooltipVisible = !isLocked && p < 0.15f && cancelProgress == 0f
                val tooltipAlpha = ((1f - p * 5f).coerceIn(0f, 1f) * (1f - cancelProgress * 3f).coerceIn(0f, 1f))
                val endPaddingPx = (parentWidthPx - pillLeftPx - pillWidthPx).coerceAtLeast(0f)
                val endPaddingDp = with(density) { endPaddingPx.toDp() }

                Row(
                    modifier = Modifier
                        .absoluteOffset { IntOffset(pillLeftPx.roundToInt(), pillTopPx.roundToInt()) }
                        .fillMaxWidth()
                        .padding(end = endPaddingDp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.Top
                ) {
                    if (tooltipVisible && tooltipAlpha > 0.05f) {
                        Surface(
                            modifier = Modifier
                                .padding(end = Spacing.space8, top = Spacing.space8)
                                .graphicsLayer { alpha = tooltipAlpha },
                            shape = RoundedCornerShape(Spacing.space8),
                            color = MaterialTheme.colorScheme.inverseSurface,
                            shadowElevation = 2.dp,
                            tonalElevation = 2.dp
                        ) {
                            Text(
                                text = stringResource(R.string.voice_recording_lock_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                modifier = Modifier.padding(horizontal = Spacing.space8, vertical = Spacing.space4),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    LockPillCapsule(
                        progress = lockProgress,
                        cancelProgress = cancelProgress,
                        isLocked = isLocked,
                        reducedMotion = reducedMotion,
                        onLockClick = if (isLocked) { { onVoiceAction(VoiceAction.FinishRecording) } } else null,
                        modifier = Modifier.width(LOCK_PILL_WIDTH)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingStatusOverlay(voiceState: VoiceState.Recording, reducedMotion: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.space16),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.space4)
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

/**
 * WhatsApp-style vertical pill capsule lock indicator.
 * Shows a lock icon at the top and a bouncing chevron arrow below it.
 * Appears above the resting mic button during recording.
 * Rides a FIXED track: slides up as the user drags up, with a NON-MONOTONIC
 * height (grows mid-drag, shrinks to a circle at the lock target) and a small
 * width growth at the lock commit. Lock icon centers in the locked circle.
 */
@Composable
private fun LockPillCapsule(
    progress: Float,
    cancelProgress: Float,
    isLocked: Boolean,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
    onLockClick: (() -> Unit)? = null
) {
    val chevronTravel = rememberLockPillChevronTravel(reducedMotion)
    val cancelFade = (1f - cancelProgress * 2.5f).coerceIn(0f, 1f)
    val baseAlpha = if (reducedMotion) 0.95f else (0.85f + 0.15f * progress.coerceIn(0f, 1f))
    val alpha = (if (isLocked) 1f else baseAlpha) * cancelFade

    // Direct-drive fluidity: during a drag the pill height, tint, and scale track the
    // finger's lockProgress exactly. Springs are reserved for the commit (Recording -> Locked)
    // and the release return (Locked -> return) transitions so the motion stays responsive.
    val isClosed = isLocked || progress >= 0.85f
    val iconVector = if (isClosed) Icons.Default.Lock else Icons.Default.LockOpen

    val targetIconColor = if (isClosed) {
        MaterialTheme.colorScheme.primary
    } else {
        lerp(
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.primary,
            progress.coerceIn(0f, 1f)
        )
    }
    val iconTint by animateColorAsState(
        targetValue = targetIconColor,
        animationSpec = if (reducedMotion) snap() else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 500f),
        label = "lockIconColor"
    )

    val targetScale = if (isLocked) 1.04f else (1f + 0.04f * progress.coerceIn(0f, 1f))
    val scaleSpec: AnimationSpec<Float> = if (isLocked) {
        // Low damping gives an overshoot-and-settle tactile "click" when locking commits.
        spring(dampingRatio = 0.2f, stiffness = Spring.StiffnessHigh)
    } else {
        snap()
    }
    val scale = animateFloatAsState(targetValue = targetScale, animationSpec = scaleSpec, label = "lockPillScale").value

    val iconScale by animateFloatAsState(
        targetValue = if (isClosed) 1.1f else 1f,
        animationSpec = if (reducedMotion) snap() else spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "lockIconScale"
    )

    val chevronDragOffset = -8.dp * progress.coerceIn(0f, 1f)
    val chevronAlpha = if (isLocked) 0f else {
        ((1f - progress * 2.2f).coerceIn(0f, 1f) * 0.7f)
    }

    // Height is NON-MONOTONIC and tracks the fixed-track slide (WhatsApp behavior):
    //  - At rest (p=0): full capsule height.
    //  - Mid-drag (~p=0.5): the capsule "inflates" toward the finger (peak height).
    //  - Near lock (p→1): shrinks into a circle at the lock target.
    // Width stays LOCK_PILL_WIDTH until the lock commit, where height==width makes a circle.
    // Direct-drive fluidity: while dragging the height snaps to progress so the capsule
    // tracks the finger exactly. The spring only smooths the commit to Locked and the
    // release return, keeping the drag itself finger-following and responsive.
    val p = progress.coerceIn(0f, 1f)
    val growthCurve = 4f * p * (1f - p) // 0 at p=0/1, 1 at p=0.5
    val growFactor = 1f + LOCK_PILL_HEIGHT_GROWTH * growthCurve
    val targetHeight = if (isLocked) {
        LOCK_PILL_LOCK_DIAMETER
    } else {
        lerpDp(LOCK_PILL_BASE_HEIGHT * growFactor, LOCK_PILL_LOCK_DIAMETER, p)
    }
    val heightSpec: AnimationSpec<androidx.compose.ui.unit.Dp> = if (isLocked) {
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
    } else {
        snap()
    }
    val animatedHeight = animateDpAsState(targetValue = targetHeight, animationSpec = heightSpec, label = "lockPillHeight").value

    // Width grows toward the lock diameter as the drag progresses, settling into a circle at lock.
    val targetWidth = if (isLocked) {
        LOCK_PILL_LOCK_DIAMETER
    } else {
        lerpDp(LOCK_PILL_WIDTH, LOCK_PILL_LOCK_DIAMETER, ((p - 0.7f) / 0.3f).coerceIn(0f, 1f))
    }
    val widthSpec: AnimationSpec<androidx.compose.ui.unit.Dp> = if (isLocked) {
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)
    } else {
        snap()
    }
    val animatedWidth by animateDpAsState(targetValue = targetWidth, animationSpec = widthSpec, label = "lockPillWidth")

    Surface(
        modifier = modifier
            .width(animatedWidth)
            .height(animatedHeight)
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (isLocked && onLockClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true),
                        onClick = onLockClick
                    )
                } else Modifier
            )
            .testTag("voice-lock-pill"),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 4.dp,
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = if (isLocked) Alignment.Center else Alignment.TopCenter
        ) {
            // Lock icon: centered in the locked circle, top of the capsule while dragging.
            Icon(
                imageVector = iconVector,
                contentDescription = stringResource(if (isClosed) R.string.voice_recording_locked_state else R.string.voice_recording_lock_hint),
                tint = iconTint,
                modifier = Modifier
                    .then(if (isLocked) Modifier else Modifier.padding(top = Spacing.space12 + 1.dp))
                    .size(LOCK_PILL_ICON_SIZE)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
                    .testTag("voice-lock-pill-icon")
            )

            // Bouncing chevron arrow below the lock, fading out as pill shrinks
            if (chevronAlpha > 0.01f) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = chevronAlpha),
                    modifier = Modifier
                        .padding(top = Spacing.space12 + 1.dp + LOCK_PILL_ICON_SIZE + Spacing.space8)
                        .size(LOCK_PILL_CHEVRON_SIZE)
                        .offset(y = if (reducedMotion) 0.dp else (chevronTravel.dp + chevronDragOffset))
                        .testTag("voice-lock-pill-chevron")
                )
            }
        }
    }
}

@Composable
private fun rememberLockPillChevronTravel(reducedMotion: Boolean): Float {
    if (reducedMotion) return 0f
    val transition = rememberInfiniteTransition(label = "voiceLockPillChevron")
    val chevronTravel by transition.animateFloat(
        initialValue = LOCK_PILL_CHEVRON_START_OFFSET_DP,
        targetValue = LOCK_PILL_CHEVRON_END_OFFSET_DP,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = LOCK_PILL_CHEVRON_BOUNCE_DURATION_MILLIS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voiceLockPillChevronTravel"
    )
    return chevronTravel
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
    // Mic fades out as the user drags up toward the lock target (WhatsApp behavior).
    val lockProgress = recordingState?.lockProgress?.coerceIn(0f, 1f) ?: 0f
    val targetMicAlpha = (1f - lockProgress).coerceIn(0f, 1f)
    val micAlpha = animateFloatForMotion(
        targetValue = targetMicAlpha,
        reducedMotion = reducedMotion,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "canonicalMicAlpha"
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
                    alpha = micAlpha
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
