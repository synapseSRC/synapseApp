package com.synapse.social.studioasinc.feature.call.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.auth.ui.util.AnimationUtil
import com.synapse.social.studioasinc.feature.shared.theme.DarkOnSuccess
import com.synapse.social.studioasinc.feature.shared.theme.DarkSuccess
import com.synapse.social.studioasinc.feature.shared.theme.DarkSuccessContainer
import com.synapse.social.studioasinc.feature.shared.theme.LightOnSuccess
import com.synapse.social.studioasinc.feature.shared.theme.LightSuccess
import com.synapse.social.studioasinc.feature.shared.theme.LightSuccessContainer
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.raisedControlColor
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt

/**
 * The answering control: a wide rounded track with Decline on the left, Answer on the right, and a
 * phone thumb that has to be carried to either end.
 *
 * The gesture is the primary path (a ring answered by a misplaced tap is a real cost), so the labels
 * double as explicit buttons and the track exposes both outcomes as accessibility actions. The
 * commit is latched so one gesture can never fire both callbacks.
 */
@Composable
fun SlideToAnswerBar(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
    overMedia: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    // Answer green is pinned to the static palette: Material You re-derives every non-error role
    // per wallpaper, and a call answer affordance must never turn cyan.
    val success = if (isDark) DarkSuccess else LightSuccess
    val onSuccess = if (isDark) DarkOnSuccess else LightOnSuccess
    val successContainer = if (isDark) DarkSuccessContainer else LightSuccessContainer
    val declineContainer = scheme.errorContainer
    val mediaText = if (overMedia) callMediaTextColor() else null
    val labelColor = mediaText ?: scheme.onSurface
    // Over video a scrim-black track vanishes on dark frames, so the chrome color itself becomes a
    // faint glass veil with a hairline edge; over the voice page the solid track is used.
    val trackColor = mediaText?.copy(alpha = 0.12f) ?: scheme.surfaceContainerHigh
    val trackEdge = if (overMedia) {
        Modifier.border(Sizes.BorderThinAlt, labelColor.copy(alpha = 0.24f), CircleShape)
    } else {
        Modifier
    }
    val thumbContainer = raisedControlColor()
    val reducedMotion = AnimationUtil.rememberReducedMotion()

    val declineLabel = stringResource(R.string.call_control_decline)
    val answerLabel = stringResource(R.string.call_control_answer)
    val sliderLabel = stringResource(R.string.call_answer_slider)

    val offset = remember { Animatable(0f) }
    var committed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun fire(action: () -> Unit) {
        if (committed) return
        committed = true
        action()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(CallDimens.AnswerBarHeight)
    ) {
        val density = LocalDensity.current
        val maxOffsetPx = with(density) {
            (((maxWidth - CallDimens.AnswerThumb) / 2) - CallDimens.AnswerTrackInset).toPx()
        }.coerceAtLeast(0f)
        // The drag callbacks are remembered once and keep the travel of the composition that created
        // them, so a size change would clamp the thumb to stale limits while the commit test used the
        // new ones.
        val latestTravel by rememberUpdatedState(maxOffsetPx)

        LaunchedEffect(maxOffsetPx) {
            if (maxOffsetPx > 0f) {
                offset.snapTo(offset.value.coerceIn(-maxOffsetPx, maxOffsetPx))
            }
        }

        val settle: AnimationSpec<Float> = if (reducedMotion) {
            snap()
        } else {
            // Critically damped: a spring that overshoots pushes the thumb past the lip of the track
            // and wobbles it back, which reads as the control losing the gesture.
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
        }

        val progress = if (maxOffsetPx > 0f) (offset.value / maxOffsetPx).coerceIn(-1f, 1f) else 0f
        val towardDecline = (-progress).coerceIn(0f, 1f)
        val towardAnswer = progress.coerceIn(0f, 1f)

        val dragState = rememberDraggableState { delta ->
            if (committed) return@rememberDraggableState
            // Applied synchronously: a launched snapTo lets every delta that arrives before the next
            // frame read the same pre-move value, so all but the last collapse and the thumb falls
            // behind the finger — and the commit test then measures a position nobody ever saw.
            runBlocking {
                offset.snapTo((offset.value + delta).coerceIn(-latestTravel, latestTravel))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CallDimens.AnswerBarHeight)
                .clip(CircleShape)
                .background(trackColor)
                .then(trackEdge)
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        if (committed) return@draggable
                        val position = offset.value
                        val commitAt = latestTravel * CallDimens.AnswerCommitFraction
                        // A flick answers only when it also carried the thumb partway to the end:
                        // velocity on its own lets a swipe that never left the centre end a call.
                        val flickAt = latestTravel * CallDimens.AnswerFlickTravelFraction
                        val direction = when {
                            position >= commitAt -> 1f
                            position <= -commitAt -> -1f
                            position >= flickAt && velocity >= FLING_VELOCITY_PX -> 1f
                            position <= -flickAt && velocity <= -FLING_VELOCITY_PX -> -1f
                            else -> 0f
                        }
                        // Committed before the settle starts: the glide is feedback, not a gate, and an
                        // answer held behind it dies with a cancellation the moment the next touch lands.
                        if (direction != 0f) fire(if (direction < 0f) onDecline else onAccept)
                        scope.launch { offset.animateTo(direction * latestTravel, settle) }
                    }
                )
                .semantics {
                    contentDescription = sliderLabel
                    customActions = listOf(
                        CustomAccessibilityAction(declineLabel) { fire(onDecline); true },
                        CustomAccessibilityAction(answerLabel) { fire(onAccept); true }
                    )
                }
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to declineContainer.copy(alpha = 0.85f * towardDecline),
                            0.45f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1f to successContainer.copy(alpha = 0.85f * towardAnswer)
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .matchParentSize()
                    .padding(horizontal = Spacing.space24),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnswerLabel(
                    text = declineLabel,
                    color = if (towardDecline > 0.01f) {
                        lerp(labelColor, scheme.error, towardDecline)
                    } else {
                        labelColor
                    },
                    alpha = 1f - 0.5f * towardDecline,
                    enabled = !committed,
                    onClick = { fire(onDecline) }
                )
                Spacer(Modifier.weight(1f))
                AnswerLabel(
                    text = answerLabel,
                    color = if (towardAnswer > 0.01f) {
                        lerp(labelColor, success, towardAnswer)
                    } else {
                        labelColor
                    },
                    alpha = 1f - 0.5f * towardAnswer,
                    enabled = !committed,
                    onClick = { fire(onAccept) }
                )
            }

            val thumbContainerColor = when {
                towardDecline > 0f -> lerp(thumbContainer, scheme.error, towardDecline)
                towardAnswer > 0f -> lerp(thumbContainer, success, towardAnswer)
                else -> thumbContainer
            }
            val thumbTint = when {
                towardDecline > 0f -> lerp(success, scheme.onError, towardDecline)
                towardAnswer > 0f -> lerp(success, onSuccess, towardAnswer)
                else -> success
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset { IntOffset(offset.value.roundToInt(), 0) }
                    .size(CallDimens.AnswerThumb)
                    .shadow(Sizes.ElevationLow, CircleShape)
                    .clip(CircleShape)
                    .background(thumbContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (progress < -0.05f) CallIcons.endCall else CallIcons.acceptCall,
                    contentDescription = null,
                    modifier = Modifier.size(CallDimens.AnswerBarIcon),
                    tint = thumbTint
                )
            }
        }
    }
}

@Composable
private fun AnswerLabel(
    text: String,
    color: Color,
    alpha: Float,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
        color = color.copy(alpha = color.alpha * alpha),
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = Spacing.space12, vertical = Spacing.space12)
    )
}

private const val FLING_VELOCITY_PX = 1_200f
