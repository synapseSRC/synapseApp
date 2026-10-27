package com.synapse.social.studioasinc.feature.profile.profile.animations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer

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

