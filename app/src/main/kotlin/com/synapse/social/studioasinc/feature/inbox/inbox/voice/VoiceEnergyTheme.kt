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
    // values (iBodyTop, iCrestScale, iWarmth) as uniforms and the Canvas fallback reads the same
    // composition object, so the two renderers cannot drift apart.
    //
    // Two personalities, driven by the real microphone envelope:
    //   calm       -> boundary near 0.76 of the height, only a couple of dp of movement, and a
    //                 blue/violet volume with a wide, soft diffusion
    //   expressive -> boundary around 0.60 with pronounced freeform deformation and the whole
    //                 Google spectrum present in the volume
    const val BODY_TOP_CALM = 0.76f
    const val BODY_TOP_FULL = 0.60f

    /** Freeform deformation of the boundary: at rest, per unit of energy, and per amplitude impulse. */
    const val CREST_CALM = 0.07f
    const val CREST_PER_ENERGY = 0.09f
    const val CREST_PER_RIPPLE = 0.02f

    /**
     * Slow idle undulation, applied whether or not the microphone reports anything. This is decorative
     * motion at the fluid's own phase - never a substitute amplitude value. The shader reads both
     * numbers as uniforms, so its silhouette matches the Canvas fallback's term for term.
     */
    const val IDLE_WAVE_CALM = 0.30f
    const val IDLE_WAVE_PER_ENERGY = 0.18f

    /**
     * Where the recording sits between the two personalities, per unit of amplitude. This single
     * value moves the diffusion, the deformation and the colour sweep together, which is what makes
     * the transition feel like one fluid changing rather than several effects fading.
     */
    const val WARMTH_GATE_LOW = 0.15f
    const val WARMTH_GATE_HIGH = 0.72f

    // ── Microphone drive ────────────────────────────────────────────────────────────────────
    /** Continuous modal force on the fluid surface: floor, per unit of energy, and gain. */
    const val DRIVE_FLOOR = 0.05f
    const val DRIVE_ENERGY = 0.55f
    const val DRIVE_GAIN = 10f

    /** Constant phase rate of the drive, deliberately decoupled from the energy. */
    const val DRIVE_RATE = 0.55f

    /** Scale of the modal impulse fired when the microphone envelope rises. */
    const val IMPULSE_KICK = 0.15f

    // ── Volumetric light ───────────────────────────────────────────────────────────────────
    /** How far light diffuses above the boundary: wide and soft when calm, tighter when expressive. */
    const val DIFFUSION_CALM = 0.60f
    const val DIFFUSION_EXPRESSIVE = 0.42f

    /** Narrow halo hugging the boundary. This is what makes the light look emitted, not flat. */
    const val HALO_WIDTH = 0.10f
    const val HALO_STRENGTH = 0.14f

    /** Soft pool of light along the bottom edge of the volume. */
    const val FLOOR_DIFFUSION = 0.22f
    const val FLOOR_GLOW = 0.20f

    /** Emissive boundary bloom: tight, only slightly whiter than the volume, never a neon outline. */
    const val BLOOM_WIDTH = 0.035f
    const val BLOOM_CALM = 0.10f
    const val BLOOM_EXPRESSIVE = 0.16f
    const val BLOOM_WHITE = 0.22f

    /** Overall light level of the volume, and how opaque it is over the card behind it. */
    const val VOLUME_CALM = 0.34f

    /**
     * Light inside the fluid itself, as a fraction of the light it emits upward. Without it the
     * boundary was a step from lit to unlit, which reads as a drawn line; with it the fluid glows
     * below its own surface and the boundary becomes a soft ridge instead of an edge.
     */
    const val VOLUME_INSIDE = 0.34f
    const val VOLUME_EXPRESSIVE = 0.13f
    const val VOLUME_ALPHA = 0.80f
    const val VOLUME_BLACK_ALPHA = 0.94f

    /**
     * Canvas fallback only: how hard the fluid body is painted over the light that crosses it. It is
     * the fallback's way of reproducing the difference between light inside the fluid and light
     * emitted above its surface.
     */
    const val BODY_VEIL_ALPHA = 0.62f
    const val LIGHT_GAIN = 0.95f

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

    /** Ambient opacity: deliberately a whisper of light rather than a wash over the chat. */
    const val AMBIENT_ALPHA_FLOOR = 0.10f
    const val AMBIENT_ALPHA_ENERGY = 0.22f
    const val AMBIENT_ALPHA_EXPRESSION = 0.10f

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
