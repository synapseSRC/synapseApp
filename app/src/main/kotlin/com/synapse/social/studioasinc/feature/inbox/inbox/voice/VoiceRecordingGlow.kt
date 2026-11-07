package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.LocalUiAtmosphere
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.UiAtmosphereColors
import com.synapse.social.studioasinc.feature.shared.theme.cardSurfaceColor
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.sin

private object GlowColors {
    val Cyan = Color(0xFF00F2FE)
    val ElectricBlue = Color(0xFF2979FF)
    val Magenta = Color(0xFFFF2A85)
    val SunsetGold = Color(0xFFFFB300)
    val Violet = Color(0xFF7C4DFF)
    val DeepIndigo = Color(0xFF3D5AFE)
}

private object GlowDesign {
    const val MIN_AMPLITUDE = 0.08f
    const val MAX_AMPLITUDE = 1.0f
    const val TAU = 6.2831855f
    const val FLUID_NODES = 28
}

private const val GLOW_TAU = GlowDesign.TAU

/** Intensity the surface settles on when the composer is in its ready state. */
private const val READY_INTENSITY = 0.14f

/** Intensity the surface jumps to on press, before the recorder reports amplitude. */
private const val PRESS_INTENSITY = 0.42f

/** Ready-state breathing envelope. */
private const val LIQUID_READY_BREATH_BASE = 0.10f
private const val LIQUID_READY_BREATH_DEPTH = 0.05f

/** How far the whole liquid column rises as the lock gesture progresses. */
private const val LIQUID_LIFT_FRACTION = 0.10f

/** Alpha of the error-toned wash layered over the liquid while a cancel drag is in progress. */
private const val LIQUID_CANCEL_FILL_ALPHA = 0.28f

/** Environment wash strength over the glass surface. */
private const val LIQUID_ENV_WASH_ALPHA = 0.45f

/** Light band near the top of the glass that reads as volumetric light entering the liquid. */
private const val LIQUID_MIST_TOP = 0.14f
private const val LIQUID_MIST_HEIGHT = 0.12f

private const val LIQUID_BODY_LAYER = 1
private const val LIQUID_MID_LAYER = 2
private const val LIQUID_CREST_LAYER = 3

/** Specular glints that ride the crest while it is loud enough to catch the key light. */
private const val GLINT_COUNT = 3
private const val GLINT_MIN_INTENSITY = 0.22f

/** Refracted light bands drawn below the crest. */
private const val CAUSTIC_BAND_COUNT = 2
private const val CAUSTIC_BAND_SPACING = 0.085f

private val LIQUID_EDGE_WIDTH = 1.4.dp
private val LIQUID_BLOOM_WIDTH = 4.dp
private val GLASS_RIM_STROKE = 1.8.dp
private val CAUSTIC_BAND_HEIGHT = 1.5.dp

/** Frame delta is clamped so a stalled frame can never destabilise the explicit spring integration. */
private const val MAX_SIMULATION_STEP_SECONDS = 0.04f

/**
 * The lighting the voice surface is rendered in.
 *
 * It is derived from the surrounding chat environment rather than hard-coded: the Material theme
 * roles supply the room tone and the key/fill/rim colours, and [LocalUiAtmosphere] (populated from
 * the conversation wallpaper and media colour extraction) overrides them whenever it carries a
 * colour. Warm environments light the liquid from the left, cool ones from the right.
 */
@Immutable
internal data class VoiceEnvironmentLight(
    val keyColor: Color,
    val fillColor: Color,
    val rimColor: Color,
    val roomColor: Color,
    val keyDirection: Offset,
    val intensity: Float
) {
    val keyFromLeft: Boolean get() = keyDirection.x < 0f
}

private fun createVoiceEnvironmentLight(
    scheme: ColorScheme,
    atmosphere: UiAtmosphereColors
): VoiceEnvironmentLight {
    val hasDominant = atmosphere.dominantColor.alpha > 0.01f
    val hasVibrant = atmosphere.vibrantColor.alpha > 0.01f
    val hasMuted = atmosphere.mutedColor.alpha > 0.01f

    val dominant = if (hasDominant) atmosphere.dominantColor else scheme.primary
    val vibrant = if (hasVibrant) atmosphere.vibrantColor else scheme.tertiary
    val muted = if (hasMuted) atmosphere.mutedColor else scheme.secondaryContainer

    val luminance = 0.2126f * dominant.red + 0.7152f * dominant.green + 0.0722f * dominant.blue
    // Warm environments (more red than blue) push the key light toward the left; cool ones flip it.
    val warmth = ((dominant.red - dominant.blue) + (vibrant.red - vibrant.blue)) * 0.5f
    val direction = Offset(-0.40f + warmth * 0.45f, 0.55f - warmth * 0.18f)

    return VoiceEnvironmentLight(
        keyColor = lerp(dominant, GlowColors.Cyan, 0.18f),
        fillColor = lerp(muted, GlowColors.Violet, 0.22f),
        rimColor = lerp(vibrant, Color.White, 0.35f),
        roomColor = scheme.surfaceContainerHigh,
        keyDirection = direction,
        intensity = (0.45f + 0.75f * luminance).coerceIn(0.35f, 1.15f)
    )
}

@Composable
internal fun rememberVoiceEnvironmentLight(): VoiceEnvironmentLight {
    val scheme = MaterialTheme.colorScheme
    val atmosphere = LocalUiAtmosphere.current.colors
    return remember(scheme, atmosphere) { createVoiceEnvironmentLight(scheme, atmosphere) }
}

/**
 * 1D spring-coupled fluid surface simulation with organic multi-center wave motion.
 *
 * The node heights are the actual rendered surface: they build the Canvas layer paths, feed the
 * curvature used for the specular glints, and are packed into the AGSL surface uniforms on
 * API 33+, so nothing computed here is discarded.
 *
 * @param springK restoring force pulling every node back to rest.
 * @param coupling neighbour coupling that makes the surface behave as one connected body of liquid.
 * @param damping per-step velocity loss.
 * @param impulseGain magnitude of the travelling impulse injected into the surface.
 */
internal class FluidSurfaceSimulation(
    private val nodeCount: Int = GlowDesign.FLUID_NODES,
    private val springK: Float = 7f,
    private val coupling: Float = 16f,
    private val damping: Float = 0.93f,
    private val impulseGain: Float = 4.5f
) {
    val heights = FloatArray(nodeCount) { 0f }
    private val velocities = FloatArray(nodeCount) { 0f }

    fun update(dt: Float, audioEnergy: Float, phase: Float) {
        val energy = audioEnergy.coerceIn(0f, 1f)

        // A low ambient swell keeps the liquid alive in silence; audio energy amplifies it and
        // moves the impulse centre so consecutive hits land on different parts of the surface.
        val impulse = impulseGain * (0.10f + 0.90f * energy)
        val primaryNode = ((nodeCount - 1) * (0.5f + 0.38f * sin(phase * 4.2f))).toInt().coerceIn(2, nodeCount - 3)
        velocities[primaryNode] += impulse
        velocities[primaryNode - 1] += impulse * 0.75f
        velocities[primaryNode + 1] += impulse * 0.75f
        velocities[primaryNode - 2] += impulse * 0.40f
        velocities[primaryNode + 2] += impulse * 0.40f

        // Connected fluid boundary physics step
        for (index in 0 until nodeCount) {
            val left = if (index > 0) heights[index - 1] else heights[index]
            val right = if (index < nodeCount - 1) heights[index + 1] else heights[index]

            val springForce = -springK * heights[index]
            val couplingForce = coupling * (left + right - 2f * heights[index])
            val acceleration = springForce + couplingForce

            velocities[index] = (velocities[index] + acceleration * dt) * damping
            heights[index] = (heights[index] + velocities[index] * dt).coerceIn(-0.65f, 0.65f)
        }
    }

    /** Interpolated node height at a normalised x position; feeds the AGSL surface uniforms. */
    fun sampleAt(fraction: Float): Float {
        if (nodeCount <= 1) return heights[0]
        val position = fraction.coerceIn(0f, 1f) * (nodeCount - 1)
        val lowIndex = floor(position).toInt().coerceIn(0, nodeCount - 1)
        val highIndex = (lowIndex + 1).coerceAtMost(nodeCount - 1)
        val blend = position - lowIndex
        return heights[lowIndex] + (heights[highIndex] - heights[lowIndex]) * blend
    }

    /** Centred difference slope at a node, used to light the surface edge. */
    fun slopeAt(index: Int): Float {
        val left = heights[(index - 1).coerceAtLeast(0)]
        val right = heights[(index + 1).coerceAtMost(nodeCount - 1)]
        return (right - left) * 0.5f
    }

    /**
     * Fills the surface with a fixed organic profile for static rendering (reduced motion or a
     * paused recording). The profile is amplitude independent so the surface never pops while the
     * values behind it are re-applied.
     */
    fun seedStatic(seedPhase: Float, scale: Float) {
        for (index in 0 until nodeCount) {
            val x = index.toFloat() / (nodeCount - 1)
            heights[index] = scale * (
                0.55f * sin(x * 3.4f + seedPhase) +
                    0.30f * sin(x * 7.1f - seedPhase * 1.3f) +
                    0.15f * cos(x * 11.6f + seedPhase * 0.7f)
                )
            velocities[index] = 0f
        }
    }
}

/** One translucent liquid surface: its simulation, its geometry cache and its gradients. */
private class LiquidLayer(
    val simulation: FluidSurfaceSimulation,
    val baseYFraction: Float,
    val amplitudeScale: Float,
    val fillAlpha: Float,
    val edgeAlpha: Float,
    val edgeWidthPx: Float,
    val timeScale: Float,
    val phaseScale: Float,
    val phaseOffset: Float,
    val staticScale: Float,
    val amplitudeGain: Float,
    val fillBrush: Brush,
    val edgeBrush: Brush
) {
    val wavePath = Path()
    val fillPath = Path()
}

/**
 * Creates the overlapping liquid stack, tinted by the environment light. Four surfaces, from a slow
 * heavy body to a lively crest, share the recorded amplitudes but respond at different rates, which
 * is what produces the layered aurora depth instead of one flat moving edge.
 */
private fun createLiquidLayers(edgeWidthPx: Float, light: VoiceEnvironmentLight): List<LiquidLayer> = listOf(
    LiquidLayer(
        simulation = FluidSurfaceSimulation(springK = 5.5f, coupling = 12f, damping = 0.96f, impulseGain = 2.4f),
        baseYFraction = 0.97f,
        amplitudeScale = 0.09f,
        fillAlpha = 0.74f,
        edgeAlpha = 0.16f,
        edgeWidthPx = edgeWidthPx,
        timeScale = 0.50f,
        phaseScale = 0.60f,
        phaseOffset = 0.4f,
        staticScale = 0.42f,
        amplitudeGain = 0.75f,
        fillBrush = Brush.verticalGradient(
            lerp(GlowColors.DeepIndigo, light.roomColor, 0.30f).copy(alpha = 0.80f),
            lerp(GlowColors.Violet, light.fillColor, 0.24f).copy(alpha = 0.58f),
            lerp(GlowColors.ElectricBlue, light.keyColor, 0.20f).copy(alpha = 0.26f)
        ),
        edgeBrush = Brush.horizontalGradient(
            lerp(GlowColors.ElectricBlue, light.keyColor, 0.30f),
            lerp(GlowColors.Violet, light.rimColor, 0.20f)
        )
    ),
    LiquidLayer(
        simulation = FluidSurfaceSimulation(springK = 7.5f, coupling = 14f, damping = 0.95f, impulseGain = 3.2f),
        baseYFraction = 0.89f,
        amplitudeScale = 0.14f,
        fillAlpha = 0.60f,
        edgeAlpha = 0.24f,
        edgeWidthPx = edgeWidthPx,
        timeScale = 0.72f,
        phaseScale = 0.80f,
        phaseOffset = 1.1f,
        staticScale = 0.52f,
        amplitudeGain = 0.90f,
        fillBrush = Brush.verticalGradient(
            lerp(GlowColors.Cyan, light.keyColor, 0.26f).copy(alpha = 0.62f),
            lerp(GlowColors.ElectricBlue, light.fillColor, 0.20f).copy(alpha = 0.46f),
            lerp(GlowColors.Magenta, light.fillColor, 0.18f).copy(alpha = 0.28f)
        ),
        edgeBrush = Brush.horizontalGradient(
            lerp(GlowColors.Cyan, light.keyColor, 0.35f),
            lerp(GlowColors.Magenta, light.rimColor, 0.20f),
            lerp(GlowColors.ElectricBlue, light.fillColor, 0.20f)
        )
    ),
    LiquidLayer(
        simulation = FluidSurfaceSimulation(springK = 9f, coupling = 16f, damping = 0.94f, impulseGain = 4.0f),
        baseYFraction = 0.80f,
        amplitudeScale = 0.19f,
        fillAlpha = 0.46f,
        edgeAlpha = 0.30f,
        edgeWidthPx = edgeWidthPx,
        timeScale = 0.88f,
        phaseScale = 1.0f,
        phaseOffset = 2.2f,
        staticScale = 0.64f,
        amplitudeGain = 1.10f,
        fillBrush = Brush.verticalGradient(
            lerp(GlowColors.Magenta, light.rimColor, 0.24f).copy(alpha = 0.48f),
            lerp(GlowColors.SunsetGold, light.keyColor, 0.22f).copy(alpha = 0.34f),
            lerp(GlowColors.Violet, light.fillColor, 0.22f).copy(alpha = 0.24f)
        ),
        edgeBrush = Brush.horizontalGradient(
            lerp(GlowColors.SunsetGold, light.keyColor, 0.30f),
            lerp(GlowColors.Magenta, light.rimColor, 0.30f),
            lerp(GlowColors.Violet, light.fillColor, 0.20f)
        )
    ),
    LiquidLayer(
        simulation = FluidSurfaceSimulation(springK = 10.5f, coupling = 18f, damping = 0.93f, impulseGain = 4.6f),
        baseYFraction = 0.71f,
        amplitudeScale = 0.24f,
        fillAlpha = 0.34f,
        edgeAlpha = 0.36f,
        edgeWidthPx = edgeWidthPx,
        timeScale = 1.0f,
        phaseScale = 1.15f,
        phaseOffset = 3.1f,
        staticScale = 0.78f,
        amplitudeGain = 1.30f,
        fillBrush = Brush.verticalGradient(
            lerp(GlowColors.Cyan, light.rimColor, 0.30f).copy(alpha = 0.40f),
            lerp(GlowColors.SunsetGold, light.keyColor, 0.28f).copy(alpha = 0.28f),
            lerp(GlowColors.Cyan, light.fillColor, 0.20f).copy(alpha = 0.18f)
        ),
        edgeBrush = Brush.horizontalGradient(
            lerp(GlowColors.SunsetGold, light.keyColor, 0.40f),
            lerp(GlowColors.Magenta, light.rimColor, 0.35f),
            lerp(GlowColors.Violet, light.fillColor, 0.20f)
        )
    )
)

/**
 * Frame-driven state shared by the liquid renderer.
 *
 * All animated values are snapshot states that the canvas reads from the draw phase, so the liquid
 * advances at frame rate while composition only re-runs when the voice state itself changes.
 * Exponential easing replaces springs for tint and lift because the surface already carries the
 * physical motion and the gesture response has to stay finger-following.
 */
private class AuroraLiquidState(private val layers: List<LiquidLayer>) {
    val phase = mutableFloatStateOf(0f)
    val tick = mutableFloatStateOf(0f)
    val intensity = mutableFloatStateOf(0f)
    val cancel = mutableFloatStateOf(0f)
    val lift = mutableFloatStateOf(0f)

    /** Aligns the surface with its targets and settles a static, still-polished profile. */
    fun settle(targetIntensity: Float, targetCancel: Float, targetLift: Float) {
        intensity.floatValue = targetIntensity.coerceIn(0f, 1f)
        cancel.floatValue = targetCancel.coerceIn(0f, 1f)
        lift.floatValue = targetLift.coerceIn(0f, 1f)
        for (layer in layers) {
            layer.simulation.seedStatic(layer.phaseOffset, layer.staticScale)
        }
        tick.floatValue += 0.5f
    }

    fun advance(
        dtSeconds: Float,
        targetIntensity: Float,
        targetCancel: Float,
        targetLift: Float,
        breathing: Boolean,
        timeScale: Float
    ) {
        val step = dtSeconds.coerceIn(0.001f, MAX_SIMULATION_STEP_SECONDS)
        val easedTarget = if (breathing) {
            LIQUID_READY_BREATH_BASE + LIQUID_READY_BREATH_DEPTH * sin(phase.floatValue * 1.35f)
        } else {
            targetIntensity
        }

        intensity.floatValue += (easedTarget.coerceIn(0f, 1f) - intensity.floatValue) * (1f - exp(-7f * step))
        cancel.floatValue += (targetCancel.coerceIn(0f, 1f) - cancel.floatValue) * (1f - exp(-11f * step))
        lift.floatValue += (targetLift.coerceIn(0f, 1f) - lift.floatValue) * (1f - exp(-9f * step))

        val nextPhase = (phase.floatValue + step * timeScale * (0.35f + 1.35f * intensity.floatValue)) % 1000f
        phase.floatValue = nextPhase

        for (layer in layers) {
            layer.simulation.update(
                step * layer.timeScale,
                intensity.floatValue * layer.amplitudeGain,
                nextPhase * layer.phaseScale + layer.phaseOffset
            )
        }
        tick.floatValue += 1f
    }
}

private const val VOLUMETRIC_AGSL_SHADER = """
uniform float2 iResolution;
uniform float2 iLightDir;
uniform float iTime;
uniform float iAmplitude;
uniform float iCancel;
uniform float iLift;
uniform float iEnvIntensity;
uniform float4 lightKey;
uniform float4 lightFill;
uniform float4 lightRim;
uniform float4 lightRoom;
uniform float4 uCrest;
uniform float4 uMid;
uniform float4 uBody;
uniform float4 colorCyan;
uniform float4 colorBlue;
uniform float4 colorMagenta;
uniform float4 colorGold;
uniform float4 colorViolet;
uniform float4 colorSurface;
uniform float4 colorError;

float waveSample(float4 samples, float x) {
    float position = clamp(x, 0.0, 1.0) * 3.0;
    float fraction = fract(position);
    float eased = smoothstep(0.0, 1.0, fraction);
    float a01 = mix(samples.x, samples.y, eased);
    float a12 = mix(samples.y, samples.z, eased);
    float a23 = mix(samples.z, samples.w, eased);
    float low = mix(a01, a12, step(1.0, position));
    float high = mix(a12, a23, step(2.0, position));
    return mix(low, high, step(1.0, position));
}

half4 main(in float2 fragCoord) {
    float2 uv = fragCoord / iResolution;

    float micro = sin(uv.x * 7.0 - iTime * 1.7) * 0.030 * iAmplitude
                + cos(uv.x * 12.5 + iTime * 1.15) * 0.018 * iAmplitude
                + sin(uv.x * 3.1 + iTime * 0.65) * 0.016;

    float energy = 0.55 + 0.85 * iAmplitude;

    float crestY = 0.60 - waveSample(uCrest, uv.x) * 0.34 * energy - micro * 0.55 - iLift * 0.16;
    float midY = 0.70 - waveSample(uMid, uv.x) * 0.30 * energy - micro * 0.78;
    float bodyY = 0.82 - waveSample(uBody, uv.x) * 0.24 * energy - micro;
    float deepY = 0.96 - waveSample(uBody, uv.x) * 0.12 * energy;

    float crestMask = smoothstep(crestY - 0.26, crestY + 0.08, uv.y);
    float midMask = smoothstep(midY - 0.30, midY + 0.10, uv.y);
    float bodyMask = smoothstep(bodyY - 0.34, bodyY + 0.12, uv.y);
    float deepMask = smoothstep(deepY - 0.30, deepY + 0.14, uv.y);

    if (deepMask <= 0.002) {
        return half4(0.0, 0.0, 0.0, 0.0);
    }

    // Surface normal from the simulated crest slope, lit by the environment key light.
    float slope = (waveSample(uCrest, uv.x + 0.02) - waveSample(uCrest, uv.x - 0.02)) * 7.0;
    float3 normal = normalize(float3(-slope * energy, 1.0, 0.35 + max(slope, 0.0) * 0.5));
    float3 lightVector = normalize(float3(iLightDir.x, iLightDir.y, 0.75));
    float diffuse = clamp(dot(normal, lightVector), 0.0, 1.0);
    float3 halfVector = normalize(lightVector + float3(0.0, 0.0, 1.0));
    float specular = pow(clamp(dot(normal, halfVector), 0.0, 1.0), 26.0);
    float rimLight = pow(clamp(1.0 - normal.z, 0.0, 1.0), 1.5);

    float c1X = 0.20 + 0.18 * sin(iTime * 0.9 + uv.y * 1.8);
    float c2X = 0.50 + 0.20 * cos(iTime * 0.75 + uv.y * 1.4);
    float c3X = 0.80 + 0.18 * sin(iTime * 1.05 - uv.y * 2.1);
    float c4Y = 0.42 + 0.12 * sin(iTime * 0.6 + uv.x * 2.4);

    float d1 = length(float2(uv.x - c1X, uv.y - 0.95));
    float d2 = length(float2(uv.x - c2X, uv.y - 0.88));
    float d3 = length(float2(uv.x - c3X, uv.y - 0.95));
    float d4 = length(float2(uv.x - 0.5, uv.y - c4Y));

    float3 deepColor = colorBlue.rgb * 0.75 + colorViolet.rgb * 0.35;
    float3 bodyColor = colorCyan.rgb * smoothstep(0.78, 0.0, d1)
                     + colorMagenta.rgb * smoothstep(0.70, 0.0, d2)
                     + colorGold.rgb * smoothstep(0.60, 0.0, d2) * 0.55
                     + colorViolet.rgb * smoothstep(0.78, 0.0, d3);
    float3 midColor = colorCyan.rgb * smoothstep(0.60, 0.0, d1)
                    + colorViolet.rgb * smoothstep(0.55, 0.0, d3)
                    + colorGold.rgb * smoothstep(0.45, 0.0, d2) * 0.45;
    float3 crestColor = colorCyan.rgb * smoothstep(0.55, 0.0, d4) * 0.55
                      + colorGold.rgb * smoothstep(0.45, 0.0, d4) * 0.35;

    float3 ambient = lightRoom.rgb * 0.45 + lightFill.rgb * (0.35 + 0.35 * iEnvIntensity);
    float3 keyLight = lightKey.rgb * iEnvIntensity * (0.45 + 0.85 * diffuse);
    float3 rim = lightRim.rgb * (specular * 1.7 + rimLight * 0.35 * (0.4 + 0.6 * iAmplitude));

    float3 color = mix(colorSurface.rgb, deepColor, deepMask * 0.55);
    color = mix(color, bodyColor * (ambient + keyLight * 0.50), bodyMask * (0.55 + 0.35 * iAmplitude));
    color = mix(color, midColor * (ambient + keyLight * 0.65), midMask * 0.80);
    color += crestColor * (0.35 + 0.45 * iAmplitude) * crestMask;
    color += rim;
    color = mix(color, colorError.rgb, iCancel * 0.55);

    // Depth fog: thicker liquid absorbs more of the room light toward the bottom of the glass.
    color = mix(color, lightRoom.rgb * 0.55, smoothstep(0.55, 1.05, uv.y) * 0.50);

    // Crest specular line and the caustics refracted just below it.
    float crestLine = smoothstep(0.020, 0.0, abs(uv.y - crestY)) * (0.35 + 0.45 * iAmplitude);
    color += crestLine * (lightRim.rgb * 0.75 + lightKey.rgb * 0.35);

    float caustic = sin(uv.y * 46.0 + iTime * 1.6 + waveSample(uCrest, uv.x) * 6.0);
    float causticBand = smoothstep(crestY, crestY + 0.34, uv.y) * (1.0 - smoothstep(crestY + 0.34, crestY + 0.62, uv.y));
    color += smoothstep(0.0, 1.0, caustic) * causticBand * lightRim.rgb * 0.10 * (0.5 + 0.5 * iAmplitude);

    float alpha = deepMask * (0.74 + 0.22 * iAmplitude);
    alpha = max(alpha, crestMask * 0.44);
    return half4(color, alpha);
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class LiquidShaderHolder {
    val runtimeShader = RuntimeShader(VOLUMETRIC_AGSL_SHADER)
    val shaderBrush = ShaderBrush(runtimeShader)

    init {
        runtimeShader.setFloatUniform("colorCyan", GlowColors.Cyan.red, GlowColors.Cyan.green, GlowColors.Cyan.blue, 1f)
        runtimeShader.setFloatUniform("colorBlue", GlowColors.ElectricBlue.red, GlowColors.ElectricBlue.green, GlowColors.ElectricBlue.blue, 1f)
        runtimeShader.setFloatUniform("colorMagenta", GlowColors.Magenta.red, GlowColors.Magenta.green, GlowColors.Magenta.blue, 1f)
        runtimeShader.setFloatUniform("colorGold", GlowColors.SunsetGold.red, GlowColors.SunsetGold.green, GlowColors.SunsetGold.blue, 1f)
        runtimeShader.setFloatUniform("colorViolet", GlowColors.Violet.red, GlowColors.Violet.green, GlowColors.Violet.blue, 1f)
    }

    /**
     * Pushes the simulated surface and the environment lighting into the shader. The crest, mid and
     * body layers are sampled at four fixed positions and smoothly reconstructed in the shader, so
     * the shader's liquid edge is the same surface the Canvas fallback draws.
     */
    fun updateUniforms(
        width: Float,
        height: Float,
        timeSeconds: Float,
        amplitude: Float,
        cancel: Float,
        lift: Float,
        light: VoiceEnvironmentLight,
        surfaceColor: Color,
        errorColor: Color,
        crest: FluidSurfaceSimulation,
        mid: FluidSurfaceSimulation,
        body: FluidSurfaceSimulation
    ) {
        runtimeShader.setFloatUniform("iResolution", width, height)
        runtimeShader.setFloatUniform("iTime", timeSeconds)
        runtimeShader.setFloatUniform("iAmplitude", amplitude)
        runtimeShader.setFloatUniform("iCancel", cancel)
        runtimeShader.setFloatUniform("iLift", lift)
        runtimeShader.setFloatUniform("iEnvIntensity", light.intensity)
        runtimeShader.setFloatUniform("iLightDir", light.keyDirection.x, light.keyDirection.y)
        runtimeShader.setFloatUniform("lightKey", light.keyColor.red, light.keyColor.green, light.keyColor.blue, 1f)
        runtimeShader.setFloatUniform("lightFill", light.fillColor.red, light.fillColor.green, light.fillColor.blue, 1f)
        runtimeShader.setFloatUniform("lightRim", light.rimColor.red, light.rimColor.green, light.rimColor.blue, 1f)
        runtimeShader.setFloatUniform("lightRoom", light.roomColor.red, light.roomColor.green, light.roomColor.blue, 1f)
        runtimeShader.setFloatUniform("uCrest", crest.sampleAt(0.125f), crest.sampleAt(0.375f), crest.sampleAt(0.625f), crest.sampleAt(0.875f))
        runtimeShader.setFloatUniform("uMid", mid.sampleAt(0.125f), mid.sampleAt(0.375f), mid.sampleAt(0.625f), mid.sampleAt(0.875f))
        runtimeShader.setFloatUniform("uBody", body.sampleAt(0.125f), body.sampleAt(0.375f), body.sampleAt(0.625f), body.sampleAt(0.875f))
        runtimeShader.setFloatUniform("colorSurface", surfaceColor.red, surfaceColor.green, surfaceColor.blue, 1f)
        runtimeShader.setFloatUniform("colorError", errorColor.red, errorColor.green, errorColor.blue, 1f)
    }
}

/** Directional gradients applied to the glass so the environment light is visible on the surface. */
private class LiquidLightingBrushes(private val light: VoiceEnvironmentLight) {
    private val forward = light.keyFromLeft

    val environmentWash: Brush = Brush.linearGradient(
        colors = listOf(
            light.keyColor.copy(alpha = 0.30f),
            Color.Transparent,
            light.roomColor.copy(alpha = 0.55f)
        ),
        start = if (forward) Offset.Zero else Offset.Infinite,
        end = if (forward) Offset.Infinite else Offset.Zero
    )

    val bloom: Brush = Brush.horizontalGradient(
        listOf(
            light.rimColor.copy(alpha = 0.55f),
            light.keyColor.copy(alpha = 0.35f),
            light.rimColor.copy(alpha = 0.55f)
        )
    )

    val mist: Brush = Brush.horizontalGradient(
        listOf(
            light.keyColor.copy(alpha = 0.32f),
            light.rimColor.copy(alpha = 0.26f),
            light.fillColor.copy(alpha = 0.24f),
            light.keyColor.copy(alpha = 0.30f)
        )
    )

    val caustic: Brush = Brush.horizontalGradient(
        listOf(
            light.rimColor.copy(alpha = 0.22f),
            light.keyColor.copy(alpha = 0.14f),
            light.rimColor.copy(alpha = 0.22f)
        )
    )

    val glint: Brush = Brush.radialGradient(
        listOf(light.rimColor.copy(alpha = 0.85f), light.keyColor.copy(alpha = 0.25f), Color.Transparent)
    )
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

/**
 * Shared frame clock for voice surfaces. Retained unchanged for surfaces that animate outside the
 * liquid renderer; the aurora surface drives its own clock so composition stays off the animation
 * path.
 */
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

/** Spring-smoothed intensity helper, retained for voice surfaces that animate outside the canvas. */
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

/**
 * Builds the open wave path and the closed fill path for one liquid layer from its simulation
 * heights. Both paths are reused across frames; no allocation happens during animation.
 */
private fun buildLiquidPaths(
    wavePath: Path,
    fillPath: Path,
    heights: FloatArray,
    width: Float,
    height: Float,
    baseYFraction: Float,
    amplitudeScale: Float,
    liftFraction: Float,
    energy: Float
) {
    val nodeCount = heights.size
    if (nodeCount < 2 || width <= 0f || height <= 0f) return

    val baseY = height * (baseYFraction - liftFraction)
    val scale = height * amplitudeScale * (0.5f + 0.9f * energy)

    wavePath.reset()
    fillPath.reset()

    var previousX = 0f
    var previousY = baseY - heights[0] * scale
    wavePath.moveTo(previousX, previousY)
    fillPath.moveTo(previousX, previousY)

    for (index in 1 until nodeCount) {
        val nextX = width * index / (nodeCount - 1)
        val nextY = baseY - heights[index] * scale
        val controlX = (previousX + nextX) * 0.5f
        wavePath.cubicTo(controlX, previousY, controlX, nextY, nextX, nextY)
        fillPath.cubicTo(controlX, previousY, controlX, nextY, nextX, nextY)
        previousX = nextX
        previousY = nextY
    }

    fillPath.lineTo(width, height)
    fillPath.lineTo(0f, height)
    fillPath.close()
}

/**
 * Liquid Aurora surface behind the voice composer.
 *
 * Each layer is simulated with [FluidSurfaceSimulation] and lit by the environment light taken from
 * the theme and the chat atmosphere. Rendering either goes through the AGSL volumetric shader
 * (API 33+, receiving the simulated surface and the light as uniforms) or through an
 * allocation-free Canvas fallback that draws the same paths with the same lighting model.
 */
@Composable
fun VoiceRecordingGlowSurface(
    voiceState: VoiceState,
    reducedMotionOverride: Boolean,
    modifier: Modifier = Modifier
) {
    val reducedMotion = reducedMotionOverride
    val surfaceColor = cardSurfaceColor()
    val errorColor = MaterialTheme.colorScheme.error
    val light = rememberVoiceEnvironmentLight()
    val shape = RoundedCornerShape(Sizes.CornerMassive)
    val density = LocalDensity.current

    val active = voiceState.isGlowActive()
    val isReady = voiceState is VoiceState.VoiceReady
    val isPressing = voiceState is VoiceState.Pressing
    val visible = active || isReady || isPressing
    val isPaused = (voiceState as? VoiceState.Locked)?.isPaused == true
    val amplitude = if (active) voiceState.glowAmplitude() else 0f
    val cancelTarget = (voiceState as? VoiceState.Recording)?.cancelProgress?.coerceIn(0f, 1f) ?: 0f
    val liftTarget = when (voiceState) {
        is VoiceState.Recording -> voiceState.lockProgress.coerceIn(0f, 1f)
        is VoiceState.Locked -> 1f
        else -> 0f
    }
    val intensityTarget = when {
        isPressing -> PRESS_INTENSITY
        active -> if (isPaused) amplitude * 0.45f else amplitude
        isReady -> READY_INTENSITY
        else -> 0f
    }
    // Pressing compresses the liquid like a struck membrane; recording renders it at full depth.
    val compression = if (isPressing) 0.72f else 1f
    // Locked recordings drift deliberately slower so the composition reads as stable.
    val timeScale = when {
        isPressing -> 1.25f
        voiceState is VoiceState.Locked -> 0.6f
        else -> 1f
    }

    val edgeWidthPx = with(density) { LIQUID_EDGE_WIDTH.toPx() }
    val bloomWidthPx = with(density) { LIQUID_BLOOM_WIDTH.toPx() }
    val rimStrokePx = with(density) { GLASS_RIM_STROKE.toPx() }
    val causticHeightPx = with(density) { CAUSTIC_BAND_HEIGHT.toPx() }
    val layers = remember(edgeWidthPx, light) { createLiquidLayers(edgeWidthPx, light) }
    val liquid = remember(layers) { AuroraLiquidState(layers) }
    val lighting = remember(light) { LiquidLightingBrushes(light) }

    val rimBrush = remember(light) {
        Brush.verticalGradient(
            colors = listOf(
                lerp(light.rimColor, Color.White, 0.45f),
                light.keyColor,
                light.fillColor
            )
        )
    }
    val errorBrush = remember(errorColor) {
        Brush.horizontalGradient(errorColor, errorColor.copy(alpha = 0.55f))
    }

    val surfaceAlpha = animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "voiceLiquidSettle"
    )

    // Shader construction is guarded: a device that rejects the AGSL program falls back to Canvas.
    val shaderHolder = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { LiquidShaderHolder() }.getOrNull()
        } else {
            null
        }
    }

    val latestIntensity = rememberUpdatedState(intensityTarget)
    val latestCancel = rememberUpdatedState(cancelTarget)
    val latestLift = rememberUpdatedState(liftTarget)
    val latestBreathing = rememberUpdatedState(isReady)
    val latestTimeScale = rememberUpdatedState(timeScale)

    // Paused and reduced-motion states stop the clock entirely and render a settled surface.
    val animate = visible && !reducedMotion && !isPaused

    SideEffect {
        if (!animate) {
            liquid.settle(latestIntensity.value, latestCancel.value, latestLift.value)
        }
    }

    LaunchedEffect(layers, animate) {
        if (!animate) return@LaunchedEffect
        var previousFrameNanos = 0L
        while (true) {
            val frameNanos = withFrameNanos { it }
            if (previousFrameNanos != 0L) {
                val dt = ((frameNanos - previousFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                liquid.advance(
                    dtSeconds = dt,
                    targetIntensity = latestIntensity.value,
                    targetCancel = latestCancel.value,
                    targetLift = latestLift.value,
                    breathing = latestBreathing.value,
                    timeScale = latestTimeScale.value
                )
            }
            previousFrameNanos = frameNanos
        }
    }

    Box(modifier = modifier.clip(shape)) {
        Canvas(Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            if (width <= 0f || height <= 0f) return@Canvas

            // Draw-phase reads only: the canvas invalidates itself as the liquid advances.
            val surfaceAlphaValue = surfaceAlpha.value
            val tick = liquid.tick.floatValue
            val phase = liquid.phase.floatValue
            val intensity = liquid.intensity.floatValue
            val cancel = liquid.cancel.floatValue
            val lift = liquid.lift.floatValue

            val maxCornerPx = with(density) { Sizes.CornerMassive.toPx() }
            val corner = CornerRadius(minOf(maxCornerPx, height / 2f))

            drawRoundRect(color = surfaceColor, cornerRadius = corner)

            if (surfaceAlphaValue <= 0.001f) return@Canvas

            val supportsAgsl = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shaderHolder != null

            if (supportsAgsl && shaderHolder != null) {
                shaderHolder.updateUniforms(
                    width = width,
                    height = height,
                    timeSeconds = if (reducedMotion) 0f else phase,
                    amplitude = intensity,
                    cancel = cancel,
                    lift = lift,
                    light = light,
                    surfaceColor = surfaceColor,
                    errorColor = errorColor,
                    crest = layers[LIQUID_CREST_LAYER].simulation,
                    mid = layers[LIQUID_MID_LAYER].simulation,
                    body = layers[LIQUID_BODY_LAYER].simulation
                )
                drawRoundRect(
                    brush = shaderHolder.shaderBrush,
                    cornerRadius = corner,
                    alpha = surfaceAlphaValue
                )
            } else {
                val energy = intensity * compression
                for (layer in layers) {
                    buildLiquidPaths(
                        wavePath = layer.wavePath,
                        fillPath = layer.fillPath,
                        heights = layer.simulation.heights,
                        width = width,
                        height = height,
                        baseYFraction = layer.baseYFraction,
                        amplitudeScale = layer.amplitudeScale,
                        liftFraction = lift * LIQUID_LIFT_FRACTION,
                        energy = energy
                    )

                    drawPath(
                        path = layer.fillPath,
                        brush = layer.fillBrush,
                        alpha = (layer.fillAlpha * surfaceAlphaValue * (0.55f + 0.70f * intensity)).coerceIn(0f, 1f)
                    )

                    if (cancel > 0.01f) {
                        drawPath(
                            path = layer.fillPath,
                            color = errorColor,
                            alpha = (cancel * LIQUID_CANCEL_FILL_ALPHA * surfaceAlphaValue).coerceIn(0f, 1f)
                        )
                    }

                    // Light catches the leading edge of every surface; the bloom pass widens it.
                    drawPath(
                        path = layer.wavePath,
                        brush = lighting.bloom,
                        alpha = (0.10f * surfaceAlphaValue * (0.4f + 0.8f * intensity)).coerceIn(0f, 1f),
                        style = Stroke(width = bloomWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = layer.wavePath,
                        brush = layer.edgeBrush,
                        alpha = (layer.edgeAlpha * surfaceAlphaValue * (0.45f + 0.85f * intensity)).coerceIn(0f, 1f),
                        style = Stroke(width = layer.edgeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                val crestLayer = layers[LIQUID_CREST_LAYER]
                val crestBaseY = height * (crestLayer.baseYFraction - lift * LIQUID_LIFT_FRACTION)
                val crestScale = height * crestLayer.amplitudeScale * (0.5f + 0.9f * energy)
                val crestHeights = crestLayer.simulation.heights
                val nodeCount = crestHeights.size

                // Caustics: refracted light bands drifting below the crest.
                for (band in 0 until CAUSTIC_BAND_COUNT) {
                    val bandOffset = CAUSTIC_BAND_SPACING * (band + 1)
                    val bandY = crestBaseY + height * bandOffset * (1f + 0.05f * sin(phase * 1.1f + band))
                    if (bandY >= height) continue
                    drawRoundRect(
                        brush = lighting.caustic,
                        topLeft = Offset(width * 0.06f, bandY),
                        size = Size(width * 0.88f, causticHeightPx),
                        cornerRadius = CornerRadius(causticHeightPx / 2f, causticHeightPx / 2f),
                        alpha = (surfaceAlphaValue * (0.55f - 0.18f * band) * (0.35f + 0.65f * intensity)).coerceIn(0f, 1f)
                    )
                }

                // Specular glints ride the crest where its own curvature faces the key light.
                if (intensity > GLINT_MIN_INTENSITY) {
                    val directionSign = if (light.keyFromLeft) 1f else -1f
                    val glintRadius = (height * 0.10f).coerceIn(2f, 14f)
                    for (index in 1 until nodeCount - 1) {
                        val slope = crestLayer.simulation.slopeAt(index)
                        val facing = slope * directionSign
                        if (facing <= 0.02f) continue
                        val travel = 0.5f + 0.5f * sin(phase * 1.6f + index * 0.7f)
                        val glintAlpha = (facing * 2.4f * travel * (intensity - GLINT_MIN_INTENSITY) * 3.2f).coerceIn(0f, 0.5f)
                        if (glintAlpha <= 0.01f) continue
                        val glintX = width * index / (nodeCount - 1)
                        val glintY = crestBaseY - crestHeights[index] * crestScale
                        drawCircle(
                            brush = lighting.glint,
                            radius = glintRadius * (0.6f + 0.6f * travel),
                            center = Offset(glintX, glintY),
                            alpha = glintAlpha * surfaceAlphaValue
                        )
                    }
                }

                // Environment wash: the room light falling across the glass, plus depth.
                drawRoundRect(
                    brush = lighting.environmentWash,
                    cornerRadius = corner,
                    alpha = (LIQUID_ENV_WASH_ALPHA * surfaceAlphaValue).coerceIn(0f, 1f)
                )

                // Volumetric light entering the top of the liquid.
                val mistHeight = (height * LIQUID_MIST_HEIGHT).coerceAtLeast(1f)
                val mistTop = height * (LIQUID_MIST_TOP - 0.02f * sin(tick * 0.02f))
                drawRoundRect(
                    brush = lighting.mist,
                    topLeft = Offset(0f, mistTop),
                    size = Size(width, mistHeight),
                    cornerRadius = CornerRadius(mistHeight / 2f, mistHeight / 2f),
                    alpha = (surfaceAlphaValue * (0.12f + 0.22f * intensity)).coerceIn(0f, 1f)
                )
            }

            // Glass specular rim and refraction highlight (drawn for both render paths).
            val strokeInsetPx = rimStrokePx / 2f
            val rimCornerRadiusPx = with(density) { (Sizes.CornerMassive - strokeInsetPx.toDp()).toPx() }
            val rimCorner = CornerRadius(
                minOf(rimCornerRadiusPx, (height - rimStrokePx) / 2f).coerceAtLeast(0f)
            )
            val rimSize = Size((width - rimStrokePx).coerceAtLeast(0f), (height - rimStrokePx).coerceAtLeast(0f))

            drawRoundRect(
                brush = rimBrush,
                topLeft = Offset(strokeInsetPx, strokeInsetPx),
                size = rimSize,
                cornerRadius = rimCorner,
                style = Stroke(width = rimStrokePx),
                alpha = (surfaceAlphaValue * (0.28f + 0.30f * intensity + 0.22f * lift) * light.intensity).coerceIn(0f, 1f)
            )

            if (cancel > 0.01f) {
                drawRoundRect(
                    brush = errorBrush,
                    topLeft = Offset(strokeInsetPx, strokeInsetPx),
                    size = rimSize,
                    cornerRadius = rimCorner,
                    style = Stroke(width = rimStrokePx),
                    alpha = (cancel * 0.8f * surfaceAlphaValue).coerceIn(0f, 1f)
                )
            }
        }
    }
}

/**
 * Ambient aurora spill around the composer while a recording is live. Slower and softer than the
 * surface itself, tinted by the same environment light, and settled to a still image when motion is
 * reduced or the recording is paused.
 */
private class AmbientGlowAnimator {
    val phase = mutableFloatStateOf(0f)
    val tick = mutableFloatStateOf(0f)
    val intensity = mutableFloatStateOf(0f)
    val cancel = mutableFloatStateOf(0f)

    fun settle(targetIntensity: Float, targetCancel: Float) {
        intensity.floatValue = targetIntensity.coerceIn(0f, 1f)
        cancel.floatValue = targetCancel.coerceIn(0f, 1f)
        tick.floatValue += 0.5f
    }

    fun advance(dtSeconds: Float, targetIntensity: Float, targetCancel: Float) {
        val step = dtSeconds.coerceIn(0.001f, MAX_SIMULATION_STEP_SECONDS)
        intensity.floatValue += (targetIntensity.coerceIn(0f, 1f) - intensity.floatValue) * (1f - exp(-6f * step))
        cancel.floatValue += (targetCancel.coerceIn(0f, 1f) - cancel.floatValue) * (1f - exp(-10f * step))
        phase.floatValue = (phase.floatValue + step * (0.22f + 0.55f * intensity.floatValue)) % 1000f
        tick.floatValue += 1f
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
    val isPaused = (voiceState as? VoiceState.Locked)?.isPaused == true
    val amplitude = voiceState.glowAmplitude()
    val cancelTarget = (voiceState as? VoiceState.Recording)?.cancelProgress?.coerceIn(0f, 1f) ?: 0f
    val errorColor = MaterialTheme.colorScheme.error
    val light = rememberVoiceEnvironmentLight()

    val pools = remember(light) {
        listOf(
            Brush.radialGradient(
                listOf(
                    lerp(GlowColors.Cyan, light.keyColor, 0.35f).copy(alpha = 0.85f),
                    lerp(GlowColors.ElectricBlue, light.fillColor, 0.30f).copy(alpha = 0.35f),
                    Color.Transparent
                )
            ),
            Brush.radialGradient(
                listOf(
                    lerp(GlowColors.Magenta, light.rimColor, 0.30f).copy(alpha = 0.80f),
                    lerp(GlowColors.SunsetGold, light.keyColor, 0.30f).copy(alpha = 0.35f),
                    Color.Transparent
                )
            ),
            Brush.radialGradient(
                listOf(
                    lerp(GlowColors.Violet, light.fillColor, 0.30f).copy(alpha = 0.85f),
                    lerp(GlowColors.DeepIndigo, light.roomColor, 0.30f).copy(alpha = 0.35f),
                    Color.Transparent
                )
            )
        )
    }
    val cancelPool = remember(errorColor) {
        Brush.radialGradient(listOf(errorColor.copy(alpha = 0.55f), Color.Transparent))
    }

    val animator = remember { AmbientGlowAnimator() }
    val latestAmplitude = rememberUpdatedState(amplitude)
    val latestCancel = rememberUpdatedState(cancelTarget)
    val animate = !reducedMotion && !isPaused

    SideEffect {
        if (!animate) animator.settle(latestAmplitude.value, latestCancel.value)
    }

    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        var previousFrameNanos = 0L
        while (true) {
            val frameNanos = withFrameNanos { it }
            if (previousFrameNanos != 0L) {
                val dt = ((frameNanos - previousFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                animator.advance(dt, latestAmplitude.value, latestCancel.value)
            }
            previousFrameNanos = frameNanos
        }
    }

    Canvas(modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val tick = animator.tick.floatValue
        val phase = animator.phase.floatValue
        val intensity = animator.intensity.floatValue
        val cancel = animator.cancel.floatValue

        val angle = phase * GLOW_TAU
        // A slow vertical breath keeps the spill from ever looking like a static still.
        val breath = 1f + 0.04f * sin(tick * 0.015f)
        val radius = maxOf(width * 0.70f, height * 1.05f) * breath
        val alpha = ((0.12f + 0.26f * intensity) * light.intensity).coerceIn(0f, 1f)

        val leftCenter = Offset(width * (0.25f + 0.08f * sin(angle)), height * 0.82f)
        val midCenter = Offset(width * (0.50f + 0.08f * cos(angle * 0.9f)), height * 0.85f)
        val rightCenter = Offset(width * (0.75f - 0.08f * cos(angle * 0.8f)), height * 0.82f)
        val centers = listOf(leftCenter, midCenter, rightCenter)

        for (index in pools.indices) {
            val center = centers[index]
            val poolRadius = if (index == 1) radius * 0.85f else radius
            // Two passes per pool: a wide soft bloom with a tighter core reads as volumetric light.
            drawCircle(brush = pools[index], radius = poolRadius, center = center, alpha = alpha * 0.55f)
            drawCircle(brush = pools[index], radius = poolRadius * 0.55f, center = center, alpha = alpha * 0.45f)
        }

        if (cancel > 0.01f) {
            drawCircle(
                brush = cancelPool,
                radius = radius * 0.8f,
                center = midCenter,
                alpha = (cancel * 0.7f * alpha).coerceIn(0f, 1f)
            )
        }
    }
}
