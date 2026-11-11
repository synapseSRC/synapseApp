package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.synapse.social.studioasinc.feature.auth.ui.util.AnimationUtil
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.chat.effects.ChatEffectAnimation
import com.synapse.social.studioasinc.shared.domain.model.chat.effects.MessageEffect

/**
 * Paints the effects resolved for one message.
 *
 * The message text is never touched here: effects are drawn beside it, so the message keeps its
 * content and its delivery semantics while gaining the flourish.
 *
 * Motion is opt-out. When the device asks for reduced motion the same effects render as a static row
 * of emoji, which keeps the feature usable and predictable instead of removing it.
 */
@Composable
fun WordEffectOverlay(
    effects: List<MessageEffect>,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = AnimationUtil.rememberReducedMotion()
) {
    if (effects.isEmpty()) return

    val transition = rememberInfiniteTransition(label = "word_effect")
    val pulseScale by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "word_effect_pulse"
    )
    val twinkleAlpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "word_effect_twinkle"
    )
    val drift by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "word_effect_drift"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.space4)
    ) {
        effects.forEach { effect ->
            val motionEnabled = !reducedMotion
            val scale = if (motionEnabled && effect.animation == ChatEffectAnimation.PULSE) pulseScale else 1f
            val alpha = if (!motionEnabled) 1f else when (effect.animation) {
                ChatEffectAnimation.SPARKLE -> twinkleAlpha
                ChatEffectAnimation.GLOW -> twinkleAlpha
                else -> 1f
            }
            val offsetY = if (motionEnabled && effect.animation == ChatEffectAnimation.CONFETTI) drift else 0f

            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                    translationY = offsetY
                }
            ) {
                Text(
                    text = effect.emoji,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = Spacing.space8, vertical = Spacing.space4)
                )
            }
        }
    }
}
