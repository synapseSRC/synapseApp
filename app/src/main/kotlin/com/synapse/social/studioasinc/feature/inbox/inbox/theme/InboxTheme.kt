package com.synapse.social.studioasinc.feature.inbox.inbox.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

    val UnreadAccent = LightPrimary
    val UnreadAccentLight = DarkPrimary

    val PinnedBackground = com.synapse.social.studioasinc.feature.shared.theme.PinnedBackground
    val PinnedBackgroundDark = com.synapse.social.studioasinc.feature.shared.theme.PinnedBackgroundDark
    val PinnedIcon = AccentYellow

    val SwipeArchive = AccentBlue
    val SwipeDelete = LightError
    val SwipeMute = AccentOrange
    val SwipePin = AccentYellow

    val StoryGradientStart = com.synapse.social.studioasinc.feature.shared.theme.StoryGradientStart
    val StoryGradientMiddle = com.synapse.social.studioasinc.feature.shared.theme.StoryGradientMiddle
    val StoryGradientEnd = com.synapse.social.studioasinc.feature.shared.theme.StoryGradientEnd

    val TypingDot = LightPrimary
    val TypingDotLight = DarkPrimary

    val storyRingGradient: Brush
        get() = Brush.sweepGradient(
            colors = listOf(
                StoryGradientStart,
                StoryGradientMiddle,
                StoryGradientEnd,
                StoryGradientStart
            )
        )
}

object InboxShapes {
    val ChatBadge = CircleShape
    val AvatarShape = CircleShape
    val SearchBar = RoundedCornerShape(Sizes.IconExtraLarge)
    val ChatItemCard = RoundedCornerShape(Spacing.Medium)
    val SwipeActionShape = RoundedCornerShape(Spacing.SmallMedium)
    val TabIndicator = RoundedCornerShape(50)
    val FABShape = RoundedCornerShape(Spacing.Medium)
    val GroupedListTopShape = RoundedCornerShape(topStart = Spacing.Medium, topEnd = Spacing.Medium, bottomStart = Spacing.None, bottomEnd = Spacing.None)
    val GroupedListMiddleShape = RoundedCornerShape(Spacing.None)
    val GroupedListBottomShape = RoundedCornerShape(topStart = Spacing.None, topEnd = Spacing.None, bottomStart = Spacing.Medium, bottomEnd = Spacing.Medium)
    val GroupedListSingleShape = RoundedCornerShape(Spacing.Medium)
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
