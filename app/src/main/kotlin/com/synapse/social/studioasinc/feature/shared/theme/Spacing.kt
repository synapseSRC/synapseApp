package com.synapse.social.studioasinc.feature.shared.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp



object Spacing {

    val None = 0.dp

    // My design decision (not in M3): 2dp micro spacing
    val Tiny = 2.dp

    val ExtraSmall = 4.dp

    // My design decision (not in M3): 6dp intermediate spacing
    val ExtraSmallMedium = 6.dp

    val Small = 8.dp

    // My design decision (not in M3): 10dp intermediate spacing
    val SmallPlus = 10.dp

    val SmallMedium = 12.dp

    val Medium = 16.dp

    val MediumLarge = 20.dp

    val Large = 24.dp

    val ExtraLarge = 32.dp

    val Huge = 48.dp

    // My design decision (not in M3): Component specific heights
    val ButtonHeight = 40.dp
    val NavBarHeight = 60.dp
}

object Sizes {
    // M3 Expressive & Project Specific Icon Sizes
    val IconSmall = 12.dp
    val IconSemiSmall = 14.dp
    val IconSemiMedium = 16.dp
    val IconMedium = 18.dp
    val IconDefault = 20.dp
    val IconLarge = 24.dp
    val IconExtraLarge = 28.dp
    val IconHuge = 32.dp
    val IconMassive = 40.dp
    val IconGiant = 48.dp

    // My design decision (not in M3): Avatar sizes for social feeds and chat
    val AvatarTiny = 28.dp
    val AvatarSmall = 36.dp
    val AvatarMedium = 40.dp
    val AvatarDefault = 48.dp
    val AvatarLarge = 56.dp
    val AvatarExtraLarge = 80.dp
    val AvatarHuge = 110.dp

    // My design decision (not in M3): Border widths
    val BorderThin = 1.dp
    val BorderHairline = 0.5.dp
    val BorderDefault = 2.dp
    val BorderSelected = 3.dp

    // M3 Expressive Corner Scale Mappings
    val CornerSmall = 4.dp
    val CornerMedium = 8.dp
    val CornerDefault = 12.dp
    val CornerLarge = 16.dp
    val CornerExtraLarge = 24.dp

    val CornerMassive = 28.dp
    val CornerSharp = 2.dp

    // My design decision (not in M3): Layout & Container component sizing
    val HeightSmall = 24.dp
    val HeightButtonSmall = 28.dp
    val HeightMedium = 40.dp
    val HeightDefault = 56.dp
    val HeightLarge = 80.dp
    val Height100 = 100.dp
    val Height120 = 120.dp
    val HeightPreview = 300.dp
    val HeightStoryTray = 180.dp
    val HeightStoryTrayExpanded = 220.dp
    val HeightMediaSingle = 400.dp
    val HeightMediaGrid2 = 400.dp
    val HeightMediaGrid3Top = 350.dp
    val HeightMediaGridSmall = 150.dp
    val HeightMediaGridMedium = 200.dp
    val HeightMediaGridLarge = 250.dp
    val HeightSheetContent = 500.dp
    val HeightExtraLarge = 200.dp

    val HeightTiny = 14.dp
    val HeightMassive = 250.dp
    val WidthLarge = 80.dp
    val WidthExtraLarge = 100.dp
    val WidthMassive = 110.dp

    val ShimmerTextSmall = 14.dp
    val ShimmerTextMedium = 28.dp
    val ShimmerWidthSmall = 60.dp
    val ShimmerWidthSmallMedium = 80.dp
    val ShimmerWidthMedium = 100.dp
    val ShimmerWidthLarge = 120.dp
    val ShimmerWidthExtraLarge = 150.dp

    val AvatarHugeOffset = 60.dp
    val AvatarHugeHalfOffset = 55.dp
    val CommentIndent = 68.dp
    val CommentMaxIndent = 64.dp

    val SendButton = 44.dp
    val SendButtonCompact = 36.dp
    val InputButtonCompact = 40.dp
    val BubbleMaxWidth = 280.dp
    val ChatMaxWidth = 240.dp
    val StatusDot = 10.dp

    val AvatarSemiLarge = 64.dp
    val AvatarXLarge = 88.dp
    val AvatarProfile = 96.dp
    val AvatarLargeProfile = 120.dp
    val HeightCard = 140.dp
    val HeightBanner = 100.dp
    val MediaPreviewSmall = 160.dp
    val HeightChip = 32.dp
    val EmptyStateIcon = 120.dp
    val EmptyStateIconSmall = 100.dp
    val LogoSize = 85.dp
    val QRCodeSize = 250.dp
    val HeartSize = 100.dp

    val MaxGridHeight = 2000.dp
    val AvatarBorder = 4.dp
    val ProfileImageOffset = (-48).dp

    val HeightAvatarLarge = 72.dp
    val IconSmallMedium = 22.dp
    val IconSemiMediumLarge = 18.dp
    val BorderThinAlt = 1.5.dp

    val ElevationLow = 2.dp
    val PaddingTopClose = 28.dp
    val CornerFull = 32.dp
    val TouchTarget = 44.dp
}

/**
 * Batch-media stack geometry tuned to the chat bubble constraints and supplied anatomy reference.
 * My design decision (not in M3): the fan rotations and scales create dimensional card layering.
 */
// MaxFrontWidth + StackHorizontalReserve is a theoretical cap; runtime width is always
// clamped to the parent constraints, which are narrower than that cap in chat bubbles.
object BatchMediaCardMetrics {
    val MaxFrontWidth = 272.dp
    val StackHorizontalReserve = Spacing.Huge
    val StackVerticalReserve = Sizes.CornerMassive
    const val AspectRatio = 4f / 3f

    // M3 Elevation Levels 1/2/3 (1/3/6dp); project-specific mapping creates depth within the stack.
    val RearShadowElevation = 1.dp
    val MiddleShadowElevation = 3.dp
    val FrontShadowElevation = 6.dp

    const val FrontRotationDegrees = -4f
    const val MiddleRotationDegrees = 6f
    const val RearRotationDegrees = 8f
    const val MiddleScale = 0.91f
    const val RearScale = 0.84f

    val MiddleOffsetX = Spacing.Medium
    val MiddleOffsetY = -Spacing.ExtraSmall
    val RearOffsetX = Spacing.ExtraLarge
    val RearOffsetY = -Spacing.ExtraSmall
}

object FontSizes {
    val ExtraSmall = 10.sp
    val Small = 12.sp
    val FontSizeDisplay = 36.sp
}
