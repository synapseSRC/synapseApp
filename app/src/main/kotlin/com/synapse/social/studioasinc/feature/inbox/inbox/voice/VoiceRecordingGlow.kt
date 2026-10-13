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
import com.synapse.social.studioasinc.feature.shared.theme.Sizes

/** A small set of tuning knobs for the voice glow; geometry ratios below are a single normalized art recipe. */
private object GlowDesign {
    const val MIN_AMPLITUDE = 0.08f
    const val MAX_AMPLITUDE = 1f
    const val PHASE_CENTER = 0.5f
    const val MAX_PHASE_DRIFT = 0.04f
    const val FLOW_ALPHA_BASE = 0.07f
    const val FLOW_ALPHA_FROM_AMPLITUDE = 0.28f
    const val AMBIENT_ALPHA_BASE = 0.035f
    const val AMBIENT_ALPHA_FROM_AMPLITUDE = 0.095f
    const val AMBIENT_RADIUS_WIDTH = 0.7f
    const val AMBIENT_RADIUS_HEIGHT = 1.05f
}

private val GLOW_BORDER_STROKE_WIDTH = 1.2.dp

@Composable
internal fun rememberVoiceGlowPhase(reducedMotion: Boolean, label: String): Float {
    if (reducedMotion) return GlowDesign.PHASE_CENTER
    return VoiceGlowPhaseAnimation(label)
}

@Composable
private fun VoiceGlowPhaseAnimation(label: String): Float {
    val transition = rememberInfiniteTransition(label = label)
    val springOffset by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessVeryLow
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voiceGlowSpringDrift"
    )
    return GlowDesign.PHASE_CENTER + springOffset * GlowDesign.MAX_PHASE_DRIFT
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
    is VoiceState.Recording -> (amplitudeHistory.lastOrNull() ?: GlowDesign.MIN_AMPLITUDE)
        .coerceIn(GlowDesign.MIN_AMPLITUDE, GlowDesign.MAX_AMPLITUDE)
    is VoiceState.Locked -> (amplitudeHistory.lastOrNull() ?: GlowDesign.MIN_AMPLITUDE)
        .coerceIn(GlowDesign.MIN_AMPLITUDE, GlowDesign.MAX_AMPLITUDE)
    else -> 0f
}

@Composable
fun VoiceRecordingGlowSurface(
    voiceState: VoiceState,
    reducedMotionOverride: Boolean,
    modifier: Modifier = Modifier
) {
    val reducedMotion = reducedMotionOverride
    val colorScheme = MaterialTheme.colorScheme
    val surface = colorScheme.surfaceContainerHigh
    val shape = RoundedCornerShape(Sizes.CornerMassive)
    if (!voiceState.isGlowActive()) {
        Box(modifier.background(surface, shape))
        return
    }

    val phase = rememberVoiceGlowPhase(reducedMotion, "voiceRecordingInterior")
    val amplitude = voiceState.glowAmplitude()
    val intensity = rememberVoiceGlowIntensity(amplitude, reducedMotion, "voiceGlowIntensity")
    val primaryGlow = colorScheme.primary
    val secondaryGlow = colorScheme.secondary
    val tertiaryGlow = colorScheme.tertiary

    Canvas(modifier.clip(shape)) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        // Normalized ratios form one compositional recipe so the sweep scales with the pill, not the device.
        val corner = CornerRadius(height / 2f)
        val boundedIntensity = intensity.coerceIn(GlowDesign.MIN_AMPLITUDE, GlowDesign.MAX_AMPLITUDE)
        val flowAlpha = GlowDesign.FLOW_ALPHA_BASE + GlowDesign.FLOW_ALPHA_FROM_AMPLITUDE * boundedIntensity
        val phaseOffset = phase - GlowDesign.PHASE_CENTER
        val flowX = phaseOffset * width * 0.9f
        drawRoundRect(color = surface, cornerRadius = corner)
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    primaryGlow.copy(alpha = flowAlpha * 0.62f),
                    secondaryGlow.copy(alpha = flowAlpha * 0.88f),
                    tertiaryGlow.copy(alpha = flowAlpha * 0.76f),
                    secondaryGlow.copy(alpha = flowAlpha * 0.94f),
                    primaryGlow.copy(alpha = flowAlpha * 0.62f)
                ),
                start = Offset(flowX - width * 0.42f, 0f),
                end = Offset(flowX + width * 0.9f, height)
            ),
            cornerRadius = corner
        )
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryGlow.copy(alpha = flowAlpha * 0.7f),
                    secondaryGlow.copy(alpha = flowAlpha * 0.42f),
                    Color.Transparent
                ),
                center = Offset(width * (0.16f + phase * 0.66f), height * 0.42f),
                radius = width * 0.72f
            ),
            cornerRadius = corner
        )
        val strokeWidth = GLOW_BORDER_STROKE_WIDTH.toPx()
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    primaryGlow.copy(alpha = flowAlpha * 1.8f),
                    secondaryGlow.copy(alpha = flowAlpha * 2f),
                    tertiaryGlow.copy(alpha = flowAlpha * 1.9f),
                    primaryGlow.copy(alpha = flowAlpha * 1.8f)
                ),
                start = Offset(flowX - width * 0.4f, 0f),
                end = Offset(flowX + width, height)
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
    reducedMotionOverride: Boolean,
    modifier: Modifier = Modifier
) {
    if (!voiceState.isGlowActive()) return
    val reducedMotion = reducedMotionOverride

    val phase = rememberVoiceGlowPhase(reducedMotion, "voiceRecordingAmbient")
    val intensity = rememberVoiceGlowIntensity(voiceState.glowAmplitude(), reducedMotion, "voiceAmbientIntensity")
    val colorScheme = MaterialTheme.colorScheme
    val primaryGlow = colorScheme.primary
    val secondaryGlow = colorScheme.secondary
    val tertiaryGlow = colorScheme.tertiary

    Canvas(modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        // Keep the spill soft and proportional to the composer; amplitude only tunes its restrained opacity.
        val radius = maxOf(width * GlowDesign.AMBIENT_RADIUS_WIDTH, height * GlowDesign.AMBIENT_RADIUS_HEIGHT)
        val alpha = GlowDesign.AMBIENT_ALPHA_BASE + GlowDesign.AMBIENT_ALPHA_FROM_AMPLITUDE * intensity.coerceIn(GlowDesign.MIN_AMPLITUDE, GlowDesign.MAX_AMPLITUDE)
        val drift = phase - GlowDesign.PHASE_CENTER
        val centerY = height * 0.88f
        val leftCenter = Offset(width * (0.27f + drift * 0.12f), centerY)
        val rightCenter = Offset(width * (0.73f - drift * 0.1f), centerY)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryGlow.copy(alpha = alpha), secondaryGlow.copy(alpha = alpha * 0.52f), Color.Transparent),
                center = leftCenter,
                radius = radius
            ),
            center = leftCenter,
            radius = radius
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tertiaryGlow.copy(alpha = alpha * 0.92f), secondaryGlow.copy(alpha = alpha * 0.45f), Color.Transparent),
                center = rightCenter,
                radius = radius
            ),
            center = rightCenter,
            radius = radius
        )
        val center = Offset(width * (0.5f + drift * 0.14f), centerY + height * 0.12f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondaryGlow.copy(alpha = alpha * 0.62f), Color.Transparent),
                center = center,
                radius = radius * 0.82f
            ),
            center = center,
            radius = radius * 0.82f
        )
    }
}
