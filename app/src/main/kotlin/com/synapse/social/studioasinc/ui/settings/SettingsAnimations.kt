package com.synapse.social.studioasinc.ui.settings

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.Alignment

object SettingsAnimations {
    private const val SCREEN_TRANSITION_DURATION = 300

    val enterTransition: EnterTransition = fadeIn(
        animationSpec = tween(
            durationMillis = SCREEN_TRANSITION_DURATION,
            easing = FastOutSlowInEasing
        )
    ) + slideInHorizontally(
        animationSpec = tween(
            durationMillis = SCREEN_TRANSITION_DURATION,
            easing = FastOutSlowInEasing
        ),
        initialOffsetX = { fullWidth -> fullWidth / 4 }
    )

    val exitTransition: ExitTransition = fadeOut(
        animationSpec = tween(
            durationMillis = SCREEN_TRANSITION_DURATION,
            easing = FastOutSlowInEasing
        )
    ) + slideOutHorizontally(
        animationSpec = tween(
            durationMillis = SCREEN_TRANSITION_DURATION,
            easing = FastOutSlowInEasing
        ),
        targetOffsetX = { fullWidth -> -fullWidth / 4 }
    )

    val popEnterTransition: EnterTransition = fadeIn(
        animationSpec = tween(
            durationMillis = SCREEN_TRANSITION_DURATION,
            easing = FastOutSlowInEasing
        )
    ) + slideInHorizontally(
        animationSpec = tween(
            durationMillis = SCREEN_TRANSITION_DURATION,
            easing = FastOutSlowInEasing
        ),
        initialOffsetX = { fullWidth -> -fullWidth / 4 }
    )

    val popExitTransition: ExitTransition = fadeOut(
        animationSpec = tween(
            durationMillis = SCREEN_TRANSITION_DURATION,
            easing = FastOutSlowInEasing
        )
    ) + slideOutHorizontally(
        animationSpec = tween(
            durationMillis = SCREEN_TRANSITION_DURATION,
            easing = FastOutSlowInEasing
        ),
        targetOffsetX = { fullWidth -> fullWidth / 4 }
    )

    const val pressScale: Float = 0.98f
}

