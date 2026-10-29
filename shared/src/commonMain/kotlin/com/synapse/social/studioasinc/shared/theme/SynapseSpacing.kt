package com.synapse.social.studioasinc.shared.theme

/**
 * Synapse spacing scale — aligned to Material 3 geometric progression (4dp base).
 *
 * Primary scale uses M3 step-based naming (space1=4dp → space9=64dp).
 * Legacy dp-value tokens are retained with @Deprecated annotations for migration.
 */
object SynapseSpacing {
    // ── M3 Geometric Scale (4dp base, step-based naming) ─────────────────────
    // M3: 0, 4, 8, 12, 16, 20, 24, 32, 48, 64 dp
    const val space0 = 0f      // 0dp  — no spacing
    const val space4 = 4f      // 4dp  — micro gap
    const val space8 = 8f      // 8dp  — icon spacing, small padding
    const val space12 = 12f    // 12dp — default element spacing
    const val space16 = 16f    // 16dp — card margins, medium spacing
    const val space20 = 20f    // 20dp — section gap
    const val space24 = 24f    // 24dp — list spacing, major element gap
    const val space32 = 32f    // 32dp — page margins
    const val space48 = 48f    // 48dp — touch target minimum, elevation gap
    const val space64 = 64f    // 64dp — max page margin, large separator

    // ── M3 Geometric Scale Sub-object (step-based naming) ────────────────────
    object SpacingScale {
        const val step0 = 0f      // 0dp  — no spacing
        const val step1 = 4f      // 4dp  — micro gap
        const val step2 = 8f      // 8dp  — icon spacing, small padding
        const val step3 = 12f     // 12dp — default element spacing
        const val step4 = 16f     // 16dp — card margins, medium spacing
        const val step5 = 20f     // 20dp — section gap
        const val step6 = 24f     // 24dp — list spacing, major element gap
        const val step7 = 32f     // 32dp — page margins
        const val step8 = 48f     // 48dp — touch target minimum, elevation gap
        const val step9 = 64f     // 64dp — max page margin, large separator
    }
}

/**
 * Component dimensions — separated from the spacing scale to prevent confusion.
 * Icons, avatars, borders, heights, widths, and other component-specific sizes.
 */
object SynapseSizes {
    // Icon Sizes
    const val IconExtraSmall = 8f
    const val IconSmall = 12f
    const val IconSemiSmall = 14f
    const val IconSemiMedium = 16f
    const val IconMedium = 18f
    const val IconDefault = 20f
    const val IconLarge = 24f
    const val IconExtraLarge = 28f
    const val IconHuge = 32f
    const val IconMassive = 40f
    const val IconGiant = 48f

    // Avatar Sizes (not in M3)
    const val AvatarCompact = 32f
    const val AvatarTiny = 28f
    const val AvatarSmall = 36f
    const val AvatarMedium = 40f
    const val AvatarDefault = 48f
    const val AvatarLarge = 56f
    const val AvatarExtraLarge = 80f
    /** Diameter of the extra-large avatar variant, in dp. */
    const val AvatarXLarge = 88f
    const val AvatarHuge = 110f

    // Border Widths (not in M3)
    const val StatusDotSmall = 8f
    const val BorderThin = 1f
    const val BorderHairline = 0.5f
    const val BorderDefault = 2f
    const val BorderSelected = 3f

    // Corner Scale (M3)
    const val CornerSmall = 4f
    const val CornerMedium = 8f
    const val CornerDefault = 12f
    const val CornerLarge = 16f
    const val CornerExtraLarge = 24f
    const val CornerMassive = 28f
    const val CornerSharp = 2f

    // Component Dimensions
    const val ButtonHeight = 40f
    const val ButtonHeightLarge = 48f
    const val NavBarHeight = 60f
    const val HeightSmall = 24f
    const val HeightButtonSmall = 28f
    const val HeightMedium = 40f
    const val HeightDefault = 56f
    const val HeightLarge = 80f
    const val Height100 = 100f
    const val Height120 = 120f
    const val HeightPreview = 300f
    const val HeightStoryTray = 180f
    /** Height of the expanded story tray, in dp. */
    const val HeightStoryTrayExpanded = 220f
    const val HeightMediaSingle = 400f
    const val HeightMediaGrid2 = 400f
    const val HeightMediaGrid3Top = 350f
    const val HeightMediaGridSmall = 150f
    const val HeightMediaGridMedium = 200f
    const val HeightMediaGridLarge = 250f
    const val HeightSheetContent = 500f
    const val HeightExtraLarge = 200f

    const val HeightTiny = 14f
    const val HeightMassive = 250f
    const val WidthLarge = 80f
    const val WidthExtraLarge = 100f
    const val WidthMassive = 110f

    const val ShimmerTextSmall = 14f
    const val ShimmerTextMedium = 28f
    const val ShimmerWidthTiny = 32f
    const val ShimmerWidthExtraSmall = 40f
    const val ShimmerActionWidth = 48f
    const val ShimmerWidthSmall = 60f
    const val ShimmerWidthSmallMedium = 80f
    const val ShimmerWidthMedium = 100f
    const val ShimmerWidthLarge = 120f
    const val ShimmerWidthExtraLarge = 150f

    const val SendButton = 44f
    const val SendButtonCompact = 36f
    const val InputButtonCompact = 40f
    const val BubbleMaxWidth = 280f
    const val ChatMaxWidth = 240f
    const val StatusDot = 10f

    const val AvatarSemiLarge = 64f
    const val AvatarProfile = 96f
    const val AvatarLargeProfile = 120f
    const val HeightCard = 140f
    const val HeightBanner = 100f
    const val MediaPreviewSmall = 160f
    const val HeightChip = 32f
    const val EmptyStateIcon = 120f
    const val EmptyStateIconSmall = 100f
    const val LogoSize = 85f
    const val QRCodeSize = 250f
    const val HeartSize = 100f

    const val MaxGridHeight = 2000f
    const val AvatarBorder = 4f
    const val ProfileImageOffset = -48f

    // Tuned batch-media stack geometry, independent of the spacing scale.
    /** Horizontal reserve for the chat batch-media stack, in dp. */
    const val BatchMediaStackHorizontalReserve = 48f
    /** Vertical reserve for the chat batch-media stack, in dp. */
    const val BatchMediaStackVerticalReserve = 28f
}