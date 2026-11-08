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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.cardSurfaceColor
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.tanh
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

    /** Smoothed, normalised audio energy. Drives illumination and colour, not the geometry. */
    var energy by mutableFloatStateOf(VoiceEnergyDesign.MIN_AMPLITUDE)
        private set

    /** Newest sample of amplitudeHistory: the level the surface follows this frame. */
    var level by mutableFloatStateOf(VoiceEnergyDesign.MIN_AMPLITUDE)
        private set

    /** Loudest sample of the recent window, i.e. how loud the last half second has been. */
    var peak by mutableFloatStateOf(VoiceEnergyDesign.MIN_AMPLITUDE)
        private set

    /** Mean sample-to-sample change across the window, normalised: how lively the speech is. */
    var activity by mutableFloatStateOf(0f)
        private set

    /** Decaying envelope of the most recent rise in microphone energy. */
    var onset by mutableFloatStateOf(0f)
        private set

    /** Microphone-driven deformation gain that scales the whole silhouette. */
    var deformation by mutableFloatStateOf(0f)
        private set

    /** Horizontal position of the region the voice most recently excited, 0..1. */
    var hotSpot by mutableFloatStateOf(0.5f)
        private set

    /** Decaying kinetic impulse of the most recent loudness rise. */
    var ripple by mutableFloatStateOf(0f)
        private set

    /** Accumulated animation seconds; frozen while recording is paused. */
    var elapsed by mutableFloatStateOf(0f)
        private set

    /** Slow travel phase used for colour movement. */
    var drift by mutableFloatStateOf(0f)
        private set

    /**
     * Per-mode amplitudes, already divided by the level each mode reaches in ordinary speech and
     * damped by its energy gate, plus the mode phases. Five scalars are the whole contract between the
     * simulation and either renderer.
     */
    val harmonicAmplitude = mutableStateListOf(0f, 0f, 0f, 0f, 0f)
    val harmonicPhase = mutableStateListOf(0f, 0f, 0f, 0f, 0f)

    private var envelope = 0.1f
    private var oscillator = 0f

    /** Sample-to-sample rise of the newest history entry, kept for the onset path. */
    private var sampleRise = 0f

    /** Where the next onset will be delivered, and the smoothed position the renderer sees. */
    private var hotTarget = 0.5f

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
     * Static composition for reduced motion. No animation loop runs, so the frame keeps a legible
     * fixed silhouette instead of a moving field.
     */
    fun snapshot(amplitude: Float) {
        clearNodes()
        firstFrameSeconds = -1f
        pausedSeconds = 0f
        drivePhase = 0f
        elapsed = 0f
        drift = 0f
        ripple = 0f
        onset = 0f
        oscillator = 0f
        val target = amplitude.coerceIn(VoiceEnergyDesign.MIN_AMPLITUDE, VoiceEnergyDesign.MAX_AMPLITUDE)
        energy = target
        level = target
        peak = target
        activity = 0f
        envelope = target
        deformation = deformationFor(target, target, 0f)
        hotSpot = 0.5f
        hotTarget = 0.5f
        harmonicPhase[0] = 0.6f
        harmonicPhase[1] = 2.4f
        harmonicPhase[2] = 4.1f
        harmonicPhase[3] = 1.3f
        harmonicPhase[4] = 3.2f
        for (mode in 0 until VoiceEnergyDesign.HARMONICS) {
            harmonicAmplitude[mode] = VoiceEnergyDesign.SNAPSHOT_MODES[mode] +
                VoiceEnergyDesign.SNAPSHOT_MODE_ENERGY[mode] * target
        }
    }

    /**
     * Advances one frame from the microphone's recent history.
     *
     * [history] is the recorder's amplitudeHistory: one smoothed sample every 60ms. Reading the window
     * rather than only its last value is what lets the geometry respond to successive peaks and to how
     * lively the speech is, instead of tracking one number. [fallbackAmplitude] covers states that have
     * no live audio (draft playback, reduced motion), and [paused] freezes the surface.
     */
    fun update(
        dtSeconds: Float,
        history: List<Float>,
        fallbackAmplitude: Float,
        paused: Boolean,
        absoluteSeconds: Float
    ) {
        val dt = dtSeconds.coerceIn(0f, VoiceEnergyDesign.MAX_FRAME_DELTA_SECONDS)

        if (firstFrameSeconds < 0f) firstFrameSeconds = absoluteSeconds

        // A paused recording stops receiving audio: the clock freezes and the field settles.
        if (paused) {
            pausedSeconds += dt
        } else {
            // Phase from the shared frame clock, so both field instances (composer surface and ambient
            // band) share one timeline instead of accumulating their own frame deltas.
            elapsed = (absoluteSeconds - firstFrameSeconds - pausedSeconds).coerceAtLeast(0f)
            drift = wrap(drift + dt * (0.12f + 0.55f * energy))
        }

        readSignals(history, fallbackAmplitude, paused)

        val target = if (paused) VoiceEnergyDesign.PAUSED_ENERGY else level
        // Illumination keeps a calm release, because light that snaps looks like a flash.
        energy = if (target > energy) energy * 0.30f + target * 0.70f
        else energy * 0.90f + target * 0.10f

        // Geometry does not: it follows the level much more directly, which is what makes ordinary
        // conversation visibly move the surface.
        deformation = deformationFor(level, peak, activity)

        oscillator = wrap(oscillator + dt * (0.30f + 1.05f * energy))
        if (!paused) {
            drivePhase = if (drivePhase >= VoiceEnergyDesign.TAU) drivePhase - VoiceEnergyDesign.TAU
            else drivePhase + dt * VoiceEnergyDesign.DRIVE_RATE
        }

        val rise = target - envelope
        envelope = envelope * 0.86f + target * 0.14f
        if (!paused) {
            val onsetRise = (rise * VoiceEnergyDesign.IMPULSE_GAIN).coerceAtLeast(0f)
            val sampleOnset = (sampleRise * VoiceEnergyDesign.IMPULSE_GAIN).coerceAtLeast(0f)
            onset = maxOf(onset * VoiceEnergyDesign.ONSET_DECAY, onsetRise, sampleOnset)
                .coerceIn(0f, 1f)
            val strength = maxOf(onsetRise, sampleOnset).coerceIn(0f, 1f)
            if (strength > VoiceEnergyDesign.ONSET_MINIMUM) {
                advanceHotSpot()
                injectImpulse(strength)
                ripple = (ripple + strength).coerceAtMost(1f)
            }
        }
        // While paused the impulse field decays faster so no high-energy deformation lingers.
        ripple *= if (paused) 0.80f else 0.93f

        stepFluid(dt, paused)
        extractHarmonics()
    }

    /**
     * Reduces the recent microphone history to the four numbers the simulation needs: the current
     * level, the loudest sample in the window, how much the signal is changing, and the newest rise.
     * Reading the window is deliberately allocation-free: it walks the list by index and writes into
     * preallocated state.
     */
    private fun readSignals(history: List<Float>, fallbackAmplitude: Float, paused: Boolean) {
        sampleRise = 0f
        if (paused) {
            level = VoiceEnergyDesign.MIN_AMPLITUDE
            peak = VoiceEnergyDesign.MIN_AMPLITUDE
            activity = 0f
            return
        }
        if (history.isEmpty()) {
            val value = fallbackAmplitude.coerceIn(VoiceEnergyDesign.MIN_AMPLITUDE, VoiceEnergyDesign.MAX_AMPLITUDE)
            level = value
            peak = value
            activity = 0f
            return
        }

        val count = minOf(history.size, VoiceEnergyDesign.SIGNAL_WINDOW)
        val from = history.size - count
        var loudest = 0f
        var change = 0f
        var previous = 0f
        for (index in from until history.size) {
            val sample = history[index].coerceIn(0f, 1f)
            if (sample > loudest) loudest = sample
            if (index > from) change += abs(sample - previous)
            previous = sample
        }
        level = previous.coerceIn(VoiceEnergyDesign.MIN_AMPLITUDE, VoiceEnergyDesign.MAX_AMPLITUDE)
        peak = loudest.coerceIn(VoiceEnergyDesign.MIN_AMPLITUDE, VoiceEnergyDesign.MAX_AMPLITUDE)
        val mean = if (count > 1) change / (count - 1).toFloat() else 0f
        activity = (mean / VoiceEnergyDesign.ACTIVITY_REFERENCE).coerceIn(0f, 1f)
        if (count > 1) {
            sampleRise = (history[history.size - 1] - history[history.size - 2]).coerceAtLeast(0f)
        }
    }

    private fun deformationFor(level: Float, peak: Float, activity: Float): Float =
        (VoiceEnergyDesign.MAG_FLOOR +
            VoiceEnergyDesign.MAG_SLOPE * sqrt(level) -
            VoiceEnergyDesign.MAG_SQRT_OFFSET +
            VoiceEnergyDesign.MAG_PEAK * peak +
            VoiceEnergyDesign.MAG_ACTIVITY * activity)
            .coerceIn(0f, VoiceEnergyDesign.MAG_MAX)

    private fun clearNodes() {
        for (i in 0 until nodeCount) {
            nodeHeights[i] = 0f
            nodeVelocities[i] = 0f
        }
    }

    private fun wrap(value: Float): Float = if (value >= 1000f) value - 1000f else value

    /** Moves the excited region along the surface so successive onsets deform different places. */
    private fun advanceHotSpot() {
        val wander = abs(sin(oscillator * 0.9f))
        val step = (VoiceEnergyDesign.HOT_STEP + VoiceEnergyDesign.HOT_WANDER * wander) /
            (nodeCount - 1).toFloat()
        hotTarget += step
        if (hotTarget >= 1f) hotTarget -= 1f
        hotSpot += shortestStep(hotTarget - hotSpot) * VoiceEnergyDesign.HOT_EMA
        if (hotSpot < 0f) hotSpot += 1f
        if (hotSpot > 1f) hotSpot -= 1f
    }

    /** Signed distance along the ring, in 0..1 units, taking the short way round. */
    private fun shortestStep(delta: Float): Float = when {
        delta > 0.5f -> delta - 1f
        delta < -0.5f -> delta + 1f
        else -> delta
    }

    /**
     * Impulse launched when the voice suddenly gets louder.
     *
     * Two things happen at once. A Gaussian kernel lifts the nodes around [hotTarget], which is what
     * produces a localised bulge instead of an evenly pushed surface. On top of that a smaller modal
     * push excites the low orders coherently, so the deformation that follows is an organic shape
     * rather than a spike: the previous single-node kick projected almost nothing when the oscillator
     * happened to point at a node of the mode, which is why a shout used to deform the surface less
     * than normal speech.
     */
    private fun injectImpulse(strength: Float) {
        val kick = strength * VoiceEnergyDesign.IMPULSE_KICK
        val primaryPhase = oscillator * 4.2f
        val secondaryPhase = oscillator * 2.6f
        val kernelWidth = 2f * (VoiceEnergyDesign.KERNEL_NODES / (nodeCount - 1).toFloat()) *
            (VoiceEnergyDesign.KERNEL_NODES / (nodeCount - 1).toFloat())
        for (i in 0 until nodeCount) {
            val x = i.toFloat() / (nodeCount - 1).toFloat()
            val delta = shortestStep(x - hotTarget)
            val kernel = exp(-(delta * delta) / kernelWidth)
            val modal = VoiceEnergyDesign.IMPULSE_MODAL_WEIGHTS
            nodeVelocities[i] += kick * (kernel +
                VoiceEnergyDesign.IMPULSE_MODAL * (
                    sin(VoiceEnergyDesign.TAU * x + primaryPhase) * modal[0] +
                        sin(VoiceEnergyDesign.TAU * 2f * x + secondaryPhase) * modal[1]
                    ))
        }
    }

    private fun stepFluid(dt: Float, paused: Boolean) {

        // The continuous drive now follows the microphone level itself rather than the doubly smoothed
        // energy, so a syllable pushes the fluid while it is being spoken. The phase still advances at
        // a constant rate: tying it to the energy swept the resonance faster when loud and made loud
        // speech deform the surface less than normal speech.
        if (!paused) {
            val drive = (VoiceEnergyDesign.DRIVE_FLOOR +
                VoiceEnergyDesign.DRIVE_LEVEL * level +
                VoiceEnergyDesign.DRIVE_PEAK * peak * 0.4f) * dt * VoiceEnergyDesign.DRIVE_GAIN
            for (i in 0 until nodeCount) {
                val x = i.toFloat() / (nodeCount - 1).toFloat()
                val weights = VoiceEnergyDesign.DRIVE_MODE_WEIGHTS
                nodeVelocities[i] += drive * (
                    sin(VoiceEnergyDesign.TAU * x + drivePhase) * weights[0] +
                        sin(VoiceEnergyDesign.TAU * 2f * x + drivePhase * 0.55f) * weights[1] +
                        sin(VoiceEnergyDesign.TAU * 3f * x - drivePhase * 0.40f) * weights[2] +
                        sin(VoiceEnergyDesign.TAU * 5f * x + drivePhase * 0.27f) * weights[3]
                    )
            }
        }

        for (i in 0 until nodeCount) {
            val left = if (i > 0) nodeHeights[i - 1] else nodeHeights[i]
            val right = if (i < nodeCount - 1) nodeHeights[i + 1] else nodeHeights[i]
            val springForce = -VoiceEnergyDesign.FLUID_SPRING * nodeHeights[i]
            val couplingForce = VoiceEnergyDesign.FLUID_COUPLING * (left + right - 2f * nodeHeights[i])
            nodeVelocities[i] = (nodeVelocities[i] + (springForce + couplingForce) * dt) *
                VoiceEnergyDesign.FLUID_DAMPING
            nodeHeights[i] = (nodeHeights[i] + nodeVelocities[i] * dt).coerceIn(-0.9f, 0.9f)
        }
    }

    /**
     * Projects the simulated node heights onto the five modes the renderers rebuild the silhouette
     * from.
     *
     * Each mode is divided by the amplitude it reaches in ordinary speech, so the silhouette keeps a
     * usable size at conversation level instead of collapsing into a flat line, and each mode is then
     * gated by how loud the voice is: a quiet surface shows one broad swell, a loud one shows several
     * pronounced peaks and valleys.
     */
    private fun extractHarmonics() {
        val normalisation = 2f / nodeCount
        for (mode in 0 until VoiceEnergyDesign.HARMONICS) {
            val cosines = cosineBasis[mode]
            val sines = sineBasis[mode]
            var a = 0f
            var b = 0f
            for (i in 0 until nodeCount) {
                val height = nodeHeights[i]
                a += height * cosines[i]
                b += height * sines[i]
            }
            a *= normalisation
            b *= normalisation
            val raw = sqrt(a * a + b * b)
            var normalised = (raw / VoiceEnergyDesign.HARMONIC_REFERENCE[mode])
                .coerceAtMost(VoiceEnergyDesign.HARMONIC_MAX)
            if (mode > 0) {
                normalised *= smoothstep(
                    VoiceEnergyDesign.MODE_GATE_LEVEL[mode],
                    VoiceEnergyDesign.MODE_GATE_LEVEL[mode] + VoiceEnergyDesign.MODE_GATE_WIDTH[mode],
                    level
                )
            }
            harmonicAmplitude[mode] = normalised
            harmonicPhase[mode] = atan2(a, b)
        }
    }
}

/**
 * AGSL renderer for the composer's energy surface (API 33+).
 *
 * The surface is a lit volume, not a coloured graphic drawn over a button. One organic boundary is
 * built from the five live modes of the fluid simulation plus the bulges the voice just pushed into
 * it; light is diffused away from that boundary through several overlapping falloffs of different
 * widths, and the fluid's own glow meets the light it spills into the air across a wide band. There
 * is therefore no line to anti-alias anywhere in the frame: the illumination dissolves into the black
 * over roughly 20dp instead of ending at an edge.
 *
 * Colour is one broad, low-frequency sweep through a single cyclic spectrum, so adjacent samples are
 * always neighbours on the wheel and regions blend instead of averaging into grey.
 *
 * Uniforms are set on a single cached shader instance - no shader object is created per frame.
 */
private const val VOICE_ENERGY_AGSL_SHADER = """
uniform float2 iResolution;
uniform float iTime;
uniform float iAmplitude;
uniform float iRipple;
uniform float iDrift;
// Modal amplitudes and phases. The amplitudes arrive already divided by the level each mode reaches in
// ordinary speech and already damped by its energy gate, so the shader and the Canvas fallback render
// the same silhouette from the same five numbers.
uniform float iHarmonic1;
uniform float iHarmonic2;
uniform float iHarmonic3;
uniform float iHarmonic4;
uniform float iHarmonic5;
uniform float iPhase1;
uniform float iPhase2;
uniform float iPhase3;
uniform float iPhase4;
uniform float iPhase5;
// Shape weights of the modal mix, so a change is made once in VoiceEnergyDesign for both renderers.
uniform float iModeWeight2;
uniform float iModeWeight3;
uniform float iModeWeight4;
uniform float iModeWeight5;
// Composition, computed on the Kotlin side (VoiceEnergyComposition) and shared with the Canvas
// fallback, so the amplitude -> geometry mapping exists exactly once.
uniform float iBodyTop;
uniform float iCrestScale;
uniform float iWarmth;
// Localised deformation: how strong the bulge is, where the voice put it, and the ripple that follows.
uniform float iOnset;
uniform float iHotSpot;
uniform float iBulgeWidth;
uniform float iBulgePrimary;
uniform float iBulgeSecondary;
uniform float iBulgeSecondaryOffset;
uniform float iBulgeSecondaryWidth;
uniform float iTransientRipple;
uniform float iProfileLimit;
// Volumetric light. Several overlapping falloffs of different widths are what make the illumination
// read as light inside a fluid instead of a shape with an edge: a broad diffusion, a wider halo, an
// even wider veil, the inner glow of the fluid, the pool at its base and a soft (not specular) bloom.
uniform float iDiffusionCalm;
uniform float iDiffusionExpressive;
uniform float iEdgeSoftness;
uniform float iHaloWidth;
uniform float iHaloStrength;
uniform float iVeilWidth;
uniform float iVeilStrength;
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
// Light inside the fluid, as a fraction of the light it emits upward. It decays away from the surface,
// and the band over which it meets the outside light is wide, so the luminance never steps there.
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

// Organic silhouette. The five modes are the live output of the fluid simulation, the bulges are the
// places the voice most recently pushed, and the idle term keeps the surface undulating when the
// microphone is quiet without ever inventing an amplitude value.
//
// The profile is evaluated on a slowly bending axis, so the contours lean, pinch and stay asymmetric,
// and the sum is soft-saturated: when the voice is loud the crest flattens smoothly at its extremes
// instead of clipping into a flat-topped wave.
// AGSL has no tanh (the driver rejected it with "no match for tanh(float)" and the shader failed to
// compile at runtime), so the soft saturation is written out from exp. This is limit*tanh(v/limit)
// evaluated safely: exp only ever sees a non-positive argument, so a loud crest cannot overflow it.
// It is declared before use because functions must be declared before they are called.
float softSaturate(float value, float limit) {
    float decay = exp(-2.0 * abs(value) / limit);
    float magnitude = limit * (1.0 - decay) / (1.0 + decay);
    return value < 0.0 ? -magnitude : magnitude;
}

float surfaceProfile(float x, float t, float amp) {
    const float TAU = 6.2831853;
    float warped = x
        + 0.075 * sin(TAU * (x * 0.62 - t * 0.13))
        + 0.035 * sin(TAU * (x * 1.90 + t * 0.21));
    float d = iHarmonic1 * sin(TAU * warped + iPhase1);
    d += iHarmonic2 * iModeWeight2 * sin(TAU * 2.0 * warped + iPhase2);
    d += iHarmonic3 * iModeWeight3 * sin(TAU * 3.0 * warped + iPhase3);
    d += iHarmonic4 * iModeWeight4 * sin(TAU * 4.0 * warped + iPhase4);
    d += iHarmonic5 * iModeWeight5 * sin(TAU * 5.0 * warped + iPhase5);
    // Rounded bulges centred on the excited region, plus a smaller echo of it elsewhere on the surface.
    float primary = (x - iHotSpot) / iBulgeWidth;
    d += iBulgePrimary * iOnset * exp(-primary * primary);
    float echo = fract(iHotSpot + iBulgeSecondaryOffset);
    float secondary = (x - echo) / (iBulgeWidth * iBulgeSecondaryWidth);
    d += iBulgeSecondary * iOnset * exp(-secondary * secondary);
    // Travelling ripple that follows an onset, and the decaying impulse of the loudness rise itself.
    d += iOnset * iTransientRipple * sin(TAU * (warped * 1.6 - t * 0.33));
    d += iRipple * 0.10 * sin(TAU * (warped * 1.15 + t * 0.40));
    d += (iIdleCalm + iIdleEnergy * amp)
        * sin(TAU * (warped * 0.9 + 0.22 * sin(TAU * warped)) - t * 0.19);
    return softSaturate(d, iProfileLimit);
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

    // ---- Volumetric light, with no boundary to speak of -----------------------------------
    // Signed distance to the surface; positive inside the fluid. Everything below is a function of
    // that distance alone, and the fluid's own glow meets the light it spills into the air across a
    // wide band. That is what removes the defined edge: there is no line to anti-alias, only a
    // luminance transition about 20dp wide.
    float d = y - crest;
    float above = max(-d, 0.0);
    float below = max(d, 0.0);
    float diffusion = iDiffusionCalm + (iDiffusionExpressive - iDiffusionCalm) * expression;
    float inside = exp(-below / diffusion) * iVolumeInside;
    float outside = exp(-above / diffusion) * iVolumeCalm
        + exp(-above / iHaloWidth) * iHaloStrength
        + exp(-above / iVeilWidth) * iVeilStrength;
    float blend = smoothstep(-iEdgeSoftness, iEdgeSoftness, -d);
    float floorGlow = exp(-(1.0 - y) / iFloorDiffusion) * iFloorGlow;
    float bloom = exp(-abs(d) / (iBloomWidth + 0.015 * expression))
        * (iBloomCalm + iBloomExpressive * expression);

    float volume = mix(inside, outside, blend) + floorGlow + bloom;
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
        runtimeShader.setFloatUniform("iHarmonic4", field.harmonicAmplitude[3])
        runtimeShader.setFloatUniform("iHarmonic5", field.harmonicAmplitude[4])
        runtimeShader.setFloatUniform("iPhase1", field.harmonicPhase[0])
        runtimeShader.setFloatUniform("iPhase2", field.harmonicPhase[1])
        runtimeShader.setFloatUniform("iPhase3", field.harmonicPhase[2])
        runtimeShader.setFloatUniform("iPhase4", field.harmonicPhase[3])
        runtimeShader.setFloatUniform("iPhase5", field.harmonicPhase[4])
        runtimeShader.setFloatUniform("iModeWeight2", VoiceEnergyDesign.HARMONIC_WEIGHT[1])
        runtimeShader.setFloatUniform("iModeWeight3", VoiceEnergyDesign.HARMONIC_WEIGHT[2])
        runtimeShader.setFloatUniform("iModeWeight4", VoiceEnergyDesign.HARMONIC_WEIGHT[3])
        runtimeShader.setFloatUniform("iModeWeight5", VoiceEnergyDesign.HARMONIC_WEIGHT[4])
        runtimeShader.setFloatUniform("iBodyTop", composition.bodyTop)
        runtimeShader.setFloatUniform("iCrestScale", composition.crestScale)
        runtimeShader.setFloatUniform("iWarmth", composition.warmth)
        runtimeShader.setFloatUniform("iOnset", composition.onset)
        runtimeShader.setFloatUniform("iHotSpot", composition.hotSpot)
        runtimeShader.setFloatUniform("iBulgeWidth", VoiceEnergyDesign.BULGE_WIDTH)
        runtimeShader.setFloatUniform("iBulgePrimary", VoiceEnergyDesign.BULGE_PRIMARY)
        runtimeShader.setFloatUniform("iBulgeSecondary", VoiceEnergyDesign.BULGE_SECONDARY)
        runtimeShader.setFloatUniform("iBulgeSecondaryOffset", VoiceEnergyDesign.BULGE_SECONDARY_OFFSET)
        runtimeShader.setFloatUniform("iBulgeSecondaryWidth", VoiceEnergyDesign.BULGE_SECONDARY_WIDTH)
        runtimeShader.setFloatUniform("iTransientRipple", VoiceEnergyDesign.TRANSIENT_RIPPLE)
        runtimeShader.setFloatUniform("iProfileLimit", VoiceEnergyDesign.PROFILE_LIMIT)
        runtimeShader.setFloatUniform("iDiffusionCalm", VoiceEnergyDesign.DIFFUSION_CALM)
        runtimeShader.setFloatUniform("iDiffusionExpressive", VoiceEnergyDesign.DIFFUSION_EXPRESSIVE)
        runtimeShader.setFloatUniform("iEdgeSoftness", VoiceEnergyDesign.EDGE_SOFTNESS)
        runtimeShader.setFloatUniform("iHaloWidth", VoiceEnergyDesign.HALO_WIDTH)
        runtimeShader.setFloatUniform("iHaloStrength", VoiceEnergyDesign.HALO_STRENGTH)
        runtimeShader.setFloatUniform("iVeilWidth", VoiceEnergyDesign.VEIL_WIDTH)
        runtimeShader.setFloatUniform("iVeilStrength", VoiceEnergyDesign.VEIL_STRENGTH)
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

    /** Height of the fluid surface as a fraction of the composer height. */
    var bodyTop = VoiceEnergyDesign.BODY_TOP_CALM
        private set

    /** Fraction of the composer height the silhouette may travel, from the microphone signal. */
    var crestScale = VoiceEnergyDesign.CREST_SPAN * VoiceEnergyDesign.CREST_BASE
        private set

    /**
     * 0 = calm personality (blue and violet, wide and soft diffusion), 1 = expressive (full spectrum,
     * pronounced freeform boundary). Driven by the live microphone level, so it changes as someone
     * starts speaking instead of following the slower illumination.
     */
    var warmth = 0f
        private set

    /** Strength of the localised bulge and travelling ripple, from the onset envelope. */
    var onset = 0f
        private set

    /** Horizontal centre of the excited region, 0..1. */
    var hotSpot = 0.5f
        private set

    fun update(field: VoiceEnergyField, presence: Float) {
        val amp = (field.energy * presence).coerceIn(0f, 1f)
        amplitude = amp
        bodyTop = VoiceEnergyDesign.BODY_TOP_CALM +
            (VoiceEnergyDesign.BODY_TOP_FULL - VoiceEnergyDesign.BODY_TOP_CALM) * amp
        crestScale = VoiceEnergyDesign.CREST_SPAN * (VoiceEnergyDesign.CREST_BASE +
            VoiceEnergyDesign.CREST_DEFORM * field.deformation)
        warmth = smoothstep(VoiceEnergyDesign.WARMTH_GATE_LOW, VoiceEnergyDesign.WARMTH_GATE_HIGH, field.level)
        onset = (field.onset * presence).coerceIn(0f, 1f)
        hotSpot = field.hotSpot
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

    /** How much of the live modal shape this layer shows, and how much of the localised bulges. */
    var modeGain = 1f
    var bulgeGain = 1f

    /** Repositions this component for the current frame, without allocating. */
    fun set(
        baseLine: Float,
        scale: Float,
        xScale: Float,
        timeScale: Float,
        timeShift: Float,
        modeGain: Float = 1f,
        bulgeGain: Float = 1f
    ) {
        this.baseLine = baseLine
        this.scale = scale
        this.xScale = xScale
        this.timeScale = timeScale
        this.timeShift = timeShift
        this.modeGain = modeGain
        this.bulgeGain = bulgeGain
    }
}

/**
 * Shared silhouette maths, mirroring the AGSL surfaceProfile term for term so the two renderers draw
 * the same shape: five live modes weighted into one organic curve, the rounded bulges the voice just
 * pushed into the surface, a travelling onset ripple, a decaying impulse ripple and the slow idle
 * undulation, all soft-saturated so a loud crest flattens instead of clipping.
 */
private fun layerProfile(
    field: VoiceEnergyField,
    composition: VoiceEnergyComposition,
    x: Float,
    time: Float,
    layer: VoiceLayerShape
): Float {
    val warped = x +
        0.075f * sin(VoiceEnergyDesign.TAU * (x * 0.62f - time * 0.13f)) +
        0.035f * sin(VoiceEnergyDesign.TAU * (x * 1.90f + time * 0.21f))
    val amplitudes = field.harmonicAmplitude
    val phases = field.harmonicPhase
    var profile = amplitudes[0] * sin(VoiceEnergyDesign.TAU * warped + phases[0])
    for (mode in 1 until VoiceEnergyDesign.HARMONICS) {
        profile += amplitudes[mode] * VoiceEnergyDesign.HARMONIC_WEIGHT[mode] *
            sin(VoiceEnergyDesign.TAU * (mode + 1).toFloat() * warped + phases[mode])
    }
    profile *= layer.modeGain

    if (layer.bulgeGain > 0f && composition.onset > 0.001f) {
        val bulge = composition.onset * layer.bulgeGain
        profile += VoiceEnergyDesign.BULGE_PRIMARY * bulge *
            gaussian(x - composition.hotSpot, VoiceEnergyDesign.BULGE_WIDTH)
        val echo = wrapFraction(composition.hotSpot + VoiceEnergyDesign.BULGE_SECONDARY_OFFSET)
        profile += VoiceEnergyDesign.BULGE_SECONDARY * bulge *
            gaussian(x - echo, VoiceEnergyDesign.BULGE_WIDTH * VoiceEnergyDesign.BULGE_SECONDARY_WIDTH)
        profile += VoiceEnergyDesign.TRANSIENT_RIPPLE * bulge *
            sin(VoiceEnergyDesign.TAU * (warped * 1.6f - time * 0.33f))
    }

    profile += field.ripple * 0.10f *
        sin(VoiceEnergyDesign.TAU * (warped * 1.15f + time * 0.40f))
    profile += (VoiceEnergyDesign.IDLE_WAVE_CALM +
        VoiceEnergyDesign.IDLE_WAVE_PER_ENERGY * composition.amplitude) *
        sin(VoiceEnergyDesign.TAU * (warped * 0.9f + 0.22f * sin(VoiceEnergyDesign.TAU * warped)) -
            time * 0.19f)

    return VoiceEnergyDesign.PROFILE_LIMIT *
        tanh(profile / VoiceEnergyDesign.PROFILE_LIMIT)
}

private fun gaussian(delta: Float, width: Float): Float {
    val scaled = delta / width
    return exp(-scaled * scaled)
}

private fun wrapFraction(value: Float): Float {
    var result = value
    while (result >= 1f) result -= 1f
    while (result < 0f) result += 1f
    return result
}

/** Evaluates the composite silhouette of [layers] at horizontal fraction [x]. */
private fun contourY(
    x: Float,
    field: VoiceEnergyField,
    layers: List<VoiceLayerShape>,
    composition: VoiceEnergyComposition
): Float {
    var base = 0f
    var wobble = 0f
    for (index in layers.indices) {
        val layer = layers[index]
        base += layer.baseLine
        if (layer.scale == 0f) continue
        val scaledX = x * layer.xScale
        val time = field.elapsed * layer.timeScale + layer.timeShift
        wobble += layer.scale * layerProfile(field, composition, scaledX, time, layer)
    }
    // The profile is positive upward, exactly as in the shader, while the crest is a pixel offset from
    // the top of the canvas - so moving the surface up means subtracting it.
    return base - wobble
}

private fun buildContourPath(
    path: Path,
    field: VoiceEnergyField,
    width: Float,
    height: Float,
    layers: List<VoiceLayerShape>,
    composition: VoiceEnergyComposition,
    startFraction: Float = 0f,
    endFraction: Float = 1f,
    closeToBottom: Boolean = false
) {
    val samples = VoiceEnergyDesign.FALLBACK_SAMPLES
    path.reset()
    val startX = width * startFraction
    path.moveTo(startX, contourY(startFraction, field, layers, composition))
    for (i in 1..samples) {
        val step = startFraction + (endFraction - startFraction) * (i.toFloat() / samples.toFloat())
        path.lineTo(width * step, contourY(step, field, layers, composition))
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
 * The fallback reproduces the shader's volumetric model with gradients instead of per-fragment maths.
 * The fluid body is painted as several nested, low-opacity layers rather than one solid fill, so its
 * own edge fades in rather than appearing as a step, and the light above it is a stack of wide, faint
 * strokes - many spatial widths, always low opacity - which is what makes the transition to black
 * gradual instead of a drawn line.
 */
private class VoiceEnergyFallbackArt(density: Density) {
    val crestPath = Path()
    val bodyPath = Path()
    private val crestShape = VoiceLayerShape()
    private val bodyShapes = List(VoiceEnergyDesign.BODY_LAYERS) { VoiceLayerShape() }

    /** One shared layer list, so drawing the nested body never allocates. */
    val crestLayers: List<VoiceLayerShape> = listOf(crestShape)
    val bodyLayers: List<List<VoiceLayerShape>> = bodyShapes.map { listOf(it) }

    /** The fluid volume under the boundary: near-black, with a soft pool of light at its base. */
    val bodyVolume: Brush = Brush.verticalGradient(
        0.00f to VoiceEnergyColors.VolumeBlack,
        0.52f to VoiceEnergyColors.VolumeBlack,
        0.86f to VoiceEnergyColors.DeepIndigo.copy(alpha = 0.50f),
        1.00f to VoiceEnergyColors.Blue.copy(alpha = 0.26f)
    )

    /** Diffusion strokes: many widths, low opacity, widest and faintest first. */
    val lightStrokes: List<Stroke> = VoiceEnergyDesign.FALLBACK_STROKE_WIDTHS_DP.map { widthDp ->
        Stroke(width = with(density) { widthDp.dp.toPx() }, cap = StrokeCap.Round)
    }
    val lightAlphas: List<Float> = VoiceEnergyDesign.FALLBACK_STROKE_ALPHAS.toList()

    /** Wide, faint boundary bloom. Never a specular line. */
    val bloomStroke: Stroke = Stroke(
        width = with(density) { VoiceEnergyDesign.FALLBACK_BLOOM_WIDTH_DP.dp.toPx() },
        cap = StrokeCap.Round
    )

    /** One colour sweep per expression bucket, so the fallback never cross-fades two sweeps into grey. */
    val lightFills: List<Brush> = lightSweepBrushes(fadedEnds = false, whiten = 0f)
    val bloomFills: List<Brush> = lightSweepBrushes(fadedEnds = false, whiten = VoiceEnergyDesign.BLOOM_WHITE)

    /** Maps the shared composition onto this frame's boundary shape. */
    fun updateShapes(height: Float, composition: VoiceEnergyComposition) {
        crestShape.set(height * composition.bodyTop, height * composition.crestScale, 1f, 1f, 0f)
    }

    /** Positions the nested fluid layers, each 5% of the height below the one above it. */
    fun updateBody(height: Float, composition: VoiceEnergyComposition) {
        val offset = height * VoiceEnergyDesign.BODY_LAYER_OFFSET
        for (index in bodyShapes.indices) {
            bodyShapes[index].set(
                baseLine = height * composition.bodyTop + offset * index,
                scale = height * composition.crestScale,
                xScale = 1f,
                timeScale = 1f,
                timeShift = 0f
            )
        }
    }

    fun bodyLayer(index: Int): List<VoiceLayerShape> = bodyLayers[index]

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
 * violet..blue, at full expression it walks the whole wheel. The warm colours therefore emerge from
 * the volume as the voice rises.
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
                if (whiten > 0f) color = lerp(color, VoiceEnergyColors.Specular, whiten)
                if (fadedEnds) color = color.copy(alpha = color.alpha * sin(PI * t).toFloat())
                t to color
            }
        )
    }
}

/** Drawing resources for the light above the composer, built once per density. */
private class VoiceAmbientArt(density: Density) {
    val crestPath = Path()
    private val crestShape = VoiceLayerShape()
    val crestLayers: List<VoiceLayerShape> = listOf(crestShape)

    /**
     * The composer's own diffusion, widened and faded at the ends, and showing less of the live modal
     * shape than the surface inside the pill so it reads as the pill glowing into the dark.
     */
    val lightStrokes: List<Stroke> = VoiceEnergyDesign.AMBIENT_STROKE_WIDTHS_DP.map { widthDp ->
        Stroke(width = with(density) { widthDp.dp.toPx() }, cap = StrokeCap.Round)
    }
    val lightAlphas: List<Float> = VoiceEnergyDesign.AMBIENT_STROKE_ALPHAS.toList()
    val lightFills: List<Brush> = lightSweepBrushes(fadedEnds = true, whiten = 0f)

    fun updateShapes(height: Float, composition: VoiceEnergyComposition) {
        crestShape.set(
            baseLine = height * (VoiceEnergyDesign.AMBIENT_BASE -
                VoiceEnergyDesign.AMBIENT_LIFT * composition.amplitude),
            scale = height * (VoiceEnergyDesign.AMBIENT_SCALE_CALM +
                VoiceEnergyDesign.AMBIENT_SCALE_ENERGY * composition.amplitude),
            xScale = VoiceEnergyDesign.AMBIENT_X_SCALE,
            timeScale = VoiceEnergyDesign.AMBIENT_TIME_SCALE,
            timeShift = VoiceEnergyDesign.AMBIENT_TIME_SHIFT,
            modeGain = VoiceEnergyDesign.AMBIENT_MODE_GAIN,
            bulgeGain = VoiceEnergyDesign.AMBIENT_BULGE_GAIN
        )
    }

    fun sweepIndex(expression: Float): Int =
        (expression.coerceIn(0f, 1f) * (VoiceEnergyDesign.SWEEP_BUCKETS - 1).toFloat()).roundToInt()
}

/**
 * The composer surface, Canvas path. The pill is a near-black volume and the colour is light emitted
 * from the fluid boundary and diffused upward through many wide, faint passes, so nothing reads as a
 * line drawn over a button.
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
    buildContourPath(art.crestPath, field, width, height, art.crestLayers, composition)

    val expression = composition.warmth
    val bucket = art.sweepIndex(expression)
    val rippleBoost = 0.85f + 0.25f * field.ripple

    // Diffusion first: many widths, each pass faint, so the light spreads gradually into the black.
    val volume = VoiceEnergyDesign.VOLUME_CALM + VoiceEnergyDesign.VOLUME_EXPRESSIVE * expression
    art.lightStrokes.forEachIndexed { index, stroke ->
        val alpha = art.lightAlphas[index] * volume * rippleBoost * presence
        if (alpha <= 0.002f) return@forEachIndexed
        drawPath(art.crestPath, brush = art.lightFills[bucket], alpha = alpha, style = stroke)
    }

    // Wide, faint bloom straddling the boundary. It softens the transition rather than drawing a line.
    drawPath(
        path = art.crestPath,
        brush = art.bloomFills[bucket],
        alpha = (VoiceEnergyDesign.BLOOM_CALM + VoiceEnergyDesign.BLOOM_EXPRESSIVE * expression) *
            rippleBoost * presence,
        style = art.bloomStroke
    )

    // Then the fluid body, as nested low-opacity layers: the light that crossed the boundary is dimmed
    // a little more with each layer, so the surface fades in instead of starting at an edge.
    art.updateBody(height, composition)
    for (index in 0 until VoiceEnergyDesign.BODY_LAYERS) {
        buildContourPath(
            path = art.bodyPath,
            field = field,
            width = width,
            height = height,
            layers = art.bodyLayer(index),
            composition = composition,
            closeToBottom = true
        )
        drawPath(
            path = art.bodyPath,
            brush = art.bodyVolume,
            alpha = presence * VoiceEnergyDesign.BODY_LAYER_ALPHA
        )
    }
}

/**
 * Ambient light above the composer, Canvas path: the composer's own diffusion, widened, faded and
 * clipped to the lower part of the band, so it reads as the pill glowing into the dark.
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
        buildContourPath(art.crestPath, field, width, height, art.crestLayers, composition)
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

/**
 * The recent microphone history the geometry deforms from: one smoothed sample every 60ms, so the
 * window carries the successive peaks of speech, how quickly it is changing and where it is loudest,
 * rather than a single number. States without live audio return an empty list, and the field then
 * falls back to [glowAmplitude], which is how a draft replays its recorded envelope.
 */
internal fun VoiceState.glowAmplitudeHistory(): List<Float> = when (this) {
    is VoiceState.Recording -> amplitudeHistory
    is VoiceState.Locked -> if (isPaused) emptyList() else amplitudeHistory
    else -> emptyList()
}

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
    history: List<Float>,
    fallbackAmplitude: Float,
    reducedMotion: Boolean
): VoiceEnergyField {
    val field = remember { VoiceEnergyField() }
    val latestHistory by rememberUpdatedState(history)
    val latestFallback by rememberUpdatedState(fallbackAmplitude)
    val latestPaused by rememberUpdatedState(paused)

    if (reducedMotion) {
        SideEffect { field.snapshot(latestFallback) }
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
                    history = latestHistory,
                    fallbackAmplitude = latestFallback,
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
    val history = if (showField) voiceState.glowAmplitudeHistory() else emptyList()
    val amplitude = if (showField) voiceState.glowAmplitude() else VoiceEnergyDesign.MIN_AMPLITUDE
    val field = rememberVoiceEnergyField(showField, isPaused, history, amplitude, reducedMotion)
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
    // A driver that rejects the shader program must not take the composer down with it: the AGSL path
    // is attempted once, and any failure leaves the Canvas renderer to draw the same composition.
    val shader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { VoiceEnergyShader() }.getOrNull()
        } else {
            null
        }
    }
    val fallbackArt = remember(density, shader) {
        if (shader == null) VoiceEnergyFallbackArt(density) else null
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
        history = voiceState.glowAmplitudeHistory(),
        fallbackAmplitude = voiceState.glowAmplitude(),
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
