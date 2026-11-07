package com.synapse.social.studioasinc.feature.inbox.inbox.voice.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.inbox.inbox.voice.VoiceEnergyDesign

/**
 * Seekable amplitude history used by the voice draft player.
 *
 * When [activeSpectrum] is supplied the played/unplayed split is drawn as one continuous colour
 * field spanning the whole waveform: a single remembered horizontal gradient brush is sampled by
 * every bar, so neighbouring bars carry neighbouring hues and the transition at the playhead is a
 * per-bar ramp instead of a hard switch. Only that one brush is allocated, and only when the
 * waveform is re-measured.
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
    activeSpectrum: List<Color>? = null,
    onSeek: ((Float) -> Unit)? = null
) {
    val barsCount = 36
    val normalizedHistory = remember(amplitudeHistory, barsCount) {
        if (amplitudeHistory.isEmpty()) {
            List(barsCount) { 0.12f }
        } else if (amplitudeHistory.size < barsCount) {
            val padding = List(barsCount - amplitudeHistory.size) { 0.12f }
            padding + amplitudeHistory
        } else {
            amplitudeHistory.takeLast(barsCount)
        }
    }

    var measuredWidthPx by remember { mutableFloatStateOf(0f) }
    val spectrumBrush = remember(measuredWidthPx, activeSpectrum) {
        val spectrum = activeSpectrum
        if (spectrum == null || spectrum.size < 2 || measuredWidthPx <= 0f) null
        else Brush.horizontalGradient(
            colors = spectrum,
            startX = 0f,
            endX = measuredWidthPx
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .onSizeChanged { measuredWidthPx = it.width.toFloat() }
            .pointerInput(onSeek, isDraftPlayback) {
                if (isDraftPlayback && onSeek != null) {
                    detectTapGestures { offset ->
                        val clickedFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek(clickedFraction)
                    }
                }
            }
    ) {
        val totalWidth = size.width
        val maxHeight = size.height
        val barWidth = 3.dp.toPx()
        val barSpacing = 2.5.dp.toPx()
        val totalBarWidthWithSpacing = barWidth + barSpacing

        val maxBarsThatFit = ((totalWidth + barSpacing) / totalBarWidthWithSpacing).toInt().coerceAtLeast(1)
        val visibleBars = normalizedHistory.takeLast(maxBarsThatFit)

        val startX = (totalWidth - (visibleBars.size * totalBarWidthWithSpacing - barSpacing)) / 2f

        val idleAlpha = if (isPaused) VoiceEnergyDesign.WAVEFORM_IDLE_ALPHA * 0.6f
        else VoiceEnergyDesign.WAVEFORM_IDLE_ALPHA

        visibleBars.forEachIndexed { index, rawAmp ->
            val amp = rawAmp.coerceIn(0.08f, 1f)
            val barHeight = (maxHeight * amp).coerceIn(6.dp.toPx(), maxHeight)
            val topY = (maxHeight - barHeight) / 2f
            val x = startX + index * totalBarWidthWithSpacing

            // Fraction of this bar that the playhead has already crossed, so the played edge
            // blends across one bar instead of snapping at a bar boundary.
            val playedFraction = if (isDraftPlayback) {
                (progressFraction * visibleBars.size - index).coerceIn(0f, 1f)
            } else {
                1f
            }

            if (spectrumBrush != null) {
                drawRoundRect(
                    brush = spectrumBrush,
                    topLeft = Offset(x, topY),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                    alpha = idleAlpha
                )
                if (playedFraction > 0f) {
                    drawRoundRect(
                        brush = spectrumBrush,
                        topLeft = Offset(x, topY),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                        alpha = VoiceEnergyDesign.WAVEFORM_PLAYED_ALPHA * playedFraction
                    )
                }
            } else {
                val isPlayed = isDraftPlayback && ((index.toFloat() / visibleBars.size) <= progressFraction)
                val color = when {
                    isPaused -> inactiveColor
                    isDraftPlayback -> if (isPlayed) activeColor else inactiveColor
                    else -> activeColor
                }

                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, topY),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
    }
}
