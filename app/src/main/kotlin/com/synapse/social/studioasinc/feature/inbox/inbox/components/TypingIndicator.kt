package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.inbox.inbox.theme.InboxAnimations
import com.synapse.social.studioasinc.feature.inbox.inbox.theme.InboxColors
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.Sizes

/**
 * Incoming-style typing bubble: a floating pill with accent-colored dots and a
 * subtle shadow so it reads as a transient element rather than part of the
 * background.
 *
 * @param reducedMotion Honors the system reduced-motion setting — when true the
 *   dots are rendered statically and no animation is started.
 * @param displayName When set (group chats), renders the sender's name above the
 *   dots so the indicator is attributable to a specific participant.
 */
@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
    displayName: String? = null,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = Sizes.ElevationNone,
        shadowElevation = Sizes.ElevationLow,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = Spacing.space16,
                vertical = Spacing.space8,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!displayName.isNullOrBlank()) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(Spacing.space8))
            }
            TypingDots(reducedMotion = reducedMotion)
        }
    }
}

@Composable
private fun TypingDots(reducedMotion: Boolean) {
    if (reducedMotion) {
        // Static fallback — no motion under reduced-motion settings.
        repeat(3) { index ->
            Dot(alpha = 0.6f)
            if (index < 2) Spacer(modifier = Modifier.width(Spacing.space4))
        }
        return
    }

    // Single InfiniteTransition drives all three dots. The stagger is baked into
    // the keyframe spec, and reading progressState.value inside graphicsLayer
    // skips recomposition and layout passes entirely for 60/120fps performance.
    val bouncePx = with(LocalDensity.current) { Sizes.TypingIndicatorBounceOffset.toPx() }
    val transition = rememberInfiniteTransition(label = "typingDots")
    val dotProgresses = List(3) { index ->
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec = InboxAnimations.typingBounceSpec(index),
            label = "dot$index",
        )
    }

    dotProgresses.forEachIndexed { index, progressState ->
        Box(
            modifier = Modifier
                .graphicsLayer {
                    val p = progressState.value
                    translationY = -p * bouncePx
                    alpha = 0.45f + 0.55f * p
                    val s = 0.85f + 0.15f * p
                    scaleX = s
                    scaleY = s
                }
                .size(Sizes.TypingIndicatorDot)
                .background(InboxColors.TypingDot, CircleShape),
        )
        if (index < dotProgresses.size - 1) {
            Spacer(modifier = Modifier.width(Spacing.space4))
        }
    }
}

@Composable
private fun Dot(alpha: Float = 1f) =
    Box(
        modifier = Modifier
            .graphicsLayer { this.alpha = alpha }
            .size(Sizes.TypingIndicatorDot)
            .background(InboxColors.TypingDot, CircleShape),
    )
