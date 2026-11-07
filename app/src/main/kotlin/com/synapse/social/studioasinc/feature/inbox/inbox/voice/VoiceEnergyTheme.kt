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
    //   silence      -> body crest at 0.62, i.e. ~38% covered, ~0.4dp of travel (effectively still)
    //   normal voice -> ~45% covered, ~4dp of crest travel with peaks around ~6dp
    //   loud voice   -> ~51% covered, ~8dp of travel with peaks around ~12dp
    // The mean stays inside the 35-55% band; only crest peaks reach past it.
    const val BODY_TOP_CALM = 0.62f
    const val BODY_TOP_FULL = 0.52f

    /** Organic travel of the crest around its mean height, in composer-relative units. */
    const val CREST_CALM = 0.08f
    const val CREST_PER_ENERGY = 0.20f

    /** Extra crest travel contributed by a fresh amplitude impulse. */
    const val CREST_PER_RIPPLE = 0.08f

    /** The two ribbon bands floating above the crest: vertical offset and band half-width. */
    const val BAND_1_OFFSET_CALM = 0.10f
    const val BAND_1_OFFSET_PER_ENERGY = 0.06f
    const val BAND_1_WIDTH = 0.070f
    const val BAND_1_WIDTH_PER_ENERGY = 0.040f
    const val BAND_2_OFFSET_CALM = 0.21f
    const val BAND_2_OFFSET_PER_ENERGY = 0.10f
    const val BAND_2_WIDTH = 0.100f
    const val BAND_2_WIDTH_PER_ENERGY = 0.050f

    /** Per-layer silhouette shaping. The shader mirrors these as its band1/band2 terms. */
    const val LAYER_1_X_SCALE = 0.83f
    const val LAYER_1_TIME_SCALE = 0.71f
    const val LAYER_1_SHAPE = 0.62f
    const val LAYER_2_X_SCALE = 1.17f
    const val LAYER_2_TIME_SCALE = 0.53f
    const val LAYER_2_SHAPE = 0.48f

    /** Ribbon band opacity: at silence, per unit of energy, and dimmer for the higher band. */
    const val BAND_ALPHA_FLOOR = 0.55f
    const val BAND_ALPHA_PER_ENERGY = 0.45f
    const val BAND_ALPHA_LAYER_STEP = 0.18f

    /**
     * Warm (Google accent) colours start mixing in here, per unit of composer amplitude: silence
     * stays blue/violet/cyan, normal speech is already warm-dominant, a shout saturates.
     */
    const val WARMTH_GATE_LOW = 0.15f
    const val WARMTH_GATE_HIGH = 0.72f

    /**
     * Top of the warm stratum, composer-relative: below it the warm accents take over. The shader
     * ramps this smoothly (0.40 to 0.86), the Canvas fallback clips it at this single line.
     */
    const val WARM_STRATUM_TOP = 0.38f

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
     * Calibrated so the waves clear the composer itself: at normal voice they sit 60-140dp above the
     * bottom edge of the 184dp ambient band, i.e. above the input bar rather than behind it.
     */
    const val AMBIENT_BASE = 0.66f
    const val AMBIENT_LIFT = 0.30f
    const val AMBIENT_LAYER_STEP = 0.15f

    /** Wave amplitude: at rest and per unit of energy, plus the damping of higher layers. */
    const val AMBIENT_SCALE_CALM = 0.05f
    const val AMBIENT_SCALE_ENERGY = 0.09f
    const val AMBIENT_LAYER_SCALE_STEP = 0.15f

    /** Ambient waves are stretched horizontally and move more slowly than the surface. */
    const val AMBIENT_X_SCALE = 0.55f
    const val AMBIENT_TIME_SCALE = 0.62f
    const val AMBIENT_TIME_SHIFT = 1.1f
    const val AMBIENT_TIME_SHIFT_STEP = 1.9f

    /** Ambient wave and pool opacity, and the damping of higher layers. */
    const val AMBIENT_ALPHA_FLOOR = 0.60f
    const val AMBIENT_ALPHA_ENERGY = 0.40f
    const val AMBIENT_ALPHA_LAYER_STEP = 0.26f
    const val AMBIENT_POOL_ALPHA = 0.06f
    const val AMBIENT_POOL_ALPHA_ENERGY = 0.16f
    const val AMBIENT_POOL_RADIUS_WIDTH = 0.55f
    const val AMBIENT_POOL_RADIUS_HEIGHT = 0.80f
    const val AMBIENT_POOL_RADIUS_CALM = 0.90f
    const val AMBIENT_POOL_RADIUS_ENERGY = 0.18f

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
