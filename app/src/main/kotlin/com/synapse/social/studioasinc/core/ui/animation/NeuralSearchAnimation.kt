package com.synapse.social.studioasinc.core.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.synapse.social.studioasinc.feature.shared.theme.SynapseBlue
import com.synapse.social.studioasinc.feature.shared.theme.SynapseLightBlue
import kotlin.random.Random

@Composable
fun NeuralParticleEmitter(
    targetRect: Rect?,
    isAnimating: Boolean,
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (targetRect == null || !isAnimating) return

    var canvasSize by remember { mutableStateOf(Size.Zero) }

    val particles = remember(targetRect, canvasSize) {
        if (canvasSize == Size.Zero) emptyList()
        else List(40) {
            Particle(
                startOffset = generateEdgeOffset(canvasSize),
                targetOffset = Offset(
                    targetRect.left + Random.nextFloat() * targetRect.width,
                    targetRect.top + Random.nextFloat() * targetRect.height
                ),
                color = if (Random.nextBoolean()) SynapseBlue else SynapseLightBlue,
                radius = Random.nextFloat() * 4f + 2f,
                delay = Random.nextInt(0, 400)
            )
        }
    }

    val progress = remember { Animatable(0f) }

    LaunchedEffect(isAnimating) {
        if (isAnimating) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
            )
            onAnimationComplete()
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        canvasSize = size
        particles.forEach { particle ->
            // Adjust particleProgress to account for delay
            val totalDuration = 1000f
            val particleDuration = 600f
            val currentTime = progress.value * totalDuration
            val particleProgress = ((currentTime - particle.delay) / particleDuration).coerceIn(0f, 1f)

            if (particleProgress > 0f) {
                val currentOffset = Offset(
                    x = particle.startOffset.x + (particle.targetOffset.x - particle.startOffset.x) * particleProgress,
                    y = particle.startOffset.y + (particle.targetOffset.y - particle.startOffset.y) * particleProgress
                )

                val alpha = if (particleProgress > 0.8f) (1f - particleProgress) * 5f else 1f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(particle.color.copy(alpha = alpha * 0.6f), Color.Transparent),
                        center = currentOffset,
                        radius = particle.radius * 3
                    ),
                    center = currentOffset,
                    radius = particle.radius * 3
                )

                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.9f),
                    center = currentOffset,
                    radius = particle.radius
                )
            }
        }
    }
}

private fun generateEdgeOffset(size: Size): Offset {
    val side = Random.nextInt(4)
    return when (side) {
        0 -> Offset(Random.nextFloat() * size.width, -50f) // Top
        1 -> Offset(Random.nextFloat() * size.width, size.height + 50f) // Bottom
        2 -> Offset(-50f, Random.nextFloat() * size.height) // Left
        else -> Offset(size.width + 50f, Random.nextFloat() * size.height) // Right
    }
}

private data class Particle(
    val startOffset: Offset,
    val targetOffset: Offset,
    val color: Color,
    val radius: Float,
    val delay: Int
)
