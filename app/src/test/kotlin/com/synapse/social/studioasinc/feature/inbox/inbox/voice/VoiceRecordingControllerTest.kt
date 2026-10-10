package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import java.io.File

@Suppress("UNCHECKED_CAST")
private fun <T> anyObject(): T {
    any<T>()
    return null as T
}

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceRecordingControllerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val testTimeProvider = TestTimeProvider(1000000000L)

    private lateinit var mockRecorder: VoiceRecorder
    private lateinit var controller: VoiceRecordingController

    private var hapticCount = 0
    private var sentFile: File? = null
    private var sendShouldSucceed = true
    private var sendErrorMessage: String? = null

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockRecorder = mock(VoiceRecorder::class.java)
        `when`(mockRecorder.amplitudeFlow).thenReturn(MutableStateFlow(0))
        hapticCount = 0
        sentFile = null
        sendShouldSucceed = true
        sendErrorMessage = null

        controller = VoiceRecordingController(
            scope = testScope,
            voiceRecorder = mockRecorder,
            voicePlayerManager = null,
            getOutputFile = { tempFolder.newFile("test_audio.m4a") },
            timeProvider = testTimeProvider,
            onHapticPerform = { hapticCount++ },
            onSendVoiceMessage = { file, _, onResult ->
                sentFile = file
                onResult(sendShouldSucceed, sendErrorMessage)
                null
            },
            gestureConfig = VoiceGestureConfig(
                holdThresholdMs = 180L,
                cancelThresholdDp = 100f,
                lockThresholdDp = 90f,
                directionSlopDp = 12f
            ),
            cancelThresholdPx = 100f,
            lockThresholdPx = 90f,
            directionSlopPx = 12f
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `hold mic directly from Normal state enters Pressing then Recording`() {
        assertEquals(VoiceState.Normal, controller.state.value)

        // Hold starts directly from Normal
        controller.handleAction(VoiceAction.StartRecording)
        assertEquals(VoiceState.Pressing, controller.state.value)

        testScope.advanceTimeBy(200)

        assertTrue(controller.state.value is VoiceState.Recording)
        verify(mockRecorder, times(1)).start(anyObject(), anyObject())
    }

    @Test
    fun `single tap mic starts recording and enters locked state immediately`() {
        assertEquals(VoiceState.Normal, controller.state.value)

        controller.handleAction(VoiceAction.StartRecording)
        assertEquals(VoiceState.Pressing, controller.state.value)

        // Release before hold threshold (single tap)
        testScope.advanceTimeBy(50)
        controller.handleAction(VoiceAction.ReleaseRecording)

        assertTrue(controller.state.value is VoiceState.Locked)
        verify(mockRecorder, times(1)).start(anyObject(), anyObject())
    }

    @Test
    fun `pointer down and holding past threshold starts MediaRecorder and enters Recording`() {
        controller.handleAction(VoiceAction.OpenVoiceMode)
        controller.handleAction(VoiceAction.StartRecording)
        assertEquals(VoiceState.Pressing, controller.state.value)

        // Advance past 180ms threshold
        testScope.advanceTimeBy(200)

        assertTrue(controller.state.value is VoiceState.Recording)
        verify(mockRecorder, times(1)).start(anyObject(), anyObject())
    }

    @Test
    fun `dragging left beyond threshold arms cancel`() {
        controller.handleAction(VoiceAction.OpenVoiceMode)
        controller.handleAction(VoiceAction.StartRecording)
        testScope.advanceTimeBy(200)

        controller.handleAction(VoiceAction.Dragged(deltaX = -120f, deltaY = 0f))

        val state = controller.state.value as VoiceState.Recording
        assertTrue(state.isArmedForCancel)
        assertEquals(1f, state.cancelProgress, 0.01f)
    }

    @Test
    fun `releasing while armed for cancel deletes recording and returns to Normal`() {
        controller.handleAction(VoiceAction.StartRecording)
        testScope.advanceTimeBy(200)

        controller.handleAction(VoiceAction.Dragged(deltaX = -120f, deltaY = 0f))
        controller.handleAction(VoiceAction.ReleaseRecording)

        assertEquals(VoiceState.Normal, controller.state.value)
        verify(mockRecorder).cancel()
    }

    @Test
    fun `mid-gesture direction change - dragging up partway then left arms cancel and cancels on release`() {
        controller.handleAction(VoiceAction.StartRecording)
        testScope.advanceTimeBy(200)

        // Drag up partway (-40f)
        controller.handleAction(VoiceAction.Dragged(deltaX = 0f, deltaY = -40f))
        var state = controller.state.value as VoiceState.Recording
        assertEquals(0.44f, state.lockProgress, 0.05f)

        // Move finger back down and left (-120f)
        controller.handleAction(VoiceAction.Dragged(deltaX = -120f, deltaY = 40f))
        state = controller.state.value as VoiceState.Recording
        assertTrue(state.isArmedForCancel)

        // Release finger
        controller.handleAction(VoiceAction.ReleaseRecording)
        assertEquals(VoiceState.Normal, controller.state.value)
        verify(mockRecorder).cancel()
    }

    @Test
    fun `mid-gesture direction change - dragging left partway then up locks recording`() {
        controller.handleAction(VoiceAction.StartRecording)
        testScope.advanceTimeBy(200)

        // Drag left partway (-50f)
        controller.handleAction(VoiceAction.Dragged(deltaX = -50f, deltaY = 0f))
        var state = controller.state.value as VoiceState.Recording
        assertEquals(0.5f, state.cancelProgress, 0.05f)

        // Drag up beyond lock threshold (-100f)
        controller.handleAction(VoiceAction.Dragged(deltaX = 50f, deltaY = -100f))
        assertTrue(controller.state.value is VoiceState.Locked)
    }

    @Test
    fun `upward drag beyond lock threshold transitions to Locked`() {
        controller.handleAction(VoiceAction.OpenVoiceMode)
        controller.handleAction(VoiceAction.StartRecording)
        testScope.advanceTimeBy(200)

        controller.handleAction(VoiceAction.Dragged(deltaX = 0f, deltaY = -100f))

        assertTrue(controller.state.value is VoiceState.Locked)
    }

    @Test
    fun `releasing finger after locking keeps recording alive`() {
        controller.handleAction(VoiceAction.OpenVoiceMode)
        controller.handleAction(VoiceAction.StartRecording)
        testScope.advanceTimeBy(200)
        controller.handleAction(VoiceAction.Dragged(deltaX = 0f, deltaY = -100f))

        assertTrue(controller.state.value is VoiceState.Locked)

        controller.handleAction(VoiceAction.ReleaseRecording)

        assertTrue(controller.state.value is VoiceState.Locked)
    }

    @Test
    fun `atomic pause failure transitions to Failed state`() {
        controller.handleAction(VoiceAction.OpenVoiceMode)
        controller.handleAction(VoiceAction.StartRecording)
        testScope.advanceTimeBy(200)
        controller.handleAction(VoiceAction.Dragged(deltaX = 0f, deltaY = -100f))

        `when`(mockRecorder.pause()).thenThrow(RuntimeException("Pause failed"))

        controller.handleAction(VoiceAction.TogglePause)

        val state = controller.state.value
        assertTrue(state is VoiceState.Failed)
        assertEquals("Pause failed", (state as VoiceState.Failed).error)
    }

    @Test
    fun `finish recording produces VoiceDraft when file exists`() {
        val testFile = tempFolder.newFile("recording.m4a")
        testFile.writeText("audio data")
        `when`(mockRecorder.stop()).thenReturn(VoiceRecorderStopResult.Success(testFile))

        controller.handleAction(VoiceAction.OpenVoiceMode)
        controller.handleAction(VoiceAction.StartRecording)
        testScope.advanceTimeBy(200)
        testTimeProvider.advanceTimeBy(1000)

        controller.handleAction(VoiceAction.FinishRecording)

        val draftState = controller.state.value
        assertTrue(draftState is VoiceState.VoiceDraft)
        assertEquals(testFile, (draftState as VoiceState.VoiceDraft).audioFile)
    }

    @Test
    fun `stop EmptyOrTooShort gracefully resets to Normal`() {
        `when`(mockRecorder.stop()).thenReturn(VoiceRecorderStopResult.EmptyOrTooShort)

        controller.handleAction(VoiceAction.StartRecording)
        testScope.advanceTimeBy(200)

        controller.handleAction(VoiceAction.FinishRecording)

        assertEquals(VoiceState.Normal, controller.state.value)
    }

    @Test
    fun `send failure preserves draft file in Failed state and allows retry`() {
        val testFile = tempFolder.newFile("draft_fail.m4a")
        testFile.writeText("audio data")
        `when`(mockRecorder.stop()).thenReturn(VoiceRecorderStopResult.Success(testFile))

        controller.handleAction(VoiceAction.OpenVoiceMode)
        controller.handleAction(VoiceAction.StartRecording)
        testScope.advanceTimeBy(200)
        testTimeProvider.advanceTimeBy(1000)
        controller.handleAction(VoiceAction.FinishRecording)
        assertTrue("Expected VoiceDraft before send but was ${controller.state.value}", controller.state.value is VoiceState.VoiceDraft)

        sendShouldSucceed = false
        sendErrorMessage = "Network error"

        controller.handleAction(VoiceAction.SendDraft)
        testScope.advanceUntilIdle()

        val failedState = controller.state.value
        assertTrue("State error expectation failed: $failedState", failedState is VoiceState.Failed)
        assertEquals("Network error", (failedState as VoiceState.Failed).error)
        assertEquals(testFile, failedState.draftFile)
        assertTrue(testFile.exists())

        sendShouldSucceed = true
        controller.handleAction(VoiceAction.SendDraft)
        testScope.advanceUntilIdle()

        assertEquals(VoiceState.Normal, controller.state.value)
        assertFalse(testFile.exists())
    }

    @Test
    fun `permission flow - when granted after request permission, starts recording`() {
        controller.requestPermissionAndRecord()
        controller.onPermissionResult(true)

        testScope.advanceTimeBy(50)
        assertTrue(controller.state.value is VoiceState.Locked)
        verify(mockRecorder, times(1)).start(anyObject(), anyObject())
    }

    @Test
    fun `permission flow - when denied after request permission, resets safely`() {
        controller.requestPermissionAndRecord()
        controller.onPermissionResult(false)

        assertEquals(VoiceState.Normal, controller.state.value)
        verify(mockRecorder, never()).start(anyObject(), anyObject())
    }

    @Test
    fun `send failure preserves original recorded duration in Failed state and sends real duration on retry`() {
        val testFile = tempFolder.newFile("draft_duration.m4a")
        testFile.writeText("audio data")
        `when`(mockRecorder.stop()).thenReturn(VoiceRecorderStopResult.Success(testFile))

        controller.handleAction(VoiceAction.OpenVoiceMode)
        controller.handleAction(VoiceAction.StartRecording)
        testScope.advanceTimeBy(200)
        testTimeProvider.advanceTimeBy(12000) // 12 seconds
        controller.handleAction(VoiceAction.FinishRecording)

        val draftState = controller.state.value as VoiceState.VoiceDraft

        var sentDuration = 0L
        controller.setSendHandler { file, durationMs, onResult ->
            sentDuration = durationMs
            sentFile = file
            onResult(false, "Upload error")
            null
        }

        controller.handleAction(VoiceAction.SendDraft)
        testScope.advanceUntilIdle()

        val failedState = controller.state.value as VoiceState.Failed
        assertEquals(draftState.durationMs, sentDuration)
        assertEquals(draftState.durationMs, failedState.durationMs)

        // Retry sending
        controller.setSendHandler { file, durationMs, onResult ->
            sentDuration = durationMs
            sentFile = file
            onResult(true, null)
            null
        }

        controller.handleAction(VoiceAction.SendDraft)
        testScope.advanceUntilIdle()

        assertEquals(draftState.durationMs, sentDuration)
        assertEquals(VoiceState.Normal, controller.state.value)
    }

    @Test
    fun `pause duration is excluded from recorded duration`() {
        val testFile = tempFolder.newFile("draft_pause.m4a")
        testFile.writeText("audio data")
        `when`(mockRecorder.stop()).thenReturn(VoiceRecorderStopResult.Success(testFile))

        controller.handleAction(VoiceAction.OpenVoiceMode)
        controller.handleAction(VoiceAction.StartRecording)
        testScope.advanceTimeBy(200)

        // Record for 3s
        testTimeProvider.advanceTimeBy(3000)

        // Lock & Pause
        controller.handleAction(VoiceAction.Dragged(deltaX = 0f, deltaY = -100f))
        assertTrue(controller.state.value is VoiceState.Locked)

        controller.handleAction(VoiceAction.TogglePause)
        assertTrue((controller.state.value as VoiceState.Locked).isPaused)

        // Stay paused for 5s (wall clock advances 5s)
        testTimeProvider.advanceTimeBy(5000)

        // Resume & record 2s
        controller.handleAction(VoiceAction.TogglePause)
        assertFalse((controller.state.value as VoiceState.Locked).isPaused)
        testTimeProvider.advanceTimeBy(2000)

        controller.handleAction(VoiceAction.FinishRecording)

        val draftState = controller.state.value as VoiceState.VoiceDraft
        // Total active time = 3s + 2s = 5000ms (paused 5000ms is excluded)
        assertEquals(5000L, draftState.durationMs)
    }

    @Test
    fun `upload timeout cancels upload job and late callback is ignored`() {
        val testFile = tempFolder.newFile("draft_timeout_cancel.m4a")
        testFile.writeText("audio data")
        `when`(mockRecorder.stop()).thenReturn(VoiceRecorderStopResult.Success(testFile))

        controller.handleAction(VoiceAction.OpenVoiceMode)
        controller.handleAction(VoiceAction.StartRecording)
        testScope.advanceTimeBy(200)
        testTimeProvider.advanceTimeBy(5000)
        controller.handleAction(VoiceAction.FinishRecording)

        var uploadJobCancelled = false
        var capturedCallback: ((Boolean, String?) -> Unit)? = null

        controller.setSendHandler { _, _, onResult ->
            capturedCallback = onResult
            testScope.launch {
                try {
                    delay(60000L) // hangs longer than 45s timeout
                } finally {
                    uploadJobCancelled = true
                }
            }
        }

        controller.handleAction(VoiceAction.SendDraft)
        assertTrue(controller.state.value is VoiceState.Sending)

        testScope.advanceTimeBy(46000L)

        val stateAfterTimeout = controller.state.value
        assertTrue("Expected Failed state but was $stateAfterTimeout", stateAfterTimeout is VoiceState.Failed)
        val failedState = stateAfterTimeout as VoiceState.Failed
        assertEquals("Upload timed out", failedState.error)
        assertEquals(5000L, failedState.durationMs)
        assertTrue(uploadJobCancelled)

        // Late callback should be ignored due to AtomicBoolean isHandled
        capturedCallback?.invoke(true, null)
        assertEquals(failedState, controller.state.value)
    }
}
