package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.cardSurfaceColor
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Spring-coupled 1D fluid surface that carries live microphone energy, and the single source of
 * animation truth for the voice recording glow.
 *
 * The node heights are no longer dead code: every frame the surface is reduced to three low-order
 * harmonics which are published as Compose state and consumed by BOTH rendering paths, so the AGSL
 * shader (API 33+) and the Compose Canvas fallback (API 26..32) trace the same silhouette.
 *
 * Published values are Compose state on purpose: the renderers read them inside the draw phase, so
 * the field drives one invalidation per frame without ever recomposing the composer.
 */
internal class VoiceEnergyField(private val nodeCount: Int = VoiceEnergyDesign.FLUID_NODES) {

    private val nodeHeights = FloatArray(nodeCount)
    private val nodeVelocities = FloatArray(nodeCount)
    private val cosineBasis = Array(VoiceEnergyDesign.HARMONICS) { FloatArray(nodeCount) }
    private val sineBasis = Array(VoiceEnergyDesign.HARMONICS) { FloatArray(nodeCount) }

    /** Smoothed, normalised audio energy (attack is fast, release is calm). */
    var energy by mutableFloatStateOf(VoiceEnergyDesign.MIN_AMPLITUDE)
        private set

    /** Decaying kinetic impulse of the most recent loudness rise. */
    var ripple by mutableFloatStateOf(0f)
        private set

    /** Accumulated animation seconds; frozen while recording is paused. */
    var elapsed by mutableFloatStateOf(0f)
        private set

    /** Slow travel phase used for colour movement and ribbon drift. */
    var drift by mutableFloatStateOf(0f)
        private set

    /** Harmonic displacement amplitudes of the simulated surface. */
    val harmonicAmplitude = mutableStateListOf(0f, 0f, 0f)

    /** Harmonic phases of the simulated surface. */
    val harmonicPhase = mutableStateListOf(0f, 0f, 0f)

    private var envelope = 0.1f
    private var oscillator = 0f

    init {
        for (harmonic in 0 until VoiceEnergyDesign.HARMONICS) {
            val order = (harmonic + 1).toFloat()
            for (i in 0 until nodeCount) {
                val x = i.toFloat() / (nodeCount - 1).toFloat()
                cosineBasis[harmonic][i] = cos(VoiceEnergyDesign.TAU * order * x)
                sineBasis[harmonic][i] = sin(VoiceEnergyDesign.TAU * order * x)
            }
        }
        clearNodes()
    }

    /**
     * Static but attractive composition for reduced motion. No animation loop is started, so the
     * energy surface stays a fixed, legible gradient composition instead of a moving field.
     */
    fun snapshot(amplitude: Float) {
        clearNodes()
        elapsed = 0f
        drift = 0f
        ripple = 0f
        oscillator = 0f
        val target = amplitude.coerceIn(VoiceEnergyDesign.MIN_AMPLITUDE, VoiceEnergyDesign.MAX_AMPLITUDE)
        energy = target
        envelope = target
        harmonicAmplitude[0] = target * 0.42f
        harmonicAmplitude[1] = target * 0.24f
        harmonicAmplitude[2] = target * 0.14f
        harmonicPhase[0] = 0.6f
        harmonicPhase[1] = 2.4f
        harmonicPhase[2] = 4.1f
    }

    fun update(dtSeconds: Float, amplitude: Float, paused: Boolean) {
        val dt = dtSeconds.coerceIn(0f, VoiceEnergyDesign.MAX_FRAME_DELTA_SECONDS)

        // A paused recording stops receiving audio: the clock freezes and the field settles.
        if (!paused) {
            elapsed = wrap(elapsed + dt)
            drift = wrap(drift + dt * (0.12f + 0.55f * energy))
        }

        val target = if (paused) {
            VoiceEnergyDesign.PAUSED_ENERGY
        } else {
            amplitude.coerceIn(VoiceEnergyDesign.MIN_AMPLITUDE, VoiceEnergyDesign.MAX_AMPLITUDE)
        }

        // Fast attack so speech reads immediately, slow release so the surface decays gracefully.
        energy = if (target > energy) energy * 0.30f + target * 0.70f
        else energy * 0.90f + target * 0.10f

        val rise = target - envelope
        envelope = envelope * 0.86f + target * 0.14f
        if (!paused && rise > 0.02f) {
            val impulse = (rise * VoiceEnergyDesign.IMPULSE_GAIN).coerceAtMost(1f)
            ripple = (ripple + impulse).coerceAtMost(1f)
            injectImpulse(impulse)
        }
        // While paused the impulse field decays faster so no high-energy deformation lingers.
        ripple *= if (paused) 0.80f else 0.93f

        oscillator = wrap(oscillator + dt * (0.30f + 1.05f * energy))

        stepFluid(dt, paused)
        extractHarmonics()
    }

    private fun clearNodes() {
        for (i in 0 until nodeCount) {
            nodeHeights[i] = 0f
            nodeVelocities[i] = 0f
        }
    }

    private fun wrap(value: Float): Float = if (value >= 1000f) value - 1000f else value

    /** Audio impulses enter the surface where the oscillator is currently most active. */
    private fun injectImpulse(strength: Float) {
        val centre = ((nodeCount - 1) * (0.5f + 0.38f * sin(oscillator * 4.2f)))
            .toInt()
            .coerceIn(2, nodeCount - 3)
        val kick = strength * 5.0f
        nodeVelocities[centre] += kick
        nodeVelocities[centre - 1] += kick * 0.75f
        nodeVelocities[centre + 1] += kick * 0.75f
        nodeVelocities[centre - 2] += kick * 0.40f
        nodeVelocities[centre + 2] += kick * 0.40f
    }

    private fun stepFluid(dt: Float, paused: Boolean) {
        val springK = 9f
        val coupling = 15f
        val damping = 0.96f

        // A gentle seam of motion keeps the surface breathing while the voice is calm.
        if (!paused) {
            val drive = (0.10f + 0.55f * energy) * dt * 1.4f
            for (i in 0 until nodeCount) {
                val x = i.toFloat() / (nodeCount - 1).toFloat()
                nodeVelocities[i] += drive * sin(VoiceEnergyDesign.TAU * 1.5f * x + oscillator * 0.9f)
            }
        }

        for (i in 0 until nodeCount) {
            val left = if (i > 0) nodeHeights[i - 1] else nodeHeights[i]
            val right = if (i < nodeCount - 1) nodeHeights[i + 1] else nodeHeights[i]
            val springForce = -springK * nodeHeights[i]
            val couplingForce = coupling * (left + right - 2f * nodeHeights[i])
            nodeVelocities[i] = (nodeVelocities[i] + (springForce + couplingForce) * dt) * damping
            nodeHeights[i] = (nodeHeights[i] + nodeVelocities[i] * dt).coerceIn(-0.9f, 0.9f)
        }
    }

    /**
     * Projects the simulated node heights onto the three lowest harmonics so the shader and the
     * Canvas fallback can rebuild the same silhouette from a handful of scalars.
     */
    private fun extractHarmonics() {
        val normalisation = 2f / nodeCount
        for (harmonic in 0 until VoiceEnergyDesign.HARMONICS) {
            val cosines = cosineBasis[harmonic]
            val sines = sineBasis[harmonic]
            var a = 0f
            var b = 0f
            for (i in 0 until nodeCount) {
                val height = nodeHeights[i]
                a += height * cosines[i]
                b += height * sines[i]
            }
            a *= normalisation
            b *= normalisation
            harmonicAmplitude[harmonic] = sqrt(a * a + b * b).coerceAtMost(VoiceEnergyDesign.MAX_HARMONIC)
            harmonicPhase[harmonic] = atan2(a, b)
        }
    }
}

/**
 * Layer 1 + 2 + 3 of the energy field, rendered by the Android runtime shader on API 33+.
 *
 * Uniforms are set on a single cached shader instance - no shader object is created per frame.
 */
private const val VOICE_ENERGY_AGSL_SHADER = """
uniform float2 iResolution;
uniform float iTime;
uniform float iAmplitude;
uniform float iRipple;
uniform float iDrift;
uniform float iHarmonic1;
uniform float iHarmonic2;
uniform float iHarmonic3;
uniform float iPhase1;
uniform float iPhase2;
uniform float iPhase3;
uniform float4 cBlue;
uniform float4 cViolet;
uniform float4 cCyan;
uniform float4 cCoral;
uniform float4 cAmber;
uniform float4 cGreen;
uniform float4 cPink;

// Soft Gaussian weight. Every colour pool uses it so hues travel through one another instead of
// forming rigid stripes.
float pool(float value, float centre, float width) {
    float d = (value - centre) / width;
    return exp(-d * d);
}

// Organic silhouette. The harmonics are the live output of the fluid simulation; the two slow
// irrational drift terms keep the surface breathing while the voice is calm.
float surfaceProfile(float x, float t, float amp) {
    const float TAU = 6.2831853;
    float d = iHarmonic1 * sin(TAU * x + iPhase1);
    d += iHarmonic2 * 0.55 * sin(TAU * 2.0 * x + iPhase2);
    d += iHarmonic3 * 0.32 * sin(TAU * 3.0 * x + iPhase3);
    d += 0.055 * amp * sin(TAU * 1.7 * x - t * 0.55);
    d += 0.030 * amp * sin(TAU * 2.9 * x + t * 0.83);
    return d;
}

// Layer 3 - multicolour energy flow. Blue and violet hold the foundation, cyan runs through it,
// and the warm roles only enter as the normalised amplitude (warmth) rises.
float3 energyColor(float x, float warmth, float travel) {
    const float TAU = 6.2831853;
    // Roughly 1.5 hue cycles travel across the surface instead of a single slow sweep, so several
    // related hues are on screen at once and hand over to each other through the Gaussian pools.
    float coolMix = 0.5 + 0.5 * sin(TAU * x * 1.45 - travel * 0.95);
    float3 cool = mix(cBlue.rgb, cViolet.rgb, coolMix);
    float coolCyan = pool(x, 0.30 + 0.16 * sin(travel * 0.70), 0.26);
    cool = mix(cool, cCyan.rgb, clamp(coolCyan * 0.62, 0.0, 0.80));

    float warmMix = 0.5 + 0.5 * sin(TAU * x * 1.70 + travel * 1.15);
    float3 warm = mix(cCoral.rgb, cAmber.rgb, warmMix);
    float warmGreen = pool(x, 0.74 - 0.14 * cos(travel * 0.80), 0.22);
    warm = mix(warm, cGreen.rgb, clamp(warmGreen * 0.70, 0.0, 0.86));
    float warmPink = pool(x, 0.10 + 0.12 * cos(travel * 0.55), 0.18);
    warm = mix(warm, cPink.rgb, clamp(warmPink * 0.55, 0.0, 0.68));

    return mix(cool, warm, warmth);
}

half4 main(in float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float x = clamp(uv.x, 0.0, 1.0);
    float y = uv.y;
    float amp = clamp(iAmplitude, 0.0, 1.0);
    // The warm roles used to sit behind a very high energy gate, so a conversational voice rendered
    // as one flat blue/violet mass. The gate now opens at speaking level and saturates below peak, so
    // the whole spectrum stays on screen and keeps re-mixing while someone talks.
    float warmth = smoothstep(0.12, 0.60, amp);
    float travel = iTime * 0.055 + iDrift * 0.02;

    // ---- Layer 1: primary energy surface ------------------------------------------------
    float bodyLine = 0.90 - 0.05 * amp;
    float profileScale = 0.10 + 0.13 * amp + 0.05 * iRipple;
    float edge = bodyLine - surfaceProfile(x, iTime, amp) * profileScale;

    // Concentrated leading edge with a softly diffused tail upward (Gemini directional gradient).
    float leading = pool(y, edge + 0.012, 0.030 + 0.018 * amp);
    float innerHalo = pool(y, edge + 0.075, 0.150);
    float depthFade = exp(-max(y - edge, 0.0) / 0.55);
    float inside = smoothstep(edge - 0.02, edge + 0.05, y);
    float tail = (y < edge) ? exp(-(edge - y) / (0.16 + 0.10 * amp)) : 0.0;
    float volume = clamp(
        leading + tail * 0.85 + inside * (0.30 + 0.45 * depthFade) + innerHalo * 0.30,
        0.0, 1.0);

    // ---- Layer 2: translucent energy ribbons at independent speeds and phases ------------
    float r1 = 0.86 - surfaceProfile(x * 0.92, iTime * 0.78 + 1.7, amp) * (0.075 + 0.105 * amp) - 0.10;
    float r2 = 0.80 - surfaceProfile(x * 1.11, iTime * 0.61 + 4.3, amp) * (0.065 + 0.095 * amp) - 0.22;
    float r3 = 0.92 - surfaceProfile(x * 0.75, iTime * 0.93 + 7.9, amp) * (0.055 + 0.085 * amp) - 0.04;
    float ribbon = pool(y, r1, 0.085) * 0.55 + pool(y, r2, 0.120) * 0.40 + pool(y, r3, 0.070) * 0.50;
    ribbon *= (0.35 + 0.65 * amp) * (inside * 0.90 + tail * 0.35);

    // ---- Composite ----------------------------------------------------------------------
    float3 col = energyColor(x, warmth, travel);
    float3 ribbonTint = energyColor(fract(x + 0.22), warmth, travel + 1.35);
    col += ribbonTint * ribbon * 0.60;
    col *= 0.52 + 0.48 * amp;

    float alpha = clamp(volume * (0.55 + 0.40 * amp), 0.0, 0.94) + ribbon * 0.16;
    alpha = clamp(alpha, 0.0, 0.96);
    if (alpha <= 0.002) {
        return half4(0.0, 0.0, 0.0, 0.0);
    }

    col += col * leading * (0.35 + 0.45 * amp);              // controlled bloom on the leading edge
    col += col * innerHalo * 0.22;                            // volumetric inner illumination
    col += float3(1.0) * leading * (0.10 + 0.22 * amp);       // specular highlight
    col = clamp(col, 0.0, 1.0);

    return half4(col, alpha);
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class VoiceEnergyShader {
    val runtimeShader = RuntimeShader(VOICE_ENERGY_AGSL_SHADER)
    val shaderBrush = ShaderBrush(runtimeShader)

    init {
        setColor("cBlue", VoiceEnergyColors.Blue)
        setColor("cViolet", VoiceEnergyColors.Violet)
        setColor("cCyan", VoiceEnergyColors.Cyan)
        setColor("cCoral", VoiceEnergyColors.Coral)
        setColor("cAmber", VoiceEnergyColors.Amber)
        setColor("cGreen", VoiceEnergyColors.Green)
        setColor("cPink", VoiceEnergyColors.Pink)
    }

    private fun setColor(name: String, color: Color) {
        runtimeShader.setFloatUniform(name, color.red, color.green, color.blue, color.alpha)
    }

    fun updateUniforms(width: Float, height: Float, field: VoiceEnergyField, presence: Float) {
        val amplitude = field.energy * presence
        runtimeShader.setFloatUniform("iResolution", width, height)
        runtimeShader.setFloatUniform("iTime", field.elapsed)
        runtimeShader.setFloatUniform("iAmplitude", amplitude)
        runtimeShader.setFloatUniform("iRipple", field.ripple)
        runtimeShader.setFloatUniform("iDrift", field.drift)
        runtimeShader.setFloatUniform("iHarmonic1", field.harmonicAmplitude[0])
        runtimeShader.setFloatUniform("iHarmonic2", field.harmonicAmplitude[1])
        runtimeShader.setFloatUniform("iHarmonic3", field.harmonicAmplitude[2])
        runtimeShader.setFloatUniform("iPhase1", field.harmonicPhase[0])
        runtimeShader.setFloatUniform("iPhase2", field.harmonicPhase[1])
        runtimeShader.setFloatUniform("iPhase3", field.harmonicPhase[2])
    }
}

/**
 * Preallocated drawing resources for the Canvas fallback. Everything that would otherwise be
 * recreated every frame (paths, strokes, gradients) is built once per density.
 */
private class VoiceEnergyFallbackArt(density: Density) {
    val primaryPath = Path()
    val ribbonPaths: List<Path> = List(2) { Path() }
    val ribbonStrokes: List<Stroke> = listOf(11.dp, 6.dp).map { widthDp ->
        Stroke(width = with(density) { widthDp.toPx() })
    }
    val edgeStrokes: List<Stroke> = listOf(18.dp, 10.dp, 4.dp).map { widthDp ->
        Stroke(width = with(density) { widthDp.toPx() })
    }
    val specularStroke: Stroke = Stroke(width = with(density) { 1.dp.toPx() })
    val edgeAlphas: List<Float> = listOf(0.05f, 0.09f, 0.18f)
    val calmBrush: Brush = Brush.horizontalGradient(colorStops = calmEnergyStops())
    val expressiveBrush: Brush = Brush.horizontalGradient(colorStops = expressiveEnergyStops())
}

/** Radial halo gradients for the ambient glow, built once. */
private class VoiceAmbientArt {
    val calmBrush: Brush = Brush.radialGradient(
        colorStops = arrayOf(
            0.00f to VoiceEnergyColors.Blue.copy(alpha = 0.85f),
            0.34f to VoiceEnergyColors.Violet.copy(alpha = 0.45f),
            0.68f to VoiceEnergyColors.DeepIndigo.copy(alpha = 0.16f),
            1.00f to Color.Transparent
        )
    )
    val expressiveBrush: Brush = Brush.radialGradient(
        colorStops = arrayOf(
            0.00f to VoiceEnergyColors.Coral.copy(alpha = 0.80f),
            0.35f to VoiceEnergyColors.Amber.copy(alpha = 0.45f),
            0.72f to VoiceEnergyColors.Green.copy(alpha = 0.18f),
            1.00f to Color.Transparent
        )
    )
}

private fun calmEnergyStops(): Array<Pair<Float, Color>> = arrayOf(
    0.00f to VoiceEnergyColors.DeepIndigo,
    0.22f to VoiceEnergyColors.Blue,
    0.44f to VoiceEnergyColors.Violet,
    0.66f to VoiceEnergyColors.Cyan,
    0.86f to VoiceEnergyColors.Blue,
    1.00f to VoiceEnergyColors.Violet
)

private fun expressiveEnergyStops(): Array<Pair<Float, Color>> = arrayOf(
    0.00f to VoiceEnergyColors.Blue,
    0.18f to VoiceEnergyColors.Violet,
    0.38f to VoiceEnergyColors.Cyan,
    0.55f to VoiceEnergyColors.Coral,
    0.72f to VoiceEnergyColors.Amber,
    0.88f to VoiceEnergyColors.Green,
    1.00f to VoiceEnergyColors.Pink
)

/**
 * Shared silhouette maths. The Canvas fallback reproduces the shader profile exactly, so old
 * devices see the same organic geometry and the same audio reactivity as new ones.
 */
private fun energySurfaceY(
    x: Float,
    field: VoiceEnergyField,
    baseLine: Float,
    scale: Float,
    timeShift: Float
): Float {
    val amplitudes = field.harmonicAmplitude
    val phases = field.harmonicPhase
    val time = field.elapsed + timeShift
    var profile = amplitudes[0] * sin(VoiceEnergyDesign.TAU * x + phases[0])
    profile += amplitudes[1] * 0.55f * sin(VoiceEnergyDesign.TAU * 2f * x + phases[1])
    profile += amplitudes[2] * 0.32f * sin(VoiceEnergyDesign.TAU * 3f * x + phases[2])
    profile += 0.055f * field.energy * sin(VoiceEnergyDesign.TAU * 1.7f * x - time * 0.55f)
    profile += 0.030f * field.energy * sin(VoiceEnergyDesign.TAU * 2.9f * x + time * 0.83f)
    return baseLine - profile * scale
}

private fun buildEnergyPath(
    path: Path,
    field: VoiceEnergyField,
    width: Float,
    height: Float,
    baseLine: Float,
    scale: Float,
    timeShift: Float,
    closeToBottom: Boolean
) {
    val samples = VoiceEnergyDesign.FALLBACK_SAMPLES
    path.reset()
    path.moveTo(0f, energySurfaceY(0f, field, baseLine, scale, timeShift))
    for (i in 1..samples) {
        val step = i.toFloat() / samples.toFloat()
        path.lineTo(width * step, energySurfaceY(step, field, baseLine, scale, timeShift))
    }
    if (closeToBottom) {
        path.lineTo(width, height)
        path.lineTo(0f, height)
        path.close()
    }
}

private fun DrawScope.drawVoiceEnergyFallback(
    field: VoiceEnergyField,
    art: VoiceEnergyFallbackArt,
    presence: Float
) {
    val width = size.width
    val height = size.height
    if (width <= 0f || height <= 0f) return

    val energy = field.energy
    val warmth = ((energy - 0.12f) / 0.48f).coerceIn(0f, 1f)
    val rippleBoost = 0.75f + 0.35f * field.ripple
    val baseLine = height * (0.90f - 0.05f * energy)
    val scale = height * (0.10f + 0.13f * energy + 0.05f * field.ripple)

    // Layer 2 - secondary ribbons: open organic contours drawn as wide translucent arcs.
    for (index in art.ribbonPaths.indices) {
        val path = art.ribbonPaths[index]
        val ribbonTimeShift = if (index == 0) VoiceEnergyDesign.RIBBON_TIME_SHIFT_1
        else VoiceEnergyDesign.RIBBON_TIME_SHIFT_2
        buildEnergyPath(
            path = path,
            field = field,
            width = width,
            height = height,
            baseLine = baseLine - height * (0.10f + 0.09f * index),
            scale = scale * (0.85f - 0.15f * index),
            timeShift = ribbonTimeShift,
            closeToBottom = false
        )
        val ribbonAlpha = presence * (0.15f - 0.05f * index) * (0.45f + 0.55f * energy)
        if (ribbonAlpha <= 0.002f) continue
        drawPath(path, brush = art.calmBrush, alpha = ribbonAlpha, style = art.ribbonStrokes[index])
        drawPath(path, brush = art.expressiveBrush, alpha = ribbonAlpha * warmth, style = art.ribbonStrokes[index])
    }

    // Layer 1 - primary energy surface.
    buildEnergyPath(
        path = art.primaryPath,
        field = field,
        width = width,
        height = height,
        baseLine = baseLine,
        scale = scale,
        timeShift = 0f,
        closeToBottom = true
    )

    val fillAlpha = presence * (0.34f + 0.42f * energy)
    clipPath(art.primaryPath) {
        drawRect(brush = art.calmBrush, alpha = fillAlpha)
        drawRect(brush = art.expressiveBrush, alpha = fillAlpha * warmth)
    }

    // Illuminated boundary, diffused tail and bloom - wide, faint strokes instead of a blur pass.
    art.edgeStrokes.forEachIndexed { index, stroke ->
        val edgeAlpha = presence * art.edgeAlphas[index] * rippleBoost * (0.55f + 0.55f * energy)
        if (edgeAlpha <= 0.002f) return@forEachIndexed
        drawPath(art.primaryPath, brush = art.calmBrush, alpha = edgeAlpha, style = stroke)
        drawPath(art.primaryPath, brush = art.expressiveBrush, alpha = edgeAlpha * warmth, style = stroke)
    }

    // Specular highlight riding the leading edge.
    drawPath(
        path = art.primaryPath,
        brush = SolidColor(VoiceEnergyColors.Specular),
        alpha = presence * (0.08f + 0.20f * energy),
        style = art.specularStroke
    )
}

private fun DrawScope.drawVoiceAmbientHalos(field: VoiceEnergyField, art: VoiceAmbientArt) {
    val width = size.width
    val height = size.height
    if (width <= 0f || height <= 0f) return

    val energy = field.energy
    val warmth = ((energy - 0.12f) / 0.48f).coerceIn(0f, 1f)
    val drift = field.drift
    val radius = maxOf(width * 0.62f, height * 0.95f) * (0.92f + 0.16f * energy)
    val coreAlpha = 0.10f + 0.22f * energy

    // Three travelling pools keep the illumination local around the composer and let the colour
    // follow the same cool -> warm transition as the primary surface.
    val left = Offset(width * (0.24f + 0.07f * sin(drift * 0.90f)), height * 0.86f)
    val middle = Offset(width * (0.50f + 0.07f * cos(drift * 0.80f)), height * 0.90f)
    val right = Offset(width * (0.76f - 0.07f * sin(drift * 1.05f)), height * 0.86f)

    drawCircle(brush = art.calmBrush, center = left, radius = radius, alpha = coreAlpha * 0.55f)
    drawCircle(brush = art.expressiveBrush, center = left, radius = radius, alpha = coreAlpha * 0.55f * warmth)
    drawCircle(brush = art.calmBrush, center = middle, radius = radius, alpha = coreAlpha * 0.50f)
    drawCircle(brush = art.expressiveBrush, center = middle, radius = radius, alpha = coreAlpha * 0.50f * warmth)
    drawCircle(brush = art.calmBrush, center = right, radius = radius, alpha = coreAlpha * 0.55f)
    drawCircle(brush = art.expressiveBrush, center = right, radius = radius, alpha = coreAlpha * 0.55f * warmth)
}

private fun VoiceState.isGlowActive(): Boolean = this is VoiceState.Recording || this is VoiceState.Locked

internal fun VoiceState.glowAmplitude(): Float = when (this) {
    is VoiceState.Recording -> (amplitudeHistory.lastOrNull() ?: VoiceEnergyDesign.MIN_AMPLITUDE)
        .coerceIn(VoiceEnergyDesign.MIN_AMPLITUDE, VoiceEnergyDesign.MAX_AMPLITUDE)
    is VoiceState.Locked -> if (isPaused) VoiceEnergyDesign.MIN_AMPLITUDE else
        (amplitudeHistory.lastOrNull() ?: VoiceEnergyDesign.MIN_AMPLITUDE)
            .coerceIn(VoiceEnergyDesign.MIN_AMPLITUDE, VoiceEnergyDesign.MAX_AMPLITUDE)
    is VoiceState.VoiceDraft -> draftGlowAmplitude()
    else -> 0f
}

/**
 * Amplitude source for a recorded draft: the envelope is sampled at the current playback
 * position, so replaying or scrubbing the draft lifts the surface exactly where the original
 * audio did. An idle draft reports the silence floor and settles through the paused path.
 */
private fun VoiceState.VoiceDraft.draftGlowAmplitude(): Float {
    if (!isPlaying || durationMs <= 0L || amplitudeHistory.isEmpty()) return VoiceEnergyDesign.MIN_AMPLITUDE
    val fraction = (playbackPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    val index = (fraction * (amplitudeHistory.size - 1).toFloat()).roundToInt()
        .coerceIn(0, amplitudeHistory.size - 1)
    return amplitudeHistory[index]
        .coerceIn(VoiceEnergyDesign.MIN_AMPLITUDE, VoiceEnergyDesign.MAX_AMPLITUDE)
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
 * Single frame-driven animation loop for the energy field. The loop is keyed on the voice stage
 * and the reduced-motion flag only, so it is never restarted for a changing amplitude value, and
 * it is cancelled when the component is inactive or disposed.
 */
@Composable
private fun rememberVoiceEnergyField(
    active: Boolean,
    paused: Boolean,
    amplitude: Float,
    reducedMotion: Boolean
): VoiceEnergyField {
    val field = remember { VoiceEnergyField() }
    val latestAmplitude by rememberUpdatedState(amplitude)
    val latestPaused by rememberUpdatedState(paused)

    if (reducedMotion) {
        SideEffect { field.snapshot(latestAmplitude) }
        return field
    }

    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        var previousFrameNanos = 0L
        while (true) {
            val frameNanos = withFrameNanos { it }
            if (previousFrameNanos != 0L) {
                field.update(
                    dtSeconds = (frameNanos - previousFrameNanos) / 1_000_000_000f,
                    amplitude = latestAmplitude,
                    paused = latestPaused
                )
            }
            previousFrameNanos = frameNanos
        }
    }
    return field
}

@Composable
fun VoiceRecordingGlowSurface(
    voiceState: VoiceState,
    reducedMotionOverride: Boolean,
    modifier: Modifier = Modifier
) {
    val active = voiceState.isGlowActive()
    // A recorded draft keeps the energy surface - the same spectrum, held at a calmer intensity -
    // so the voice bar stays part of one visual language after the finger lifts instead of
    // collapsing into a flat card. The large ambient halos stay recording-only.
    val isDraft = voiceState is VoiceState.VoiceDraft
    val showField = active || isDraft
    val reducedMotion = reducedMotionOverride
    val surfaceColor = cardSurfaceColor()
    val shape = RoundedCornerShape(Sizes.CornerMassive)
    val isPaused = when (voiceState) {
        is VoiceState.Locked -> voiceState.isPaused
        is VoiceState.VoiceDraft -> !voiceState.isPlaying
        else -> false
    }
    val amplitude = if (showField) voiceState.glowAmplitude() else VoiceEnergyDesign.MIN_AMPLITUDE
    val field = rememberVoiceEnergyField(showField, isPaused, amplitude, reducedMotion)

    // Read inside the draw phase so the settle fade never recomposes the composer.
    val presenceState = animateFloatAsState(
        targetValue = when {
            active -> 1f
            isDraft -> VoiceEnergyDesign.DRAFT_PRESENCE
            else -> 0f
        },
        animationSpec = if (reducedMotion) snap() else
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "voiceLiquidSettle"
    )

    val density = LocalDensity.current
    val shader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) VoiceEnergyShader() else null
    }
    val fallbackArt = remember(density) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) VoiceEnergyFallbackArt(density) else null
    }
    val rimBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                VoiceEnergyColors.Specular.copy(alpha = 1f),
                VoiceEnergyColors.Cyan.copy(alpha = 0.70f),
                VoiceEnergyColors.Violet.copy(alpha = 0.62f)
            )
        )
    }
    val rimStroke = remember(density) { Stroke(width = with(density) { 1.8.dp.toPx() }) }

    Box(modifier = modifier.clip(shape)) {
        Canvas(Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            if (width <= 0f || height <= 0f) return@Canvas

            val maxCornerPx = with(density) { Sizes.CornerMassive.toPx() }
            val corner = CornerRadius(minOf(maxCornerPx, height / 2f))

            drawRoundRect(color = surfaceColor, cornerRadius = corner)

            val presence = presenceState.value
            if (presence <= 0.001f) return@Canvas

            if (shader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                shader.updateUniforms(width, height, field, presence)
                drawRoundRect(brush = shader.shaderBrush, cornerRadius = corner, alpha = presence)
            } else if (fallbackArt != null) {
                drawVoiceEnergyFallback(field, fallbackArt, presence)
            }

            // Outer specular rim keeps the energy field inside the composer's rounded container.
            val borderAlpha = presence * (0.32f + 0.28f * field.energy)
            val strokePx = with(density) { 1.8.dp.toPx() }
            val strokeCornerRadiusPx = with(density) { (Sizes.CornerMassive - (strokePx / 2f).toDp()).toPx() }
            val strokeCorner = CornerRadius(minOf(strokeCornerRadiusPx, (height - strokePx) / 2f).coerceAtLeast(0f))
            drawRoundRect(
                brush = rimBrush,
                topLeft = Offset(strokePx / 2f, strokePx / 2f),
                size = Size((width - strokePx).coerceAtLeast(0f), (height - strokePx).coerceAtLeast(0f)),
                cornerRadius = strokeCorner,
                alpha = borderAlpha,
                style = rimStroke
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
    val isPaused = (voiceState as? VoiceState.Locked)?.isPaused == true
    val field = rememberVoiceEnergyField(
        active = true,
        paused = isPaused,
        amplitude = voiceState.glowAmplitude(),
        reducedMotion = reducedMotion
    )
    val haloArt = remember { VoiceAmbientArt() }

    Canvas(modifier) {
        drawVoiceAmbientHalos(field, haloArt)
    }
}

