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
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import kotlin.math.cos
import kotlin.math.sin

private object GlowColors {
    val Cyan = Color(0xFF00F2FE)
    val ElectricBlue = Color(0xFF4FACFE)
    val BrightViolet = Color(0xFF00C6FF)
    val DeepPurple = Color(0xFF7000FF)
    val CrestHighlight = Color(0xFFE0FFFF)
    val SpecularRim = Color(0x80FFFFFF)
}

private object GlowDesign {
    const val MIN_AMPLITUDE = 0.08f
    const val MAX_AMPLITUDE = 1.0f
    const val TAU = 6.2831855f
    const val FLUID_NODES = 28
}

/** 1D spring-coupled fluid surface simulation with dramatic audio reactivity */
internal class FluidSurfaceSimulation(private val nodeCount: Int = GlowDesign.FLUID_NODES) {
    val heights = FloatArray(nodeCount) { 0f }
    private val velocities = FloatArray(nodeCount) { 0f }

    fun update(dt: Float, audioEnergy: Float, phase: Float) {
        val springK = 8f
        val coupling = 18f
        val damping = 0.92f

        // Audio energy introduces energetic wave impulses across fluid surface
        if (audioEnergy > 0.08f) {
            val impulse = audioEnergy * 4.8f
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

private const val LIQUID_AGSL_SHADER = """
uniform float2 iResolution;
uniform float iTime;
uniform float iAmplitude;
uniform float4 colorCyan;
uniform float4 colorBlue;
uniform float4 colorViolet;

half4 main(in float2 fragCoord) {
    float2 uv = fragCoord / iResolution;

    // High-amplitude, multi-frequency wave displacement (up to 40% height variation)
    float w1 = sin(uv.x * 8.0 + iTime * 4.5) * 0.16 * iAmplitude;
    float w2 = cos(uv.x * 15.0 - iTime * 3.2) * 0.10 * iAmplitude;
    float w3 = sin(uv.x * 4.0 + iTime * 2.0) * 0.04; // Alive baseline motion

    float waveHeight = 0.64 - (w1 + w2 + w3 + iAmplitude * 0.18);

    if (uv.y < waveHeight) {
        // Frosted liquid glass interior shine above wave crest
        float glassShine = smoothstep(0.0, waveHeight, uv.y) * (0.08 + 0.06 * iAmplitude);
        return half4(colorCyan.rgb, glassShine);
    }

    // Liquid body fill with vibrant cyan-blue-violet gradient
    float depth = (uv.y - waveHeight) / max(0.01, 1.0 - waveHeight);
    float xPos = uv.x + sin(iTime * 0.9 + uv.y * 3.0) * 0.18;

    float3 col = mix(colorCyan.rgb, colorBlue.rgb, clamp(xPos * 1.2, 0.0, 1.0));
    col = mix(col, colorViolet.rgb, clamp(depth * 1.3, 0.0, 1.0));

    // Intense luminous crest highlight along wave boundary
    float crest = smoothstep(0.05, 0.0, abs(uv.y - waveHeight));
    col += float3(0.60, 0.95, 1.0) * crest * (0.85 + 0.40 * iAmplitude);

    float alpha = clamp(0.82 + 0.18 * iAmplitude, 0.0, 1.0);
    return half4(col, alpha);
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class LiquidShaderHolder {
    val runtimeShader = RuntimeShader(LIQUID_AGSL_SHADER)
    val shaderBrush = ShaderBrush(runtimeShader)

    init {
        runtimeShader.setFloatUniform("colorCyan", GlowColors.Cyan.red, GlowColors.Cyan.green, GlowColors.Cyan.blue, GlowColors.Cyan.alpha)
        runtimeShader.setFloatUniform("colorBlue", GlowColors.ElectricBlue.red, GlowColors.ElectricBlue.green, GlowColors.ElectricBlue.blue, GlowColors.ElectricBlue.alpha)
        runtimeShader.setFloatUniform("colorViolet", GlowColors.DeepPurple.red, GlowColors.DeepPurple.green, GlowColors.DeepPurple.blue, GlowColors.DeepPurple.alpha)
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
internal fun rememberVoiceGlowPhase(amplitude: Float, active: Boolean, reducedMotion: Boolean, label: String): Float {
    var phase by remember(label) { mutableFloatStateOf(0f) }
    val latestAmplitude by rememberUpdatedState(amplitude)
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
                val speed = 0.35f + 1.15f * latestAmplitude
                phase += elapsedSeconds * speed
                if (phase >= 1000f) phase -= 1000f
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
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)
    val shape = RoundedCornerShape(Sizes.CornerMassive)
    val targetAmplitude = if (active) voiceState.glowAmplitude() else 0f
    val intensity = rememberVoiceGlowIntensity(targetAmplitude, reducedMotion, "voiceGlowIntensity")
    val phase = rememberVoiceGlowPhase(intensity, active, reducedMotion, "voiceRecordingInterior")

    val activeAlpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "voiceLiquidSettle"
    )

    val fluidSim = remember { FluidSurfaceSimulation() }
    val liquidPath = remember { Path() }
    val crestPath = remember { Path() }

    // AGSL Shader instance cached for API 33+
    val shaderHolder = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LiquidShaderHolder()
        } else null
    }

    LaunchedEffect(phase, intensity, active) {
        if (active) {
            fluidSim.update(0.04f, intensity, phase)
        }
    }

    Box(modifier = modifier.clip(shape)) {
        Canvas(Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            if (width <= 0f || height <= 0f) return@Canvas

            val corner = CornerRadius(height / 2f)

            // Translucent glass base surface
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
                // Canvas Path Fallback (API 26..32) with dramatic 40% wave displacement
                val nodes = GlowDesign.FLUID_NODES
                val stepX = width / (nodes - 1)
                val baseWaveY = height * (0.65f - 0.20f * intensity)

                liquidPath.reset()
                crestPath.reset()

                val startY = (baseWaveY + fluidSim.heights[0] * height * 0.5f).coerceIn(0f, height)
                liquidPath.moveTo(0f, startY)
                crestPath.moveTo(0f, startY)

                for (i in 0 until nodes - 1) {
                    val x1 = i * stepX
                    val y1 = (baseWaveY + fluidSim.heights[i] * height * 0.5f).coerceIn(0f, height)
                    val x2 = (i + 1) * stepX
                    val y2 = (baseWaveY + fluidSim.heights[i + 1] * height * 0.5f).coerceIn(0f, height)

                    val cx = (x1 + x2) / 2f
                    val cy = (y1 + y2) / 2f

                    liquidPath.quadraticTo(x1, y1, cx, cy)
                    crestPath.quadraticTo(x1, y1, cx, cy)
                }

                val lastY = (baseWaveY + fluidSim.heights[nodes - 1] * height * 0.5f).coerceIn(0f, height)
                liquidPath.lineTo(width, lastY)
                crestPath.lineTo(width, lastY)

                // Close liquid path around bottom edge
                liquidPath.lineTo(width, height)
                liquidPath.lineTo(0f, height)
                liquidPath.close()

                // Draw liquid body fill
                drawPath(
                    path = liquidPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            GlowColors.Cyan.copy(alpha = 0.95f * activeAlpha),
                            GlowColors.ElectricBlue.copy(alpha = 0.90f * activeAlpha),
                            GlowColors.BrightViolet.copy(alpha = 0.85f * activeAlpha),
                            GlowColors.DeepPurple.copy(alpha = 0.95f * activeAlpha)
                        ),
                        startY = height * 0.35f,
                        endY = height
                    )
                )

                // Draw luminous wave crest highlight
                drawPath(
                    path = crestPath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            GlowColors.Cyan,
                            GlowColors.CrestHighlight,
                            GlowColors.ElectricBlue,
                            GlowColors.BrightViolet
                        )
                    ),
                    style = Stroke(width = 3.5.dp.toPx())
                )
            }

            // Glass Specular Rim Border & Refraction Highlight
            val borderAlpha = activeAlpha * (0.35f + intensity * 0.25f)
            val strokePx = 1.8.dp.toPx()
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GlowColors.SpecularRim.copy(alpha = borderAlpha * 1.5f),
                        GlowColors.Cyan.copy(alpha = borderAlpha),
                        GlowColors.DeepPurple.copy(alpha = borderAlpha * 0.9f)
                    )
                ),
                topLeft = Offset(strokePx / 2f, strokePx / 2f),
                size = Size((width - strokePx).coerceAtLeast(0f), (height - strokePx).coerceAtLeast(0f)),
                cornerRadius = CornerRadius(((height - strokePx) / 2f).coerceAtLeast(0f)),
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
    val phase = rememberVoiceGlowPhase(amplitude, true, reducedMotion, "voiceRecordingAmbient")
    val intensity = rememberVoiceGlowIntensity(amplitude, reducedMotion, "voiceAmbientIntensity")

    Canvas(modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val angle = phase * GlowDesign.TAU
        val radius = maxOf(width * 0.70f, height * 1.05f)
        val alpha = 0.15f + 0.25f * amplitude.coerceIn(0f, 1f)

        val leftCenter = Offset(width * (0.28f + 0.08f * sin(angle)), height * 0.82f)
        val rightCenter = Offset(width * (0.72f - 0.08f * cos(angle * 0.8f)), height * 0.82f)
        val center = Offset(width * 0.5f, height * 0.90f)

        drawCircle(
            brush = Brush.radialGradient(
                listOf(GlowColors.Cyan.copy(alpha = alpha), GlowColors.ElectricBlue.copy(alpha = alpha * 0.5f), Color.Transparent),
                center = leftCenter,
                radius = radius
            ),
            center = leftCenter,
            radius = radius
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(GlowColors.BrightViolet.copy(alpha = alpha * 0.85f), GlowColors.ElectricBlue.copy(alpha = alpha * 0.40f), Color.Transparent),
                center = rightCenter,
                radius = radius
            ),
            center = rightCenter,
            radius = radius
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(GlowColors.ElectricBlue.copy(alpha = alpha * 0.65f), Color.Transparent),
                center = center,
                radius = radius * 0.80f
            ),
            center = center,
            radius = radius * 0.80f
        )
    }
}
