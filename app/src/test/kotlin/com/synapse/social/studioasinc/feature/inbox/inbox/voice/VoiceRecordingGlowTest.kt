package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceRecordingGlowTest {
    @Test
    fun `glow follows the latest microphone amplitude and quiet remains nonzero`() {
        val quiet = VoiceState.Recording(amplitudeHistory = listOf(0.08f, 0.12f))
        val loud = VoiceState.Recording(amplitudeHistory = listOf(0.08f, 0.86f))
        val paused = VoiceState.Locked(amplitudeHistory = listOf(0.94f), isPaused = true)

        assertEquals(0.12f, quiet.glowAmplitude(), 0.001f)
        assertEquals(0.86f, loud.glowAmplitude(), 0.001f)
        assertTrue(loud.glowAmplitude() > quiet.glowAmplitude())
        assertEquals(0.08f, paused.glowAmplitude(), 0.001f)
    }
}
