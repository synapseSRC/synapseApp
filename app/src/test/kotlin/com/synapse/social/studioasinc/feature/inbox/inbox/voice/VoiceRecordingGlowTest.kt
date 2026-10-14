package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        assertEquals(0.08f, paused.glowAmplitude(), 0.001f)
    }

    @Test
    fun cancelTintFollowsNormalizedDragProgressAndReturnsToNormalAtZero() {
        val normal = Color(0xFF315A82)
        val cancel = Color(0xFFE53935)
        assertEquals(normal, voiceCancelTint(normal, cancel, 0f))
        assertEquals(cancel, voiceCancelTint(normal, cancel, 1f))
        assertEquals(cancel, voiceCancelTint(normal, cancel, 2f))
        val midpoint = voiceCancelTint(normal, cancel, 0.5f)
        assertTrue(midpoint.red > normal.red && midpoint.red < cancel.red)
        assertTrue(midpoint.green > cancel.green && midpoint.green < normal.green)
    }

    @Test
    fun quietAndLoudAudioDriveDifferentLiquidSpeedTravelDeformationAndPigment() {
        val quiet = voiceLiquidMotion(0.08f)
        val loud = voiceLiquidMotion(0.92f)

        assertTrue("loud audio must move the fluid faster", loud.cyclesPerSecond > quiet.cyclesPerSecond)
        assertTrue("loud audio must travel farther", loud.travelRatio > quiet.travelRatio)
        assertTrue("loud audio must deform the contour more", loud.deformation > quiet.deformation)
        assertTrue("loud audio must increase pigment energy", loud.pigmentAlpha > quiet.pigmentAlpha)
        assertTrue("quiet audio remains visibly fluid", quiet.pigmentAlpha > 0f && quiet.cyclesPerSecond > 0f)
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
                    Text(rememberVoiceGlowPhase(amplitude.value, active = true, reducedMotion = true, label = "reducedPhase").toString(), Modifier.testTag("reduced-phase"))
                    Text(rememberVoiceGlowIntensity(amplitude.value, true, "reducedIntensity").toString(), Modifier.testTag("reduced-intensity"))
                    Text(rememberVoiceShimmerPhase(true, "reducedShimmer").toString(), Modifier.testTag("reduced-shimmer"))
                }
            }
        }

        composeRule.onNodeWithTag("reduced-phase").assertTextEquals("0.0")
        composeRule.onNodeWithTag("reduced-intensity").assertTextEquals("0.12")
        composeRule.onNodeWithTag("reduced-shimmer").assertTextEquals("0.5")
        composeRule.mainClock.advanceTimeBy(2_000L)
        composeRule.onNodeWithTag("reduced-phase").assertTextEquals("0.0")
        composeRule.runOnIdle { amplitude.value = 0.84f }
        composeRule.onNodeWithTag("reduced-intensity").assertTextEquals("0.84")
        composeRule.mainClock.advanceTimeBy(1_000L)
        composeRule.onNodeWithTag("reduced-intensity").assertTextEquals("0.84")
    }

    @Test
    fun glowPhaseStopsWhenInactiveAndResumesOnTheComposeFrameClock() {
        val active = mutableStateOf(true)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MaterialTheme {
                Text(
                    rememberVoiceGlowPhase(0.5f, active.value, reducedMotion = false, label = "activePhase").toString(),
                    Modifier.testTag("active-phase")
                )
            }
        }

        composeRule.mainClock.advanceTimeBy(1_000L)
        val movingPhase = textFor("active-phase").toFloat()
        assertTrue("active glow phase should advance", movingPhase > 0f)

        composeRule.runOnIdle { active.value = false }
        val inactivePhase = textFor("active-phase").toFloat()
        composeRule.mainClock.advanceTimeBy(1_000L)
        assertEquals("inactive glow must stop requesting phase updates", inactivePhase, textFor("active-phase").toFloat(), 0f)

        composeRule.runOnIdle { active.value = true }
        composeRule.mainClock.advanceTimeBy(128L)
        assertNotEquals("active glow resumes using the Compose frame clock", inactivePhase, textFor("active-phase").toFloat())
    }

    @Test
    fun allowedMotionDriftsGlowPhaseAndSmoothsAmplitudeChanges() {
        val amplitude = mutableStateOf(0.12f)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            MaterialTheme {
                Column {
                    Text(rememberVoiceGlowPhase(amplitude.value, active = true, reducedMotion = false, label = "animatedPhase").toString(), Modifier.testTag("animated-phase"))
                    Text(rememberVoiceGlowIntensity(amplitude.value, false, "animatedIntensity").toString(), Modifier.testTag("animated-intensity"))
                    Text(rememberVoiceShimmerPhase(false, "animatedShimmer").toString(), Modifier.testTag("animated-shimmer"))
                }
            }
        }
        val initialPhase = textFor("animated-phase").toFloat()
        val initialShimmer = textFor("animated-shimmer").toFloat()
        composeRule.mainClock.advanceTimeBy(2_000L)
        val movedPhase = textFor("animated-phase").toFloat()
        val movedShimmer = textFor("animated-shimmer").toFloat()
        assertNotEquals("ambient phase should drift with motion enabled", initialPhase, movedPhase)
        assertNotEquals("cancel hint shimmer should move continuously", initialShimmer, movedShimmer)

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
