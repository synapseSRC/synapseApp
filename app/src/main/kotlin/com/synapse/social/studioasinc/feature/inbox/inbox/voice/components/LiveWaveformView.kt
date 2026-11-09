package com.synapse.social.studioasinc.feature.inbox.inbox.voice.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun LiveWaveformView(
    amplitudeHistory: List<Float>,
    modifier: Modifier = Modifier,
    progressFraction: Float = 0f,
    isDraftPlayback: Boolean = false,
    isPaused: Boolean = false,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
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

    Canvas(
        modifier = modifier
            .fillMaxHeight()
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

        visibleBars.forEachIndexed { index, rawAmp ->
            val amp = rawAmp.coerceIn(0.08f, 1f)
            val barHeight = (maxHeight * amp).coerceIn(6.dp.toPx(), maxHeight)
            val topY = (maxHeight - barHeight) / 2f
            val x = startX + index * totalBarWidthWithSpacing

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
