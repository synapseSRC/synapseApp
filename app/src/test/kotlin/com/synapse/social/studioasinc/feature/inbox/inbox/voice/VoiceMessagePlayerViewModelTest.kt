package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import android.content.Context
import com.synapse.social.studioasinc.core.media.VoicePlaybackState
import com.synapse.social.studioasinc.core.media.VoicePlayerManager
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceMessagePlayerViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val voicePlayerManager = mock(VoicePlayerManager::class.java)
    private val managerStateFlow = MutableStateFlow(VoicePlaybackState())

    private class FakeVoiceDownloadCache : VoiceDownloadCache(mock(HttpClient::class.java), mock(Context::class.java)) {
        var resultToReturn: Result<String> = Result.success("/local/cache/voice1.m4a")
        override suspend fun getLocalPath(url: String): Result<String> = resultToReturn
    }

    private val fakeVoiceDownloadCache = FakeVoiceDownloadCache()
    private lateinit var viewModel: VoiceMessagePlayerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        `when`(voicePlayerManager.playbackState).thenReturn(managerStateFlow)
        viewModel = VoiceMessagePlayerViewModel(
            voicePlayerManager = voicePlayerManager,
            voiceDownloadCache = fakeVoiceDownloadCache
        ).apply {
            ioDispatcher = testDispatcher
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialize sets mediaUrl and loads local path successfully`() = runTest {
        val testUrl = "https://cdn.example.com/voice1.m4a"
        fakeVoiceDownloadCache.resultToReturn = Result.success("/local/cache/voice1.m4a")

        viewModel.initialize(testUrl)

        val state = viewModel.uiState.value
        assertEquals(testUrl, state.mediaUrl)
        assertEquals("/local/cache/voice1.m4a", state.localPath)
        assertFalse(state.isDownloading)
        assertNull(state.downloadError)
        assertTrue(state.waveformAmplitudes.isNotEmpty())
    }

    @Test
    fun `initialize sets downloadError when voiceDownloadCache fails`() = runTest {
        val testUrl = "https://cdn.example.com/fail.m4a"
        fakeVoiceDownloadCache.resultToReturn = Result.failure(RuntimeException("Network timeout"))

        viewModel.initialize(testUrl)

        val state = viewModel.uiState.value
        assertEquals(testUrl, state.mediaUrl)
        assertNull(state.localPath)
        assertFalse(state.isDownloading)
        assertEquals("Network timeout", state.downloadError)
    }

    @Test
    fun `onPlayPauseClicked starts playback when localPath is available`() = runTest {
        val testUrl = "https://cdn.example.com/voice1.m4a"
        fakeVoiceDownloadCache.resultToReturn = Result.success("/local/cache/voice1.m4a")

        viewModel.initialize(testUrl)

        viewModel.onPlayPauseClicked()

        verify(voicePlayerManager).play(
            url = testUrl,
            mediaPath = "/local/cache/voice1.m4a",
            speed = 1.0f
        )
    }

    @Test
    fun `onPlayPauseClicked pauses when already playing`() = runTest {
        val testUrl = "https://cdn.example.com/voice1.m4a"
        fakeVoiceDownloadCache.resultToReturn = Result.success("/local/cache/voice1.m4a")

        viewModel.initialize(testUrl)

        // Simulate active playing state
        managerStateFlow.value = VoicePlaybackState(
            activeUrl = testUrl,
            isPlaying = true,
            currentPositionMs = 5000L,
            durationMs = 20000L
        )

        viewModel.onPlayPauseClicked()

        verify(voicePlayerManager).pause()
    }

    @Test
    fun `onSpeedToggle cycles speed 1x to 1,5x to 2x to 1x`() = runTest {
        val testUrl = "https://cdn.example.com/voice1.m4a"
        fakeVoiceDownloadCache.resultToReturn = Result.success("/local/cache/voice1.m4a")

        viewModel.initialize(testUrl)

        managerStateFlow.value = VoicePlaybackState(activeUrl = testUrl)

        // Initial speed is 1.0f
        assertEquals(1.0f, viewModel.uiState.value.playbackSpeed, 0.01f)

        // Toggle 1: 1.0f -> 1.5f
        viewModel.onSpeedToggle()
        assertEquals(1.5f, viewModel.uiState.value.playbackSpeed, 0.01f)
        verify(voicePlayerManager).setPlaybackSpeed(1.5f)

        // Toggle 2: 1.5f -> 2.0f
        viewModel.onSpeedToggle()
        assertEquals(2.0f, viewModel.uiState.value.playbackSpeed, 0.01f)
        verify(voicePlayerManager).setPlaybackSpeed(2.0f)

        // Toggle 3: 2.0f -> 1.0f
        viewModel.onSpeedToggle()
        assertEquals(1.0f, viewModel.uiState.value.playbackSpeed, 0.01f)
        verify(voicePlayerManager).setPlaybackSpeed(1.0f)
    }

    @Test
    fun `onSeek calculates target position and calls seekTo on manager`() = runTest {
        val testUrl = "https://cdn.example.com/voice1.m4a"
        fakeVoiceDownloadCache.resultToReturn = Result.success("/local/cache/voice1.m4a")

        viewModel.initialize(testUrl)

        managerStateFlow.value = VoicePlaybackState(
            activeUrl = testUrl,
            isPlaying = true,
            durationMs = 20000L
        )

        viewModel.onSeek(0.5f) // Seek to 50% = 10000ms

        assertEquals(10000L, viewModel.uiState.value.currentPositionMs)
        verify(voicePlayerManager).seekTo(10000L)
    }

    @Test
    fun `another active player pauses local player state while preserving position`() = runTest {
        val testUrl1 = "https://cdn.example.com/voice1.m4a"
        val testUrl2 = "https://cdn.example.com/voice2.m4a"

        fakeVoiceDownloadCache.resultToReturn = Result.success("/local/cache/voice1.m4a")

        viewModel.initialize(testUrl1)

        // Voice 1 is currently playing at 5 seconds
        managerStateFlow.value = VoicePlaybackState(
            activeUrl = testUrl1,
            isPlaying = true,
            currentPositionMs = 5000L,
            durationMs = 15000L
        )

        assertTrue(viewModel.uiState.value.isPlaying)
        assertEquals(5000L, viewModel.uiState.value.currentPositionMs)

        // Voice 2 starts playing (single active player rule)
        managerStateFlow.value = VoicePlaybackState(
            activeUrl = testUrl2,
            isPlaying = true,
            currentPositionMs = 1000L,
            durationMs = 10000L
        )

        // Voice 1 should now be paused, but position preserved at 5 seconds
        assertFalse(viewModel.uiState.value.isPlaying)
        assertEquals(5000L, viewModel.uiState.value.currentPositionMs)
    }
}
