package com.synapse.social.studioasinc.feature.inbox.inbox.voice.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OdometerTimerText(
    durationMs: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    val minStr = String.format("%02d", minutes)
    val secStr = String.format("%02d", seconds)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        // Minutes - tens digit
        OdometerDigit(digit = minStr[0], style = style, color = color)
        // Minutes - ones digit
        OdometerDigit(digit = minStr[1], style = style, color = color)

        // Colon (stationary)
        Text(
            text = ":",
            style = style,
            color = color,
            modifier = Modifier.padding(horizontal = 1.dp)
        )

        // Seconds - tens digit
        OdometerDigit(digit = secStr[0], style = style, color = color)
        // Seconds - ones digit
        OdometerDigit(digit = secStr[1], style = style, color = color)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun OdometerDigit(
    digit: Char,
    style: TextStyle,
    color: Color
) {
    Box(
        modifier = Modifier.width(10.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = digit,
            transitionSpec = {
                val isForward = if (initialState == '9' && targetState == '0') {
                    true
                } else if (initialState == '0' && targetState == '9') {
                    false
                } else {
                    targetState > initialState
                }

                if (isForward) {
                    (slideInVertically(animationSpec = tween(180)) { height -> height } + fadeIn(tween(180))) togetherWith
                        (slideOutVertically(animationSpec = tween(180)) { height -> -height } + fadeOut(tween(180)))
                } else {
                    (slideInVertically(animationSpec = tween(180)) { height -> -height } + fadeIn(tween(180))) togetherWith
                        (slideOutVertically(animationSpec = tween(180)) { height -> height } + fadeOut(tween(180)))
                }
            },
            label = "digitOdometer"
        ) { char ->
            Text(
                text = char.toString(),
                style = style,
                color = color
            )
        }
    }
}
