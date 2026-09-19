package com.synapse.social.studioasinc.feature.shared.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp



object ProfileDimensions {

    val spacing0 = Spacing.None
    val spacing2 = Spacing.Tiny
    val spacing4 = Spacing.ExtraSmall
    val spacing8 = Spacing.Small
    val spacing12 = Spacing.SmallMedium
    val spacing16 = Spacing.Medium
    val spacing20 = Spacing.MediumLarge
    val spacing24 = Spacing.Large
    val spacing32 = Spacing.ExtraLarge
    val spacing48 = Spacing.Huge


    val profileImageSmall = Sizes.AvatarMedium
    val profileImageMedium = Sizes.AvatarLarge
    val profileImageLarge = Sizes.AvatarXLarge
    val profileImageXLarge = Sizes.AvatarHuge
    val profileImageSize = Sizes.AvatarProfile


    val coverPhotoHeight = Sizes.HeightStoryTray
    val coverPhotoHeightExpanded = Sizes.HeightStoryTrayExpanded


    val storyRingWidth = Sizes.BorderSelected
    val storyRingPadding = Sizes.BorderSelected


    val iconSizeSmall = Sizes.IconSemiMedium
    val iconSizeMedium = Sizes.IconDefault
    val iconSize = Sizes.IconLarge
    val iconSizeLarge = Sizes.IconHuge


    val chipHeight = Sizes.HeightChip
    val buttonHeight = Sizes.HeightMedium
    val buttonCornerRadius = Sizes.CornerDefault


    val photoGridSpacing = Spacing.Tiny
    val photoGridCornerRadius = Sizes.CornerSmall


    val cardCornerRadius = Sizes.CornerLarge
    val cardElevation = Sizes.ElevationLow


    val parallaxFactor = 0.5f
}



object ProfileAnimations {
    const val durationShort = 150
    const val durationMedium = 300
    const val durationLong = 500
    const val durationXLong = 800

    const val staggerDelay = 50
    const val countAnimationDuration = 600
    const val shimmerDuration = 1000
}



object ProfileColors {

    val storyRingStart = StoryGradientStart
    val storyRingMiddle = StoryRingMiddle
    val storyRingEnd = StoryRingEnd


    val coverOverlayStart = Color.Transparent
    val coverOverlayMiddle = Color.Black.copy(alpha = 0.1f)
    val coverOverlayEnd = Color.Black.copy(alpha = 0.4f)


    val verifiedBadge = SynapseBlue


    val shimmerBase = ShimmerBase
    val shimmerHighlight = ShimmerHighlight
}



object ProfileBrushes {


    @Composable
    @ReadOnlyComposable
    fun storyRingGradient(): List<Color> = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primary
    )



    fun coverOverlayGradient(): Brush = Brush.verticalGradient(
        colors = listOf(
            ProfileColors.coverOverlayStart,
            ProfileColors.coverOverlayMiddle,
            ProfileColors.coverOverlayEnd
        )
    )



    fun shimmerGradient(offset: Float): Brush = Brush.linearGradient(
        colors = listOf(
            ProfileColors.shimmerBase.copy(alpha = 0.6f),
            ProfileColors.shimmerHighlight.copy(alpha = 0.2f),
            ProfileColors.shimmerBase.copy(alpha = 0.6f)
        ),
        start = androidx.compose.ui.geometry.Offset(offset - 200f, offset - 200f),
        end = androidx.compose.ui.geometry.Offset(offset, offset)
    )
}



val ProfileTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.25).sp
    ),
    displaySmall = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
    ),
    headlineLarge = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold
    ),
    headlineMedium = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold
    ),
    headlineSmall = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleLarge = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal
    ),
    labelLarge = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
    )
)



fun Int.toFormattedCount(): String {
    return when {
        this >= 1_000_000_000 -> String.format("%.1fB", this / 1_000_000_000.0)
        this >= 1_000_000 -> String.format("%.1fM", this / 1_000_000.0)
        this >= 10_000 -> String.format("%.1fK", this / 1_000.0)
        this >= 1_000 -> String.format("%.1fK", this / 1_000.0)
        else -> this.toString()
    }.replace(".0", "")
}
