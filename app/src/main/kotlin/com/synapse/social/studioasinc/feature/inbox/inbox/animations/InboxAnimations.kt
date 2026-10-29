package com.synapse.social.studioasinc.ui.inbox.animations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import com.synapse.social.studioasinc.feature.inbox.inbox.theme.InboxAnimations as AnimSpecs

fun Modifier.pressScale(
    pressed: Boolean,
    pressedScale: Float = 0.98f
): Modifier = composed {
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = tween(
            durationMillis = 100,
            easing = FastOutSlowInEasing
        ),
        label = "pressScale"
    )

    this.scale(scale)
}

@Composable
fun <T> SmoothCrossfade(
    targetState: T,
    modifier: Modifier = Modifier,
    animationSpec: FiniteAnimationSpec<Float> = tween(
        durationMillis = AnimSpecs.MediumDurationMs,
        easing = FastOutSlowInEasing
    ),
    label: String = "SmoothCrossfade",
    content: @Composable (T) -> Unit
) {
    Crossfade(
        targetState = targetState,
        modifier = modifier,
        animationSpec = animationSpec,
        label = label,
        content = content
    )
}

