package com.synapse.social.studioasinc.feature.inbox.inbox.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Shared-element key for a person's picture.
 *
 * The conversation's floating pill and the Contact Info header derive it from the same user id, so
 * the element keeps one identity wherever that person appears: tap, tap again, go back, come back,
 * and it is still the same picture travelling. A conversation whose participant id is not known
 * falls back to the chat id, which both screens also receive, so the two sides still agree.
 */
internal fun chatAvatarHeroKey(userId: String?, chatId: String): String =
    if (!userId.isNullOrBlank()) "chat-avatar-user-" + userId else "chat-avatar-chat-" + chatId

/**
 * One smooth spring with no overshoot, of the same family as the create-post button's movement:
 * the picture arrives where it is going rather than bouncing there, and settles in roughly the time
 * the screen around it takes to fade.
 */
private val ChatAvatarHeroBounds: BoundsTransform = BoundsTransform { _, _ ->
    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
}

/**
 * The shared-element modifier for a person's picture, or [Modifier] when the surrounding navigation
 * did not supply the transition scopes (previews, tests) - which keeps both the floating header and
 * Contact Info usable on their own.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun rememberChatAvatarHero(
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    chatId: String,
    userId: String? = null
): Modifier {
    if (sharedTransitionScope == null || animatedVisibilityScope == null) return Modifier
    return with(sharedTransitionScope) {
        // The picture is round in both screens, and stays round while the overlay carries it.
        val circleClip = remember { OverlayClip(CircleShape) }
        Modifier.sharedElement(
            sharedContentState = rememberSharedContentState(key = chatAvatarHeroKey(userId, chatId)),
            animatedVisibilityScope = animatedVisibilityScope,
            boundsTransform = ChatAvatarHeroBounds,
            clipInOverlayDuringTransition = circleClip
        )
    }
}
