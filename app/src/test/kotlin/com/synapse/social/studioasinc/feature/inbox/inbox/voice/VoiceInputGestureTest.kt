package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import android.content.Context
import android.content.res.Configuration
import android.provider.Settings
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
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.fetchSemanticsNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
        val context = ApplicationProvider.getApplicationContext<Context>()
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        Settings.Global.putFloat(context.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 0f)

        recorder = Mockito.mock(VoiceRecorder::class.java)
        Mockito.`when`(recorder.amplitudeFlow).thenReturn(MutableStateFlow(0))
        outputFile = File.createTempFile("voice-gesture", ".m4a")
        controller = VoiceRecordingController(
            scope = testScope,
            voiceRecorder = recorder,
            voicePlayerManager = null,
            getOutputFile = { outputFile },
            gestureConfig = VoiceGestureConfig(holdThresholdMs = 0L),
            cancelThresholdPx = 100f,
            lockThresholdPx = 120f,
            directionSlopPx = 12f
        )
        controller.handleAction(VoiceAction.StartRecording)
        testScope.advanceTimeBy(1L)
        assertTrue(controller.state.value is VoiceState.Recording)
    }

    @After
    fun cleanUp() {
        controller.cleanup()
        outputFile.delete()
        val context = ApplicationProvider.getApplicationContext<Context>()
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        Settings.Global.putFloat(context.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
    }

    @Test
    fun horizontalSwipeOutsideMicBoundsCancelsAndReleasesExactlyOnce() {
        composeRule.setContent { micUnderTest() }
        composeRule.onNodeWithTag("gesture-test-mic").performTouchInput {
            down(center)
            moveBy(Offset(x = -size.width.toFloat() * 3f, y = 0f))
            up()
        }

        composeRule.runOnIdle {
            assertTrue(actions.filterIsInstance<VoiceAction.Dragged>().any { it.deltaX < 0f })
            assertEquals(1, actions.count { it == VoiceAction.ReleaseRecording })
            assertEquals(VoiceState.Normal, controller.state.value)
            Mockito.verify(recorder, Mockito.times(1)).cancel()
        }
    }

    @Test
    fun upwardSwipeOutsideMicBoundsLocksAndReleaseKeepsRecordingAlive() {
        composeRule.setContent { micUnderTest() }
        composeRule.onNodeWithTag("gesture-test-mic").performTouchInput {
            down(center)
            moveBy(Offset(x = 0f, y = -size.height.toFloat() * 3f))
            up()
        }

        composeRule.runOnIdle {
            assertTrue(actions.filterIsInstance<VoiceAction.Dragged>().any { it.deltaY < 0f })
            assertEquals(1, actions.count { it == VoiceAction.ReleaseRecording })
            assertTrue(controller.state.value is VoiceState.Locked)
            Mockito.verify(recorder, Mockito.never()).cancel()
        }
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
                        modifier = Modifier.width(260.dp).height(220.dp).clipToBounds(),
                        contentAlignment = Alignment.Center
                    ) {
                        VoiceInputHost(
                            voiceState = recordingState.value,
                            onVoiceAction = {},
                            showSendButton = false,
                            onSendMessage = {},
                            modifier = Modifier.fillMaxWidth(0.85f),
                            normalContent = {}
                        )
                    }
                }
            }
        }

        composeRule.onNodeWithTag("voice-cancel-instruction").assertTextEquals("বাতিল করতে স্লাইড করুন")
        val initialBar = bounds("voice-recording-bar")
        composeRule.runOnIdle {
            recordingState.value = (recordingState.value as VoiceState.Recording).copy(isArmedForCancel = true)
        }
        composeRule.onNodeWithTag("voice-cancel-instruction").assertTextEquals("বাতিল করতে ছেড়ে দিন")
        val armedBar = bounds("voice-recording-bar")
        assertEquals("cancel feedback must not resize the pill", initialBar.height, armedBar.height, 0.5f)
        composeRule.runOnIdle {
            recordingState.value = (recordingState.value as VoiceState.Recording).copy(isArmedForCancel = false)
        }
        composeRule.onNodeWithTag("voice-cancel-instruction").assertTextEquals("বাতিল করতে স্লাইড করুন")
        composeRule.onNodeWithTag("voice-slide-lock-hint").assertTextEquals("লক করতে ওপরে স্লাইড করুন")
        composeRule.onNodeWithTag("voice-recording-timer").assertExists()

        val rootWidth = 260.dp.value * composeRule.density.density
        val timer = bounds("voice-recording-timer")
        val cancel = bounds("voice-cancel-instruction")
        val mic = bounds("voice-recording-mic")
        val lockHint = bounds("voice-slide-lock-hint")

        assertFalse("timer and cancel text overlap", overlaps(timer, cancel))
        assertFalse("cancel text overlaps the microphone", overlaps(cancel, mic))
        assertFalse("lock hint overlaps the timer", overlaps(lockHint, timer))
        assertFalse("lock hint overlaps the microphone", overlaps(lockHint, mic))
        assertTrue("lock hint is clipped on the left", lockHint.left >= 0f)
        assertTrue("lock hint is clipped on the right", lockHint.right <= rootWidth)
    }

    @Composable
    private fun micUnderTest() {
        val voiceState by controller.state.collectAsState()
        MaterialTheme {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CanonicalMicButton(
                    voiceState = voiceState,
                    onVoiceAction = { action ->
                        actions.add(action)
                        controller.handleAction(action)
                    },
                    modifier = Modifier.testTag("gesture-test-mic")
                )
            }
        }
    }

    private fun bounds(tag: String): Rect = composeRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    private fun overlaps(first: Rect, second: Rect): Boolean =
        first.left < second.right && second.left < first.right && first.top < second.bottom && second.top < first.bottom
}
