package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.fetchSemanticsNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VoiceRecordingGlowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun glowAmplitudeUsesLatestSampleForRecordingAndPausedLockedStates() {
        val quiet = VoiceState.Recording(amplitudeHistory = listOf(0.08f, 0.12f))
        val loud = VoiceState.Recording(amplitudeHistory = listOf(0.08f, 0.86f))
        val paused = VoiceState.Locked(amplitudeHistory = listOf(0.94f), isPaused = true)

        assertEquals(0.12f, quiet.glowAmplitude(), 0.001f)
        assertEquals(0.86f, loud.glowAmplitude(), 0.001f)
        assertTrue(loud.glowAmplitude() > quiet.glowAmplitude())
        assertEquals(0.94f, paused.glowAmplitude(), 0.001f)
    }

    @Test
    fun glowSurfaceRendersForRecordingAndLockedAndAmbientFollowsActiveState() {
        val voiceState = mutableStateOf<VoiceState>(VoiceState.Recording(amplitudeHistory = listOf(0.42f)))
        composeRule.setContent {
            MaterialTheme {
                Column {
                    VoiceRecordingGlowSurface(
                        voiceState = voiceState.value,
                        modifier = Modifier.width(280.dp).height(56.dp).testTag("voice-glow-surface"),
                        reducedMotionOverride = true
                    )
                    VoiceRecordingAmbientGlow(
                        voiceState = voiceState.value,
                        modifier = Modifier.width(280.dp).height(72.dp).testTag("voice-ambient-glow"),
                        reducedMotionOverride = true
                    )
                }
            }
        }

        composeRule.onNodeWithTag("voice-glow-surface").assertExists()
        composeRule.onNodeWithTag("voice-ambient-glow").assertExists()
        composeRule.runOnIdle { voiceState.value = VoiceState.Locked(amplitudeHistory = listOf(0.76f)) }
        composeRule.onNodeWithTag("voice-glow-surface").assertExists()
        composeRule.onNodeWithTag("voice-ambient-glow").assertExists()
        composeRule.runOnIdle { voiceState.value = VoiceState.Normal }
        composeRule.onNodeWithTag("voice-glow-surface").assertExists()
        composeRule.onNodeWithTag("voice-ambient-glow").assertDoesNotExist()
    }

    @Test
    fun reducedMotionKeepsPhaseStaticAndUsesImmediateAmplitudeIntensity() {
        val amplitude = mutableStateOf(0.12f)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MaterialTheme {
                Column {
                    Text(rememberVoiceGlowPhase(true, "reducedPhase").toString(), Modifier.testTag("reduced-phase"))
                    Text(rememberVoiceGlowIntensity(amplitude.value, true, "reducedIntensity").toString(), Modifier.testTag("reduced-intensity"))
                }
            }
        }

        composeRule.onNodeWithTag("reduced-phase").assertTextEquals("0.5")
        composeRule.onNodeWithTag("reduced-intensity").assertTextEquals("0.12")
        composeRule.mainClock.advanceTimeBy(2_000L)
        composeRule.onNodeWithTag("reduced-phase").assertTextEquals("0.5")
        composeRule.runOnIdle { amplitude.value = 0.84f }
        composeRule.onNodeWithTag("reduced-intensity").assertTextEquals("0.84")
        composeRule.mainClock.advanceTimeBy(1_000L)
        composeRule.onNodeWithTag("reduced-intensity").assertTextEquals("0.84")
    }

    @Test
    fun allowedMotionDriftsGlowPhaseAndSmoothsAmplitudeChanges() {
        val amplitude = mutableStateOf(0.12f)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MaterialTheme {
                Column {
                    Text(rememberVoiceGlowPhase(false, "animatedPhase").toString(), Modifier.testTag("animated-phase"))
                    Text(rememberVoiceGlowIntensity(amplitude.value, false, "animatedIntensity").toString(), Modifier.testTag("animated-intensity"))
                }
            }
        }
        val initialPhase = textFor("animated-phase").toFloat()
        composeRule.mainClock.advanceTimeBy(250L)
        val movedPhase = textFor("animated-phase").toFloat()
        assertNotEquals("ambient phase should drift with motion enabled", initialPhase, movedPhase)

        val initialIntensity = textFor("animated-intensity").toFloat()
        composeRule.runOnIdle { amplitude.value = 0.84f }
        composeRule.mainClock.advanceTimeBy(64L)
        val intermediateIntensity = textFor("animated-intensity").toFloat()
        assertTrue(intermediateIntensity > initialIntensity)
        assertTrue(intermediateIntensity < 0.84f)
        composeRule.mainClock.advanceTimeBy(1_500L)
        assertEquals(0.84f, textFor("animated-intensity").toFloat(), 0.01f)
    }

    private fun textFor(tag: String): String = composeRule.onNodeWithTag(tag)
        .fetchSemanticsNode().config[SemanticsProperties.Text].first().text
}
