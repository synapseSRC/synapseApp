package com.synapse.social.studioasinc.feature.inbox.inbox.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.synapse.social.studioasinc.feature.shared.theme.*
import androidx.compose.ui.unit.dp

object InboxColors {
    val OnlineGreenLight = com.synapse.social.studioasinc.feature.shared.theme.OnlineGreenLight

    val PinnedBackground = com.synapse.social.studioasinc.feature.shared.theme.PinnedBackground
    val PinnedBackgroundDark = com.synapse.social.studioasinc.feature.shared.theme.PinnedBackgroundDark

    val StoryGradientStart = com.synapse.social.studioasinc.feature.shared.theme.StoryGradientStart
    val StoryGradientMiddle = com.synapse.social.studioasinc.feature.shared.theme.StoryGradientMiddle
    val StoryGradientEnd = com.synapse.social.studioasinc.feature.shared.theme.StoryGradientEnd

    val TypingDot: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary
}

object InboxShapes {
    val SearchBar: androidx.compose.ui.graphics.Shape
        @OptIn(ExperimentalMaterial3ExpressiveApi::class)
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes.extraExtraLarge

    val ChatItemCard: androidx.compose.ui.graphics.Shape
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes.medium

    val GroupedListTopShape = RoundedCornerShape(topStart = Sizes.CornerLarge, topEnd = Sizes.CornerLarge, bottomStart = Sizes.CornerSmall, bottomEnd = Sizes.CornerSmall)
    val GroupedListMiddleShape = RoundedCornerShape(Sizes.CornerSmall)
    val GroupedListBottomShape = RoundedCornerShape(topStart = Sizes.CornerSmall, topEnd = Sizes.CornerSmall, bottomStart = Sizes.CornerLarge, bottomEnd = Sizes.CornerLarge)
    val GroupedListSingleShape: androidx.compose.ui.graphics.Shape
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes.medium
}

object InboxAnimations {
    const val MediumDurationMs = 300

    /**
     * Per-dot bounce spec for the typing indicator. The stagger is baked into
     * the keyframe timings so a single InfiniteTransition can drive all three
     * dots and the motion is stable across recompositions.
     *
     * @param dotIndex 0-based dot index; delays the movement phase by
     *   [dotIndex] * 150 ms.
     */
    fun typingBounceSpec(dotIndex: Int): InfiniteRepeatableSpec<Float> = infiniteRepeatable(
        animation = keyframes {
            durationMillis = 600
            val delayMs = dotIndex * 150
            0f at 0 using LinearEasing
            -5f at (150 + delayMs) using FastOutSlowInEasing
            0f at (300 + delayMs) using FastOutSlowInEasing
            0f at 600 using LinearEasing
        },
        repeatMode = RepeatMode.Restart
    )
}

object InboxDimens {
    val AvatarSize = Sizes.AvatarLarge
    val GroupedItemGap = Spacing.space4
    val SearchBarHeight = Sizes.AvatarLarge
}

object InboxTheme {
    val colors: InboxColors
        @Composable
        @ReadOnlyComposable
        get() = InboxColors

    val shapes: InboxShapes
        @Composable
        @ReadOnlyComposable
        get() = InboxShapes

    val animations: InboxAnimations
        @Composable
        @ReadOnlyComposable
        get() = InboxAnimations

    val dimens: InboxDimens
        @Composable
        @ReadOnlyComposable
        get() = InboxDimens
}
