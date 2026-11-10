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
