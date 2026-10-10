package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceWaveformExtractorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `extractAmplitudes produces requested bar count with normalized values`() = runTest {
        val testFile = tempFolder.newFile("test_audio.m4a")
        // Write mock audio bytes
        testFile.writeBytes(ByteArray(1000) { (it % 128).toByte() })

        val barCount = 36
        val amplitudes = VoiceWaveformExtractor.extractAmplitudes(
            filePath = testFile.absolutePath,
            barCount = barCount,
            dispatcher = testDispatcher
        )

        assertEquals(barCount, amplitudes.size)
        amplitudes.forEach { amp ->
            assertTrue("Amplitude $amp should be >= 0.15f", amp >= 0.15f)
            assertTrue("Amplitude $amp should be <= 1.0f", amp <= 1.0f)
        }
    }

    @Test
    fun `extractAmplitudes produces deterministic results for same file`() = runTest {
        val testFile = tempFolder.newFile("deterministic_audio.m4a")
        testFile.writeBytes(ByteArray(500) { (it * 3).toByte() })

        val run1 = VoiceWaveformExtractor.extractAmplitudes(
            filePath = testFile.absolutePath,
            barCount = 32,
            dispatcher = testDispatcher
        )
        val run2 = VoiceWaveformExtractor.extractAmplitudes(
            filePath = testFile.absolutePath,
            barCount = 32,
            dispatcher = testDispatcher
        )

        assertEquals(run1, run2)
    }

    @Test
    fun `extractAmplitudes falls back gracefully for non-existent or empty file`() = runTest {
        val missingPath = tempFolder.root.absolutePath + "/missing.m4a"
        val barCount = 36

        val amplitudes = VoiceWaveformExtractor.extractAmplitudes(
            filePath = missingPath,
            barCount = barCount,
            dispatcher = testDispatcher
        )

        assertEquals(barCount, amplitudes.size)
        amplitudes.forEach { amp ->
            assertTrue(amp in 0.15f..1.0f)
        }
    }

    @Test
    fun `generateFallbackAmplitudes produces consistent values for same seed string`() {
        val seed = "https://example.com/voice/123.m4a"
        val result1 = VoiceWaveformExtractor.generateFallbackAmplitudes(seed, 30)
        val result2 = VoiceWaveformExtractor.generateFallbackAmplitudes(seed, 30)

        assertEquals(result1, result2)
        assertEquals(30, result1.size)
    }
}
