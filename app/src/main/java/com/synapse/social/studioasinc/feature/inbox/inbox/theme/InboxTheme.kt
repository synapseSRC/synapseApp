package com.synapse.social.studioasinc.ui.inbox.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp



object InboxColors {

    val OnlineGreen = Color(0xFF4CAF50)
    val OnlineGreenLight = Color(0xFF81C784)
    val OfflineGray = Color(0xFF9E9E9E)


    val UnreadAccent = Color(0xFF6750A4)
    val UnreadAccentLight = Color(0xFFD0BCFF)


    val PinnedBackground = Color(0xFFFFF8E1)
    val PinnedBackgroundDark = Color(0xFF3E2723)
    val PinnedIcon = Color(0xFFFFB300)


    val SwipeArchive = Color(0xFF2196F3)
    val SwipeDelete = Color(0xFFF44336)
    val SwipeMute = Color(0xFFFF9800)
    val SwipePin = Color(0xFFFFB300)


    val StoryGradientStart = Color(0xFFE040FB)
    val StoryGradientMiddle = Color(0xFFFF5722)
    val StoryGradientEnd = Color(0xFFFFEB3B)


    val TypingDot = Color(0xFF6750A4)
    val TypingDotLight = Color(0xFFD0BCFF)



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
    val SearchBar = RoundedCornerShape(28.dp)
    val ChatItemCard = RoundedCornerShape(16.dp)
    val SwipeActionShape = RoundedCornerShape(12.dp)
    val TabIndicator = RoundedCornerShape(50)
    val FABShape = RoundedCornerShape(16.dp)
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
    val AvatarSize = 56.dp
    val AvatarSizeSmall = 40.dp
    val OnlineIndicatorSize = 14.dp
    val OnlineIndicatorBorder = 2.dp
    val UnreadBadgeSize = 22.dp
    val UnreadBadgeSizeSmall = 18.dp
    val StoryRingWidth = 3.dp
    val ChatItemPadding = 16.dp
    val ChatItemVerticalSpacing = 4.dp
    val SectionHeaderHeight = 36.dp
    val SwipeActionIconSize = 28.dp
    val FABSize = 56.dp
    val SearchBarHeight = 56.dp
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
