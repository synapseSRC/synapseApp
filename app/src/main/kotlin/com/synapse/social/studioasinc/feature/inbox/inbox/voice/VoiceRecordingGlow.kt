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

// Decorative project accents (not M3 semantic text colors); content remains on theme roles.
private val VoiceGlowCyan = Color(0xFF46EAF5)
private val VoiceGlowBlue = Color(0xFF397BFF)
private val VoiceGlowViolet = Color(0xFF9B5CFF)

@Composable
private fun rememberVoiceGlowPhase(reducedMotion: Boolean, label: String): Float {
    val transition = rememberInfiniteTransition(label = label)
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        // Project design decision (not an M3 token): slow drift keeps the glow alive during silence.
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voiceGlowPhase"
    )
    return if (reducedMotion) 0.5f else phase
}

private fun VoiceState.isGlowActive(): Boolean = this is VoiceState.Recording || this is VoiceState.Locked

internal fun VoiceState.glowAmplitude(): Float = when (this) {
    is VoiceState.Recording -> amplitudeHistory.lastOrNull() ?: 0.08f
    is VoiceState.Locked -> if (isPaused) 0.08f else amplitudeHistory.lastOrNull() ?: 0.08f
    else -> 0f
}

@Composable
fun VoiceRecordingGlowSurface(voiceState: VoiceState, modifier: Modifier = Modifier) {
    val surface = MaterialTheme.colorScheme.surfaceContainerHigh
    val shape = RoundedCornerShape(Sizes.CornerMassive)
    if (!voiceState.isGlowActive()) {
        Box(modifier.background(surface, shape))
        return
    }

    val reducedMotion = AnimationUtil.rememberReducedMotion()
    val phase = rememberVoiceGlowPhase(reducedMotion, "voiceRecordingInterior")
    val amplitude = voiceState.glowAmplitude().coerceIn(0.08f, 1f)
    val intensity by animateFloatAsState(
        targetValue = amplitude,
        animationSpec = spring(dampingRatio = 1f, stiffness = 3800f),
        label = "voiceGlowIntensity"
    )

    Canvas(modifier.clip(shape)) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val corner = CornerRadius(height / 2f)
        val flowAlpha = 0.07f + 0.28f * intensity.coerceIn(0.08f, 1f)
        val flowX = (phase - 0.5f) * width * 0.9f
        drawRoundRect(color = surface, cornerRadius = corner)
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    VoiceGlowCyan.copy(alpha = flowAlpha * 0.62f),
                    VoiceGlowBlue.copy(alpha = flowAlpha * 0.88f),
                    VoiceGlowViolet.copy(alpha = flowAlpha * 0.76f),
                    VoiceGlowBlue.copy(alpha = flowAlpha * 0.94f),
                    VoiceGlowCyan.copy(alpha = flowAlpha * 0.62f)
                ),
                start = Offset(flowX - width * 0.42f, 0f),
                end = Offset(flowX + width * 0.9f, height)
            ),
            cornerRadius = corner
        )
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    VoiceGlowCyan.copy(alpha = 0.04f + intensity * 0.18f),
                    VoiceGlowBlue.copy(alpha = 0.025f + intensity * 0.09f),
                    Color.Transparent
                ),
                center = Offset(width * (0.16f + phase * 0.66f), height * 0.42f),
                radius = width * 0.72f
            ),
            cornerRadius = corner
        )
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(VoiceGlowViolet.copy(alpha = 0.025f + intensity * 0.13f), Color.Transparent),
                center = Offset(width * (0.82f - phase * 0.42f), height * 0.8f),
                radius = width * 0.52f
            ),
            cornerRadius = corner
        )
        val strokeWidth = 1.2.dp.toPx()
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    VoiceGlowCyan.copy(alpha = 0.32f + intensity * 0.4f),
                    VoiceGlowBlue.copy(alpha = 0.28f + intensity * 0.42f),
                    VoiceGlowViolet.copy(alpha = 0.3f + intensity * 0.4f),
                    VoiceGlowCyan.copy(alpha = 0.32f + intensity * 0.4f)
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
fun VoiceRecordingAmbientGlow(voiceState: VoiceState, modifier: Modifier = Modifier) {
    if (!voiceState.isGlowActive()) return

    val reducedMotion = AnimationUtil.rememberReducedMotion()
    val phase = rememberVoiceGlowPhase(reducedMotion, "voiceRecordingAmbient")
    val amplitude = voiceState.glowAmplitude().coerceIn(0.08f, 1f)
    val intensity by animateFloatAsState(
        targetValue = amplitude,
        animationSpec = spring(dampingRatio = 1f, stiffness = 3800f),
        label = "voiceAmbientIntensity"
    )

    Canvas(modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val radius = maxOf(width * 0.7f, height * 1.05f)
        val alpha = 0.035f + 0.095f * intensity.coerceIn(0.08f, 1f)
        val drift = phase - 0.5f
        val centerY = height * 0.88f
        val leftCenter = Offset(width * (0.27f + drift * 0.12f), centerY)
        val rightCenter = Offset(width * (0.73f - drift * 0.1f), centerY)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(VoiceGlowCyan.copy(alpha = alpha), VoiceGlowBlue.copy(alpha = alpha * 0.52f), Color.Transparent),
                center = leftCenter,
                radius = radius
            ),
            center = leftCenter,
            radius = radius
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(VoiceGlowViolet.copy(alpha = alpha * 0.92f), VoiceGlowBlue.copy(alpha = alpha * 0.45f), Color.Transparent),
                center = rightCenter,
                radius = radius
            ),
            center = rightCenter,
            radius = radius
        )
        val center = Offset(width * (0.5f + drift * 0.14f), centerY + height * 0.12f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(VoiceGlowBlue.copy(alpha = alpha * 0.62f), Color.Transparent),
                center = center,
                radius = radius * 0.82f
            ),
            center = center,
            radius = radius * 0.82f
        )
    }
}
