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

    // Lighting.
    val Specular = Color(0xFFFFFFFF)
}

/**
 * Tunable constants of the voice energy field. Grouped here so the animation architecture is
 * inspectable in one place instead of being spread across the renderer.
 */
internal object VoiceEnergyDesign {
    // ── Composition: how much of the composer the energy body occupies ───────────────────────
    // These numbers are the amplitude -> geometry contract. The AGSL shader receives the values
    // derived from them as uniforms (iBodyTop, iCrestScale, iBand*), and the Canvas fallback reads
    // the same composition object, so both renderers stay in step.
    //
    // Coverage of the composer's interior, measured against the simulated field:
    //   silence      -> body crest at 0.62, i.e. ~38% covered, ~2dp of idle undulation
    //   normal voice -> ~43% covered, roughly 5dp of crest travel with peaks around ~9dp
    //   loud voice   -> ~47% covered, roughly 9dp of travel with peaks around ~14dp
    // The mean stays inside the 35-55% band; only crest peaks reach past it.
    const val BODY_TOP_CALM = 0.62f
    const val BODY_TOP_FULL = 0.52f

    /**
     * Organic travel of the crest around its mean height, in composer-relative units. The calm value
     * matters more than it looks: at 0.08 the contour was visually a straight rule when the
     * microphone was quiet, which is why the surface read as a bar rather than a surface.
     */
    const val CREST_CALM = 0.16f
    const val CREST_PER_ENERGY = 0.20f

    /** Extra crest travel contributed by a fresh amplitude impulse. */
    const val CREST_PER_RIPPLE = 0.045f

    /**
     * Slow idle undulation, applied whether or not the microphone reports anything. This is decorative
     * motion at the same phase as the fluid - never a substitute amplitude value. The shader receives
     * both numbers as uniforms so its silhouette matches the Canvas fallback's term for term.
     */
    const val IDLE_WAVE_CALM = 0.22f
    const val IDLE_WAVE_PER_ENERGY = 0.14f

    /** The two ribbon bands floating above the crest: vertical offset and band half-width. */
    const val BAND_1_OFFSET_CALM = 0.10f
    const val BAND_1_OFFSET_PER_ENERGY = 0.06f
    const val BAND_1_WIDTH = 0.070f
    const val BAND_1_WIDTH_PER_ENERGY = 0.040f
    const val BAND_2_OFFSET_CALM = 0.21f
    const val BAND_2_OFFSET_PER_ENERGY = 0.10f
    const val BAND_2_WIDTH = 0.100f
    const val BAND_2_WIDTH_PER_ENERGY = 0.050f

    /**
     * Per-layer silhouette shaping. The shader mirrors these as its band1/band2 terms, so changing a
     * value here means changing the matching literal in the AGSL profile as well.
     */
    const val LAYER_1_X_SCALE = 0.68f
    const val LAYER_1_TIME_SCALE = 0.71f
    const val LAYER_1_SHAPE = 0.62f
    const val LAYER_2_X_SCALE = 1.42f
    const val LAYER_2_TIME_SCALE = 0.53f
    const val LAYER_2_SHAPE = 0.48f

    /** Opacity of each ribbon band at its centre. The shader reads these as uniforms. */
    const val BAND_1_WEIGHT = 0.26f
    const val BAND_2_WEIGHT = 0.18f

    /**
     * How much warm colour the spectrum picks up with voice energy. Warmth no longer swaps a cool
     * gradient for a warm one - the spectrum is a single wheel - it stretches and shifts the hue
     * index, so louder speech walks further around the wheel instead of desaturating toward grey.
     */
    const val WARMTH_GATE_LOW = 0.15f
    const val WARMTH_GATE_HIGH = 0.72f

    // ── Renderer composition ────────────────────────────────────────────────────────────────
    // Alpha and brightness of the layers. The aim is a dark translucent foundation that the spectrum
    // illuminates from within; the previous values laid a bright, desaturated slab over the capsule.
    const val BODY_ALPHA_CALM = 0.38f
    const val BODY_ALPHA_PER_ENERGY = 0.28f

    /** Illuminated leading edge at the crest: a coloured glow, and no white specular line at all. */
    const val CREST_ALPHA_CALM = 0.20f
    const val CREST_ALPHA_PER_ENERGY = 0.26f

    /** Vertical brightness falloff: dimmer toward the top, so the timer and controls keep contrast. */
    const val CEILING_FLOOR = 0.36f
    const val CEILING_RANGE = 0.64f

    const val LUMA_CALM = 0.64f
    const val LUMA_PER_ENERGY = 0.22f

    /**
     * Concentrated luminous regions. Each formation drifts with an amplitude-locked speed and hangs
     * below the local crest, so it always sits inside the body and never behind the controls.
     */
    const val CORE_TRAVEL_TIME = 0.055f
    const val CORE_TRAVEL_DRIFT = 0.02f
    const val CORE_ALPHA_CALM = 0.10f
    const val CORE_ALPHA_PER_ENERGY = 0.10f
    const val CORE_A_DRIFT_X = 0.14f
    const val CORE_A_DROP = 0.075f
    const val CORE_A_DROP_ENERGY = 0.055f
    const val CORE_A_WOBBLE = 0.020f
    const val CORE_A_RADIUS_X = 0.21f
    const val CORE_A_RADIUS_X_ENERGY = 0.05f
    const val CORE_A_RADIUS_Y = 0.080f
    const val CORE_A_RADIUS_Y_ENERGY = 0.035f
    const val CORE_B_DRIFT_X = 0.13f
    const val CORE_B_DROP = 0.145f
    const val CORE_B_DROP_ENERGY = 0.075f
    const val CORE_B_WOBBLE = 0.018f
    const val CORE_B_RADIUS_X = 0.17f
    const val CORE_B_RADIUS_X_ENERGY = 0.04f
    const val CORE_B_RADIUS_Y = 0.065f
    const val CORE_B_RADIUS_Y_ENERGY = 0.030f

    // ── Microphone drive ────────────────────────────────────────────────────────────────────
    /** Continuous modal force on the fluid surface: floor, per unit of energy, and gain. */
    const val DRIVE_FLOOR = 0.05f
    const val DRIVE_ENERGY = 0.55f
    const val DRIVE_GAIN = 10f

    /** Constant phase rate of the drive. Decoupled from the energy on purpose (see VoiceEnergyField). */
    const val DRIVE_RATE = 0.55f

    /** Scale of the modal impulse fired when the microphone envelope rises. */
    const val IMPULSE_KICK = 0.15f

    // ── Ambient band above the composer ─────────────────────────────────────────────────────
    /**
     * Base height of the lowest ambient wave (band-relative) and how far voice energy lifts it.
     * Calibrated so the waves clear the composer itself: at normal voice they sit 60-120dp above the
     * bottom edge of the 184dp ambient band, i.e. above the input bar rather than behind it.
     */
    const val AMBIENT_BASE = 0.72f
    const val AMBIENT_LIFT = 0.16f
    const val AMBIENT_LAYER_STEP = 0.13f

    /**
     * Wave amplitude: at rest and per unit of energy, plus the damping of higher layers. Bounded on
     * purpose - with these values the highest wave crest stays around 0.25 of the band, comfortably
     * inside [AMBIENT_CLIP_TOP], so the safety clip is never actually reached and cannot draw an edge.
     */
    const val AMBIENT_SCALE_CALM = 0.06f
    const val AMBIENT_SCALE_ENERGY = 0.06f
    const val AMBIENT_LAYER_SCALE_STEP = 0.30f

    /**
     * Ambient waves run across more than one cycle and move more slowly than the surface: at 0.55 of a
     * cycle across the full width the contours were so shallow that they read as straight rules.
     */
    const val AMBIENT_X_SCALE = 1.05f
    const val AMBIENT_TIME_SCALE = 0.62f
    const val AMBIENT_TIME_SHIFT = 1.1f
    const val AMBIENT_TIME_SHIFT_STEP = 1.9f

    /** Ambient wave and pool opacity, and the damping of higher layers. Kept deliberately subtle. */
    const val AMBIENT_ALPHA_FLOOR = 0.26f
    const val AMBIENT_ALPHA_ENERGY = 0.34f
    const val AMBIENT_ALPHA_LAYER_STEP = 0.30f
    const val AMBIENT_POOL_ALPHA = 0.05f
    const val AMBIENT_POOL_ALPHA_ENERGY = 0.12f
    const val AMBIENT_POOL_RADIUS_WIDTH = 0.26f
    const val AMBIENT_POOL_RADIUS_HEIGHT = 0.30f
    const val AMBIENT_POOL_RADIUS_CALM = 0.60f
    const val AMBIENT_POOL_RADIUS_ENERGY = 0.20f

    /**
     * Top of the ambient drawing area, band-relative (0 = top of the band). Everything the ambient
     * draws is clipped below this line so chat content above it can never be touched.
     */
    const val AMBIENT_CLIP_TOP = 0.20f

    /** Silence floor: the surface never collapses completely while the microphone is live. */
    const val MIN_AMPLITUDE = 0.08f
    const val MAX_AMPLITUDE = 1f

    /** Subdued resting energy used while a locked recording is paused. */
    const val PAUSED_ENERGY = 0.12f

    const val TAU = 6.2831855f

    /** Spring-coupled fluid nodes that carry the live audio energy. */
    const val FLUID_NODES = 24

    /** Number of harmonics extracted from the fluid simulation and fed to the renderer. */
    const val HARMONICS = 3

    /** Upper clamp for a simulated harmonic so the silhouette can never become chaotic. */
    const val MAX_HARMONIC = 0.62f

    /** How strongly a rising microphone envelope kicks the fluid surface. */
    const val IMPULSE_GAIN = 6f

    /** Samples used to trace the organic silhouette in the Canvas fallback (API 26..32). */
    const val FALLBACK_SAMPLES = 28

    const val MAX_FRAME_DELTA_SECONDS = 0.05f

    /** Drift of the secondary energy ribbons, expressed as a time shift in seconds. */
    const val RIBBON_TIME_SHIFT_1 = 1.7f
    const val RIBBON_TIME_SHIFT_2 = 4.3f

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
