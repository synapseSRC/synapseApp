package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import androidx.compose.ui.graphics.Color

/**
 * Centralised Google-AI spectrum palette for the voice energy field.
 *
 * Project design decision (not an M3 colour-role set): M3 roles describe UI semantics, and the
 * voice surface is a decorative, non-interactive element - the sanctioned M3 use for abstract
 * shapes and expressive colour. Every colour literal for the energy renderer lives here so no
 * composable scatters values, and both rendering paths (AGSL + Compose Canvas) read one source
 * of truth. Blue, violet and cyan form the calm foundation; coral, amber, green and pink are
 * introduced dynamically as normalised audio energy rises.
 */
internal object VoiceEnergyColors {
    // Calm foundation - the resting appearance.
    val DeepIndigo = Color(0xFF3D5AFE)
    val Blue = Color(0xFF4285F4)
    val Violet = Color(0xFF9B72FF)
    val Cyan = Color(0xFF27D6F5)

    // Expressive accents that emerge with audio energy.
    val Coral = Color(0xFFFF5F5B)
    val Amber = Color(0xFFFFC247)
    val Green = Color(0xFF34D399)
    val Pink = Color(0xFFFF7EB6)

    // Lighting and volume.
    val Specular = Color(0xFFFFFFFF)

    /** Deep black the light lives inside - the reference images are lit volumes on black, not cards. */
    val VolumeBlack = Color(0xFF04050A)
}

/**
 * Tunable constants of the voice energy field. Grouped here so the animation architecture is
 * inspectable in one place instead of being spread across the renderer.
 */
internal object VoiceEnergyDesign {
    // ── Composition: where the fluid boundary sits ──────────────────────────────────────────
    // These numbers are the amplitude -> geometry contract. The AGSL shader receives the derived
    // values as uniforms and the Canvas fallback reads the same composition object, so the two
    // renderers cannot drift apart.
    //
    // Two personalities, driven by the real microphone envelope:
    //   calm       -> boundary near 0.76 of the height, a few dp of movement, blue/violet volume
    //   expressive -> boundary around 0.60, pronounced freeform deformation, full spectrum
    const val BODY_TOP_CALM = 0.76f
    const val BODY_TOP_FULL = 0.60f

    /**
     * Freeform deformation of the boundary, as a fraction of the composer height:
     *
     *   crestScale = CREST_SPAN * (CREST_BASE + CREST_DEFORM * deformation)
     *
     * Measured against the simulated microphone pipeline this puts the peak-to-valley travel at
     * roughly 5-7dp at silence, 14-20dp for ordinary conversation and 26-30dp for a shout on a 72dp
     * composer. The old geometry produced 1-2dp for conversation, which is why speech barely moved it.
     */
    const val CREST_SPAN = 0.13f
    const val CREST_BASE = 0.55f
    const val CREST_DEFORM = 0.45f

    /**
     * Slow idle undulation, applied whether or not the microphone reports anything. Decorative motion
     * at the fluid's own phase, never a substitute amplitude value.
     */
    const val IDLE_WAVE_CALM = 0.30f
    const val IDLE_WAVE_PER_ENERGY = 0.18f

    /**
     * Where the recording sits between the two personalities. This single value moves the diffusion,
     * the deformation and the colour sweep together, so the change reads as one fluid rather than as
     * several effects fading in and out.
     */
    const val WARMTH_GATE_LOW = 0.15f
    const val WARMTH_GATE_HIGH = 0.72f

    // ── Microphone signals derived from amplitudeHistory ───────────────────────────────────
    /**
     * Number of recent history samples the field reads. The recorder appends one sample every 60ms, so
     * this covers roughly half a second: enough to see the successive peaks of speech instead of only
     * the newest value.
     */
    const val SIGNAL_WINDOW = 8

    /** Mean sample-to-sample change that counts as a fully lively voice, used to scale ACTIVITY. */
    const val ACTIVITY_REFERENCE = 0.10f

    /** Normalises the history window to a small set of renderer inputs. */
    const val MAG_FLOOR = 0.05f
    const val MAG_SLOPE = 1.05f
    const val MAG_SQRT_OFFSET = 0.05f
    const val MAG_PEAK = 0.22f
    const val MAG_ACTIVITY = 0.30f
    const val MAG_MAX = 1.05f

    /** Per-frame decay of the onset (transient) envelope, applied at 60fps. */
    const val ONSET_DECAY = 0.86f

    /** Smallest onset worth pushing into the fluid, so a hiss does not keep the surface busy. */
    const val ONSET_MINIMUM = 0.03f

    // ── Fluid simulation ───────────────────────────────────────────────────────────────────
    /**
     * Continuous force on the fluid nodes. The floor is deliberately tiny: a quiet room must not keep
     * the surface resonating, and the idle undulation above supplies the breathing instead.
     */
    const val DRIVE_FLOOR = 0.004f
    const val DRIVE_LEVEL = 1.5f
    const val DRIVE_PEAK = 0.6f
    const val DRIVE_GAIN = 9f

    /** Constant phase rate of the drive, deliberately decoupled from how loud the voice is. */
    const val DRIVE_RATE = 0.55f

    /** How strongly a rise in microphone energy kicks the fluid, and the shape of that kick. */
    const val IMPULSE_GAIN = 7f
    const val IMPULSE_KICK = 0.30f

    /**
     * Share of an onset delivered as a coherent modal push. The rest goes into a Gaussian kernel
     * around HOT_SPOT, which is what makes one part of the surface lift further than its neighbours.
     */
    const val IMPULSE_MODAL = 0.45f

    /** Radius of the spatial impulse kernel, in fluid nodes. */
    const val KERNEL_NODES = 2.6f

    /** How far the hot region advances along the surface on each onset, and how fast it is tracked. */
    const val HOT_STEP = 5f
    const val HOT_WANDER = 4.2f
    const val HOT_EMA = 0.12f

    /** Spring, neighbour coupling and damping of the fluid ring that carries the audio energy. */
    const val FLUID_SPRING = 9f
    const val FLUID_COUPLING = 15f
    const val FLUID_DAMPING = 0.96f

    /** Spatial weights of the continuous drive, and of the modal half of an onset impulse. */
    private val _DRIVE_MODE_WEIGHTS = floatArrayOf(0.60f, 0.30f, 0.15f, 0.08f)
    val DRIVE_MODE_WEIGHTS: List<Float> get() = _DRIVE_MODE_WEIGHTS.asList()
    private val _IMPULSE_MODAL_WEIGHTS = floatArrayOf(0.70f, 0.40f)
    val IMPULSE_MODAL_WEIGHTS: List<Float> get() = _IMPULSE_MODAL_WEIGHTS.asList()

    /** Spring-coupled fluid nodes that carry the live audio energy. */
    const val FLUID_NODES = 24

    /** Modal orders extracted from the fluid simulation and handed to the renderer. */
    const val HARMONICS = 5

    /**
     * Per-mode reference and shape weight. Each mode is divided by the amplitude it reaches in ordinary
     * speech, so the silhouette keeps a usable size at conversation level instead of collapsing into a
     * flat line, and the weights mix the modes into one organic curve.
     */
    private val _HARMONIC_REFERENCE = floatArrayOf(0.30f, 0.22f, 0.07f, 0.045f, 0.045f)
    val HARMONIC_REFERENCE: List<Float> get() = _HARMONIC_REFERENCE.asList()
    private val _HARMONIC_WEIGHT = floatArrayOf(1f, 0.62f, 0.40f, 0.26f, 0.17f)
    val HARMONIC_WEIGHT: List<Float> get() = _HARMONIC_WEIGHT.asList()

    /** Ceiling on a normalised mode, so a shout cannot push the silhouette into chaos. */
    const val HARMONIC_MAX = 2f

    /**
     * A mode only appears once the voice is loud enough to excite it, so a quiet surface shows one
     * broad swell and a loud one shows several pronounced peaks and valleys. This is the "number of
     * visible peaks" response, and the widths keep the appearance smooth rather than popping.
     */
    private val _MODE_GATE_LEVEL = floatArrayOf(0f, 0.10f, 0.16f, 0.24f, 0.32f)
    val MODE_GATE_LEVEL: List<Float> get() = _MODE_GATE_LEVEL.asList()
    private val _MODE_GATE_WIDTH = floatArrayOf(0f, 0.06f, 0.08f, 0.10f, 0.12f)
    val MODE_GATE_WIDTH: List<Float> get() = _MODE_GATE_WIDTH.asList()

    /**
     * Fixed mode mix used by the reduced-motion snapshot, so a still frame is a legible composition
     * rather than whatever the simulation happened to be holding.
     */
    private val _SNAPSHOT_MODES = floatArrayOf(0.35f, 0.20f, 0.10f, 0.05f, 0.04f)
    val SNAPSHOT_MODES: List<Float> get() = _SNAPSHOT_MODES.asList()
    private val _SNAPSHOT_MODE_ENERGY = floatArrayOf(0.45f, 0.25f, 0.15f, 0.10f, 0.08f)
    val SNAPSHOT_MODE_ENERGY: List<Float> get() = _SNAPSHOT_MODE_ENERGY.asList()

    /** Soft limit on the assembled profile. tanh saturation keeps the crest smooth when it is loud. */
    const val PROFILE_LIMIT = 1.6f

    /** Localised bulges around the region the voice just excited. */
    const val BULGE_WIDTH = 0.20f
    const val BULGE_PRIMARY = 0.55f
    const val BULGE_SECONDARY = 0.30f
    const val BULGE_SECONDARY_OFFSET = 0.37f
    const val BULGE_SECONDARY_WIDTH = 1.5f

    /** Travelling ripple that follows an onset. */
    const val TRANSIENT_RIPPLE = 0.35f

    /** Samples used to trace the organic silhouette in the Canvas fallback (API 26..32). */
    const val FALLBACK_SAMPLES = 28

    // ── Volumetric light and soft edges ────────────────────────────────────────────────────
    /** How far light diffuses away from the boundary: wide and soft when calm, tighter when loud. */
    const val DIFFUSION_CALM = 0.60f
    const val DIFFUSION_EXPRESSIVE = 0.42f

    /**
     * Width of the band over which the light inside the fluid meets the light above it. A narrow band
     * is what produced the visible boundary in the previous build: the two sides were separate
     * exponentials joined at the crest, so the luminance stepped there. This band blends them.
     */
    const val EDGE_SOFTNESS = 0.11f

    /** Overlapping low-opacity layers of different widths, which is what makes light look volumetric. */
    const val HALO_WIDTH = 0.22f
    const val HALO_STRENGTH = 0.12f
    const val VEIL_WIDTH = 0.55f
    const val VEIL_STRENGTH = 0.08f

    /** Soft pool of light along the bottom edge of the volume. */
    const val FLOOR_DIFFUSION = 0.26f
    const val FLOOR_GLOW = 0.16f

    /** Wide, faint boundary bloom. Only slightly whiter than the volume: no neon outline, no specular. */
    const val BLOOM_WIDTH = 0.10f
    const val BLOOM_CALM = 0.07f
    const val BLOOM_EXPRESSIVE = 0.11f
    const val BLOOM_WHITE = 0.10f

    /** Overall light level of the volume, and how opaque it is over the card behind it. */
    const val VOLUME_CALM = 0.42f

    /**
     * Light inside the fluid itself, as a fraction of the light it emits upward. It decays away from
     * the surface, so the fluid glows below its own boundary and dims downward.
     */
    const val VOLUME_INSIDE = 0.75f
    const val VOLUME_EXPRESSIVE = 0.13f
    const val VOLUME_ALPHA = 0.80f
    const val VOLUME_BLACK_ALPHA = 0.94f
    const val LIGHT_GAIN = 0.95f

    /**
     * Canvas fallback only: the fluid body is painted as nested, low-opacity layers instead of one
     * solid fill, so its own edge fades in instead of appearing as a step.
     */
    const val BODY_LAYERS = 5
    const val BODY_LAYER_OFFSET = 0.05f
    const val BODY_LAYER_ALPHA = 0.12f

    /** Width of the fallback's bloom stroke, and how much of the live shape the ambient shows. */
    const val FALLBACK_BLOOM_WIDTH_DP = 6f
    const val AMBIENT_MODE_GAIN = 0.55f
    const val AMBIENT_BULGE_GAIN = 0.30f

    /** Diffusion strokes for the fallback: many widths, low opacity, faintest and widest first. */
    private val _FALLBACK_STROKE_WIDTHS_DP = floatArrayOf(64f, 46f, 32f, 22f, 14f, 8f, 4f)
    val FALLBACK_STROKE_WIDTHS_DP: List<Float> get() = _FALLBACK_STROKE_WIDTHS_DP.asList()
    private val _FALLBACK_STROKE_ALPHAS = floatArrayOf(0.05f, 0.07f, 0.10f, 0.14f, 0.19f, 0.25f, 0.30f)
    val FALLBACK_STROKE_ALPHAS: List<Float> get() = _FALLBACK_STROKE_ALPHAS.asList()

    // ── Colour sweep ───────────────────────────────────────────────────────────────────────
    /**
     * The volume samples ONE cyclic spectrum along a single broad, low-frequency sweep. At calm the
     * sweep covers violet..blue only; at full expression it walks the wheel from violet round through
     * coral and yellow to cyan. Every sample is a neighbour of the next, so regions blend into each
     * other instead of averaging into grey, and the warm colours live inside the volume rather than
     * along its edge.
     */
    const val HUE_BASE = 0.98f
    const val HUE_SPAN_CALM = 0.14f
    const val HUE_SPAN_EXPRESSIVE = 0.90f

    /** The Canvas fallback prebuilds one sweep per bucket, so it never cross-fades two sweeps. */
    const val SWEEP_BUCKETS = 21
    const val SWEEP_STOPS = 9

    // ── Ambient light above the composer ───────────────────────────────────────────────────
    /** Height of the ambient boundary inside the band, and how far voice energy lifts it. */
    const val AMBIENT_BASE = 0.62f
    const val AMBIENT_LIFT = 0.10f

    /** Ambient deformation, stretched horizontally and moving more slowly than the composer surface. */
    const val AMBIENT_SCALE_CALM = 0.035f
    const val AMBIENT_SCALE_ENERGY = 0.045f
    const val AMBIENT_X_SCALE = 1.05f
    const val AMBIENT_TIME_SCALE = 0.62f
    const val AMBIENT_TIME_SHIFT = 1.1f

    /** Ambient opacity and its diffusion strokes: a whisper of light, not a wash over the chat. */
    const val AMBIENT_ALPHA_FLOOR = 0.10f
    const val AMBIENT_ALPHA_ENERGY = 0.22f
    const val AMBIENT_ALPHA_EXPRESSION = 0.10f
    private val _AMBIENT_STROKE_WIDTHS_DP = floatArrayOf(76f, 54f, 38f, 26f, 16f, 9f)
    val AMBIENT_STROKE_WIDTHS_DP: List<Float> get() = _AMBIENT_STROKE_WIDTHS_DP.asList()
    private val _AMBIENT_STROKE_ALPHAS = floatArrayOf(0.045f, 0.06f, 0.08f, 0.11f, 0.15f, 0.19f)
    val AMBIENT_STROKE_ALPHAS: List<Float> get() = _AMBIENT_STROKE_ALPHAS.asList()

    /**
     * Top of the ambient drawing area, band-relative (0 = top of the band). Everything the ambient
     * draws is clipped below this line, and the widest stroke stays well under it - the guard is
     * invisible rather than an edge.
     */
    const val AMBIENT_CLIP_TOP = 0.16f

    /** Silence floor: the surface never collapses completely while the microphone is live. */
    const val MIN_AMPLITUDE = 0.08f
    const val MAX_AMPLITUDE = 1f

    /** Subdued resting energy used while a locked recording is paused. */
    const val PAUSED_ENERGY = 0.12f

    const val TAU = 6.2831855f

    const val MAX_FRAME_DELTA_SECONDS = 0.05f

    /**
     * Draft playback is a review state, so the field stays clearly calmer than a live take while
     * keeping the same colour language as recording.
     */
    const val DRAFT_PRESENCE = 0.72f

    /** Alpha of the not-yet-played part of the draft waveform, drawn with the spectrum brush. */
    const val WAVEFORM_IDLE_ALPHA = 0.30f

    /** Alpha of the played part of the draft waveform. */
    const val WAVEFORM_PLAYED_ALPHA = 0.95f
}

/**
 * Ordered colour sweep for the draft waveform player. It is the same Google-spectrum the energy
 * field blends through, so the preview reads as part of one surface instead of a single flat
 * accent colour; the gradient itself does the blending (every bar samples the next hue).
 */
internal fun voiceEnergySpectrum(): List<Color> = listOf(
    VoiceEnergyColors.Blue,
    VoiceEnergyColors.Cyan,
    VoiceEnergyColors.Violet,
    VoiceEnergyColors.Pink,
    VoiceEnergyColors.Coral,
    VoiceEnergyColors.Amber,
    VoiceEnergyColors.Green
)
