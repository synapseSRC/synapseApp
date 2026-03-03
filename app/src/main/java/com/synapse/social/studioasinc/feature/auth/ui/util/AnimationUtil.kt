package com.synapse.social.studioasinc.feature.auth.ui.util

import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext



object AnimationUtil {



    @Composable
    fun rememberReducedMotion(): Boolean {
        val context = LocalContext.current
        return remember(context) {
            val animationScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            val transitionScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1.0f
            )
            animationScale == 0f || transitionScale == 0f
        }
    }



    @Composable
    fun animatedScale(pressed: Boolean): Float {
        val reducedMotion = rememberReducedMotion()
        return if (reducedMotion) 1f else {
            animateFloatAsState(
                targetValue = if (pressed) 0.95f else 1f,
                animationSpec = tween(durationMillis = 100),
                label = "Button Scale"
            ).value
        }
    }
}
