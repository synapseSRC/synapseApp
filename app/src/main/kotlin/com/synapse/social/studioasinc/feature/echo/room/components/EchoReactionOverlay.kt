package com.synapse.social.studioasinc.feature.echo.room.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.echo.room.EchoFloatingReaction
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

/**
 * Ephemeral reactions shared over the room's data channel.
 *
 * The list is already capped and age-limited by the ViewModel; this only draws it, bottom-up, so
 * the newest bubble is the one closest to the controls. Reactions are decorative, so the emoji is
 * also the accessibility label.
 */
@Composable
fun EchoReactionOverlay(
    reactions: List<EchoFloatingReaction>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.BottomEnd) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(Spacing.space4),
            modifier = Modifier.padding(end = Spacing.space16, bottom = Spacing.space8)
        ) {
            reactions.forEach { reaction ->
                key(reaction.id) {
                    EchoReactionBubble(reaction)
                }
            }
        }
    }
}

@Composable
private fun EchoReactionBubble(reaction: EchoFloatingReaction) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val description = stringResource(R.string.echo_room_react_with, reaction.emoji)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(FADE_MILLIS)) +
            slideInVertically(tween(RISE_MILLIS)) { height -> height } +
            scaleIn(tween(RISE_MILLIS), initialScale = 0.7f)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            tonalElevation = Sizes.ElevationLow,
            modifier = Modifier.semantics { contentDescription = description }
        ) {
            Text(
                text = reaction.emoji,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = Spacing.space12, vertical = Spacing.space4)
            )
        }
    }
}

private const val FADE_MILLIS = 180
private const val RISE_MILLIS = 260
