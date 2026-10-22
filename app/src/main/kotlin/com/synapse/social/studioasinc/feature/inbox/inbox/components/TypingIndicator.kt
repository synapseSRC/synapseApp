package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.inbox.inbox.theme.InboxAnimations
import com.synapse.social.studioasinc.feature.inbox.inbox.theme.InboxColors
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.Sizes

/**
 * Incoming-style typing bubble: asymmetric rounded shape (massive on the sender's
 * side, small on the bottom-left tail), accent-colored dots, and a subtle shadow
 * so it reads as a floating, transient element rather than part of the background.
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
    val shape = remember {
        RoundedCornerShape(
            topStart = Sizes.CornerMassive,
            topEnd = Sizes.CornerMassive,
            bottomStart = Sizes.CornerSmall,
            bottomEnd = Sizes.CornerMassive,
        )
    }

    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = Sizes.ElevationNone,
        shadowElevation = Sizes.ElevationLow,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = Spacing.space16,
                vertical = Spacing.space6,
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
            Dot()
            if (index < 2) Spacer(modifier = Modifier.width(Spacing.space4))
        }
        return
    }

    // Single InfiniteTransition drives all three dots. The stagger is baked into
    // the keyframe spec, so the motion is stable across recompositions (no
    // re-delaying) and is gated by the reduced-motion flag above.
    val transition = rememberInfiniteTransition(label = "typingDots")
    val offsets = List(3) { index ->
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec = InboxAnimations.typingBounceSpec(index),
            label = "dot$index",
        )
    }

    offsets.forEachIndexed { index, offset ->
        Box(
            modifier = Modifier
                .offset(y = (-Sizes.TypingIndicatorBounceOffset.value * offset.value).dp)
                .size(Sizes.TypingIndicatorDot)
                .background(InboxColors.TypingDot, CircleShape),
        )
        if (index < offsets.size - 1) {
            Spacer(modifier = Modifier.width(Spacing.space4))
        }
    }
}

@Composable
private fun Dot() =
    Box(
        modifier = Modifier
            .size(Sizes.TypingIndicatorDot)
            .background(InboxColors.TypingDot, CircleShape),
    )