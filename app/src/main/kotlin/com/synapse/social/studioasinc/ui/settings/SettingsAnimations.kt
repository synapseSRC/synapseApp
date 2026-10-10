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
    private const val CONTENT_ANIMATION_DURATION = 200
    private const val PRESS_ANIMATION_DURATION = 100



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



    val expandAnimation: EnterTransition = expandVertically(
        animationSpec = tween(
            durationMillis = CONTENT_ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        ),
        expandFrom = Alignment.Top
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = CONTENT_ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        )
    )



    val collapseAnimation: ExitTransition = shrinkVertically(
        animationSpec = tween(
            durationMillis = CONTENT_ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        ),
        shrinkTowards = Alignment.Top
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = CONTENT_ANIMATION_DURATION,
            easing = FastOutSlowInEasing
        )
    )



    const val pressScale: Float = 0.98f



    const val pressDuration: Int = PRESS_ANIMATION_DURATION



    val pressAnimationSpec = tween<Float>(
        durationMillis = PRESS_ANIMATION_DURATION,
        easing = FastOutSlowInEasing
    )
}



object SettingsNavigationAnimations {



    fun <T> AnimatedContentTransitionScope<T>.settingsEnterTransition(): EnterTransition {
        return SettingsAnimations.enterTransition
    }



    fun <T> AnimatedContentTransitionScope<T>.settingsExitTransition(): ExitTransition {
        return SettingsAnimations.exitTransition
    }



    fun <T> AnimatedContentTransitionScope<T>.settingsPopEnterTransition(): EnterTransition {
        return SettingsAnimations.popEnterTransition
    }



    fun <T> AnimatedContentTransitionScope<T>.settingsPopExitTransition(): ExitTransition {
        return SettingsAnimations.popExitTransition
    }
}
