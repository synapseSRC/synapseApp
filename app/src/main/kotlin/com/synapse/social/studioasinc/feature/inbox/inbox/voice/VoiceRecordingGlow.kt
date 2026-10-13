package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.auth.ui.util.AnimationUtil
import com.synapse.social.studioasinc.feature.shared.theme.Sizes

private object GlowTokens {
    const val MIN_AMPLITUDE = 0.08f
    const val MAX_AMPLITUDE = 1f
    const val STATIC_PHASE = 0.5f
    const val PHASE_DRIFT_DURATION_MILLIS = 6_800
    const val FLOW_ALPHA_BASE = 0.07f
    const val FLOW_ALPHA_PER_INTENSITY = 0.28f
    const val FLOW_PHASE_TRAVEL = 0.9f
    const val FLOW_START_PHASE = 0.42f
    const val FLOW_END_PHASE = 0.9f
    const val FLOW_PRIMARY_ALPHA = 0.62f
    const val FLOW_SECONDARY_ALPHA = 0.88f
    const val FLOW_TERTIARY_ALPHA = 0.76f
    const val FLOW_END_SECONDARY_ALPHA = 0.94f
    const val RADIAL_PRIMARY_BASE_ALPHA = 0.04f
    const val RADIAL_PRIMARY_INTENSITY_ALPHA = 0.18f
    const val RADIAL_SECONDARY_BASE_ALPHA = 0.025f
    const val RADIAL_SECONDARY_INTENSITY_ALPHA = 0.09f
    const val RADIAL_CENTER_START = 0.16f
    const val RADIAL_CENTER_TRAVEL = 0.66f
    const val RADIAL_CENTER_Y = 0.42f
    const val RADIAL_RADIUS = 0.72f
    const val BORDER_PRIMARY_BASE_ALPHA = 0.32f
    const val BORDER_PRIMARY_INTENSITY_ALPHA = 0.4f
    const val BORDER_SECONDARY_BASE_ALPHA = 0.28f
    const val BORDER_SECONDARY_INTENSITY_ALPHA = 0.42f
    const val BORDER_TERTIARY_BASE_ALPHA = 0.3f
    const val BORDER_TERTIARY_INTENSITY_ALPHA = 0.4f
    const val BORDER_END_X = 1f
    const val BORDER_START_PHASE = 0.4f
    const val AMBIENT_RADIUS_WIDTH = 0.7f
    const val AMBIENT_RADIUS_HEIGHT = 1.05f
    const val AMBIENT_ALPHA_BASE = 0.035f
    const val AMBIENT_ALPHA_PER_INTENSITY = 0.095f
    const val AMBIENT_CENTER_Y = 0.88f
    const val AMBIENT_LEFT_CENTER_X = 0.27f
    const val AMBIENT_LEFT_DRIFT = 0.12f
    const val AMBIENT_RIGHT_CENTER_X = 0.73f
    const val AMBIENT_RIGHT_DRIFT = 0.1f
    const val AMBIENT_LEFT_SECONDARY_ALPHA = 0.52f
    const val AMBIENT_RIGHT_PRIMARY_ALPHA = 0.92f
    const val AMBIENT_RIGHT_SECONDARY_ALPHA = 0.45f
    const val AMBIENT_CENTER_X = 0.5f
    const val AMBIENT_CENTER_DRIFT = 0.14f
    const val AMBIENT_CENTER_Y_OFFSET = 0.12f
    const val AMBIENT_CENTER_RADIUS = 0.82f
    const val AMBIENT_CENTER_PRIMARY_ALPHA = 0.62f
}

private val GLOW_BORDER_STROKE_WIDTH = 1.2.dp

@Composable
internal fun rememberVoiceGlowPhase(reducedMotion: Boolean, label: String): Float {
    if (reducedMotion) return GlowTokens.STATIC_PHASE
    return VoiceGlowPhaseAnimation(label)
}

@Composable
private fun VoiceGlowPhaseAnimation(label: String): Float {
    val transition = rememberInfiniteTransition(label = label)
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        // A deliberately slow linear drift keeps silence subtly alive without a noticeable pulse.
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = GlowTokens.PHASE_DRIFT_DURATION_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voiceGlowPhase"
    )
    return phase
}

@Composable
internal fun rememberVoiceGlowIntensity(target: Float, reducedMotion: Boolean, label: String): Float {
    if (reducedMotion) return target
    val intensity by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = label
    )
    return intensity
}

private fun VoiceState.isGlowActive(): Boolean = this is VoiceState.Recording || this is VoiceState.Locked

internal fun VoiceState.glowAmplitude(): Float = when (this) {
    is VoiceState.Recording -> (amplitudeHistory.lastOrNull() ?: GlowTokens.MIN_AMPLITUDE)
        .coerceIn(GlowTokens.MIN_AMPLITUDE, GlowTokens.MAX_AMPLITUDE)
    is VoiceState.Locked -> (amplitudeHistory.lastOrNull() ?: GlowTokens.MIN_AMPLITUDE)
        .coerceIn(GlowTokens.MIN_AMPLITUDE, GlowTokens.MAX_AMPLITUDE)
    else -> 0f
}

@Composable
fun VoiceRecordingGlowSurface(
    voiceState: VoiceState,
    modifier: Modifier = Modifier,
    reducedMotionOverride: Boolean? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val surface = colorScheme.surfaceContainerHigh
    val shape = RoundedCornerShape(Sizes.CornerMassive)
    if (!voiceState.isGlowActive()) {
        Box(modifier.background(surface, shape))
        return
    }

    val reducedMotion = reducedMotionOverride ?: AnimationUtil.rememberReducedMotion()
    val phase = rememberVoiceGlowPhase(reducedMotion, "voiceRecordingInterior")
    val amplitude = voiceState.glowAmplitude().coerceIn(GlowTokens.MIN_AMPLITUDE, GlowTokens.MAX_AMPLITUDE)
    val intensity = rememberVoiceGlowIntensity(amplitude, reducedMotion, "voiceGlowIntensity")
    val primaryGlow = colorScheme.primary
    val secondaryGlow = colorScheme.secondary
    val tertiaryGlow = colorScheme.tertiary

    Canvas(modifier.clip(shape)) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val corner = CornerRadius(height / 2f)
        val boundedIntensity = intensity.coerceIn(GlowTokens.MIN_AMPLITUDE, GlowTokens.MAX_AMPLITUDE)
        val flowAlpha = GlowTokens.FLOW_ALPHA_BASE + GlowTokens.FLOW_ALPHA_PER_INTENSITY * boundedIntensity
        val flowX = (phase - GlowTokens.STATIC_PHASE) * width * GlowTokens.FLOW_PHASE_TRAVEL
        drawRoundRect(color = surface, cornerRadius = corner)
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    primaryGlow.copy(alpha = flowAlpha * GlowTokens.FLOW_PRIMARY_ALPHA),
                    secondaryGlow.copy(alpha = flowAlpha * GlowTokens.FLOW_SECONDARY_ALPHA),
                    tertiaryGlow.copy(alpha = flowAlpha * GlowTokens.FLOW_TERTIARY_ALPHA),
                    secondaryGlow.copy(alpha = flowAlpha * GlowTokens.FLOW_END_SECONDARY_ALPHA),
                    primaryGlow.copy(alpha = flowAlpha * GlowTokens.FLOW_PRIMARY_ALPHA)
                ),
                start = Offset(flowX - width * GlowTokens.FLOW_START_PHASE, 0f),
                end = Offset(flowX + width * GlowTokens.FLOW_END_PHASE, height)
            ),
            cornerRadius = corner
        )
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryGlow.copy(alpha = GlowTokens.RADIAL_PRIMARY_BASE_ALPHA + boundedIntensity * GlowTokens.RADIAL_PRIMARY_INTENSITY_ALPHA),
                    secondaryGlow.copy(alpha = GlowTokens.RADIAL_SECONDARY_BASE_ALPHA + boundedIntensity * GlowTokens.RADIAL_SECONDARY_INTENSITY_ALPHA),
                    Color.Transparent
                ),
                center = Offset(width * (GlowTokens.RADIAL_CENTER_START + phase * GlowTokens.RADIAL_CENTER_TRAVEL), height * GlowTokens.RADIAL_CENTER_Y),
                radius = width * GlowTokens.RADIAL_RADIUS
            ),
            cornerRadius = corner
        )
        val strokeWidth = GLOW_BORDER_STROKE_WIDTH.toPx()
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    primaryGlow.copy(alpha = GlowTokens.BORDER_PRIMARY_BASE_ALPHA + boundedIntensity * GlowTokens.BORDER_PRIMARY_INTENSITY_ALPHA),
                    secondaryGlow.copy(alpha = GlowTokens.BORDER_SECONDARY_BASE_ALPHA + boundedIntensity * GlowTokens.BORDER_SECONDARY_INTENSITY_ALPHA),
                    tertiaryGlow.copy(alpha = GlowTokens.BORDER_TERTIARY_BASE_ALPHA + boundedIntensity * GlowTokens.BORDER_TERTIARY_INTENSITY_ALPHA),
                    primaryGlow.copy(alpha = GlowTokens.BORDER_PRIMARY_BASE_ALPHA + boundedIntensity * GlowTokens.BORDER_PRIMARY_INTENSITY_ALPHA)
                ),
                start = Offset(flowX - width * GlowTokens.BORDER_START_PHASE, 0f),
                end = Offset(flowX + width * GlowTokens.BORDER_END_X, height)
            ),
            topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
            size = Size((width - strokeWidth).coerceAtLeast(0f), (height - strokeWidth).coerceAtLeast(0f)),
            cornerRadius = CornerRadius(((height - strokeWidth) / 2f).coerceAtLeast(0f)),
            style = Stroke(width = strokeWidth)
        )
    }
}

@Composable
fun VoiceRecordingAmbientGlow(
    voiceState: VoiceState,
    modifier: Modifier = Modifier,
    reducedMotionOverride: Boolean? = null
) {
    if (!voiceState.isGlowActive()) return

    val reducedMotion = reducedMotionOverride ?: AnimationUtil.rememberReducedMotion()
    val phase = rememberVoiceGlowPhase(reducedMotion, "voiceRecordingAmbient")
    val amplitude = voiceState.glowAmplitude().coerceIn(GlowTokens.MIN_AMPLITUDE, GlowTokens.MAX_AMPLITUDE)
    val intensity = rememberVoiceGlowIntensity(amplitude, reducedMotion, "voiceAmbientIntensity")
    val colorScheme = MaterialTheme.colorScheme
    val primaryGlow = colorScheme.primary
    val secondaryGlow = colorScheme.secondary
    val tertiaryGlow = colorScheme.tertiary

    Canvas(modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val radius = maxOf(width * GlowTokens.AMBIENT_RADIUS_WIDTH, height * GlowTokens.AMBIENT_RADIUS_HEIGHT)
        val alpha = GlowTokens.AMBIENT_ALPHA_BASE + GlowTokens.AMBIENT_ALPHA_PER_INTENSITY * intensity.coerceIn(GlowTokens.MIN_AMPLITUDE, GlowTokens.MAX_AMPLITUDE)
        val drift = phase - GlowTokens.STATIC_PHASE
        val centerY = height * GlowTokens.AMBIENT_CENTER_Y
        val leftCenter = Offset(width * (GlowTokens.AMBIENT_LEFT_CENTER_X + drift * GlowTokens.AMBIENT_LEFT_DRIFT), centerY)
        val rightCenter = Offset(width * (GlowTokens.AMBIENT_RIGHT_CENTER_X - drift * GlowTokens.AMBIENT_RIGHT_DRIFT), centerY)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryGlow.copy(alpha = alpha), secondaryGlow.copy(alpha = alpha * GlowTokens.AMBIENT_LEFT_SECONDARY_ALPHA), Color.Transparent),
                center = leftCenter,
                radius = radius
            ),
            center = leftCenter,
            radius = radius
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tertiaryGlow.copy(alpha = alpha * GlowTokens.AMBIENT_RIGHT_PRIMARY_ALPHA), secondaryGlow.copy(alpha = alpha * GlowTokens.AMBIENT_RIGHT_SECONDARY_ALPHA), Color.Transparent),
                center = rightCenter,
                radius = radius
            ),
            center = rightCenter,
            radius = radius
        )
        val center = Offset(width * (GlowTokens.AMBIENT_CENTER_X + drift * GlowTokens.AMBIENT_CENTER_DRIFT), centerY + height * GlowTokens.AMBIENT_CENTER_Y_OFFSET)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondaryGlow.copy(alpha = alpha * GlowTokens.AMBIENT_CENTER_PRIMARY_ALPHA), Color.Transparent),
                center = center,
                radius = radius * GlowTokens.AMBIENT_CENTER_RADIUS
            ),
            center = center,
            radius = radius * GlowTokens.AMBIENT_CENTER_RADIUS
        )
    }
}
