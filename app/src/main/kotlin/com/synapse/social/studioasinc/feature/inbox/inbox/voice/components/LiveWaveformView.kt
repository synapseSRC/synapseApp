package com.synapse.social.studioasinc.feature.inbox.inbox.voice.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Voice waveform entry point.
 *
 * The rendering itself lives in [AuroraWaveformView]; this wrapper keeps the original call
 * surface (and its defaults) for existing call sites while forwarding playback state through.
 *
 * @param isDraftPlayback true when rendering a recorded draft; false renders live capture.
 */
@Composable
fun LiveWaveformView(
    amplitudeHistory: List<Float>,
    modifier: Modifier = Modifier,
    progressFraction: Float = 0f,
    isDraftPlayback: Boolean = false,
    isPaused: Boolean = false,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
    onSeek: ((Float) -> Unit)? = null,
    isPlaying: Boolean = false,
    isSeeking: Boolean = false,
    reducedMotion: Boolean = false
) {
    AuroraWaveformView(
        amplitudeHistory = amplitudeHistory,
        modifier = modifier,
        isLive = !isDraftPlayback,
        progressFraction = if (isDraftPlayback) progressFraction else 1f,
        isPlaying = isDraftPlayback && isPlaying,
        isPaused = isPaused,
        isSeeking = isSeeking,
        reducedMotion = reducedMotion,
        activeColor = activeColor,
        inactiveColor = inactiveColor,
        onSeek = if (isDraftPlayback) onSeek else null
    )
}
