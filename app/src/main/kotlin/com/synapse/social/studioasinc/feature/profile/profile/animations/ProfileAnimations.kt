package com.synapse.social.studioasinc.feature.profile.profile.animations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer


fun Modifier.pressAnimation() = composed {
    var isPressed by remember { mutableStateOf(false) }
    this.scale(if (isPressed) 0.95f else 1f)
}


fun Modifier.parallaxScroll(scrollOffset: Float, parallaxRatio: Float = 0.5f) = this.then(
    Modifier.graphicsLayer {
        translationY = scrollOffset * parallaxRatio
        val scale = 1f - (scrollOffset / 1000f).coerceIn(0f, 0.3f)
        scaleX = scale
        scaleY = scale
    }
)


@Composable
fun rememberLikeAnimation(): Animatable<Float, AnimationVector1D> {
    val scale = remember { Animatable(1f) }
    return scale
}

suspend fun Animatable<Float, AnimationVector1D>.animateLike() {
    animateTo(
        targetValue = 1.3f,
        animationSpec = tween(durationMillis = 100)
    )
    animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 100)
    )
}


@Composable
fun <T> AnimatedContent(
    targetState: T,
    content: @Composable (T) -> Unit
) {
    androidx.compose.animation.AnimatedContent(
        targetState = targetState,
        transitionSpec = {
            fadeIn(animationSpec = tween(250)) togetherWith
                    fadeOut(animationSpec = tween(250))
        }
    ) { state ->
        content(state)
    }
}


@Composable
fun <T> crossfadeContent(
    targetState: T,
    content: @Composable (T) -> Unit
) {
    AnimatedContent(targetState = targetState, content = content)
}


@Composable
fun ExpandableContent(
    expanded: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
        exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
    ) {
        content()
    }
}
