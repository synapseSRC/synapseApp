package com.synapse.social.studioasinc.feature.call.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import com.synapse.social.studioasinc.feature.auth.ui.util.AnimationUtil
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The calm, cool backdrop the call screens sit on.
 *
 * A flat surface color made the portrait look pasted on. This draws the themed page tone plus three
 * low-alpha radial washes in the brand accent roles that drift on lissajous paths, breathe in size
 * and opacity, and blend toward their neighbouring role, so the background keeps flowing instead of
 * reading as a static gradient. The container roles were too close to the page tone for the motion
 * to register, which is why the washes use the accent roles at a low alpha.
 *
 * Every time term is a whole harmonic of [CallDimens.AtmosphereCycleMillis], so the loop restarts
 * without a visible jump. The phase is read inside the draw scope: each frame invalidates drawing
 * only, never composition. Reduced motion freezes the washes at their resting layout.
 */
@Composable
fun CallAtmosphere(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val base = scheme.surfaceContainerLow
    val primaryWash = scheme.primary
    val secondaryWash = scheme.secondary
    val tertiaryWash = scheme.tertiary
    val phase = rememberAtmospherePhase()

    Canvas(modifier = modifier.fillMaxSize()) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        drawRect(base)

        val cycle = (phase?.value ?: 0f) * FULL_TURN
        val width = size.width
        val height = size.height

        drawWash(
            center = Offset(
                x = width * (0.50f + CallDimens.AtmospherePrimaryDriftX * sin(cycle)),
                y = height * (0.34f + CallDimens.AtmospherePrimaryDriftY * cos(cycle))
            ),
            radius = width * (0.82f + CallDimens.AtmosphereRadiusPulse * sin(cycle * 2f + 0.9f)),
            color = lerp(
                primaryWash,
                secondaryWash,
                CallDimens.AtmosphereTintBlend * (0.5f + 0.5f * sin(cycle + 0.6f))
            ),
            alpha = CallDimens.AtmosphereWashAlpha * atmosphereBreath(cycle, 0.4f)
        )
        drawWash(
            center = Offset(
                x = width * (0.15f + CallDimens.AtmosphereEdgeDriftX * cos(cycle)),
                y = height * (0.72f + CallDimens.AtmosphereEdgeDriftY * sin(cycle))
            ),
            radius = width * (0.66f + CallDimens.AtmosphereRadiusPulse * sin(cycle * 2f + 2.2f)),
            color = lerp(
                secondaryWash,
                tertiaryWash,
                CallDimens.AtmosphereTintBlend * (0.5f + 0.5f * sin(cycle + 2.4f))
            ),
            alpha = CallDimens.AtmosphereEdgeAlpha * atmosphereBreath(cycle, 1.9f)
        )
        drawWash(
            center = Offset(
                x = width * (0.85f + CallDimens.AtmosphereEdgeDriftX * sin(cycle * 2f + 0.7f)),
                y = height * (0.15f + CallDimens.AtmosphereEdgeDriftY * cos(cycle))
            ),
            radius = width * (0.56f + CallDimens.AtmosphereRadiusPulse * sin(cycle + 1.7f)),
            color = lerp(
                tertiaryWash,
                primaryWash,
                CallDimens.AtmosphereTintBlend * (0.5f + 0.5f * sin(cycle + 4.0f))
            ),
            alpha = CallDimens.AtmosphereEdgeAlpha * atmosphereBreath(cycle, 3.3f)
        )
    }
}

private fun atmosphereBreath(cycle: Float, offset: Float): Float =
    1f + CallDimens.AtmosphereAlphaPulse * sin(cycle * 2f + offset)

private const val FULL_TURN = (2.0 * PI).toFloat()

@Composable
private fun rememberAtmospherePhase(): State<Float>? {
    if (AnimationUtil.rememberReducedMotion()) return null
    val transition = rememberInfiniteTransition(label = "callAtmosphere")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(CallDimens.AtmosphereCycleMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "callAtmospherePhase"
    )
}

private fun DrawScope.drawWash(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = alpha),
                color.copy(alpha = alpha * 0.45f),
                color.copy(alpha = 0f)
            ),
            center = center,
            radius = radius
        ),
        center = center,
        radius = radius
    )
}
