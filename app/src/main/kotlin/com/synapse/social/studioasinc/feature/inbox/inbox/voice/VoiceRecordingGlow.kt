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
import androidx.compose.ui.graphics.drawscope.clipRect
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
 *
 * The composer surface and the ambient band each own an instance of this class. They receive the
 * same microphone state every frame, and the animation phase is derived from the shared monotonic
 * frame clock (not from a per-instance frame counter), so the two instances stay in lockstep and
 * read as one continuous energy system. Pausing still freezes the clock: paused frames accumulate
 * into an offset that is subtracted from the phase.
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

    /** Monotonic seconds of this instance's first animated frame, used as the phase origin. */
    private var firstFrameSeconds = -1f

    /** Seconds spent paused, subtracted from the phase so a paused take holds its shape. */
    private var pausedSeconds = 0f

    /** Constant-rate phase of the microphone drive, independent of how loud the voice is. */
    private var drivePhase = 0f

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
        firstFrameSeconds = -1f
        pausedSeconds = 0f
        drivePhase = 0f
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

    fun update(dtSeconds: Float, amplitude: Float, paused: Boolean, absoluteSeconds: Float) {
        val dt = dtSeconds.coerceIn(0f, VoiceEnergyDesign.MAX_FRAME_DELTA_SECONDS)

        if (firstFrameSeconds < 0f) firstFrameSeconds = absoluteSeconds

        // A paused recording stops receiving audio: the clock freezes and the field settles.
        if (paused) {
            pausedSeconds += dt
        } else {
            // Phase from the shared frame clock: both field instances (composer surface and ambient
            // band) therefore share one timeline instead of accumulating their own frame deltas.
            elapsed = (absoluteSeconds - firstFrameSeconds - pausedSeconds).coerceAtLeast(0f)
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

    /**
     * Impulse launched when the voice suddenly gets louder.
     *
     * The kick is modal: it is spread across the first two modes instead of a single node, so an
     * onset always excites the low orders the renderer uses. The previous single-node kick landed
     * wherever the oscillator happened to point, which could sit on a node of the mode and project
     * almost nothing - measured over a simulated take, a shout then deformed the surface LESS than
     * normal speech. With the modal kick the first harmonic travels roughly 0.07 at silence, 0.31
     * for normal speech and 0.53 for a shout, so loudness reads as deformation.
     */
    private fun injectImpulse(strength: Float) {
        val kick = strength * VoiceEnergyDesign.IMPULSE_KICK
        val primaryPhase = oscillator * 4.2f
        val secondaryPhase = oscillator * 2.6f
        for (i in 0 until nodeCount) {
            val x = i.toFloat() / (nodeCount - 1).toFloat()
            nodeVelocities[i] += kick * (
                sin(VoiceEnergyDesign.TAU * x + primaryPhase) * 0.70f +
                    sin(VoiceEnergyDesign.TAU * 2f * x + secondaryPhase) * 0.40f
                )
        }
    }

    private fun stepFluid(dt: Float, paused: Boolean) {
        val springK = 9f
        val coupling = 15f
        val damping = 0.96f

        // A gentle seam of motion keeps the surface breathing while the voice is calm, and the whole
        // drive grows linearly with the microphone amplitude, so the silhouette travels further the
        // louder someone speaks. The drive phase advances at a constant rate on purpose: tying it to
        // the energy (as the oscillator is) swept the resonance faster at high energy and made loud
        // speech deform the surface less than normal speech.
        if (!paused) {
            drivePhase = if (drivePhase >= VoiceEnergyDesign.TAU) drivePhase - VoiceEnergyDesign.TAU
            else drivePhase + dt * VoiceEnergyDesign.DRIVE_RATE
            val drive = (VoiceEnergyDesign.DRIVE_FLOOR + VoiceEnergyDesign.DRIVE_ENERGY * energy) *
                dt * VoiceEnergyDesign.DRIVE_GAIN
            for (i in 0 until nodeCount) {
                val x = i.toFloat() / (nodeCount - 1).toFloat()
                nodeVelocities[i] += drive * (
                    sin(VoiceEnergyDesign.TAU * x + drivePhase) * 0.60f +
                        sin(VoiceEnergyDesign.TAU * 2f * x + drivePhase * 0.55f) * 0.30f +
                        sin(VoiceEnergyDesign.TAU * 3f * x - drivePhase * 0.40f) * 0.15f
                    )
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
 * The voice energy composition, rendered by the Android runtime shader on API 33+.
 *
 * Geometry is amplitude-driven and comes in as uniforms (iBodyTop, iCrestScale, iBand*), the shape
 * comes from the fluid simulation's harmonics, and colour walks a Gaussian-blended hue field. Three
 * contours are layered: the body crest, and two translucent ribbon bands floating above it, each
 * with its own spatial frequency, time scale and hue offset.
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
// Composition, computed on the Kotlin side (VoiceEnergyComposition) and shared with the Canvas
// fallback, so the amplitude -> geometry mapping exists exactly once.
uniform float iBodyTop;
uniform float iCrestScale;
uniform float iBand1Offset;
uniform float iBand1Width;
uniform float iBand2Offset;
uniform float iBand2Width;
uniform float iWarmth;
uniform float4 cBlue;
uniform float4 cViolet;
uniform float4 cCyan;
uniform float4 cCoral;
uniform float4 cAmber;
uniform float4 cGreen;
uniform float4 cPink;

// Soft Gaussian weight. Every layer and every colour pool uses it, so hues and bands travel through
// one another instead of forming rigid stripes.
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

// Hue field. Blue, cyan and violet hold the cool half; coral, amber, green and pink make up the warm
// half and fade in with voice energy. More than one hue cycle travels across the surface, so several
// related colours are always on screen and hand over to each other through the Gaussian pools.
float3 energyColor(float x, float warmth, float travel) {
    const float TAU = 6.2831853;
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
    float y = clamp(uv.y, 0.0, 1.0);
    float amp = clamp(iAmplitude, 0.0, 1.0);
    float travel = iTime * 0.055 + iDrift * 0.02;

    // Slow migration of the whole hue assignment, so the field keeps re-mixing even on a held note.
    float migrate = 0.06 * sin(travel * 0.37);

    // ---- Silhouette ----------------------------------------------------------------------
    // iBodyTop is the mean crest height (0 = top of the composer): about 38% visible coverage at
    // silence, about 48% at full voice. The harmonics carry the organic shape on top of that, and a
    // fresh amplitude impulse lifts the crest further.
    float crest = iBodyTop - surfaceProfile(x, iTime, amp) * iCrestScale;

    // Two ribbon bands floating above the crest. Each one uses its own spatial frequency and time
    // scale, so all three contours move independently while sharing the same fluid simulation.
    float band1 = crest - iBand1Offset - surfaceProfile(x * 0.83, iTime * 0.71 + 1.7, amp) * iCrestScale * 0.62;
    float band2 = crest - iBand2Offset - surfaceProfile(x * 1.17, iTime * 0.53 + 4.3, amp) * iCrestScale * 0.48;

    // ---- Layer weights -------------------------------------------------------------------
    float body = smoothstep(crest - 0.012, crest + 0.035, y);
    float depth = exp(-max(y - crest, 0.0) / 0.46);
    float crestBand = pool(y, crest, 0.040 + 0.020 * amp);
    float w1 = pool(y, band1, iBand1Width) * (0.60 + 0.40 * amp);
    float w2 = pool(y, band2, iBand2Width) * (0.45 + 0.35 * amp);

    // The upper half stays softer, and the warm accents stay low: colour therefore migrates from
    // cool blue/violet at the top of the field to coral/amber/green at its base, which is both a
    // natural light stratification and what keeps the timer and the controls readable on top of it.
    float ceiling = 0.46 + 0.54 * smoothstep(0.14, 0.82, y);
    float layerWarmth = iWarmth * smoothstep(0.40, 0.86, y);

    // ---- Colour --------------------------------------------------------------------------
    float3 cBody = energyColor(fract(x + migrate), layerWarmth, travel);
    float3 cBand1 = energyColor(fract(x + migrate + 0.27), clamp(layerWarmth * 1.15, 0.0, 1.0), travel + 1.35);
    float3 cBand2 = energyColor(fract(x + migrate + 0.58), layerWarmth * 0.90, travel + 2.60);

    // Hue of the composite is the weighted average of the layers that overlap here, so an
    // overlapping ribbon genuinely mixes its colour into the body instead of painting over it.
    float3 col = cBody * body + cBand1 * w1 * 0.85 + cBand2 * w2 * 0.65;
    float weight = body + w1 * 0.85 + w2 * 0.65;
    col /= max(weight, 0.0001);

    // ---- Composite -----------------------------------------------------------------------
    float alpha = body * (0.50 + 0.30 * amp) * (0.62 + 0.38 * depth)
        + w1 * 0.32
        + w2 * 0.24
        + crestBand * (0.16 + 0.22 * amp);
    // Slightly more transparent toward the top, so the icon row keeps its contrast.
    alpha *= 0.66 + 0.34 * smoothstep(0.10, 0.85, y);
    alpha = clamp(alpha, 0.0, 0.90);
    if (alpha <= 0.002) {
        return half4(0.0, 0.0, 0.0, 0.0);
    }

    col *= ceiling;
    col += col * crestBand * (0.28 + 0.42 * amp);          // illuminated leading edge
    col += float3(1.0) * crestBand * (0.04 + 0.08 * amp);  // restrained specular, not a metal rim
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

    fun updateUniforms(
        width: Float,
        height: Float,
        field: VoiceEnergyField,
        composition: VoiceEnergyComposition
    ) {
        runtimeShader.setFloatUniform("iResolution", width, height)
        runtimeShader.setFloatUniform("iTime", field.elapsed)
        runtimeShader.setFloatUniform("iAmplitude", composition.amplitude)
        runtimeShader.setFloatUniform("iRipple", field.ripple)
        runtimeShader.setFloatUniform("iDrift", field.drift)
        runtimeShader.setFloatUniform("iHarmonic1", field.harmonicAmplitude[0])
        runtimeShader.setFloatUniform("iHarmonic2", field.harmonicAmplitude[1])
        runtimeShader.setFloatUniform("iHarmonic3", field.harmonicAmplitude[2])
        runtimeShader.setFloatUniform("iPhase1", field.harmonicPhase[0])
        runtimeShader.setFloatUniform("iPhase2", field.harmonicPhase[1])
        runtimeShader.setFloatUniform("iPhase3", field.harmonicPhase[2])
        runtimeShader.setFloatUniform("iBodyTop", composition.bodyTop)
        runtimeShader.setFloatUniform("iCrestScale", composition.crestScale)
        runtimeShader.setFloatUniform("iBand1Offset", composition.band1Offset)
        runtimeShader.setFloatUniform("iBand1Width", composition.band1Width)
        runtimeShader.setFloatUniform("iBand2Offset", composition.band2Offset)
        runtimeShader.setFloatUniform("iBand2Width", composition.band2Width)
        runtimeShader.setFloatUniform("iWarmth", composition.warmth)
    }
}

/**
 * Amplitude -> composition mapping, refreshed once per frame and consumed by BOTH renderers: the
 * AGSL shader receives it as uniforms and the Canvas fallback reads it directly. Keeping the mapping
 * in one place is what stops the two paths from drifting apart in geometry or coverage.
 */
private class VoiceEnergyComposition {
    /** Amplitude actually rendered, i.e. field energy scaled by the surface presence. */
    var amplitude = VoiceEnergyDesign.MIN_AMPLITUDE
        private set
    var bodyTop = VoiceEnergyDesign.BODY_TOP_CALM
        private set
    var crestScale = VoiceEnergyDesign.CREST_CALM
        private set
    var band1Offset = VoiceEnergyDesign.BAND_1_OFFSET_CALM
        private set
    var band1Width = VoiceEnergyDesign.BAND_1_WIDTH
        private set
    var band2Offset = VoiceEnergyDesign.BAND_2_OFFSET_CALM
        private set
    var band2Width = VoiceEnergyDesign.BAND_2_WIDTH
        private set
    var warmth = 0f
        private set

    fun update(field: VoiceEnergyField, presence: Float) {
        val amp = (field.energy * presence).coerceIn(0f, 1f)
        amplitude = amp
        bodyTop = VoiceEnergyDesign.BODY_TOP_CALM +
            (VoiceEnergyDesign.BODY_TOP_FULL - VoiceEnergyDesign.BODY_TOP_CALM) * amp
        crestScale = VoiceEnergyDesign.CREST_CALM +
            VoiceEnergyDesign.CREST_PER_ENERGY * amp +
            VoiceEnergyDesign.CREST_PER_RIPPLE * field.ripple
        band1Offset = VoiceEnergyDesign.BAND_1_OFFSET_CALM + VoiceEnergyDesign.BAND_1_OFFSET_PER_ENERGY * amp
        band1Width = VoiceEnergyDesign.BAND_1_WIDTH + VoiceEnergyDesign.BAND_1_WIDTH_PER_ENERGY * amp
        band2Offset = VoiceEnergyDesign.BAND_2_OFFSET_CALM + VoiceEnergyDesign.BAND_2_OFFSET_PER_ENERGY * amp
        band2Width = VoiceEnergyDesign.BAND_2_WIDTH + VoiceEnergyDesign.BAND_2_WIDTH_PER_ENERGY * amp
        warmth = smoothstep(VoiceEnergyDesign.WARMTH_GATE_LOW, VoiceEnergyDesign.WARMTH_GATE_HIGH, amp)
    }
}

private fun smoothstep(edge0: Float, edge1: Float, value: Float): Float {
    val t = ((value - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/**
 * Shared silhouette maths. A contour is the sum of one or more layers: the body crest (its mean
 * height plus the fluid harmonics) and, for the floating ribbon bands, an extra wobble component
 * riding on top of it. The Canvas fallback reproduces the shader profile term by term, so old
 * devices see the same organic geometry and the same audio reactivity as new ones.
 */
private class VoiceLayerShape {
    var baseLine = 0f
    var scale = 0f
    var xScale = 1f
    var timeScale = 1f
    var timeShift = 0f

    /** Repositions this component for the current frame, without allocating. */
    fun set(baseLine: Float, scale: Float, xScale: Float, timeScale: Float, timeShift: Float) {
        this.baseLine = baseLine
        this.scale = scale
        this.xScale = xScale
        this.timeScale = timeScale
        this.timeShift = timeShift
    }
}

/** Evaluates the composite silhouette of [layers] at horizontal fraction [x]. */
private fun contourY(x: Float, field: VoiceEnergyField, layers: List<VoiceLayerShape>): Float {
    val amplitudes = field.harmonicAmplitude
    val phases = field.harmonicPhase
    var base = 0f
    var wobble = 0f
    for (index in layers.indices) {
        val layer = layers[index]
        base += layer.baseLine
        if (layer.scale == 0f) continue
        val scaledX = x * layer.xScale
        val time = field.elapsed * layer.timeScale + layer.timeShift
        var profile = amplitudes[0] * sin(VoiceEnergyDesign.TAU * scaledX + phases[0])
        profile += amplitudes[1] * 0.55f * sin(VoiceEnergyDesign.TAU * 2f * scaledX + phases[1])
        profile += amplitudes[2] * 0.32f * sin(VoiceEnergyDesign.TAU * 3f * scaledX + phases[2])
        profile += 0.055f * field.energy * sin(VoiceEnergyDesign.TAU * 1.7f * scaledX - time * 0.55f)
        profile += 0.030f * field.energy * sin(VoiceEnergyDesign.TAU * 2.9f * scaledX + time * 0.83f)
        wobble += profile * layer.scale
    }
    return base - wobble
}

private fun buildContourPath(
    path: Path,
    field: VoiceEnergyField,
    width: Float,
    height: Float,
    layers: List<VoiceLayerShape>,
    startFraction: Float = 0f,
    endFraction: Float = 1f,
    closeToBottom: Boolean = false
) {
    val samples = VoiceEnergyDesign.FALLBACK_SAMPLES
    path.reset()
    val startX = width * startFraction
    path.moveTo(startX, contourY(startFraction, field, layers))
    for (i in 1..samples) {
        val step = startFraction + (endFraction - startFraction) * (i.toFloat() / samples.toFloat())
        path.lineTo(width * step, contourY(step, field, layers))
    }
    if (closeToBottom) {
        path.lineTo(width * endFraction, height)
        path.lineTo(startX, height)
        path.close()
    }
}

/**
 * Preallocated drawing resources for the Canvas fallback (API 26..32). Everything that would be
 * recreated every frame (paths, strokes, gradients, contour shapes) is built once per density.
 *
 * The fallback reproduces the shader composition with gradient brushes instead of per-fragment
 * maths: the body is filled with a horizontal hue sweep plus a coloured depth glaze, and the two
 * ribbon bands are wide translucent strokes that fade softly at their edges.
 */
private class VoiceEnergyFallbackArt(density: Density) {
    val primaryPath = Path()
    val bandPaths: List<Path> = List(2) { Path() }

    private val bodyShape = VoiceLayerShape()
    private val band1Shape = VoiceLayerShape()
    private val band2Shape = VoiceLayerShape()
    val bodyLayers: List<VoiceLayerShape> = listOf(bodyShape)
    val band1Layers: List<VoiceLayerShape> = listOf(bodyShape, band1Shape)
    val band2Layers: List<VoiceLayerShape> = listOf(bodyShape, band2Shape)

    /** Body hues, sweeping across the composer width. */
    val calmFill: Brush = Brush.horizontalGradient(colorStops = calmEnergyStops())
    val warmFill: Brush = Brush.horizontalGradient(colorStops = expressiveEnergyStops())

    /** Coloured depth glaze: the interior gets deeper toward the floor instead of grey. */
    val depthGlaze: Brush = Brush.verticalGradient(
        colorStops = arrayOf(
            0.40f to Color.Transparent,
            0.75f to VoiceEnergyColors.DeepIndigo.copy(alpha = 0.30f),
            1.00f to VoiceEnergyColors.DeepIndigo.copy(alpha = 0.52f)
        )
    )

    /** Crest illumination: a bright core inside two dimmer haloes. */
    val crestStrokes: List<Stroke> = listOf(14.dp, 7.dp, 2.5.dp).map { widthDp ->
        Stroke(width = with(density) { widthDp.toPx() })
    }
    val crestAlphas: List<Float> = listOf(0.07f, 0.13f, 0.22f)
    val specularStroke: Stroke = Stroke(width = with(density) { 1.5.dp.toPx() })

    /** Ribbon bands: a wide translucent halo plus a brighter core, one set per band. */
    val bandStrokes: List<List<Stroke>> = listOf(
        listOf(14.dp, 8.dp).map { widthDp -> Stroke(width = with(density) { widthDp.toPx() }) },
        listOf(26.dp, 14.dp).map { widthDp -> Stroke(width = with(density) { widthDp.toPx() }) }
    )
    val bandAlphas: List<Float> = listOf(0.075f, 0.085f)

    /** Maps the shared composition onto this frame's contour shapes. */
    fun updateShapes(height: Float, composition: VoiceEnergyComposition) {
        bodyShape.set(height * composition.bodyTop, height * composition.crestScale, 1f, 1f, 0f)
        band1Shape.set(
            -height * composition.band1Offset,
            height * composition.crestScale * VoiceEnergyDesign.LAYER_1_SHAPE,
            VoiceEnergyDesign.LAYER_1_X_SCALE,
            VoiceEnergyDesign.LAYER_1_TIME_SCALE,
            VoiceEnergyDesign.RIBBON_TIME_SHIFT_1
        )
        band2Shape.set(
            -height * composition.band2Offset,
            height * composition.crestScale * VoiceEnergyDesign.LAYER_2_SHAPE,
            VoiceEnergyDesign.LAYER_2_X_SCALE,
            VoiceEnergyDesign.LAYER_2_TIME_SCALE,
            VoiceEnergyDesign.RIBBON_TIME_SHIFT_2
        )
    }
}

/** Gradients and contour shapes for the ambient band above the composer, built once. */
private class VoiceAmbientArt(density: Density) {
    val wavePaths: List<Path> = List(3) { Path() }

    private val waveShapes: List<VoiceLayerShape> = List(3) { VoiceLayerShape() }
    val waveLayers: List<List<VoiceLayerShape>> = waveShapes.map { listOf(it) }

    /** Each wave spans a different slice of the width, so the light stays local to the composer. */
    val waveStart = floatArrayOf(0.04f, 0.12f, 0.20f)
    val waveEnd = floatArrayOf(0.96f, 0.88f, 0.80f)

    /** Three widths per wave: a wide faint halo, a mid glow and a brighter core. */
    val waveStrokes: List<Stroke> = listOf(30.dp, 16.dp, 6.dp).map { widthDp ->
        Stroke(width = with(density) { widthDp.toPx() })
    }
    val waveAlphas: List<Float> = listOf(0.05f, 0.085f, 0.125f)

    val calmBrush: Brush = Brush.horizontalGradient(colorStops = calmEnergyStops())
    val warmBrush: Brush = Brush.horizontalGradient(colorStops = expressiveEnergyStops())

    /** Soft pools of light just above the composer, so the wash has a visible source. */
    val calmPool: Brush = Brush.radialGradient(
        colorStops = arrayOf(
            0.00f to VoiceEnergyColors.Blue.copy(alpha = 0.60f),
            0.38f to VoiceEnergyColors.Violet.copy(alpha = 0.28f),
            0.72f to VoiceEnergyColors.DeepIndigo.copy(alpha = 0.10f),
            1.00f to Color.Transparent
        )
    )
    val warmPool: Brush = Brush.radialGradient(
        colorStops = arrayOf(
            0.00f to VoiceEnergyColors.Coral.copy(alpha = 0.55f),
            0.36f to VoiceEnergyColors.Amber.copy(alpha = 0.28f),
            0.74f to VoiceEnergyColors.Green.copy(alpha = 0.10f),
            1.00f to Color.Transparent
        )
    )

    /** Lifts each wave as the voice gets louder: the crests rise toward the message area. */
    fun updateShapes(height: Float, energy: Float) {
        for (index in waveShapes.indices) {
            waveShapes[index].set(
                baseLine = height * (VoiceEnergyDesign.AMBIENT_BASE -
                    VoiceEnergyDesign.AMBIENT_LIFT * energy -
                    VoiceEnergyDesign.AMBIENT_LAYER_STEP * index),
                scale = height * (VoiceEnergyDesign.AMBIENT_SCALE_CALM +
                    VoiceEnergyDesign.AMBIENT_SCALE_ENERGY * energy) *
                    (1f - VoiceEnergyDesign.AMBIENT_LAYER_SCALE_STEP * index),
                xScale = VoiceEnergyDesign.AMBIENT_X_SCALE,
                timeScale = VoiceEnergyDesign.AMBIENT_TIME_SCALE,
                timeShift = VoiceEnergyDesign.AMBIENT_TIME_SHIFT +
                    VoiceEnergyDesign.AMBIENT_TIME_SHIFT_STEP * index
            )
        }
    }
}

private fun calmEnergyStops(): Array<Pair<Float, Color>> = arrayOf(
    0.00f to VoiceEnergyColors.DeepIndigo,
    0.20f to VoiceEnergyColors.Blue,
    0.42f to VoiceEnergyColors.Violet,
    0.64f to VoiceEnergyColors.Cyan,
    0.84f to VoiceEnergyColors.Blue,
    1.00f to VoiceEnergyColors.Violet
)

private fun expressiveEnergyStops(): Array<Pair<Float, Color>> = arrayOf(
    0.00f to VoiceEnergyColors.Blue,
    0.16f to VoiceEnergyColors.Violet,
    0.34f to VoiceEnergyColors.Cyan,
    0.52f to VoiceEnergyColors.Coral,
    0.70f to VoiceEnergyColors.Amber,
    0.86f to VoiceEnergyColors.Green,
    1.00f to VoiceEnergyColors.Pink
)

private fun DrawScope.drawVoiceEnergyFallback(
    field: VoiceEnergyField,
    art: VoiceEnergyFallbackArt,
    composition: VoiceEnergyComposition
) {
    val width = size.width
    val height = size.height
    if (width <= 0f || height <= 0f) return

    art.updateShapes(height, composition)

    val energy = composition.amplitude
    val warmth = composition.warmth
    val rippleBoost = 0.75f + 0.35f * field.ripple

    // The two ribbon bands floating above the body crest. Each one rides on the body silhouette but
    // wobbles at its own frequency and speed, so the three contours never move as one block.
    for (index in art.bandPaths.indices) {
        val path = art.bandPaths[index]
        val layers = if (index == 0) art.band1Layers else art.band2Layers
        buildContourPath(path, field, width, height, layers)
        val bandAlpha = (VoiceEnergyDesign.BAND_ALPHA_FLOOR +
            VoiceEnergyDesign.BAND_ALPHA_PER_ENERGY * energy) *
            (1f - VoiceEnergyDesign.BAND_ALPHA_LAYER_STEP * index)
        art.bandStrokes[index].forEachIndexed { strokeIndex, stroke ->
            val alpha = art.bandAlphas[strokeIndex] * bandAlpha
            if (alpha <= 0.002f) return@forEachIndexed
            drawPath(path, brush = art.calmFill, alpha = alpha, style = stroke)
            if (warmth > 0.01f) drawPath(path, brush = art.warmFill, alpha = alpha * warmth, style = stroke)
        }
    }

    // The primary energy body, filled from its own crest down to the floor.
    buildContourPath(
        path = art.primaryPath,
        field = field,
        width = width,
        height = height,
        layers = art.bodyLayers,
        closeToBottom = true
    )

    val fillAlpha = 0.58f + 0.30f * energy
    clipPath(art.primaryPath) {
        drawRect(brush = art.calmFill, alpha = fillAlpha)
        drawRect(brush = art.depthGlaze, alpha = 0.85f)
        // The warm accents stay low in the field, mirroring the shader's vertical stratification:
        // cool blues and violets at the top, coral and amber toward the base.
        if (warmth > 0.01f) {
            clipRect(top = height * VoiceEnergyDesign.WARM_STRATUM_TOP) {
                drawRect(brush = art.warmFill, alpha = fillAlpha * warmth)
            }
        }
    }

    // Illuminated crest: a bright core inside wider haloes, plus a restrained specular line.
    art.crestStrokes.forEachIndexed { index, stroke ->
        val alpha = art.crestAlphas[index] * rippleBoost * (0.60f + 0.50f * energy)
        if (alpha <= 0.002f) return@forEachIndexed
        drawPath(art.primaryPath, brush = art.calmFill, alpha = alpha, style = stroke)
        if (warmth > 0.01f) {
            drawPath(art.primaryPath, brush = art.warmFill, alpha = alpha * warmth * 0.55f, style = stroke)
        }
    }
    drawPath(
        path = art.primaryPath,
        brush = SolidColor(VoiceEnergyColors.Specular),
        alpha = 0.03f + 0.07f * energy,
        style = art.specularStroke
    )
}

/**
 * Ambient band above the composer. The waves are built from the same fluid harmonics as the pill's
 * own surface - only stretched horizontally and lifted - so the light above the composer reads as
 * the continuation of the surface inside it instead of a separate effect.
 */
private fun DrawScope.drawVoiceAmbientWaves(
    field: VoiceEnergyField,
    art: VoiceAmbientArt,
    composition: VoiceEnergyComposition
) {
    val width = size.width
    val height = size.height
    if (width <= 0f || height <= 0f) return

    val energy = composition.amplitude
    val warmth = composition.warmth
    val drift = field.drift

    // A localized source of light just above the composer keeps the wash anchored to the pill.
    val poolRadius = maxOf(
        width * VoiceEnergyDesign.AMBIENT_POOL_RADIUS_WIDTH,
        height * VoiceEnergyDesign.AMBIENT_POOL_RADIUS_HEIGHT
    ) * (VoiceEnergyDesign.AMBIENT_POOL_RADIUS_CALM + VoiceEnergyDesign.AMBIENT_POOL_RADIUS_ENERGY * energy)
    val poolAlpha = VoiceEnergyDesign.AMBIENT_POOL_ALPHA +
        VoiceEnergyDesign.AMBIENT_POOL_ALPHA_ENERGY * energy
    val poolLeft = Offset(width * (0.26f + 0.06f * sin(drift * 0.90f)), height * 0.94f)
    val poolRight = Offset(width * (0.74f - 0.06f * sin(drift * 1.05f)), height * 0.94f)
    drawCircle(brush = art.calmPool, center = poolLeft, radius = poolRadius, alpha = poolAlpha)
    drawCircle(brush = art.calmPool, center = poolRight, radius = poolRadius, alpha = poolAlpha * 0.85f)
    if (warmth > 0.01f) {
        drawCircle(brush = art.warmPool, center = poolLeft, radius = poolRadius, alpha = poolAlpha * warmth * 0.9f)
        drawCircle(brush = art.warmPool, center = poolRight, radius = poolRadius, alpha = poolAlpha * warmth * 0.7f)
    }

    art.updateShapes(height, energy)
    for (index in art.wavePaths.indices) {
        val path = art.wavePaths[index]
        // Wave 0 hugs the composer and is the brightest; the higher waves are dimmer and shorter, so
        // the illumination lifts away from the pill and fades before it reaches the message text.
        buildContourPath(
            path = path,
            field = field,
            width = width,
            height = height,
            layers = art.waveLayers[index],
            startFraction = art.waveStart[index],
            endFraction = art.waveEnd[index]
        )
        val waveAlpha = (VoiceEnergyDesign.AMBIENT_ALPHA_FLOOR +
            VoiceEnergyDesign.AMBIENT_ALPHA_ENERGY * energy) *
            (1f - VoiceEnergyDesign.AMBIENT_ALPHA_LAYER_STEP * index)
        art.waveStrokes.forEachIndexed { strokeIndex, stroke ->
            val alpha = art.waveAlphas[strokeIndex] * waveAlpha
            if (alpha <= 0.002f) return@forEachIndexed
            val tint = if (index == 2) art.warmBrush else art.calmBrush
            drawPath(path, brush = tint, alpha = alpha, style = stroke)
            if (warmth > 0.01f) drawPath(path, brush = art.warmBrush, alpha = alpha * warmth * 0.75f, style = stroke)
        }
    }
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
 * Single frame-driven animation loop per field instance. The loop is keyed on the voice stage and
 * the reduced-motion flag only, so it is never restarted for a changing amplitude value, and it is
 * cancelled when the component is inactive or disposed. The frame timestamp doubles as the shared
 * phase clock, which is what keeps the composer surface and the ambient band synchronized.
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
                    paused = latestPaused,
                    absoluteSeconds = frameNanos / 1_000_000_000f
                )
            }
            previousFrameNanos = frameNanos
        }
    }
    return field
}

/**
 * The composer's energy surface. It fills the interior of the pill: the body crest sits around 38%
 * of the height at silence and 53% at full voice, with two translucent ribbon bands floating above
 * it. The specular rim is deliberately restrained so the energy, not the border, is the centrepiece.
 */
@Composable
fun VoiceRecordingGlowSurface(
    voiceState: VoiceState,
    reducedMotionOverride: Boolean,
    modifier: Modifier = Modifier
) {
    val active = voiceState.isGlowActive()
    // A recorded draft keeps the energy surface - the same spectrum, held at a calmer intensity -
    // so the voice bar stays part of one visual language after the finger lifts.
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
    val composition = remember { VoiceEnergyComposition() }

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
                VoiceEnergyColors.Cyan.copy(alpha = 0.55f),
                VoiceEnergyColors.Violet.copy(alpha = 0.42f)
            )
        )
    }
    val rimStroke = remember(density) { Stroke(width = with(density) { 1.2.dp.toPx() }) }

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

            composition.update(field, presence)
            if (shader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                shader.updateUniforms(width, height, field, composition)
                drawRoundRect(brush = shader.shaderBrush, cornerRadius = corner, alpha = presence)
            } else if (fallbackArt != null) {
                drawVoiceEnergyFallback(field, fallbackArt, composition)
            }

            // A soft coloured edge only: the metallic rim used to compete with the energy field.
            val borderAlpha = presence * (0.12f + 0.16f * composition.amplitude)
            val strokePx = with(density) { 1.2.dp.toPx() }
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

/**
 * Ambient energy above the composer. Rendered behind the chat content and localized to the bottom
 * of the message area: three organic waves rise from the pill, each one lower and dimmer than the
 * next, carrying the same spectrum as the surface inside it.
 */
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
    val density = LocalDensity.current
    val haloArt = remember(density) { VoiceAmbientArt(density) }
    val composition = remember { VoiceEnergyComposition() }

    Canvas(modifier) {
        composition.update(field, presence = 1f)
        drawVoiceAmbientWaves(field, haloArt, composition)
    }
}
