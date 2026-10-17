package com.synapse.social.studioasinc.feature.shared.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synapse.social.studioasinc.shared.theme.SynapseSizes
import com.synapse.social.studioasinc.shared.theme.SynapseSpacing



/** Thin Dp adapters over the shared Synapse spacing values. */
object Spacing {
    val space0 = SynapseSpacing.space0.dp
    val space2 = SynapseSpacing.space2.dp
    val space4 = SynapseSpacing.space4.dp
    val space6 = SynapseSpacing.space6.dp
    val space8 = SynapseSpacing.space8.dp
    val space10 = SynapseSpacing.space10.dp
    val space12 = SynapseSpacing.space12.dp
    val space16 = SynapseSpacing.space16.dp
    val space20 = SynapseSpacing.space20.dp
    val space24 = SynapseSpacing.space24.dp
    val space28 = SynapseSpacing.space28.dp
    val space32 = SynapseSpacing.space32.dp
    val space40 = SynapseSpacing.space40.dp
    val space48 = SynapseSpacing.space48.dp
    val space64 = SynapseSpacing.space64.dp
}

object Sizes {
    // M3 Expressive & Project Specific Icon Sizes
    val IconExtraSmall = SynapseSizes.IconExtraSmall.dp
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
    val AvatarCompact = SynapseSizes.AvatarCompact.dp
    val AvatarTiny = 28.dp
    val AvatarSmall = 36.dp
    val AvatarMedium = 40.dp
    val AvatarDefault = 48.dp
    val AvatarLarge = 56.dp
    val AvatarExtraLarge = 80.dp
    val AvatarHuge = 110.dp

    // My design decision (not in M3): Border widths
    val StatusDotSmall = SynapseSizes.StatusDotSmall.dp
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
    val ButtonHeight = SynapseSizes.ButtonHeight.dp
    val ButtonHeightLarge = SynapseSizes.ButtonHeightLarge.dp
    val NavBarHeight = SynapseSizes.NavBarHeight.dp
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
    val ShimmerWidthTiny = SynapseSizes.ShimmerWidthTiny.dp
    val ShimmerWidthExtraSmall = SynapseSizes.ShimmerWidthExtraSmall.dp
    val ShimmerActionWidth = SynapseSizes.ShimmerActionWidth.dp
    val ShimmerLineHeightSmall = 12.dp
    val ShimmerLineHeight = 16.dp
    val ShimmerLineHeightLarge = 20.dp
    val ShimmerWidthSmall = 60.dp
    val ShimmerWidthSmallMedium = 80.dp
    val ShimmerWidthMedium = 100.dp
    val ShimmerWidthLarge = 120.dp
    val ShimmerWidthExtraLarge = 150.dp


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

    val ElevationNone = 0.dp
    // Project-specific component dimensions; these are not Material 3 spacing tokens.
    val ProgressIndicatorStrokeWidth = 4.dp
    val PasswordStrengthBarHeight = 4.dp
    val StoryProgressHeight = 4.dp
    val TypingIndicatorDot = 6.dp
    val TypingIndicatorBounceOffset = 4.dp
    val WavyDividerHeight = 6.dp
    val LoadingOverlayBlurRadius = 10.dp
    val ChatInputFadeSize = 16.dp
    val MessageWaveLength = 24.dp
    val MessageWaveAmplitude = 2.dp
    val DividerThickness = 8.dp
    val ProfileGridOverlayHeight = 32.dp
    val AudioFallbackRowHeight = 32.dp
    val ElevationLow = 2.dp
    val ElevationMedium = 4.dp
    val ElevationHigh = 8.dp
    val CornerFull = 32.dp
}

/** Touch-target dimensions are accessibility/component sizes, not spacing. */
object AccessibilitySizes {
    val touchTarget = 44.dp
    val minimumTouchTarget = 48.dp
}

/**
 * Batch-media stack geometry tuned to the chat bubble constraints and supplied anatomy reference.
 * My design decision (not in M3): the fan rotations and scales create dimensional card layering.
 */
// MaxFrontWidth + StackHorizontalReserve is a theoretical cap; runtime width is always
// clamped to the parent constraints, which are narrower than that cap in chat bubbles.
object BatchMediaCardMetrics {
    val MaxFrontWidth = 272.dp
    val StackHorizontalReserve = Spacing.space48
    val StackVerticalReserve = Spacing.space28
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

    val MiddleOffsetX = Spacing.space16
    val MiddleOffsetY = -Spacing.space4
    val RearOffsetX = Spacing.space32
    val RearOffsetY = -Spacing.space4
}

object FontSizes {
    val ExtraSmall = 10.sp
    val Small = 12.sp
    val FontSizeDisplay = 36.sp
}
