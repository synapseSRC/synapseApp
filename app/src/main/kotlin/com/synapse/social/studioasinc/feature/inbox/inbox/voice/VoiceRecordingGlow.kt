package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.cardSurfaceColor
import kotlin.math.cos
import kotlin.math.sin

private object GlowColors {
    val Cyan = Color(0xFF00F2FE)
    val ElectricBlue = Color(0xFF2979FF)
    val Magenta = Color(0xFFFF2A85)
    val SunsetGold = Color(0xFFFFB300)
    val Violet = Color(0xFF7C4DFF)
    val DeepIndigo = Color(0xFF3D5AFE)
    val SpecularRim = Color(0x80FFFFFF)
}

private object GlowDesign {
    const val MIN_AMPLITUDE = 0.08f
    const val MAX_AMPLITUDE = 1.0f
    const val TAU = 6.2831855f
    const val FLUID_NODES = 28
}

/** 1D spring-coupled fluid surface simulation with organic multi-center wave motion */
internal class FluidSurfaceSimulation(private val nodeCount: Int = GlowDesign.FLUID_NODES) {
    val heights = FloatArray(nodeCount) { 0f }
    private val velocities = FloatArray(nodeCount) { 0f }

    fun update(dt: Float, audioEnergy: Float, phase: Float) {
        val springK = 7f
        val coupling = 16f
        val damping = 0.93f

        // Audio energy introduces energetic wave impulses across fluid surface
        if (audioEnergy > 0.08f) {
            val impulse = audioEnergy * 4.5f
            val primaryNode = ((nodeCount - 1) * (0.5f + 0.38f * sin(phase * 4.2f))).toInt().coerceIn(2, nodeCount - 3)
            velocities[primaryNode] += impulse
            velocities[primaryNode - 1] += impulse * 0.75f
            velocities[primaryNode + 1] += impulse * 0.75f
            velocities[primaryNode - 2] += impulse * 0.40f
            velocities[primaryNode + 2] += impulse * 0.40f
        }

        // Connected fluid boundary physics step
        for (i in 0 until nodeCount) {
            val left = if (i > 0) heights[i - 1] else heights[i]
            val right = if (i < nodeCount - 1) heights[i + 1] else heights[i]

            val springForce = -springK * heights[i]
            val couplingForce = coupling * (left + right - 2f * heights[i])
            val accel = springForce + couplingForce

            velocities[i] = (velocities[i] + accel * dt) * damping
            heights[i] = (heights[i] + velocities[i] * dt).coerceIn(-0.65f, 0.65f)
        }
    }
}

private const val SOFT_VOLUMETRIC_AGSL_SHADER = """
uniform float2 iResolution;
uniform float iTime;
uniform float iAmplitude;
uniform float4 colorCyan;
uniform float4 colorBlue;
uniform float4 colorMagenta;
uniform float4 colorGold;
uniform float4 colorViolet;

half4 main(in float2 fragCoord) {
    float2 uv = fragCoord / iResolution;

    // Organic multi-frequency wave boundary
    float w1 = sin(uv.x * 6.0 + iTime * 2.8) * 0.16 * iAmplitude;
    float w2 = cos(uv.x * 11.0 - iTime * 2.2) * 0.12 * iAmplitude;
    float w3 = sin(uv.x * 3.5 + iTime * 1.2) * 0.06;

    float waveSurface = 0.70 - (w1 + w2 + w3 + iAmplitude * 0.15);

    // Ultra-soft volumetric transition along liquid edge (no hard sharp boundary)
    float liquidVolume = smoothstep(waveSurface - 0.28, waveSurface + 0.18, uv.y);

    if (liquidVolume <= 0.001) {
        return half4(0.0, 0.0, 0.0, 0.0);
    }

    // Multi-center dynamic volumetric color pooling (Cyan, Blue, Magenta, Sunset Gold, Violet)
    float c1X = 0.20 + 0.18 * sin(iTime * 1.1 + uv.y * 2.0);
    float c2X = 0.50 + 0.18 * cos(iTime * 0.9 + uv.y * 1.5);
    float c3X = 0.80 + 0.18 * sin(iTime * 1.3 - uv.y * 2.5);

    float d1 = length(float2(uv.x - c1X, uv.y - 0.85));
    float d2 = length(float2(uv.x - c2X, uv.y - 0.80));
    float d3 = length(float2(uv.x - c3X, uv.y - 0.85));

    float3 col = colorCyan.rgb * smoothstep(0.72, 0.0, d1);
    col += colorMagenta.rgb * smoothstep(0.65, 0.0, d2);
    col += colorGold.rgb * smoothstep(0.55, 0.0, d2) * 0.65;
    col += colorViolet.rgb * smoothstep(0.72, 0.0, d3);
    col += colorBlue.rgb * (1.0 - smoothstep(0.0, 0.85, d2));

    float alpha = liquidVolume * (0.68 + 0.32 * iAmplitude);
    return half4(col, alpha);
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class LiquidShaderHolder {
    val runtimeShader = RuntimeShader(SOFT_VOLUMETRIC_AGSL_SHADER)
    val shaderBrush = ShaderBrush(runtimeShader)

    init {
        runtimeShader.setFloatUniform("colorCyan", GlowColors.Cyan.red, GlowColors.Cyan.green, GlowColors.Cyan.blue, GlowColors.Cyan.alpha)
        runtimeShader.setFloatUniform("colorBlue", GlowColors.ElectricBlue.red, GlowColors.ElectricBlue.green, GlowColors.ElectricBlue.blue, GlowColors.ElectricBlue.alpha)
        runtimeShader.setFloatUniform("colorMagenta", GlowColors.Magenta.red, GlowColors.Magenta.green, GlowColors.Magenta.blue, GlowColors.Magenta.alpha)
        runtimeShader.setFloatUniform("colorGold", GlowColors.SunsetGold.red, GlowColors.SunsetGold.green, GlowColors.SunsetGold.blue, GlowColors.SunsetGold.alpha)
        runtimeShader.setFloatUniform("colorViolet", GlowColors.Violet.red, GlowColors.Violet.green, GlowColors.Violet.blue, GlowColors.Violet.alpha)
    }

    fun updateUniforms(width: Float, height: Float, timeSeconds: Float, amplitude: Float) {
        runtimeShader.setFloatUniform("iResolution", width, height)
        runtimeShader.setFloatUniform("iTime", timeSeconds)
        runtimeShader.setFloatUniform("iAmplitude", amplitude)
    }
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
internal fun rememberVoiceGlowPhase(
    amplitude: Float,
    active: Boolean,
    isPaused: Boolean = false,
    reducedMotion: Boolean,
    label: String
): Float {
    var phase by remember(label) { mutableFloatStateOf(0f) }
    val latestAmplitude by rememberUpdatedState(amplitude)
    val latestPaused by rememberUpdatedState(isPaused)
    LaunchedEffect(active, reducedMotion) {
        if (!active || reducedMotion) {
            if (reducedMotion) phase = 0f
            return@LaunchedEffect
        }
        var previousFrameNanos = 0L
        while (active && !reducedMotion) {
            val frameNanos = withFrameNanos { it }
            if (previousFrameNanos != 0L) {
                val elapsedSeconds = ((frameNanos - previousFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                if (!latestPaused) {
                    val speed = 0.35f + 1.15f * latestAmplitude
                    phase += elapsedSeconds * speed
                    if (phase >= 1000f) phase -= 1000f
                }
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
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 450f),
        label = label
    )
    return intensity
}

@Composable
internal fun rememberVoiceShimmerPhase(reducedMotion: Boolean, label: String): Float {
    if (reducedMotion) return 0.5f
    val transition = rememberInfiniteTransition(label = label)
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "voiceCancelShimmerPhase"
    )
    return phase
}

@Composable
fun VoiceRecordingGlowSurface(
    voiceState: VoiceState,
    reducedMotionOverride: Boolean,
    modifier: Modifier = Modifier
) {
    val active = voiceState.isGlowActive()
    val reducedMotion = reducedMotionOverride
    val surfaceColor = cardSurfaceColor()
    val shape = RoundedCornerShape(Sizes.CornerMassive)
    val targetAmplitude = if (active) voiceState.glowAmplitude() else 0f
    val intensity = rememberVoiceGlowIntensity(targetAmplitude, reducedMotion, "voiceGlowIntensity")
    val isPaused = (voiceState as? VoiceState.Locked)?.isPaused == true
    val phase = rememberVoiceGlowPhase(intensity, active, isPaused, reducedMotion, "voiceRecordingInterior")

    val activeAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "voiceLiquidSettle"
    )

    val fluidSim = remember { FluidSurfaceSimulation() }

    // AGSL Shader instance cached for API 33+
    val shaderHolder = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LiquidShaderHolder()
        } else null
    }

    LaunchedEffect(phase, intensity, active, isPaused) {
        if (active && !isPaused) {
            fluidSim.update(0.04f, intensity, phase)
        }
    }

    Box(modifier = modifier.clip(shape)) {
        val density = LocalDensity.current
        Canvas(Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            if (width <= 0f || height <= 0f) return@Canvas

            val maxCornerPx = with(density) { Sizes.CornerMassive.toPx() }
            val corner = CornerRadius(minOf(maxCornerPx, height / 2f))

            drawRoundRect(
                color = surfaceColor,
                cornerRadius = corner
            )

            if (activeAlpha <= 0.001f) return@Canvas

            val supportsAgsl = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shaderHolder != null

            if (supportsAgsl) {
                shaderHolder.updateUniforms(width, height, phase, intensity * activeAlpha)
                drawRoundRect(
                    brush = shaderHolder.shaderBrush,
                    cornerRadius = corner,
                    alpha = activeAlpha
                )
            } else {
                // Soft Volumetric Canvas Fallback (API 26..32) with multi-color radial gradient pooling
                val angle = phase * GlowDesign.TAU
                val alpha = activeAlpha * (0.65f + 0.35f * intensity)

                val leftCenter = Offset(width * (0.22f + 0.10f * sin(angle)), height * 0.85f)
                val midCenter = Offset(width * (0.50f + 0.10f * cos(angle * 0.9f)), height * 0.80f)
                val rightCenter = Offset(width * (0.78f - 0.10f * sin(angle * 1.2f)), height * 0.85f)
                val radius = width * 0.55f

                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(GlowColors.Cyan.copy(alpha = alpha * 0.85f), GlowColors.ElectricBlue.copy(alpha = alpha * 0.40f), Color.Transparent),
                        center = leftCenter, radius = radius
                    ),
                    center = leftCenter, radius = radius
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(GlowColors.Magenta.copy(alpha = alpha * 0.80f), GlowColors.SunsetGold.copy(alpha = alpha * 0.45f), Color.Transparent),
                        center = midCenter, radius = radius * 0.90f
                    ),
                    center = midCenter, radius = radius * 0.90f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(GlowColors.Violet.copy(alpha = alpha * 0.85f), GlowColors.DeepIndigo.copy(alpha = alpha * 0.45f), Color.Transparent),
                        center = rightCenter, radius = radius
                    ),
                    center = rightCenter, radius = radius
                )
            }

            // Glass Specular Rim Border & Refraction Highlight
            val borderAlpha = activeAlpha * (0.35f + intensity * 0.25f)
            val strokePx = 1.8.dp.toPx()
            val strokeCornerRadiusPx = with(density) { (Sizes.CornerMassive - (strokePx / 2f).toDp()).toPx() }
            val strokeCorner = CornerRadius(minOf(strokeCornerRadiusPx, (height - strokePx) / 2f).coerceAtLeast(0f))
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GlowColors.SpecularRim.copy(alpha = borderAlpha * 1.5f),
                        GlowColors.Cyan.copy(alpha = borderAlpha),
                        GlowColors.Violet.copy(alpha = borderAlpha * 0.9f)
                    )
                ),
                topLeft = Offset(strokePx / 2f, strokePx / 2f),
                size = Size((width - strokePx).coerceAtLeast(0f), (height - strokePx).coerceAtLeast(0f)),
                cornerRadius = strokeCorner,
                style = Stroke(width = strokePx)
            )
        }
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
    val isPaused = (voiceState as? VoiceState.Locked)?.isPaused == true
    val phase = rememberVoiceGlowPhase(amplitude, true, isPaused, reducedMotion, "voiceRecordingAmbient")
    val intensity = rememberVoiceGlowIntensity(amplitude, reducedMotion, "voiceAmbientIntensity")

    Canvas(modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val angle = phase * GlowDesign.TAU
        val radius = maxOf(width * 0.70f, height * 1.05f)
        val alpha = 0.15f + 0.25f * amplitude.coerceIn(0f, 1f)

        val leftCenter = Offset(width * (0.25f + 0.08f * sin(angle)), height * 0.82f)
        val midCenter = Offset(width * (0.50f + 0.08f * cos(angle * 0.9f)), height * 0.85f)
        val rightCenter = Offset(width * (0.75f - 0.08f * cos(angle * 0.8f)), height * 0.82f)

        drawCircle(
            brush = Brush.radialGradient(
                listOf(GlowColors.Cyan.copy(alpha = alpha), GlowColors.ElectricBlue.copy(alpha = alpha * 0.5f), Color.Transparent),
                center = leftCenter, radius = radius
            ),
            center = leftCenter, radius = radius
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(GlowColors.Magenta.copy(alpha = alpha * 0.75f), GlowColors.SunsetGold.copy(alpha = alpha * 0.35f), Color.Transparent),
                center = midCenter, radius = radius * 0.85f
            ),
            center = midCenter, radius = radius * 0.85f
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(GlowColors.Violet.copy(alpha = alpha * 0.85f), GlowColors.DeepIndigo.copy(alpha = alpha * 0.40f), Color.Transparent),
                center = rightCenter, radius = radius
            ),
            center = rightCenter, radius = radius
        )
    }
}
