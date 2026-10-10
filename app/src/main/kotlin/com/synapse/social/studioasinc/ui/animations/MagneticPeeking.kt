package com.synapse.social.studioasinc.ui.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

/**
 * A modifier that adds a magnetic "stretching" effect to a component when dragged.
 * It snaps back with a spring animation and triggers haptic feedback.
 *
 * It allows vertical scrolling to pass through to parents by checking the drag ratio.
 */
fun Modifier.magneticPeeking(
    onPeek: (Offset) -> Unit = {},
    onRelease: () -> Unit = {}
): Modifier = composed {
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var totalDrag = remember { Offset.Zero }

    this.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            totalDrag = Offset.Zero
            var isPeeking = false

            drag(down.id) { change ->
                val dragAmount = change.positionChange()
                totalDrag += dragAmount

                // Determine if we should start peeking based on drag direction
                if (!isPeeking) {
                    if (abs(totalDrag.x) > abs(totalDrag.y) && abs(totalDrag.x) > 10f) {
                        isPeeking = true
                    } else if (abs(totalDrag.y) > abs(totalDrag.x) && abs(totalDrag.y) > 10f) {
                        // Vertical drag exceeds horizontal drag, let it pass through to scroll
                        return@drag
                    }
                }

                if (isPeeking) {
                    change.consume()
                    // Rubber-band effect: non-linear resistance
                    val dampenedX = rubberBand(totalDrag.x, size.width.toFloat())
                    val dampenedY = rubberBand(totalDrag.y, size.height.toFloat())

                    val dampenedOffset = Offset(dampenedX, dampenedY)

                    coroutineScope.launch {
                        offset.snapTo(dampenedOffset)
                        onPeek(dampenedOffset)
                    }
                }
            }

            // On release/cancel
            coroutineScope.launch {
                onRelease()
                if (offset.value != Offset.Zero) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    offset.animateTo(
                        targetValue = Offset.Zero,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
            }
        }
    }
    .graphicsLayer {
        translationX = offset.value.x
        translationY = offset.value.y
        // Tactile distortion
        rotationZ = (offset.value.x / 100f).coerceIn(-3f, 3f)
        rotationX = -(offset.value.y / 100f).coerceIn(-3f, 3f)
    }
}

/**
 * Standard rubber band math
 */
private fun rubberBand(offset: Float, dimension: Float): Float {
    if (dimension <= 0f) return offset * 0.4f
    val resistance = 0.55f
    val limit = dimension * 0.8f
    return (offset / (1 + (abs(offset) * resistance / limit)))
}

/**
 * A container that reveals a background "peek" content when the foreground is dragged magnetically.
 */
@Composable
fun PeekingBox(
    peekContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        // Peek content is behind
        Box(modifier = Modifier.matchParentSize()) {
            peekContent()
        }
        // Main content is on top with magnetic behavior
        Box(modifier = Modifier.magneticPeeking()) {
            content()
        }
    }
}
