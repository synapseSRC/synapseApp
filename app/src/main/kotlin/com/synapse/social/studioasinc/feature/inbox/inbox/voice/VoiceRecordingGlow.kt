package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberInfiniteTransition
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import kotlin.math.cos
import kotlin.math.sin

private object GlowDesign {
    const val MIN_AMPLITUDE = 0.08f
    const val MAX_AMPLITUDE = 1f
    const val TAU = 6.2831855f
    const val PHASE_CENTER = 0.5f
    const val SHIMMER_DURATION_MILLIS = 2_600
    const val SHIMMER_HIGHLIGHT_HALF_WIDTH = 0.10f
}

/** Audio energy drives temporal speed, travel distance, organic contour deformation, and pigment intensity. */
internal data class VoiceLiquidMotion(
    val cyclesPerSecond: Float,
    val travelRatio: Float,
    val deformation: Float,
    val pigmentAlpha: Float
)

internal fun voiceLiquidMotion(amplitude: Float): VoiceLiquidMotion {
    val bounded = amplitude.coerceIn(GlowDesign.MIN_AMPLITUDE, GlowDesign.MAX_AMPLITUDE)
    val energy = ((bounded - GlowDesign.MIN_AMPLITUDE) / (GlowDesign.MAX_AMPLITUDE - GlowDesign.MIN_AMPLITUDE))
        .coerceIn(0f, 1f)
    return VoiceLiquidMotion(
        cyclesPerSecond = 0.035f + 0.245f * energy,
        travelRatio = 0.025f + 0.135f * energy,
        deformation = 0.025f + 0.19f * energy,
        pigmentAlpha = 0.24f + 0.13f * energy
    )
}

@Composable
internal fun rememberVoiceGlowPhase(amplitude: Float, active: Boolean, reducedMotion: Boolean, label: String): Float {
    var phase by remember(label) { mutableFloatStateOf(0f) }
    val latestAmplitude by rememberUpdatedState(amplitude)
    LaunchedEffect(active, reducedMotion) {
        if (!active || reducedMotion) {
            if (reducedMotion) phase = 0f
            return@LaunchedEffect
        }
        var previousFrameNanos = 0L
        while (true) {
            val frameNanos = withFrameNanos { it }
            if (previousFrameNanos != 0L) {
                val elapsedSeconds = ((frameNanos - previousFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                val advance = elapsedSeconds * voiceLiquidMotion(latestAmplitude).cyclesPerSecond
                phase += advance
                if (phase >= 1f) phase -= 1f
            }
            previousFrameNanos = frameNanos
        }
    }
    return phase
}

@Composable
internal fun rememberVoiceGlowIntensity(target: Float, reducedMotion: Boolean, label: String): Float {
    if (reducedMotion) return target
    val intensity by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 280f),
        label = label
    )
    return intensity
}

@Composable
internal fun rememberVoiceShimmerPhase(reducedMotion: Boolean, label: String): Float {
    if (reducedMotion) return GlowDesign.PHASE_CENTER
    val transition = rememberInfiniteTransition(label = label)
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = GlowDesign.SHIMMER_DURATION_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "voiceCancelShimmerPhase"
    )
    return phase
}

private fun VoiceState.isGlowActive(): Boolean = this is VoiceState.Recording || this is VoiceState.Locked

internal fun VoiceState.glowAmplitude(): Float = when (this) {
    is VoiceState.Recording -> (amplitudeHistory.lastOrNull() ?: GlowDesign.MIN_AMPLITUDE)
        .coerceIn(GlowDesign.MIN_AMPLITUDE, GlowDesign.MAX_AMPLITUDE)
    is VoiceState.Locked -> if (isPaused) GlowDesign.MIN_AMPLITUDE else
        (amplitudeHistory.lastOrNull() ?: GlowDesign.MIN_AMPLITUDE)
            .coerceIn(GlowDesign.MIN_AMPLITUDE, GlowDesign.MAX_AMPLITUDE)
    else -> 0f
}

@Composable
fun VoiceRecordingGlowSurface(
    voiceState: VoiceState,
    reducedMotionOverride: Boolean,
    modifier: Modifier = Modifier
) {
    val active = voiceState.isGlowActive()
    val reducedMotion = reducedMotionOverride
    val colorScheme = MaterialTheme.colorScheme
    val surface = colorScheme.surfaceContainerHigh
    val shape = RoundedCornerShape(Sizes.CornerMassive)
    val targetAmplitude = if (active) voiceState.glowAmplitude() else 0f
    val intensity = rememberVoiceGlowIntensity(targetAmplitude, reducedMotion, "voiceGlowIntensity")
    val phase = rememberVoiceGlowPhase(intensity, active, reducedMotion, "voiceRecordingInterior")
    val colors = MaterialTheme.colorScheme
    val liquidContour = remember { Path() }
    val primary = colors.primary
    val secondary = colors.secondary
    val tertiary = colors.tertiary
    val activeAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 220f),
        label = "voiceLiquidSettle"
    )

    Canvas(modifier.clip(shape)) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val corner = CornerRadius(height / 2f)
        drawRoundRect(color = surface, cornerRadius = corner)
        if (activeAlpha <= 0.001f) return@Canvas

        val motion = voiceLiquidMotion(intensity)
        val energy = ((intensity - GlowDesign.MIN_AMPLITUDE) / (GlowDesign.MAX_AMPLITUDE - GlowDesign.MIN_AMPLITUDE))
            .coerceIn(0f, 1f)
        val angle = phase * GlowDesign.TAU
        val travel = motion.travelRatio
        val warp = motion.deformation
        val centerX = width * (0.48f + travel * sin(angle * 0.72f))
        val centerY = height * (0.48f + travel * 0.50f * cos(angle * 0.93f))
        val radiusX = width * (0.70f + warp * 0.18f * sin(angle * 1.13f))
        val radiusY = height * (0.82f + warp * 0.45f * cos(angle * 0.81f))

        // A changing cubic contour plus overlapping pigment lobes creates soft, morphing liquid rather than a brightness pulse.
        liquidContour.reset()
        liquidContour.apply {
            moveTo(centerX, centerY - radiusY)
            cubicTo(
                centerX + radiusX * (0.56f + warp * sin(angle)), centerY - radiusY * 1.13f,
                centerX + radiusX * 1.12f, centerY - radiusY * (0.55f + warp * cos(angle * 0.7f)),
                centerX + radiusX * (1f + warp * sin(angle * 1.4f)), centerY
            )
            cubicTo(
                centerX + radiusX * 1.10f, centerY + radiusY * (0.48f + warp * sin(angle * 0.8f)),
                centerX + radiusX * (0.52f + warp * cos(angle * 1.2f)), centerY + radiusY * 1.10f,
                centerX, centerY + radiusY * (1f + warp * sin(angle * 0.9f))
            )
            cubicTo(
                centerX - radiusX * (0.62f + warp * sin(angle * 1.1f)), centerY + radiusY * 1.07f,
                centerX - radiusX * 1.12f, centerY + radiusY * (0.42f + warp * cos(angle)),
                centerX - radiusX * (1f + warp * cos(angle * 1.3f)), centerY
            )
            cubicTo(
                centerX - radiusX * 1.08f, centerY - radiusY * (0.58f + warp * sin(angle * 0.6f)),
                centerX - radiusX * (0.48f + warp * cos(angle * 1.5f)), centerY - radiusY * 1.12f,
                centerX, centerY - radiusY
            )
            close()
        }
        val pigment = motion.pigmentAlpha * activeAlpha
        val sweep = width * (0.85f + travel)
        val sweepCenter = centerX + width * travel * cos(angle)
        drawPath(
            path = liquidContour,
            brush = Brush.linearGradient(
                colors = listOf(
                    primary.copy(alpha = pigment * 0.72f),
                    secondary.copy(alpha = pigment),
                    tertiary.copy(alpha = pigment * 0.82f),
                    primary.copy(alpha = pigment * 0.92f)
                ),
                start = Offset(sweepCenter - sweep, 0f),
                end = Offset(sweepCenter + sweep, height)
            )
        )

        val lobeRadius = width * (0.46f + 0.06f * energy)
        val leftCenter = Offset(
            width * (0.19f + travel * sin(angle * 0.87f + 0.7f)),
            height * (0.43f + travel * 1.4f * cos(angle * 0.66f))
        )
        val rightCenter = Offset(
            width * (0.79f - travel * cos(angle * 0.74f + 0.8f)),
            height * (0.55f + travel * 1.5f * sin(angle * 0.91f + 0.3f))
        )
        val centerLobe = Offset(
            width * (0.52f + travel * sin(angle * 1.1f + 1.7f)),
            height * (0.44f + travel * cos(angle * 0.8f + 1.2f))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = pigment * 0.75f), secondary.copy(alpha = pigment * 0.38f), Color.Transparent),
                center = leftCenter,
                radius = lobeRadius
            ),
            center = leftCenter,
            radius = lobeRadius
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tertiary.copy(alpha = pigment * 0.78f), secondary.copy(alpha = pigment * 0.42f), Color.Transparent),
                center = rightCenter,
                radius = lobeRadius * (0.86f + warp * 0.25f * sin(angle))
            ),
            center = rightCenter,
            radius = lobeRadius * (0.86f + warp * 0.25f * sin(angle))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondary.copy(alpha = pigment * 0.42f), Color.Transparent),
                center = centerLobe,
                radius = lobeRadius * (0.62f + warp * 0.2f * cos(angle * 1.2f))
            ),
            center = centerLobe,
            radius = lobeRadius * (0.62f + warp * 0.2f * cos(angle * 1.2f))
        )

        val borderAlpha = activeAlpha * (0.11f + energy * 0.08f)
        val stroke = 1.2.dp.toPx()
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(primary.copy(alpha = borderAlpha), secondary.copy(alpha = borderAlpha * 1.15f), tertiary.copy(alpha = borderAlpha)),
                start = Offset(sweepCenter - sweep, 0f),
                end = Offset(sweepCenter + sweep, height)
            ),
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = Size((width - stroke).coerceAtLeast(0f), (height - stroke).coerceAtLeast(0f)),
            cornerRadius = CornerRadius(((height - stroke) / 2f).coerceAtLeast(0f)),
            style = Stroke(width = stroke)
        )
    }
}

@Composable
fun VoiceRecordingAmbientGlow(
    voiceState: VoiceState,
    reducedMotionOverride: Boolean,
    modifier: Modifier = Modifier
) {
    if (!voiceState.isGlowActive()) return
    val reducedMotion = reducedMotionOverride
    val amplitude = voiceState.glowAmplitude()
    val phase = rememberVoiceGlowPhase(amplitude, true, reducedMotion, "voiceRecordingAmbient")
    val intensity = rememberVoiceGlowIntensity(amplitude, reducedMotion, "voiceAmbientIntensity")
    val colorScheme = MaterialTheme.colorScheme
    val primary = colorScheme.primary
    val secondary = colorScheme.secondary
    val tertiary = colorScheme.tertiary

    Canvas(modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val motion = voiceLiquidMotion(intensity)
        val angle = phase * GlowDesign.TAU
        val travel = motion.travelRatio
        val radius = maxOf(width * 0.70f, height * 1.05f)
        val alpha = 0.10f + 0.11f * ((intensity - GlowDesign.MIN_AMPLITUDE) / (GlowDesign.MAX_AMPLITUDE - GlowDesign.MIN_AMPLITUDE)).coerceIn(0f, 1f)
        val y = height * (0.88f + 0.05f * cos(angle))
        val left = Offset(width * (0.27f + travel * sin(angle)), y)
        val right = Offset(width * (0.73f - travel * cos(angle * 0.83f)), y)
        val center = Offset(width * (0.5f + travel * sin(angle * 0.7f + 1f)), height * 0.94f)
        drawCircle(
            brush = Brush.radialGradient(listOf(primary.copy(alpha = alpha), secondary.copy(alpha = alpha * 0.5f), Color.Transparent), left, radius),
            center = left, radius = radius
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(tertiary.copy(alpha = alpha * 0.86f), secondary.copy(alpha = alpha * 0.42f), Color.Transparent), right, radius),
            center = right, radius = radius
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(secondary.copy(alpha = alpha * 0.62f), Color.Transparent), center, radius * 0.74f),
            center = center, radius = radius * 0.74f
        )
    }
}
