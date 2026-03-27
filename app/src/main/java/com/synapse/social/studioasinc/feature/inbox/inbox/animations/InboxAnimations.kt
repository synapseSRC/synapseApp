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
import com.synapse.social.studioasinc.ui.inbox.theme.InboxAnimations as AnimSpecs



@Composable
fun AnimatedListItemEntrance(
    visible: Boolean,
    index: Int,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    val delay = index * AnimSpecs.EntranceStaggerDelayMs

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(
                durationMillis = AnimSpecs.MediumDurationMs,
                delayMillis = delay,
                easing = FastOutSlowInEasing
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = AnimSpecs.MediumDurationMs,
                delayMillis = delay,
                easing = FastOutSlowInEasing
            )
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { fullWidth -> -fullWidth },
            animationSpec = tween(
                durationMillis = AnimSpecs.ShortDurationMs,
                easing = FastOutSlowInEasing
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = AnimSpecs.ShortDurationMs,
                easing = FastOutSlowInEasing
            )
        ),
        content = content
    )
}



fun Modifier.pulseEffect(
    enabled: Boolean = true,
    minScale: Float = 0.9f,
    maxScale: Float = 1.1f
): Modifier = composed {
    if (!enabled) return@composed this

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = AnimSpecs.PulseSpec,
        label = "pulseScale"
    )

    this.scale(scale)
}



@Composable
fun BadgePopAnimation(
    targetCount: Int,
    content: @Composable (Int) -> Unit
) {
    var previousCount by remember { mutableIntStateOf(targetCount) }
    var trigger by remember { mutableStateOf(false) }

    LaunchedEffect(targetCount) {
        if (targetCount != previousCount) {
            trigger = !trigger
            previousCount = targetCount
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (trigger) 1.2f else 1f,
        animationSpec = AnimSpecs.BadgePopSpec,
        label = "badgeScale"
    )


    LaunchedEffect(trigger) {
        kotlinx.coroutines.delay(100)
    }

    androidx.compose.foundation.layout.Box(
        modifier = Modifier.scale(scale)
    ) {
        content(targetCount)
    }
}



@Composable
fun TypingDotsAnimation(): Triple<Float, Float, Float> {
    val infiniteTransition = rememberInfiniteTransition(label = "typingDots")

    val dot1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 800
                0f at 0
                -6f at 150
                0f at 300
                0f at 800
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot1"
    )

    val dot2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 800
                0f at 0
                0f at 150
                -6f at 300
                0f at 450
                0f at 800
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot2"
    )

    val dot3Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 800
                0f at 0
                0f at 300
                -6f at 450
                0f at 600
                0f at 800
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot3"
    )

    return Triple(dot1Offset, dot2Offset, dot3Offset)
}



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



fun Modifier.screenEntrance(): Modifier = composed {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = AnimSpecs.MediumDurationMs,
            easing = FastOutSlowInEasing
        ),
        label = "screenAlpha"
    )

    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = tween(
            durationMillis = AnimSpecs.MediumDurationMs,
            easing = FastOutSlowInEasing
        ),
        label = "screenOffsetY"
    )

    this
        .alpha(alpha)
        .offset { IntOffset(0, offsetY.toInt()) }
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



fun Modifier.shimmerAnimation(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")

    val translateX by transition.animateFloat(
        initialValue = -500f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    this.graphicsLayer {
        translationX = translateX
    }
}



@Composable
fun AnimatedFABContent(
    expanded: Boolean,
    collapsedContent: @Composable () -> Unit,
    expandedContent: @Composable () -> Unit
) {
    AnimatedContent(
        targetState = expanded,
        transitionSpec = {
            if (targetState) {

                (fadeIn(animationSpec = tween(150)) +
                    scaleIn(initialScale = 0.85f, animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy
                    ))).togetherWith(
                    fadeOut(animationSpec = tween(100)) +
                        scaleOut(targetScale = 0.85f)
                )
            } else {

                (fadeIn(animationSpec = tween(100)) +
                    scaleIn(initialScale = 0.85f)).togetherWith(
                    fadeOut(animationSpec = tween(150)) +
                        scaleOut(targetScale = 0.85f)
                )
            }
        },
        label = "fabContent"
    ) { isExpanded ->
        if (isExpanded) {
            expandedContent()
        } else {
            collapsedContent()
        }
    }
}
