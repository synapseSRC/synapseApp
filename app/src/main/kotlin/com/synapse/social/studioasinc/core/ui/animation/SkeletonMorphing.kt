package com.synapse.social.studioasinc.core.ui.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A component that handles a smooth transition from a skeleton/shimmer state to the actual content.
 * It performs a cross-fade and subtle scale effect to avoid jarring "pop-ins".
 */
@Composable
fun SkeletonMorphedContent(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    skeleton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    AnimatedContent(
        targetState = isLoading,
        transitionSpec = {
            if (initialState && !targetState) {
                // Transitioning from Loading (Skeleton) to Content
                (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.95f, animationSpec = tween(400)))
                    .togetherWith(fadeOut(animationSpec = tween(400)) + scaleOut(targetScale = 1.05f, animationSpec = tween(400)))
            } else {
                // Default transition for other state changes
                fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
            }
        },
        label = "SkeletonMorphing",
        modifier = modifier
    ) { loading ->
        if (loading) {
            skeleton()
        } else {
            content()
        }
    }
}
