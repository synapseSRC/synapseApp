package com.synapse.social.studioasinc.feature.inbox.inbox.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier
) {
    val dots = listOf(
        remember { Animatable(0f) },
        remember { Animatable(0f) },
        remember { Animatable(0f) }
    )

    LaunchedEffect(Unit) {
        dots.forEachIndexed { index, animatable ->
            launch {
                delay(index * 150L)
                animatable.animateTo(
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 600, easing = FastOutLinearInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
        }
    }

    Box(
        modifier = modifier
            .padding(vertical = Spacing.Small)
            .clip(RoundedCornerShape(
                topStart = Sizes.CornerMassive,
                topEnd = Sizes.CornerMassive,
                bottomStart = Spacing.ExtraSmall,
                bottomEnd = Sizes.CornerMassive
            ))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            dots.forEachIndexed { index, animatable ->
                Box(
                    modifier = Modifier
                        .offset(y = (-Spacing.ExtraSmall.value * animatable.value).dp)
                        .size(Spacing.ExtraSmallMedium)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                )
                if (index < dots.size - 1) {
                    Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                }
            }
        }
    }
}
