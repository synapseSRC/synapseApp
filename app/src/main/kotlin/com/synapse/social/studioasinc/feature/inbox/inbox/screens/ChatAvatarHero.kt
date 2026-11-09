package com.synapse.social.studioasinc.feature.inbox.inbox.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.OverlayClip
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSharedContentState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Shared-element key for a chat's profile picture. The pill in the conversation and the header of
 * Contact Info both claim the key returned here, so the picture travels between the two screens as
 * one element instead of two elements swapping places.
 */
private fun chatAvatarHeroKey(chatId: String): String = "chat-avatar-" + chatId

/**
 * The bounds spring the create-post share sheet already uses, so a shared element moves with one
 * feel across the app instead of a new curve per screen.
 */
private val ChatAvatarHeroBounds: BoundsTransform = BoundsTransform { _, _ ->
    spring(dampingRatio = 0.8f, stiffness = 380f)
}

/** The picture stays a circle while it travels through the shared-element overlay. */
private val ChatAvatarHeroClip: OverlayClip = OverlayClip(CircleShape)

/**
 * The shared-element modifier for a chat's profile picture, or [Modifier] when the surrounding
 * navigation did not supply the transition scopes (previews, tests) - which keeps both the floating
 * header and Contact Info usable on their own.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun rememberChatAvatarHero(
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    chatId: String
): Modifier {
    if (sharedTransitionScope == null || animatedVisibilityScope == null) return Modifier
    return with(sharedTransitionScope) {
        Modifier.sharedElement(
            state = rememberSharedContentState(key = chatAvatarHeroKey(chatId)),
            animatedVisibilityScope = animatedVisibilityScope,
            boundsTransform = ChatAvatarHeroBounds,
            clipInOverlayDuringTransition = ChatAvatarHeroClip
        )
    }
}
