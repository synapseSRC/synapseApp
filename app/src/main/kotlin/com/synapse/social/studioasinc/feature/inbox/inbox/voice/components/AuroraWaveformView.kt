package com.synapse.social.studioasinc.feature.inbox.inbox.voice.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.sin

/**
 * Project design decision (not an M3 token): the voice controller publishes at most 40
 * smoothed amplitude-envelope samples (one per ~60ms tick), so the visualiser is sized to
 * that envelope rather than to a fabricated spectrum.
 */
private const val WAVEFORM_HISTORY_CAPACITY = 40

/** Envelope floor the controller itself applies during silence (0.08 + 0.92 * db). */
private const val WAVEFORM_IDLE_LEVEL = 0.08f

/** Fraction of the width either side of the playhead where played/unplayed colours cross-fade. */
private const val WAVEFORM_PLAYHEAD_SOFT_BAND = 0.035f

/** Envelope level above which a bar receives a peak halo. */
private const val WAVEFORM_PEAK_THRESHOLD = 0.52f

private val WAVEFORM_BAR_WIDTH = 3.dp
private val WAVEFORM_BAR_SPACING = 2.5.dp
private val WAVEFORM_MIN_BAR_HEIGHT = 3.dp
private val WAVEFORM_TRACK_THICKNESS = 1.dp
private val WAVEFORM_PLAYHEAD_WIDTH = 2.dp
private val WAVEFORM_PLAYHEAD_GLOW = 4.dp

/**
 * Frame-driven envelope animator for [AuroraWaveformView].
 *
 * Everything the renderer needs lives here as plain arrays plus snapshot states that are only
 * ever read from the draw phase, so animation advances redraws without re-running composition.
 * No allocation happens per frame: [bars] and [targets] are fixed-size arrays.
 */
private class AuroraWaveformAnimator(val barCount: Int) {
    val bars = FloatArray(barCount) { WAVEFORM_IDLE_LEVEL }
    private val targets = FloatArray(barCount) { WAVEFORM_IDLE_LEVEL }

    /** Motion phase; read from the draw phase only. */
    val phase = mutableFloatStateOf(0f)

    /** Monotonic frame counter used to invalidate the canvas without recomposing. */
    val frameTick = mutableFloatStateOf(0f)

    private var energy = WAVEFORM_IDLE_LEVEL

    /**
     * Resamples the incoming envelope into bar targets with linear interpolation, so a new
     * sample sliding in never produces a hard step, then lowers the overall motion speed toward
     * a calm idle pace when the envelope is quiet.
     */
    fun submit(history: List<Float>) {
        val sampleCount = history.size
        if (sampleCount == 0) {
            for (index in 0 until barCount) targets[index] = WAVEFORM_IDLE_LEVEL
        } else {
            for (index in 0 until barCount) {
                val position = ((index + 0.5f) / barCount) * sampleCount - 0.5f
                val clamped = position.coerceIn(0f, (sampleCount - 1).toFloat())
                val lowIndex = floor(clamped).toInt().coerceIn(0, sampleCount - 1)
                val highIndex = (lowIndex + 1).coerceAtMost(sampleCount - 1)
                val blend = clamped - lowIndex
                val lowLevel = history[lowIndex].coerceIn(0f, 1f)
                val highLevel = history[highIndex].coerceIn(0f, 1f)
                targets[index] = lowLevel + (highLevel - lowLevel) * blend
            }
        }

        var sum = 0f
        for (index in 0 until barCount) sum += targets[index]
        energy += (sum / barCount - energy) * 0.22f
        frameTick.floatValue += 0.5f
    }

    /** Advances the displayed bars one frame with critically damped easing toward the targets. */
    fun advance(dtSeconds: Float) {
        val nextPhase = (phase.floatValue + dtSeconds * (0.6f + 2.6f * energy)) % 1000f
        phase.floatValue = nextPhase
        val easing = 1f - exp(-9f * dtSeconds)
        for (index in 0 until barCount) bars[index] += (targets[index] - bars[index]) * easing
        frameTick.floatValue += 1f
    }

    /** Snaps the display straight to the targets (inactive, paused or reduced-motion rendering). */
    fun settle() {
        for (index in 0 until barCount) bars[index] = targets[index]
        frameTick.floatValue += 0.5f
    }
}

/**
 * Voice amplitude visualiser used by both the live recording surface and the voice-draft
 * playback row.
 *
 * The single source of truth is the smoothed amplitude-envelope history produced by
 * VoiceRecordingController (and, for drafts, the extractor). Nothing here invents spectrum
 * data: bar heights are an interpolated, organically shaped reading of that envelope.
 *
 * @param isLive true while capturing audio; bars stay active-coloured and amplitude-reactive.
 * @param progressFraction played fraction of a draft (0..1).
 * @param isPlaying draft playback is running (adds a gentle playhead pulse).
 * @param isPaused draft playback is paused (dims bars and playhead).
 * @param isSeeking a seek drag is in progress (slightly larger, brighter playhead).
 * @param reducedMotion system reduced-motion setting; stops all continuous motion.
 */
@Composable
fun AuroraWaveformView(
    amplitudeHistory: List<Float>,
    modifier: Modifier = Modifier,
    isLive: Boolean = false,
    progressFraction: Float = 1f,
    isPlaying: Boolean = false,
    isPaused: Boolean = false,
    isSeeking: Boolean = false,
    reducedMotion: Boolean = false,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    peakColor: Color = MaterialTheme.colorScheme.tertiary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
    trackColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.14f),
    onSeek: ((Float) -> Unit)? = null
) {
    val animator = remember { AuroraWaveformAnimator(WAVEFORM_HISTORY_CAPACITY) }
    val progress = progressFraction.coerceIn(0f, 1f)
    val seekEnabled = onSeek != null && !isLive
    val animating = !reducedMotion && (isLive || isPlaying || isSeeking)

    SideEffect {
        animator.submit(amplitudeHistory)
        // Static surfaces (idle draft, reduced motion) must still reflect new samples.
        if (!animating) animator.settle()
    }

    LaunchedEffect(animating) {
        if (!animating) return@LaunchedEffect
        var previousFrameNanos = 0L
        while (true) {
            val frameNanos = withFrameNanos { it }
            if (previousFrameNanos != 0L) {
                val dt = ((frameNanos - previousFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                animator.advance(dt)
            }
            previousFrameNanos = frameNanos
        }
    }

    val seekModifier = if (seekEnabled) {
        Modifier
            .pointerInput(onSeek) {
                detectTapGestures { offset ->
                    val width = size.width.toFloat()
                    if (width > 0f) onSeek?.invoke((offset.x / width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(onSeek) {
                detectHorizontalDragGestures { change, _ ->
                    val width = size.width.toFloat()
                    if (width > 0f) {
                        change.consume()
                        onSeek?.invoke((change.position.x / width).coerceIn(0f, 1f))
                    }
                }
            }
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f) }
    } else {
        Modifier
    }

    Canvas(modifier = modifier.fillMaxHeight().then(seekModifier)) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        // Draw-phase reads: the canvas invalidates itself as the animator advances, and
        // composition is never re-run for animation frames.
        val frameTick = animator.frameTick.floatValue
        val phase = animator.phase.floatValue
        val bars = animator.bars
        val barCount = animator.barCount

        val barWidth = WAVEFORM_BAR_WIDTH.toPx()
        val barSpacing = WAVEFORM_BAR_SPACING.toPx()
        val slot = barWidth + barSpacing
        val visible = (((width + barSpacing) / slot).toInt()).coerceIn(1, barCount)
        val firstIndex = barCount - visible
        val totalWidth = visible * slot - barSpacing
        val startX = ((width - totalWidth) / 2f).coerceAtLeast(0f)
        val centerY = height / 2f
        val barCorner = CornerRadius(barWidth / 2f, barWidth / 2f)
        val minBarHeight = WAVEFORM_MIN_BAR_HEIGHT.toPx().coerceAtMost(height)
        val sheenPulse = if (animating) 0.02f * sin(frameTick * 0.05f) else 0f
        val levelScale = if (isLive) 0.94f else 0.90f

        val trackThickness = WAVEFORM_TRACK_THICKNESS.toPx()
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(startX, centerY - trackThickness / 2f),
            size = Size(totalWidth.coerceAtLeast(0f), trackThickness),
            cornerRadius = CornerRadius(trackThickness / 2f, trackThickness / 2f)
        )

        for (index in 0 until visible) {
            val sampleIndex = (firstIndex + index).coerceIn(0, barCount - 1)
            val rawLevel = bars[sampleIndex].coerceIn(0f, 1f)

            // Per-bar shaping keeps neighbouring bars from marching in lockstep while staying
            // driven by the same envelope; idle motion keeps silence elegant instead of dead.
            val organic = 0.76f + 0.24f * sin(sampleIndex * 1.7f + phase * 1.15f)
            val idleMotion = WAVEFORM_IDLE_LEVEL * (0.55f + 0.45f * sin(sampleIndex * 0.6f - phase * 0.85f))
            val level = (rawLevel * organic + idleMotion * (1f - rawLevel)).coerceIn(0.05f, 1f)

            val barFraction = (index + 0.5f) / visible.toFloat()
            val played = if (isLive) {
                1f
            } else {
                val distance = barFraction - progress
                when {
                    distance <= -WAVEFORM_PLAYHEAD_SOFT_BAND -> 1f
                    distance >= WAVEFORM_PLAYHEAD_SOFT_BAND -> 0f
                    else -> ((WAVEFORM_PLAYHEAD_SOFT_BAND - distance) / (2f * WAVEFORM_PLAYHEAD_SOFT_BAND))
                        .coerceIn(0f, 1f)
                }
            }

            var barColor = lerp(inactiveColor, activeColor, played)
            if (rawLevel > WAVEFORM_PEAK_THRESHOLD && played > 0.35f) {
                val peakMix = ((rawLevel - WAVEFORM_PEAK_THRESHOLD) / (1f - WAVEFORM_PEAK_THRESHOLD)) * 0.55f
                barColor = lerp(barColor, peakColor, peakMix)
            }

            val barHeight = (height * level * levelScale).coerceIn(minBarHeight, height)
            val topY = centerY - barHeight / 2f
            val leftX = startX + index * slot
            val barAlpha = (when {
                isPaused -> 0.45f
                isLive -> 0.72f + 0.28f * level
                else -> 1f
            } * if (isSeeking) 0.92f else 1f).coerceIn(0f, 1f)

            if (rawLevel > WAVEFORM_PEAK_THRESHOLD && played > 0.35f) {
                val haloAlpha = ((rawLevel - WAVEFORM_PEAK_THRESHOLD) / (1f - WAVEFORM_PEAK_THRESHOLD)) * 0.22f * barAlpha
                val haloWidth = barWidth + 2f
                drawRoundRect(
                    color = lerp(barColor, peakColor, 0.4f).copy(alpha = haloAlpha),
                    topLeft = Offset(leftX - 1f, topY - 1f),
                    size = Size(haloWidth, barHeight + 2f),
                    cornerRadius = CornerRadius(haloWidth / 2f, haloWidth / 2f)
                )
            }

            drawRoundRect(
                color = barColor,
                topLeft = Offset(leftX, topY),
                size = Size(barWidth, barHeight),
                cornerRadius = barCorner,
                alpha = barAlpha
            )

            if (level > 0.22f) {
                val sheenHeight = (barHeight * 0.34f).coerceAtLeast(barWidth * 0.6f)
                val sheenAlpha = ((0.16f + sheenPulse) * barAlpha * played.coerceAtLeast(0.35f)).coerceIn(0f, 1f)
                drawRoundRect(
                    color = lerp(barColor, Color.White, 0.55f).copy(alpha = sheenAlpha),
                    topLeft = Offset(leftX, topY),
                    size = Size(barWidth, sheenHeight),
                    cornerRadius = barCorner
                )
            }
        }

        if (seekEnabled) {
            val playheadX = (startX + totalWidth * progress).coerceIn(startX, startX + totalWidth)
            val playheadAlpha = if (isPaused) 0.5f else 1f
            val playheadWidth = WAVEFORM_PLAYHEAD_WIDTH.toPx() * if (isSeeking) 1.5f else 1f
            val playheadHeight = height * 0.62f
            drawCircle(
                color = activeColor.copy(alpha = 0.20f * playheadAlpha),
                radius = WAVEFORM_PLAYHEAD_GLOW.toPx(),
                center = Offset(playheadX, centerY)
            )
            drawRoundRect(
                color = activeColor.copy(alpha = 0.95f * playheadAlpha),
                topLeft = Offset(playheadX - playheadWidth / 2f, centerY - playheadHeight / 2f),
                size = Size(playheadWidth, playheadHeight),
                cornerRadius = CornerRadius(playheadWidth / 2f, playheadWidth / 2f)
            )
        }
    }
}
