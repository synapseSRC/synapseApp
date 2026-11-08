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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
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
import kotlin.math.PI
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
uniform float iWarmth;
// Volumetric light: how far the light diffuses upward from the crest, the narrow halo just above
// it, the soft pool along the bottom edge and the emissive crest bloom.
uniform float iDiffusionCalm;
uniform float iDiffusionExpressive;
uniform float iHaloWidth;
uniform float iHaloStrength;
uniform float iFloorDiffusion;
uniform float iFloorGlow;
uniform float iBloomWidth;
uniform float iBloomCalm;
uniform float iBloomExpressive;
uniform float iBloomWhite;
// Colour sweep along the crest: one broad, low-frequency migration that widens with the voice.
uniform float iHueBase;
uniform float iHueSpanCalm;
uniform float iHueSpanExpressive;
uniform float iVolumeCalm;
// Light inside the fluid, as a fraction of the light it emits upward. Keeps the boundary from
// reading as a step between lit and unlit.
uniform float iVolumeInside;
uniform float iVolumeAlpha;
uniform float iLightGain;
// The idle undulation amount, owned by VoiceEnergyDesign so both renderers use the same value.
uniform float iIdleCalm;
uniform float iIdleEnergy;
uniform float4 cBlue;
uniform float4 cViolet;
uniform float4 cCyan;
uniform float4 cCoral;
uniform float4 cAmber;
uniform float4 cGreen;
uniform float4 cPink;

// Organic silhouette. The harmonics are the live output of the fluid simulation. The profile is
// evaluated on a slowly bending axis, which is what makes the contours lean, pinch and stay
// asymmetric instead of reading as a ruler-straight line, and a slow idle term keeps the surface
// undulating when the microphone is quiet without ever inventing an amplitude value.
float surfaceProfile(float x, float t, float amp) {
    const float TAU = 6.2831853;
    float warped = x
        + 0.075 * sin(TAU * (x * 0.62 - t * 0.13))
        + 0.035 * sin(TAU * (x * 1.90 + t * 0.21));
    float d = iHarmonic1 * sin(TAU * warped + iPhase1);
    d += iHarmonic2 * 0.55 * sin(TAU * 2.0 * warped + iPhase2);
    d += iHarmonic3 * 0.32 * sin(TAU * 3.0 * warped + iPhase3);
    d += (iIdleCalm + iIdleEnergy * amp)
        * sin(TAU * (warped * 0.9 + 0.22 * sin(TAU * warped)) - t * 0.19);
    d += 0.10 * amp * sin(TAU * 1.7 * warped - t * 0.55);
    d += 0.055 * amp * sin(TAU * 2.9 * warped + t * 0.83);
    return d;
}

// Cyclic spectrum, ordered around the colour wheel: blue, cyan, green, amber, coral, pink, violet and
// back to blue. Every stop is a neighbour of the next, so adjacent samples always blend into a
// saturated colour. The previous version mixed a cool gradient with a warm one per fragment, and
// across that blend blue meets amber - which is exactly the desaturated grey that made the capsule
// look like brushed metal.
float3 energySpectrum(float u) {
    u = fract(u);
    float3 c = cBlue.rgb;
    c = mix(c, cCyan.rgb, smoothstep(0.00, 0.15, u));
    c = mix(c, cGreen.rgb, smoothstep(0.10, 0.31, u));
    c = mix(c, cAmber.rgb, smoothstep(0.26, 0.50, u));
    c = mix(c, cCoral.rgb, smoothstep(0.45, 0.66, u));
    c = mix(c, cPink.rgb, smoothstep(0.62, 0.80, u));
    c = mix(c, cViolet.rgb, smoothstep(0.76, 0.90, u));
    c = mix(c, cBlue.rgb, smoothstep(0.88, 1.00, u));
    return c;
}

half4 main(in float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float x = clamp(uv.x, 0.0, 1.0);
    float y = clamp(uv.y, 0.0, 1.0);
    float amp = clamp(iAmplitude, 0.0, 1.0);
    // 0 = calm personality (blue and violet, minimal deformation), 1 = expressive (full Google
    // spectrum, pronounced freeform boundary). Driven by the real microphone envelope.
    float expression = clamp(iWarmth, 0.0, 1.0);
    float travel = iTime * 0.055 + iDrift * 0.02;

    // Slow migration of the whole hue assignment, so the volume keeps re-mixing on a held note.
    float migrate = 0.06 * sin(travel * 0.37);

    // ---- Fluid boundary -------------------------------------------------------------------
    // ONE organic, asymmetric crest. The deformation grows with the voice, so the boundary is nearly
    // still when the recording is calm and unmistakably freeform when it is expressive.
    float crest = iBodyTop - surfaceProfile(x, iTime, amp) * iCrestScale;

    // ---- Volumetric light -----------------------------------------------------------------
    // Light is emitted upward from the crest and diffuses into black; the bottom edge carries a soft
    // pool. Nothing here is drawn as a line: this is the reference treatment, where the volume is lit
    // from its boundary and the colour lives inside the glow.
    float above = max(crest - y, 0.0);
    float below = max(y - crest, 0.0);
    float diffusion = iDiffusionCalm + (iDiffusionExpressive - iDiffusionCalm) * expression;
    float emitted = exp(-above / diffusion);
    float halo = exp(-above / iHaloWidth);
    // Light inside the fluid decays away from its surface, so it only exists below the crest - and the
    // gate is smooth, because a step here would put a visible seam back on the boundary.
    float inside = exp(-below / diffusion) * smoothstep(0.0, 0.012, below);
    // Pool of light along the bottom edge of the pill, which is where the volume is deepest.
    float floorGlow = exp(-(1.0 - y) / iFloorDiffusion);
    float bloom = exp(-abs(y - crest) / (iBloomWidth + 0.015 * expression));

    float volume = emitted * iVolumeCalm + inside * iVolumeInside + halo * iHaloStrength
        + floorGlow * iFloorGlow
        + bloom * (iBloomCalm + iBloomExpressive * expression);
    volume = clamp(volume, 0.0, 1.0);

    // ---- Colour ---------------------------------------------------------------------------
    // A single broad migration along the crest at low spatial frequency: violet and blue when calm,
    // and the whole Google spectrum when expressive. Each sample is a neighbour of the next on the
    // wheel, so the regions blend into one another instead of averaging out to grey - and the warm
    // colours are part of the volume, not a stripe along its edge.
    float span = iHueSpanCalm + (iHueSpanExpressive - iHueSpanCalm) * expression;
    float u = iHueBase - x * span
        + migrate
        + 0.035 * sin(travel * 0.27)
        + 0.06 * (y - 0.5) * expression;
    float3 light = energySpectrum(u);
    // The crest blooms slightly whiter, like an emissive edge. Restrained, never a neon outline.
    light = mix(light, float3(1.0), bloom * iBloomWhite);

    // ---- Composite ------------------------------------------------------------------------
    float alpha = volume * iVolumeAlpha;
    if (alpha <= 0.002) {
        return half4(0.0, 0.0, 0.0, 0.0);
    }
    float3 col = light * volume * iLightGain;
    return half4(clamp(col, 0.0, 1.0), alpha);
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
        runtimeShader.setFloatUniform("iWarmth", composition.warmth)
        runtimeShader.setFloatUniform("iDiffusionCalm", VoiceEnergyDesign.DIFFUSION_CALM)
        runtimeShader.setFloatUniform("iDiffusionExpressive", VoiceEnergyDesign.DIFFUSION_EXPRESSIVE)
        runtimeShader.setFloatUniform("iHaloWidth", VoiceEnergyDesign.HALO_WIDTH)
        runtimeShader.setFloatUniform("iHaloStrength", VoiceEnergyDesign.HALO_STRENGTH)
        runtimeShader.setFloatUniform("iFloorDiffusion", VoiceEnergyDesign.FLOOR_DIFFUSION)
        runtimeShader.setFloatUniform("iFloorGlow", VoiceEnergyDesign.FLOOR_GLOW)
        runtimeShader.setFloatUniform("iBloomWidth", VoiceEnergyDesign.BLOOM_WIDTH)
        runtimeShader.setFloatUniform("iBloomCalm", VoiceEnergyDesign.BLOOM_CALM)
        runtimeShader.setFloatUniform("iBloomExpressive", VoiceEnergyDesign.BLOOM_EXPRESSIVE)
        runtimeShader.setFloatUniform("iBloomWhite", VoiceEnergyDesign.BLOOM_WHITE)
        runtimeShader.setFloatUniform("iHueBase", VoiceEnergyDesign.HUE_BASE)
        runtimeShader.setFloatUniform("iHueSpanCalm", VoiceEnergyDesign.HUE_SPAN_CALM)
        runtimeShader.setFloatUniform("iHueSpanExpressive", VoiceEnergyDesign.HUE_SPAN_EXPRESSIVE)
        runtimeShader.setFloatUniform("iVolumeCalm", VoiceEnergyDesign.VOLUME_CALM)
        runtimeShader.setFloatUniform("iVolumeInside", VoiceEnergyDesign.VOLUME_INSIDE)
        runtimeShader.setFloatUniform("iVolumeAlpha", VoiceEnergyDesign.VOLUME_ALPHA)
        runtimeShader.setFloatUniform("iLightGain", VoiceEnergyDesign.LIGHT_GAIN)
        runtimeShader.setFloatUniform("iIdleCalm", VoiceEnergyDesign.IDLE_WAVE_CALM)
        runtimeShader.setFloatUniform("iIdleEnergy", VoiceEnergyDesign.IDLE_WAVE_PER_ENERGY)
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

    /**
     * 0 = calm personality, 1 = expressive. This is the only knob the two personalities need: the
     * renderer derives its diffusion, its colour sweep and its deformation from it.
     */
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
        // Mirrors the shader's surfaceProfile term by term: the same bending axis, the same idle
        // undulation and the same two energy-locked drift terms, so the two renderers agree.
        val warped = scaledX +
            0.075f * sin(VoiceEnergyDesign.TAU * (scaledX * 0.62f - time * 0.13f)) +
            0.035f * sin(VoiceEnergyDesign.TAU * (scaledX * 1.90f + time * 0.21f))
        var profile = amplitudes[0] * sin(VoiceEnergyDesign.TAU * warped + phases[0])
        profile += amplitudes[1] * 0.55f * sin(VoiceEnergyDesign.TAU * 2f * warped + phases[1])
        profile += amplitudes[2] * 0.32f * sin(VoiceEnergyDesign.TAU * 3f * warped + phases[2])
        profile += (VoiceEnergyDesign.IDLE_WAVE_CALM + VoiceEnergyDesign.IDLE_WAVE_PER_ENERGY * field.energy) *
            sin(VoiceEnergyDesign.TAU * (warped * 0.9f + 0.22f * sin(VoiceEnergyDesign.TAU * warped)) -
                time * 0.19f)
        profile += 0.10f * field.energy * sin(VoiceEnergyDesign.TAU * 1.7f * warped - time * 0.55f)
        profile += 0.055f * field.energy * sin(VoiceEnergyDesign.TAU * 2.9f * warped + time * 0.83f)
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
 * Preallocated drawing resources for the Canvas fallback (API 26..32).
 *
 * The fallback reproduces the shader's volumetric model with gradients instead of per-fragment maths:
 * the region below the crest is a dark fluid volume, and the light above it is a stack of nested
 * strokes on the crest contour - widest and faintest first - which yields the same smooth upward
 * diffusion without ever drawing a line.
 */
private class VoiceEnergyFallbackArt(density: Density) {
    val crestPath = Path()
    private val crestShape = VoiceLayerShape()
    val crestLayers: List<VoiceLayerShape> = listOf(crestShape)

    /** The fluid volume under the boundary: near-black, with a soft pool of light at its base. */
    val bodyVolume: Brush = Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to VoiceEnergyColors.VolumeBlack,
            0.52f to VoiceEnergyColors.VolumeBlack,
            0.86f to VoiceEnergyColors.DeepIndigo.copy(alpha = 0.50f),
            1.00f to VoiceEnergyColors.Blue.copy(alpha = 0.28f)
        )
    )

    /** Upward diffusion from the boundary: five nested widths, faintest and widest first. */
    val lightStrokes: List<Stroke> = listOf(52.dp, 34.dp, 20.dp, 11.dp, 5.dp).map { widthDp ->
        Stroke(width = with(density) { widthDp.toPx() }, cap = StrokeCap.Round)
    }
    val lightAlphas: List<Float> = listOf(0.16f, 0.24f, 0.34f, 0.48f, 0.62f)

    /** Emissive edge: tight, slightly whiter, and far from a neon outline. */
    val bloomStroke: Stroke = Stroke(width = with(density) { 3.dp.toPx() }, cap = StrokeCap.Round)

    /**
     * One colour sweep per expression bucket. Picking a prebuilt sweep rather than cross-fading two
     * of them is deliberate: blending a blue sweep with an amber one is what turns a light field grey.
     */
    val lightFills: List<Brush> = lightSweepBrushes(fadedEnds = false, whiten = 0f)
    val bloomFills: List<Brush> = lightSweepBrushes(fadedEnds = false, whiten = VoiceEnergyDesign.BLOOM_WHITE)

    /** Maps the shared composition onto this frame's boundary shape. */
    fun updateShapes(height: Float, composition: VoiceEnergyComposition) {
        crestShape.set(height * composition.bodyTop, height * composition.crestScale, 1f, 1f, 0f)
    }

    /** Which prebuilt sweep matches the current expression value. */
    fun sweepIndex(expression: Float): Int =
        (expression.coerceIn(0f, 1f) * (VoiceEnergyDesign.SWEEP_BUCKETS - 1).toFloat()).roundToInt()
}

/**
 * Samples the same colour wheel the shader walks, stop for stop. Keeping this identical is what makes
 * the fallback's hues match the AGSL path instead of being a separate approximation.
 */
private fun energySpectrumColor(u: Float): Color {
    val x = u.mod(1f)
    var color = VoiceEnergyColors.Blue
    color = lerp(color, VoiceEnergyColors.Cyan, smoothstep(0.00f, 0.15f, x))
    color = lerp(color, VoiceEnergyColors.Green, smoothstep(0.10f, 0.31f, x))
    color = lerp(color, VoiceEnergyColors.Amber, smoothstep(0.26f, 0.50f, x))
    color = lerp(color, VoiceEnergyColors.Coral, smoothstep(0.45f, 0.66f, x))
    color = lerp(color, VoiceEnergyColors.Pink, smoothstep(0.62f, 0.80f, x))
    color = lerp(color, VoiceEnergyColors.Violet, smoothstep(0.76f, 0.90f, x))
    color = lerp(color, VoiceEnergyColors.Blue, smoothstep(0.88f, 1.00f, x))
    return color
}

/**
 * Prebuilds the sweep brushes, one per expression bucket: at calm personality the sweep covers only
 * violet..blue, at full expression it walks the whole wheel from violet round to cyan. The warm
 * colours therefore *emerge* from the volume as the voice rises, which is the reference behaviour.
 *
 * [fadedEnds] fades a brush to transparent at both ends, which is what lets the ambient strokes
 * dissolve into the chat background instead of stopping on a hard vertical edge.
 */
private fun lightSweepBrushes(fadedEnds: Boolean, whiten: Float): List<Brush> {
    val buckets = VoiceEnergyDesign.SWEEP_BUCKETS
    val stops = VoiceEnergyDesign.SWEEP_STOPS
    return List(buckets) { bucket ->
        val expression = if (buckets <= 1) 0f else bucket.toFloat() / (buckets - 1).toFloat()
        val span = VoiceEnergyDesign.HUE_SPAN_CALM +
            (VoiceEnergyDesign.HUE_SPAN_EXPRESSIVE - VoiceEnergyDesign.HUE_SPAN_CALM) * expression
        Brush.horizontalGradient(
            colorStops = Array(stops) { index ->
                val t = if (stops <= 1) 0f else index.toFloat() / (stops - 1).toFloat()
                var color = energySpectrumColor(VoiceEnergyDesign.HUE_BASE - t * span)
                if (whiten > 0f) color = lerp(color, Color.White, whiten)
                if (fadedEnds) color = color.copy(alpha = color.alpha * sin(PI * t).toFloat())
                t to color
            }
        )
    }
}

/** Gradients and contour shapes for the ambient band above the composer, built once. */
/** Drawing resources for the light above the composer, built once per density. */
private class VoiceAmbientArt(density: Density) {
    val crestPath = Path()
    private val crestShape = VoiceLayerShape()
    val crestLayers: List<VoiceLayerShape> = listOf(crestShape)

    /**
     * The same nested-stroke diffusion the composer uses, wider and fainter, with ends that fade to
     * transparent: the ambient is the pill's light spilling upward into the black, not a set of
     * ribbons, and a stroke can never terminate on a visible edge.
     */
    val lightStrokes: List<Stroke> = listOf(58.dp, 38.dp, 22.dp, 12.dp).map { widthDp ->
        Stroke(width = with(density) { widthDp.toPx() }, cap = StrokeCap.Round)
    }
    val lightAlphas: List<Float> = listOf(0.10f, 0.16f, 0.24f, 0.34f)
    val lightFills: List<Brush> = lightSweepBrushes(fadedEnds = true, whiten = 0f)

    /** Rides on the composer's own boundary shape, lifted by voice energy. */
    fun updateShapes(height: Float, composition: VoiceEnergyComposition) {
        crestShape.set(
            baseLine = height * (VoiceEnergyDesign.AMBIENT_BASE -
                VoiceEnergyDesign.AMBIENT_LIFT * composition.amplitude),
            scale = height * (VoiceEnergyDesign.AMBIENT_SCALE_CALM +
                VoiceEnergyDesign.AMBIENT_SCALE_ENERGY * composition.amplitude),
            xScale = VoiceEnergyDesign.AMBIENT_X_SCALE,
            timeScale = VoiceEnergyDesign.AMBIENT_TIME_SCALE,
            timeShift = VoiceEnergyDesign.AMBIENT_TIME_SHIFT
        )
    }

    fun sweepIndex(expression: Float): Int =
        (expression.coerceIn(0f, 1f) * (VoiceEnergyDesign.SWEEP_BUCKETS - 1).toFloat()).roundToInt()
}

/**
 * The composer surface, Canvas path. The pill itself is a near-black volume; the colour is light
 * emitted from the fluid boundary and diffused upward, so nothing reads as a line drawn over a button.
 */
private fun DrawScope.drawVoiceEnergyFallback(
    field: VoiceEnergyField,
    art: VoiceEnergyFallbackArt,
    composition: VoiceEnergyComposition,
    presence: Float
) {
    val width = size.width
    val height = size.height
    if (width <= 0f || height <= 0f) return

    art.updateShapes(height, composition)

    val expression = composition.warmth
    val bucket = art.sweepIndex(expression)
    val rippleBoost = 0.85f + 0.25f * field.ripple

    // Light first: nested strokes on the boundary contour, widest and faintest first. It hugs the
    // crest and fades into the black above it; there is no second contour and no ribbon.
    val volume = VoiceEnergyDesign.VOLUME_CALM + VoiceEnergyDesign.VOLUME_EXPRESSIVE * expression
    art.lightStrokes.forEachIndexed { index, stroke ->
        val alpha = art.lightAlphas[index] * volume * rippleBoost * presence
        if (alpha <= 0.002f) return@forEachIndexed
        drawPath(art.crestPath, brush = art.lightFills[bucket], alpha = alpha, style = stroke)
    }

    // Emissive boundary: the same light, tighter and slightly whiter. Straddles the contour, so it
    // softens the boundary instead of drawing a line on it.
    drawPath(
        path = art.crestPath,
        brush = art.bloomFills[bucket],
        alpha = (VoiceEnergyDesign.BLOOM_CALM + VoiceEnergyDesign.BLOOM_EXPRESSIVE * expression) *
            rippleBoost * presence,
        style = art.bloomStroke
    )

    // The fluid body is then veiled over the light: brighter above the boundary than inside it, which
    // is what makes the boundary read as a surface. A pool of light stays visible at its base.
    buildContourPath(
        path = art.crestPath,
        field = field,
        width = width,
        height = height,
        layers = art.crestLayers,
        closeToBottom = true
    )
    drawPath(
        path = art.crestPath,
        brush = art.bodyVolume,
        alpha = presence * VoiceEnergyDesign.BODY_VEIL_ALPHA
    )
}

/**
 * Ambient light above the composer, Canvas path: the composer's own diffusion, widened, faded and
 * clipped to the lower part of the band, so it reads as the pill glowing into the dark instead of a
 * colored streak crossing the chat.
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
    val expression = composition.warmth
    val bucket = art.sweepIndex(expression)

    clipRect(top = height * VoiceEnergyDesign.AMBIENT_CLIP_TOP) {
        art.updateShapes(height, composition)
        buildContourPath(art.crestPath, field, width, height, art.crestLayers)
        val volume = VoiceEnergyDesign.AMBIENT_ALPHA_FLOOR +
            VoiceEnergyDesign.AMBIENT_ALPHA_ENERGY * energy +
            VoiceEnergyDesign.AMBIENT_ALPHA_EXPRESSION * expression
        art.lightStrokes.forEachIndexed { index, stroke ->
            val alpha = art.lightAlphas[index] * volume
            if (alpha <= 0.002f) return@forEachIndexed
            drawPath(art.crestPath, brush = art.lightFills[bucket], alpha = alpha, style = stroke)
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
 * The composer's energy surface: a lit volume inside the pill rather than a coloured graphic on top
 * of it. The pill itself goes almost black and the light is emitted from one organic fluid boundary
 * and diffuses upward. Calm keeps a small, high boundary with a blue/violet volume; as the voice
 * rises the boundary drops, deforms further and the whole spectrum appears in the light.
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
            // A deep black volume: the colour is light inside it, not paint on a card surface.
            drawRoundRect(
                color = VoiceEnergyColors.VolumeBlack,
                cornerRadius = corner,
                alpha = presence * VoiceEnergyDesign.VOLUME_BLACK_ALPHA
            )
            if (shader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                shader.updateUniforms(width, height, field, composition)
                drawRoundRect(brush = shader.shaderBrush, cornerRadius = corner, alpha = presence)
            } else if (fallbackArt != null) {
                drawVoiceEnergyFallback(field, fallbackArt, composition, presence)
            }

        }
    }
}

/**
 * Ambient light above the composer. Rendered behind the chat content and localized to the bottom of
 * the message area: it is the composer's own diffusion, widened, faded and clipped below the chat
 * content, so the pill appears to be glowing into the dark rather than underlining it with stripes.
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
