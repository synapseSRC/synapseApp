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
    val OnlineGreen = StatusOnline
    val OnlineGreenLight = com.synapse.social.studioasinc.feature.shared.theme.OnlineGreenLight
    val OfflineGray = StatusOffline

    val UnreadAccent: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    val PinnedBackground = com.synapse.social.studioasinc.feature.shared.theme.PinnedBackground
    val PinnedBackgroundDark = com.synapse.social.studioasinc.feature.shared.theme.PinnedBackgroundDark
    val PinnedIcon = AccentYellow

    val SwipeArchive: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    val SwipeDelete: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.error

    val SwipeMute: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.tertiary

    val SwipePin: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.secondary

    val StoryGradientStart = com.synapse.social.studioasinc.feature.shared.theme.StoryGradientStart
    val StoryGradientMiddle = com.synapse.social.studioasinc.feature.shared.theme.StoryGradientMiddle
    val StoryGradientEnd = com.synapse.social.studioasinc.feature.shared.theme.StoryGradientEnd

    val TypingDot: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    val storyRingGradient: Brush
        @Composable
        @ReadOnlyComposable
        get() = Brush.sweepGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.colorScheme.primary
            )
        )
}

object InboxShapes {
    val ChatBadge = CircleShape
    val AvatarShape = CircleShape

    val SearchBar: androidx.compose.ui.graphics.Shape
        @OptIn(ExperimentalMaterial3ExpressiveApi::class)
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes.extraExtraLarge

    val ChatItemCard: androidx.compose.ui.graphics.Shape
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes.medium

    val SwipeActionShape: androidx.compose.ui.graphics.Shape
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes.small

    val TabIndicator = CircleShape

    val FABShape: androidx.compose.ui.graphics.Shape
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes.large

    val GroupedListTopShape = RoundedCornerShape(topStart = Spacing.Medium, topEnd = Spacing.Medium, bottomStart = Spacing.ExtraSmall, bottomEnd = Spacing.ExtraSmall)
    val GroupedListMiddleShape = RoundedCornerShape(Spacing.ExtraSmall)
    val GroupedListBottomShape = RoundedCornerShape(topStart = Spacing.ExtraSmall, topEnd = Spacing.ExtraSmall, bottomStart = Spacing.Medium, bottomEnd = Spacing.Medium)
    val GroupedListSingleShape: androidx.compose.ui.graphics.Shape
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes.medium
}

object InboxAnimations {
    const val EntranceStaggerDelayMs = 40
    const val ShortDurationMs = 150
    const val MediumDurationMs = 300
    const val LongDurationMs = 500

    val BadgePopSpec: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val ItemEntranceSpec: AnimationSpec<Float> = tween(
        durationMillis = MediumDurationMs,
        easing = FastOutSlowInEasing
    )

    val PulseSpec: InfiniteRepeatableSpec<Float> = infiniteRepeatable(
        animation = tween(
            durationMillis = 1500,
            easing = FastOutSlowInEasing
        ),
        repeatMode = RepeatMode.Reverse
    )

    val TypingBounceSpec: InfiniteRepeatableSpec<Float> = infiniteRepeatable(
        animation = keyframes {
            durationMillis = 600
            0f at 0 using LinearEasing
            -6f at 150 using FastOutSlowInEasing
            0f at 300 using FastOutSlowInEasing
            0f at 600 using LinearEasing
        },
        repeatMode = RepeatMode.Restart
    )

    const val SwipeThresholdFraction = 0.3f

    val FABExpandSpec: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    val SearchExpandSpec: AnimationSpec<Float> = tween(
        durationMillis = MediumDurationMs,
        easing = FastOutSlowInEasing
    )
}

object InboxDimens {
    val AvatarSize = Sizes.AvatarLarge
    val AvatarSizeSmall = Sizes.AvatarMedium
    val OnlineIndicatorSize = Sizes.IconSemiSmall
    val OnlineIndicatorBorder = Sizes.BorderDefault
    val UnreadBadgeSize = Sizes.IconSmallMedium
    val UnreadBadgeSizeSmall = Sizes.IconMedium
    val StoryRingWidth = Sizes.BorderSelected
    val ChatItemPadding = Spacing.Medium
    val ChatItemVerticalSpacing = Spacing.Small
    val GroupedItemGap = Sizes.BorderDefault
    val SectionHeaderHeight = Sizes.AvatarSmall
    val SwipeActionIconSize = Sizes.IconExtraLarge
    val FABSize = Sizes.AvatarLarge
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
