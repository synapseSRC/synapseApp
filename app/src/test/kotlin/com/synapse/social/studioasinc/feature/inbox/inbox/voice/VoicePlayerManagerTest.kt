package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.synapse.social.studioasinc.core.media.VoicePlayerManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class VoicePlayerManagerTest {

    private lateinit var context: Context
    private lateinit var voicePlayerManager: VoicePlayerManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        voicePlayerManager = VoicePlayerManager(context)
    }

    @Test
    fun `play sets activeUrl and playback state`() = runTest {
        val url = "https://cdn.example.com/audio1.m4a"
        val path = "/path/to/audio1.m4a"

        voicePlayerManager.play(url, path, speed = 1.0f)

        val state = voicePlayerManager.playbackState.value
        assertEquals(url, state.activeUrl)
        assertEquals(1.0f, state.playbackSpeed, 0.01f)
    }

    @Test
    fun `playing second audio stops first audio under single active player rule`() = runTest {
        val url1 = "https://cdn.example.com/audio1.m4a"
        val path1 = "/path/to/audio1.m4a"

        val url2 = "https://cdn.example.com/audio2.m4a"
        val path2 = "/path/to/audio2.m4a"

        voicePlayerManager.play(url1, path1)
        assertEquals(url1, voicePlayerManager.playbackState.value.activeUrl)

        // Start playing second voice message
        voicePlayerManager.play(url2, path2)

        // First should be superseded by second voice message as activeUrl
        val state = voicePlayerManager.playbackState.value
        assertEquals(url2, state.activeUrl)
    }

    @Test
    fun `setPlaybackSpeed updates speed in state`() = runTest {
        val url = "https://cdn.example.com/audio1.m4a"
        val path = "/path/to/audio1.m4a"

        voicePlayerManager.play(url, path, speed = 1.0f)
        voicePlayerManager.setPlaybackSpeed(1.5f)

        assertEquals(1.5f, voicePlayerManager.playbackState.value.playbackSpeed, 0.01f)
    }

    @Test
    fun `pause updates isPlaying to false`() = runTest {
        val url = "https://cdn.example.com/audio1.m4a"
        val path = "/path/to/audio1.m4a"

        voicePlayerManager.play(url, path)
        voicePlayerManager.pause()

        assertFalse(voicePlayerManager.playbackState.value.isPlaying)
    }

    @Test
    fun `stop resets playback state`() = runTest {
        val url = "https://cdn.example.com/audio1.m4a"
        val path = "/path/to/audio1.m4a"

        voicePlayerManager.play(url, path)
        voicePlayerManager.stop()

        val state = voicePlayerManager.playbackState.value
        assertNull(state.activeUrl)
        assertFalse(state.isPlaying)
    }
}
