package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.fetchSemanticsNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val TEST_HOST_WIDTH_FRACTION = 0.85f

private val Rect.centerX: Float get() = left + width / 2f
private val Rect.centerY: Float get() = top + height / 2f

@Suppress("UNCHECKED_CAST")
private fun <T> anyObject(): T {
    Mockito.any<T>()
    return null as T
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VoiceInputGestureTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val actions = mutableListOf<VoiceAction>()
    private val testScope = TestScope(UnconfinedTestDispatcher())
    private lateinit var recorder: VoiceRecorder
    private lateinit var controller: VoiceRecordingController
    private lateinit var outputFile: File

    @Before
    fun setUp() {
        recorder = Mockito.mock(VoiceRecorder::class.java)
        Mockito.`when`(recorder.amplitudeFlow).thenReturn(MutableStateFlow(0))
        outputFile = File.createTempFile("voice-gesture", ".m4a")
        controller = VoiceRecordingController(
            scope = testScope,
            voiceRecorder = recorder,
            voicePlayerManager = null,
            getOutputFile = { outputFile },
            gestureConfig = VoiceGestureConfig(holdThresholdMs = 0L),
            cancelThresholdPx = 100f * composeRule.density.density,
            lockThresholdPx = 120f * composeRule.density.density,
            directionSlopPx = 12f * composeRule.density.density
        )
        assertEquals(VoiceState.Normal, controller.state.value)
    }

    @After
    fun cleanUp() {
        controller.cleanup()
        outputFile.delete()
    }

    @Test
    fun cancellationHappensAtDragThresholdBeforeReleaseAndOnlyOnce() {
        composeRule.setContent { micUnderTest() }
        val micSize = bounds("voice-recording-mic")
        composeRule.onNodeWithTag("gesture-test-mic").performTouchInput { down(center) }
        composeRule.onNodeWithTag("gesture-test-mic").performTouchInput {
            moveBy(Offset(x = -micSize.width * 3f, y = 0f))
        }

        composeRule.runOnIdle {
            assertEquals(1, actions.count { it == VoiceAction.StartRecording })
            assertTrue(actions.filterIsInstance<VoiceAction.Dragged>().any { it.deltaX < 0f })
            assertEquals("threshold crossing cancels before pointer-up", VoiceState.Normal, controller.state.value)
            Mockito.verify(recorder, Mockito.times(1)).cancel()
        }

        composeRule.onNodeWithTag("gesture-test-mic").performTouchInput {
            moveBy(Offset(x = micSize.width * 1.25f, y = 0f))
            up()
        }
        composeRule.runOnIdle {
            assertEquals(1, actions.count { it == VoiceAction.ReleaseRecording })
            assertEquals(VoiceState.Normal, controller.state.value)
            Mockito.verify(recorder, Mockito.times(1)).cancel()
        }
    }

    @Test
    fun upwardSwipeOutsideMicBoundsLocksAndReleaseKeepsRecordingAlive() {
        composeRule.setContent { micUnderTest() }
        val micSize = bounds("voice-recording-mic")
        composeRule.onNodeWithTag("gesture-test-mic").performTouchInput {
            down(center)
            moveBy(Offset(x = 0f, y = -micSize.height * 3f))
            up()
        }

        composeRule.runOnIdle {
            assertEquals(1, actions.count { it == VoiceAction.StartRecording })
            assertTrue(actions.filterIsInstance<VoiceAction.Dragged>().any { it.deltaY < 0f })
            assertEquals(1, actions.count { it == VoiceAction.ReleaseRecording })
            assertTrue(controller.state.value is VoiceState.Locked)
            Mockito.verify(recorder, Mockito.never()).cancel()
        }
    }

    @Test
    fun abandonedCancelAndLockSlidesUseSpringToReturnMicHome() {
        controller.handleAction(VoiceAction.StartRecording)
        testScope.advanceTimeBy(1L)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { micUnderTest(reducedMotion = false) }
        val micSize = bounds("voice-recording-mic")
        controller.updateThresholds(
            cancelPx = micSize.width * 2f,
            lockPx = micSize.height * 2f,
            slopPx = VoiceGestureConfig().directionSlopDp * composeRule.density.density
        )
        val home = bounds("voice-recording-mic-visual")

        composeRule.onNodeWithTag("gesture-test-mic").performTouchInput { down(center) }
        composeRule.onNodeWithTag("gesture-test-mic").performTouchInput {
            moveBy(Offset(x = -micSize.width, y = 0f))
        }
        val displacedX = bounds("voice-recording-mic-visual")
        assertTrue(displacedX.centerX < home.centerX)
        composeRule.onNodeWithTag("gesture-test-mic").performTouchInput { up() }
        composeRule.runOnIdle {
            val recording = controller.state.value as VoiceState.Recording
            assertEquals(0f, recording.slideOffsetPx, 0f)
            assertEquals(0f, recording.cancelProgress, 0f)
        }
        composeRule.mainClock.advanceTimeBy(64L)
        val returningX = bounds("voice-recording-mic-visual")
        assertTrue(returningX.centerX > displacedX.centerX)
        assertTrue(returningX.centerX < home.centerX)
        composeRule.mainClock.advanceTimeBy(1_000L)
        assertEquals(home.centerX, bounds("voice-recording-mic-visual").centerX, 1f)

        composeRule.onNodeWithTag("gesture-test-mic").performTouchInput { down(center) }
        composeRule.onNodeWithTag("gesture-test-mic").performTouchInput {
            moveBy(Offset(x = 0f, y = -micSize.height))
        }
        val displacedY = bounds("voice-recording-mic-visual")
        assertTrue(displacedY.centerY < home.centerY)
        composeRule.onNodeWithTag("gesture-test-mic").performTouchInput { up() }
        composeRule.runOnIdle {
            val recording = controller.state.value as VoiceState.Recording
            assertEquals(0f, recording.dragOffsetYPx, 0f)
            assertEquals(0f, recording.lockProgress, 0f)
            Mockito.verify(recorder, Mockito.never()).cancel()
            Mockito.verify(recorder, Mockito.never()).stop()
        }
        composeRule.mainClock.advanceTimeBy(64L)
        val returningY = bounds("voice-recording-mic-visual")
        assertTrue(returningY.centerY > displacedY.centerY)
        assertTrue(returningY.centerY < home.centerY)
        composeRule.mainClock.advanceTimeBy(1_000L)
        assertEquals(home.centerY, bounds("voice-recording-mic-visual").centerY, 1f)
    }

    @Test
    fun reducedMotionSkipsAbandonedSlideReturnAnimation() {
        controller.handleAction(VoiceAction.StartRecording)
        testScope.advanceTimeBy(1L)
        composeRule.setContent { micUnderTest(reducedMotion = true) }
        val micSize = bounds("voice-recording-mic")
        controller.updateThresholds(
            cancelPx = micSize.width * 2f,
            lockPx = micSize.height * 2f,
            slopPx = VoiceGestureConfig().directionSlopDp * composeRule.density.density
        )
        val home = bounds("voice-recording-mic-visual")
        composeRule.onNodeWithTag("gesture-test-mic").performTouchInput {
            down(center)
            moveBy(Offset(x = -micSize.width, y = 0f))
            up()
        }
        composeRule.runOnIdle {
            val state = controller.state.value as VoiceState.Recording
            assertEquals(0f, state.slideOffsetPx, 0f)
        }
        assertEquals("reduced motion should settle immediately", home.centerX, bounds("voice-recording-mic-visual").centerX, 1f)
    }

    @Test
    fun successfulLockSpringsMicHomeWhileLockedRecordingContinues() {
        controller.handleAction(VoiceAction.StartRecording)
        testScope.advanceTimeBy(1L)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { voiceHostUnderTest(reducedMotion = false) }
        composeRule.mainClock.advanceTimeBy(1_000L)
        val home = bounds("voice-recording-mic-visual")

        composeRule.onNodeWithTag("voice-recording-mic").performTouchInput {
            down(center)
            moveBy(Offset(x = 0f, y = -controller.lockThresholdPx * 1.25f))
        }
        composeRule.runOnIdle {
            assertTrue("threshold crossing commits locked state", controller.state.value is VoiceState.Locked)
            assertEquals(1, actions.count { it == VoiceAction.StartRecording })
            assertEquals(0, actions.count { it == VoiceAction.ReleaseRecording })
            Mockito.verify(recorder, Mockito.times(1)).start(anyObject(), anyObject())
            Mockito.verify(recorder, Mockito.never()).cancel()
            Mockito.verify(recorder, Mockito.never()).stop()
        }

        val displaced = bounds("voice-lock-returning-mic-visual")
        assertTrue("the locked transition starts from the dragged mic position", displaced.centerY < home.centerY)
        composeRule.mainClock.advanceTimeBy(64L)
        val returning = bounds("voice-lock-returning-mic-visual")
        assertTrue("the successful-lock spring moves back toward home", returning.centerY > displaced.centerY)
        assertTrue("spring remains between the displaced and home positions", returning.centerY < home.centerY)
        composeRule.mainClock.advanceTimeBy(1_200L)
        composeRule.onNodeWithTag("voice-lock-returning-mic-visual").assertDoesNotExist()
        composeRule.runOnIdle {
            assertTrue("the recorder remains active after locking", controller.state.value is VoiceState.Locked)
            Mockito.verify(recorder, Mockito.never()).cancel()
            Mockito.verify(recorder, Mockito.never()).stop()
        }
    }

    @Test
    fun lockedCancelControlRemainsFunctionalAndPauseHasAtLeast48DpTarget() {
        controller.handleAction(VoiceAction.StartRecording)
        testScope.advanceTimeBy(1L)
        controller.handleAction(VoiceAction.Dragged(deltaX = 0f, deltaY = -controller.lockThresholdPx * 1.25f))
        assertTrue(controller.state.value is VoiceState.Locked)
        composeRule.setContent { voiceHostUnderTest() }

        val pauseBounds = bounds("voice-locked-pause")
        val minTargetPx = 48f * composeRule.density.density
        assertTrue("pause target must be at least 48dp wide", pauseBounds.width >= minTargetPx - 0.5f)
        assertTrue("pause target must be at least 48dp tall", pauseBounds.height >= minTargetPx - 0.5f)
        composeRule.onNodeWithTag("voice-locked-cancel").performClick()
        composeRule.runOnIdle {
            assertEquals(VoiceState.Normal, controller.state.value)
            Mockito.verify(recorder, Mockito.times(1)).cancel()
        }
    }

    @Test
    fun lockedHostPauseResumeAndFinishControlsPreserveDraftFlow() {
        Mockito.`when`(recorder.getRecordedDurationMs()).thenReturn(1_000L)
        Mockito.`when`(recorder.stop()).thenReturn(VoiceRecorderStopResult.Success(outputFile))
        controller.handleAction(VoiceAction.StartRecording)
        testScope.advanceTimeBy(1L)
        controller.handleAction(VoiceAction.Dragged(deltaX = 0f, deltaY = -controller.lockThresholdPx * 1.25f))
        assertTrue(controller.state.value is VoiceState.Locked)
        composeRule.setContent { voiceHostUnderTest() }

        val pauseBounds = bounds("voice-locked-pause")
        val minTargetPx = 48f * composeRule.density.density
        assertTrue("pause target must be at least 48dp wide", pauseBounds.width >= minTargetPx - 0.5f)
        assertTrue("pause target must be at least 48dp tall", pauseBounds.height >= minTargetPx - 0.5f)
        composeRule.onNodeWithTag("voice-locked-pause").assertExists().performClick()
        composeRule.runOnIdle { assertTrue((controller.state.value as VoiceState.Locked).isPaused) }
        composeRule.onNodeWithTag("voice-locked-pause").performClick()
        composeRule.runOnIdle { assertFalse((controller.state.value as VoiceState.Locked).isPaused) }
        composeRule.onNodeWithTag("voice-locked-finish").assertExists().performClick()
        composeRule.runOnIdle {
            assertTrue(controller.state.value is VoiceState.VoiceDraft)
            Mockito.verify(recorder, Mockito.times(1)).stop()
            Mockito.verify(recorder, Mockito.never()).cancel()
        }
    }

    @Test
    fun lockedControlsStaySeparatedOnNarrowLayoutAtLargeFontScale() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val configuration = Configuration(baseContext.resources.configuration).apply { fontScale = 1.6f }
        val largeFontContext = baseContext.createConfigurationContext(configuration)

        composeRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalContext provides largeFontContext,
                LocalConfiguration provides configuration,
                LocalDensity provides Density(currentDensity.density, 1.6f)
            ) {
                MaterialTheme {
                    Box(Modifier.width(260.dp).height(180.dp).clipToBounds()) {
                        VoiceInputHost(
                            voiceState = VoiceState.Locked(durationMs = 12_000L, amplitudeHistory = listOf(0.3f)),
                            onVoiceAction = {},
                            showSendButton = false,
                            onSendMessage = {},
                            reducedMotionOverride = true,
                            modifier = Modifier.fillMaxWidth(),
                            normalContent = {}
                        )
                    }
                }
            }
        }

        val rootWidth = 260.dp.value * composeRule.density.density
        val cancel = bounds("voice-locked-cancel")
        val timer = bounds("voice-locked-timer")
        val status = bounds("voice-locked-status")
        val pause = bounds("voice-locked-pause")
        val send = bounds("voice-locked-finish")
        assertFalse("timer overlaps cancel", overlaps(timer, cancel))
        assertFalse("timer overlaps pause", overlaps(timer, pause))
        assertFalse("timer overlaps send", overlaps(timer, send))
        assertFalse("status overlaps controls", overlaps(status, cancel) || overlaps(status, pause) || overlaps(status, send))
        assertTrue("status clips beyond the narrow pill", status.left >= 0f && status.right <= rootWidth)
    }

    @Test
    fun localizedRecordingOverlayStaysUnclippedAndSeparatedAtCompactWidthAndLargeFontScale() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val configuration = Configuration(baseContext.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("bn"))
            fontScale = 1.6f
        }
        val localizedContext = baseContext.createConfigurationContext(configuration)
        val recordingState = mutableStateOf<VoiceState>(
            VoiceState.Recording(durationMs = 12_000L, amplitudeHistory = listOf(0.72f))
        )

        composeRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides configuration,
                LocalDensity provides Density(currentDensity.density, 1.6f)
            ) {
                MaterialTheme {
                    Box(
                        modifier = Modifier.width(260.dp).height(420.dp).clipToBounds().testTag("compact-recording-root"),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        VoiceInputHost(
                            voiceState = recordingState.value,
                            onVoiceAction = {},
                            showSendButton = false,
                            onSendMessage = {},
                            modifier = Modifier.fillMaxWidth(TEST_HOST_WIDTH_FRACTION),
                            reducedMotionOverride = true,
                            normalContent = {}
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithTag("voice-cancel-instruction").assertTextEquals("বাতিল করতে স্লাইড করুন")
        val initialBar = bounds("voice-recording-bar")
        composeRule.onNodeWithTag("voice-cancel-instruction").assertTextEquals("বাতিল করতে স্লাইড করুন")
        val cancelHint = bounds("voice-cancel-instruction")
        assertEquals("cancel feedback must not resize the pill", initialBar.height, bounds("voice-recording-bar").height, 0.5f)
        assertEquals("cancel hint must stay horizontally centered", (initialBar.left + initialBar.right) / 2f, (cancelHint.left + cancelHint.right) / 2f, 0.5f)
        composeRule.onNodeWithTag("voice-slide-lock-hint").assertTextEquals("লক করতে ওপরে স্লাইড করুন")
        composeRule.onNodeWithTag("voice-recording-timer").assertExists()

        val rootWidth = 260.dp.value * composeRule.density.density
        val bar = bounds("voice-recording-bar")
        val timer = bounds("voice-recording-timer")
        val cancel = bounds("voice-cancel-instruction")
        val mic = bounds("voice-recording-mic")
        val lockHint = bounds("voice-slide-lock-hint")
        val lockArrow = bounds("voice-lock-arrow")
        val root = bounds("compact-recording-root")

        assertEquals("timer must remain centered in the pill", (bar.left + bar.right) / 2f, (timer.left + timer.right) / 2f, 0.5f)
        assertFalse("timer and cancel text overlap", overlaps(timer, cancel))
        assertFalse("cancel text overlaps the microphone", overlaps(cancel, mic))
        assertFalse("lock hint overlaps the timer", overlaps(lockHint, timer))
        assertFalse("lock hint overlaps the microphone", overlaps(lockHint, mic))
        assertTrue("lock hint is clipped on the left", lockHint.left >= root.left)
        assertTrue("lock hint is clipped on the right", lockHint.right <= root.right)
        assertTrue("lock cue is clipped above the composer", lockHint.top >= root.top)
        assertTrue("lock arrow is clipped above the composer", lockArrow.top >= root.top)
        assertTrue("lock arrow is clipped at a horizontal edge", lockArrow.left >= root.left && lockArrow.right <= root.right)
        assertTrue("lock text should stay above the microphone", lockHint.bottom < mic.top)
        assertEquals("up arrow should track the actual microphone center", mic.centerX, lockArrow.centerX, 1f)
        assertTrue("measured composer width must remain compact", root.width <= rootWidth + 1f)
    }

    @Composable
    private fun voiceHostUnderTest(reducedMotion: Boolean = true) {
        val voiceState by controller.state.collectAsState()
        MaterialTheme {
            VoiceInputHost(
                voiceState = voiceState,
                onVoiceAction = { action ->
                    actions.add(action)
                    controller.handleAction(action)
                },
                showSendButton = false,
                onSendMessage = {},
                reducedMotionOverride = reducedMotion,
                normalContent = {}
            )
        }
    }

    @Composable
    private fun micUnderTest(reducedMotion: Boolean = true) {
        val voiceState by controller.state.collectAsState()
        MaterialTheme {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CanonicalMicButton(
                    voiceState = voiceState,
                    onVoiceAction = { action ->
                        actions.add(action)
                        controller.handleAction(action)
                    },
                    modifier = Modifier.testTag("gesture-test-mic"),
                    reducedMotionOverride = reducedMotion
                )
            }
        }
    }

    private fun bounds(tag: String): Rect = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    private fun overlaps(first: Rect, second: Rect): Boolean =
        first.left < second.right && second.left < first.right && first.top < second.bottom && second.top < first.bottom
}
