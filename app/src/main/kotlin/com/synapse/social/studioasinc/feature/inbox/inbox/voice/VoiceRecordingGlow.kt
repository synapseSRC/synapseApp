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
    const val PHASE_DRIFT_DURATION_MILLIS = 6_800
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
    // This Compose version rejects SpringSpec inside infiniteRepeatable; a long eased tween keeps silence moving without a pulse.
    val transition = rememberInfiniteTransition(label = label)
    val phasePosition by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = GlowDesign.PHASE_DRIFT_DURATION_MILLIS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voiceGlowPhasePosition"
    )
    return GlowDesign.PHASE_CENTER + phasePosition * GlowDesign.MAX_PHASE_DRIFT
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

        // Normalized ratios below are named by their role and scaled against the fixed pill geometry.
        val corner = CornerRadius(height / 2f)
        val boundedIntensity = intensity.coerceIn(GlowDesign.MIN_AMPLITUDE, GlowDesign.MAX_AMPLITUDE)
        val flowAlpha = GlowDesign.FLOW_ALPHA_BASE + GlowDesign.FLOW_ALPHA_FROM_AMPLITUDE * boundedIntensity
        val phaseOffset = phase - GlowDesign.PHASE_CENTER
        val flowTravelRatio = 0.9f
        val flowLeadingEdgeRatio = 0.42f
        val flowTrailingEdgeRatio = 0.9f
        val flowCenterX = phaseOffset * width * flowTravelRatio
        val flowLeadingEdgeX = flowCenterX - width * flowLeadingEdgeRatio
        val flowTrailingEdgeX = flowCenterX + width * flowTrailingEdgeRatio
        val primarySweepAlphaRatio = 0.62f
        val secondarySweepAlphaRatio = 0.88f
        val tertiarySweepAlphaRatio = 0.76f
        val finalSweepAlphaRatio = 0.94f
        val radialPrimaryAlphaRatio = 0.7f
        val radialSecondaryAlphaRatio = 0.42f
        val primarySweepAlpha = flowAlpha * primarySweepAlphaRatio // leading/trailing theme accent
        val secondarySweepAlpha = flowAlpha * secondarySweepAlphaRatio // center sweep emphasis
        val tertiarySweepAlpha = flowAlpha * tertiarySweepAlphaRatio // tertiary bridge
        val finalSweepAlpha = flowAlpha * finalSweepAlphaRatio // bright return accent
        val radialPrimaryAlpha = flowAlpha * radialPrimaryAlphaRatio // center highlight strength
        val radialSecondaryAlpha = flowAlpha * radialSecondaryAlphaRatio // soft color blend
        val radialCenterStartRatio = 0.16f
        val radialCenterPhaseTravelRatio = 0.66f
        val radialCenterHeightRatio = 0.42f
        val radialHighlightCenter = Offset(width * (radialCenterStartRatio + phase * radialCenterPhaseTravelRatio), height * radialCenterHeightRatio)
        val radialHighlightRadiusRatio = 0.72f
        val radialHighlightRadius = width * radialHighlightRadiusRatio
        val borderPrimaryAlphaRatio = 1.8f
        val borderSecondaryAlphaRatio = 2f
        val borderTertiaryAlphaRatio = 1.9f
        val borderPrimaryAlpha = flowAlpha * borderPrimaryAlphaRatio // border brightness relative to fill
        val borderSecondaryAlpha = flowAlpha * borderSecondaryAlphaRatio
        val borderTertiaryAlpha = flowAlpha * borderTertiaryAlphaRatio
        val borderLeadingEdgeRatio = 0.4f
        val borderTrailingEdgeRatio = 1f
        val borderLeadingEdgeX = flowCenterX - width * borderLeadingEdgeRatio
        val borderTrailingEdgeX = flowCenterX + width * borderTrailingEdgeRatio
        drawRoundRect(color = surface, cornerRadius = corner)
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    primaryGlow.copy(alpha = primarySweepAlpha),
                    secondaryGlow.copy(alpha = secondarySweepAlpha),
                    tertiaryGlow.copy(alpha = tertiarySweepAlpha),
                    secondaryGlow.copy(alpha = finalSweepAlpha),
                    primaryGlow.copy(alpha = primarySweepAlpha)
                ),
                start = Offset(flowLeadingEdgeX, 0f),
                end = Offset(flowTrailingEdgeX, height)
            ),
            cornerRadius = corner
        )
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(primaryGlow.copy(alpha = radialPrimaryAlpha), secondaryGlow.copy(alpha = radialSecondaryAlpha), Color.Transparent),
                center = radialHighlightCenter,
                radius = radialHighlightRadius
            ),
            cornerRadius = corner
        )
        val strokeWidth = GLOW_BORDER_STROKE_WIDTH.toPx()
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    primaryGlow.copy(alpha = borderPrimaryAlpha),
                    secondaryGlow.copy(alpha = borderSecondaryAlpha),
                    tertiaryGlow.copy(alpha = borderTertiaryAlpha),
                    primaryGlow.copy(alpha = borderPrimaryAlpha)
                ),
                start = Offset(borderLeadingEdgeX, 0f),
                end = Offset(borderTrailingEdgeX, height)
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

        // Named local ratios keep the three ambient lobes tunable without creating a second global token catalog.
        val radius = maxOf(width * GlowDesign.AMBIENT_RADIUS_WIDTH, height * GlowDesign.AMBIENT_RADIUS_HEIGHT)
        val alpha = GlowDesign.AMBIENT_ALPHA_BASE + GlowDesign.AMBIENT_ALPHA_FROM_AMPLITUDE * intensity.coerceIn(GlowDesign.MIN_AMPLITUDE, GlowDesign.MAX_AMPLITUDE)
        val drift = phase - GlowDesign.PHASE_CENTER
        val ambientCenterHeightRatio = 0.88f
        val ambientCenterY = height * ambientCenterHeightRatio
        val leftBaseCenterRatio = 0.27f
        val leftDriftRatio = 0.12f
        val leftBaseX = width * leftBaseCenterRatio
        val leftDriftX = width * drift * leftDriftRatio
        val rightBaseCenterRatio = 0.73f
        val rightDriftRatio = 0.1f
        val rightBaseX = width * rightBaseCenterRatio
        val rightDriftX = width * drift * rightDriftRatio
        val leftCenter = Offset(leftBaseX + leftDriftX, ambientCenterY)
        val rightCenter = Offset(rightBaseX - rightDriftX, ambientCenterY)
        val leftBlendAlphaRatio = 0.52f
        val rightPrimaryAlphaRatio = 0.92f
        val rightBlendAlphaRatio = 0.45f
        val centerHighlightAlphaRatio = 0.62f
        val leftBlendAlpha = alpha * leftBlendAlphaRatio
        val rightPrimaryAlpha = alpha * rightPrimaryAlphaRatio
        val rightBlendAlpha = alpha * rightBlendAlphaRatio
        val centerHighlightAlpha = alpha * centerHighlightAlphaRatio
        val centerBaseRatio = 0.5f
        val centerDriftRatio = 0.14f
        val centerHeightOffsetRatio = 0.12f
        val centerHighlight = Offset(width * (centerBaseRatio + drift * centerDriftRatio), ambientCenterY + height * centerHeightOffsetRatio)
        val centerHighlightRadiusRatio = 0.82f
        val centerHighlightRadius = radius * centerHighlightRadiusRatio

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryGlow.copy(alpha = alpha), secondaryGlow.copy(alpha = leftBlendAlpha), Color.Transparent),
                center = leftCenter,
                radius = radius
            ),
            center = leftCenter,
            radius = radius
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tertiaryGlow.copy(alpha = rightPrimaryAlpha), secondaryGlow.copy(alpha = rightBlendAlpha), Color.Transparent),
                center = rightCenter,
                radius = radius
            ),
            center = rightCenter,
            radius = radius
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondaryGlow.copy(alpha = centerHighlightAlpha), Color.Transparent),
                center = centerHighlight,
                radius = centerHighlightRadius
            ),
            center = centerHighlight,
            radius = centerHighlightRadius
        )
    }
}
