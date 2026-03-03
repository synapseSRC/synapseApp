package com.synapse.social.studioasinc.feature.shared.reels.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

@Composable
fun HeartAnimation(
    modifier: Modifier = Modifier,
    onAnimationEnd: () -> Unit
) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {

        launch {
            scale.animateTo(
                targetValue = 1.2f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            )
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(100)
            )
        }
        launch {
            alpha.animateTo(1f, tween(100))
        }

        delay(500)


        val scaleJob = launch {
            scale.animateTo(1.5f, tween(200))
        }
        val alphaJob = launch {
            alpha.animateTo(0f, tween(200))
        }

        joinAll(scaleJob, alphaJob)

        onAnimationEnd()
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier
                .size(100.dp)
                .scale(scale.value)
                .alpha(alpha.value)
        )
    }
}
