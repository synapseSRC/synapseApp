package com.synapse.social.studioasinc.feature.shared.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synapse.social.studioasinc.shared.theme.SynapseSizes
import com.synapse.social.studioasinc.shared.theme.SynapseSpacing

/** Thin Dp adapters over the shared Synapse spacing values. */
object Spacing {
    val space0 = SynapseSpacing.space0.dp
    val space4 = SynapseSpacing.space4.dp
    val space8 = SynapseSpacing.space8.dp
    val space12 = SynapseSpacing.space12.dp
    val space16 = SynapseSpacing.space16.dp
    val space20 = SynapseSpacing.space20.dp
    val space24 = SynapseSpacing.space24.dp
    val space32 = SynapseSpacing.space32.dp
    val space48 = SynapseSpacing.space48.dp
    val space64 = SynapseSpacing.space64.dp

    // M3-aligned geometric scale (4dp base).
    val step0 = SynapseSpacing.SpacingScale.step0.dp
    val step1 = SynapseSpacing.SpacingScale.step1.dp
    val step2 = SynapseSpacing.SpacingScale.step2.dp
    val step3 = SynapseSpacing.SpacingScale.step3.dp
    val step4 = SynapseSpacing.SpacingScale.step4.dp
    val step5 = SynapseSpacing.SpacingScale.step5.dp
    val step6 = SynapseSpacing.SpacingScale.step6.dp
    val step7 = SynapseSpacing.SpacingScale.step7.dp
    val step8 = SynapseSpacing.SpacingScale.step8.dp
    val step9 = SynapseSpacing.SpacingScale.step9.dp
}

object Sizes {
    // M3 Expressive & Project Specific Icon Sizes
    val IconExtraSmall = SynapseSizes.IconExtraSmall.dp
    val IconSmall = SynapseSizes.IconSmall.dp
    val IconSemiSmall = SynapseSizes.IconSemiSmall.dp
    val IconSemiMedium = SynapseSizes.IconSemiMedium.dp
    val IconMedium = SynapseSizes.IconMedium.dp
    val IconSmallMedium = 22.dp
    val IconDefault = SynapseSizes.IconDefault.dp
    val IconLarge = SynapseSizes.IconLarge.dp
    val IconExtraLarge = SynapseSizes.IconExtraLarge.dp
    val IconHuge = SynapseSizes.IconHuge.dp
    val IconMassive = SynapseSizes.IconMassive.dp
    val IconGiant = SynapseSizes.IconGiant.dp

    // My design decision (not in M3): Avatar sizes for social feeds and chat
    val AvatarCompact = SynapseSizes.AvatarCompact.dp
    val AvatarTiny = SynapseSizes.AvatarTiny.dp
    val AvatarSmall = SynapseSizes.AvatarSmall.dp
    val AvatarMedium = SynapseSizes.AvatarMedium.dp
    val AvatarDefault = SynapseSizes.AvatarDefault.dp
    val AvatarLarge = SynapseSizes.AvatarLarge.dp
    val AvatarExtraLarge = SynapseSizes.AvatarExtraLarge.dp
    val AvatarHuge = SynapseSizes.AvatarHuge.dp

    // My design decision (not in M3): Border widths
    val StatusDotSmall = SynapseSizes.StatusDotSmall.dp
    val BorderThin = SynapseSizes.BorderThin.dp
    val BorderThinAlt = 1.5.dp
    val BorderHairline = SynapseSizes.BorderHairline.dp
    val BorderDefault = SynapseSizes.BorderDefault.dp
    val BorderSelected = SynapseSizes.BorderSelected.dp

    // M3 Expressive Corner Scale Mappings
    val CornerSmall = SynapseSizes.CornerSmall.dp
    val CornerMedium = SynapseSizes.CornerMedium.dp
    val CornerDefault = SynapseSizes.CornerDefault.dp
    val CornerLarge = SynapseSizes.CornerLarge.dp
    val CornerExtraLarge = SynapseSizes.CornerExtraLarge.dp

    val CornerMassive = SynapseSizes.CornerMassive.dp
    val CornerSharp = SynapseSizes.CornerSharp.dp

    // My design decision (not in M3): Layout & Container component sizing
    val ButtonHeight = SynapseSizes.ButtonHeight.dp
    val ButtonHeightLarge = SynapseSizes.ButtonHeightLarge.dp
    // From M3 Expressive ButtonMedium ContainerHeight.
    val ButtonHeightM3Medium = 56.dp
    val NavBarHeight = SynapseSizes.NavBarHeight.dp
    val HeightSmall = SynapseSizes.HeightSmall.dp
    val HeightButtonSmall = SynapseSizes.HeightButtonSmall.dp
    val HeightMedium = SynapseSizes.HeightMedium.dp
    val HeightDefault = SynapseSizes.HeightDefault.dp
    val HeightLarge = SynapseSizes.HeightLarge.dp
    val Height100 = SynapseSizes.Height100.dp
    val Height120 = SynapseSizes.Height120.dp
    val HeightPreview = SynapseSizes.HeightPreview.dp
    val HeightStoryTray = SynapseSizes.HeightStoryTray.dp
    val HeightStoryTrayExpanded = SynapseSizes.HeightStoryTrayExpanded.dp
    val HeightMediaSingle = SynapseSizes.HeightMediaSingle.dp
    val HeightMediaGrid2 = SynapseSizes.HeightMediaGrid2.dp
    val HeightMediaGrid3Top = SynapseSizes.HeightMediaGrid3Top.dp
    val HeightMediaGridSmall = SynapseSizes.HeightMediaGridSmall.dp
    val HeightMediaGridMedium = SynapseSizes.HeightMediaGridMedium.dp
    val HeightMediaGridLarge = SynapseSizes.HeightMediaGridLarge.dp
    val HeightSheetContent = SynapseSizes.HeightSheetContent.dp
    val HeightExtraLarge = SynapseSizes.HeightExtraLarge.dp

    val HeightTiny = SynapseSizes.HeightTiny.dp
    val HeightMassive = SynapseSizes.HeightMassive.dp
    val WidthLarge = SynapseSizes.WidthLarge.dp
    val WidthExtraLarge = SynapseSizes.WidthExtraLarge.dp
    val WidthMassive = SynapseSizes.WidthMassive.dp

    val ShimmerTextSmall = SynapseSizes.ShimmerTextSmall.dp
    val ShimmerTextMedium = SynapseSizes.ShimmerTextMedium.dp
    val ShimmerWidthTiny = SynapseSizes.ShimmerWidthTiny.dp
    val ShimmerWidthExtraSmall = SynapseSizes.ShimmerWidthExtraSmall.dp
    val ShimmerActionWidth = SynapseSizes.ShimmerActionWidth.dp
    val ShimmerLineHeightSmall = 12.dp
    val ShimmerLineHeight = 16.dp
    val ShimmerLineHeightLarge = 20.dp
    val ShimmerWidthSmall = SynapseSizes.ShimmerWidthSmall.dp
    val ShimmerWidthSmallMedium = SynapseSizes.ShimmerWidthSmallMedium.dp
    val ShimmerWidthMedium = SynapseSizes.ShimmerWidthMedium.dp
    val ShimmerWidthLarge = SynapseSizes.ShimmerWidthLarge.dp
    val ShimmerWidthExtraLarge = SynapseSizes.ShimmerWidthExtraLarge.dp

    val SendButton = SynapseSizes.SendButton.dp
    val SendButtonCompact = SynapseSizes.SendButtonCompact.dp
    val InputButtonCompact = SynapseSizes.InputButtonCompact.dp
    val BubbleMaxWidth = SynapseSizes.BubbleMaxWidth.dp
    val ChatMaxWidth = SynapseSizes.ChatMaxWidth.dp
    val StatusDot = SynapseSizes.StatusDot.dp

    val AvatarSemiLarge = SynapseSizes.AvatarSemiLarge.dp
    val AvatarXLarge = SynapseSizes.AvatarXLarge.dp
    val AvatarProfile = SynapseSizes.AvatarProfile.dp
    val AvatarLargeProfile = SynapseSizes.AvatarLargeProfile.dp
    val HeightCard = SynapseSizes.HeightCard.dp
    val HeightBanner = SynapseSizes.HeightBanner.dp
    val MediaPreviewSmall = SynapseSizes.MediaPreviewSmall.dp
    val HeightChip = SynapseSizes.HeightChip.dp
    val EmptyStateIcon = SynapseSizes.EmptyStateIcon.dp
    val EmptyStateIconSmall = SynapseSizes.EmptyStateIconSmall.dp
    val LogoSize = SynapseSizes.LogoSize.dp
    val QRCodeSize = SynapseSizes.QRCodeSize.dp
    val HeartSize = SynapseSizes.HeartSize.dp

    val MaxGridHeight = SynapseSizes.MaxGridHeight.dp
    val AvatarBorder = SynapseSizes.AvatarBorder.dp
    val ProfileImageOffset = SynapseSizes.ProfileImageOffset.dp

    val HeightAvatarLarge = 72.dp

    val ElevationNone = 0.dp
    // Project-specific component dimensions; these are not Material 3 spacing tokens.
    val ProgressIndicatorStrokeWidth = 4.dp
    val PasswordStrengthBarHeight = 4.dp
    val StoryProgressHeight = 4.dp
    val TypingIndicatorDot = 6.dp
    val TypingIndicatorBounceOffset = 4.dp
    val WavyDividerHeight = 6.dp
    val LoadingOverlayBlurRadius = 10.dp
    val MessageWaveLength = 24.dp
    val MessageWaveAmplitude = 2.dp
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
    val StackHorizontalReserve = SynapseSizes.BatchMediaStackHorizontalReserve.dp
    val StackVerticalReserve = SynapseSizes.BatchMediaStackVerticalReserve.dp
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
    val Small = 12.sp
    val FontSizeDisplay = 36.sp
}