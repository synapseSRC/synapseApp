package com.synapse.social.studioasinc.feature.auth.ui.util

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

object AnimationUtil {
    @Composable
    fun rememberReducedMotion(): Boolean {
        val context = LocalContext.current.applicationContext
        val reducedMotion = remember(context) { mutableStateOf(readReducedMotion(context)) }

        DisposableEffect(context) {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    reducedMotion.value = readReducedMotion(context)
                }
            }
            val resolver = context.contentResolver
            resolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
                false,
                observer
            )
            resolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.TRANSITION_ANIMATION_SCALE),
                false,
                observer
            )
            reducedMotion.value = readReducedMotion(context)

            onDispose { resolver.unregisterContentObserver(observer) }
        }

        return reducedMotion.value
    }

    private fun readReducedMotion(context: Context): Boolean {
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
        return animationScale == 0f || transitionScale == 0f
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
