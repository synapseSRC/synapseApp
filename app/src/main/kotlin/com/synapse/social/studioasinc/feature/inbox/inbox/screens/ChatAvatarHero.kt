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
import androidx.compose.ui.graphics.RectangleShape

/**
 * Shared-element key for a person's picture.
 *
 * The conversation's floating pill and the Contact Info header derive it from the same user id, so
 * the element keeps one identity wherever that person appears: tap, tap again, go back, come back,
 * and it is still the same picture travelling. A conversation whose participant id is not known
 * falls back to the chat id, which both screens also receive, so the two sides still agree.
 */
internal fun chatAvatarHeroKey(userId: String?, chatId: String): String =
    chatHeroKey("chat-avatar", userId, chatId)

/**
 * The other values the pill and Contact Info both show about a person: the name above the status and
 * the active / last-seen line below it. Each one is a shared element of its own, keyed from the same
 * ids as the picture, so all three travel together instead of only the picture moving.
 */
internal enum class ChatHeaderTextHero(val keyPrefix: String) {
    /** The name: the pill's title and the contact header's headline. */
    Name("chat-name"),

    /** The status line: what the pill says about the person right now, and the same line below. */
    Status("chat-status")
}

/** Shared-element key for one line of a person's header, derived from the same ids as the picture. */
internal fun chatHeaderTextHeroKey(hero: ChatHeaderTextHero, userId: String?, chatId: String): String =
    chatHeroKey(hero.keyPrefix, userId, chatId)

/**
 * One identity per person per value, wherever that person appears. A conversation whose participant
 * id is not known - a group, for instance - falls back to the chat id, which both screens also
 * receive, so the two sides either agree on the element or deliberately do not declare it.
 */
private fun chatHeroKey(prefix: String, userId: String?, chatId: String): String =
    if (!userId.isNullOrBlank()) prefix + "-user-" + userId else prefix + "-chat-" + chatId

/**
 * One smooth spring with no overshoot, of the same family as the create-post button's movement:
 * the element arrives where it is going rather than bouncing there, and settles in roughly the time
 * the screen around it takes to fade. The picture, the name and the status all move on this spring,
 * so the three read as one movement instead of three animations that happen to overlap.
 */
private val ChatHeroBounds: BoundsTransform = BoundsTransform { _, _ ->
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
            boundsTransform = ChatHeroBounds,
            clipInOverlayDuringTransition = circleClip
        )
    }
}

/**
 * The shared-element modifier for one line of a person's header - the name or the status - or
 * [Modifier] when the surrounding navigation did not supply the transition scopes.
 *
 * Bounds rather than a plain shared element: the pill sets the same value in its own type and at its
 * own size, so the two screens own different text with different metrics. [SharedTransitionScope.sharedBounds]
 * animates the space between the two layouts while the texts cross-fade inside it, which is the mode
 * the framework documents for Text and the reason the two lines can share an identity at all.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun rememberChatHeaderTextHero(
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    chatId: String,
    hero: ChatHeaderTextHero,
    userId: String? = null
): Modifier {
    if (sharedTransitionScope == null || animatedVisibilityScope == null) return Modifier
    return with(sharedTransitionScope) {
        // A line of type is not a circle: unclipped, it can leave the pill's edge on its way out
        // instead of being cut off by the capsule it used to live in.
        val noClip = remember { OverlayClip(RectangleShape) }
        Modifier.sharedBounds(
            sharedContentState = rememberSharedContentState(
                key = chatHeaderTextHeroKey(hero, userId, chatId)
            ),
            animatedVisibilityScope = animatedVisibilityScope,
            boundsTransform = ChatHeroBounds,
            clipInOverlayDuringTransition = noClip
        )
    }
}
