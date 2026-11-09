package com.synapse.social.studioasinc.feature.permission

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.synapse.social.studioasinc.feature.shared.theme.AccessibilitySizes
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.theme.SynapseColors

/**
 * One permission's accent family: the tonal badge it owns, the glyph on that badge, and the light the
 * badge casts onto its card. There are three families because there are three permissions — the accent
 * is how a card is recognised before its title is read, not decoration.
 */
internal data class PermissionAccent(
    val badge: Color,
    val onBadge: Color,
    val glow: Color
)

/**
 * Raw values for the permission screen, taken from the approved reference and matched to the app's
 * existing palette: cool-white page, deep-teal action, navy headings, and one soft accent per
 * permission (cyan, lavender, champagne).
 *
 * These stay screen-scoped on purpose. The app's primary role is Synapse blue everywhere else, so
 * recolouring the global scheme for one onboarding step would change every other screen; here the
 * scoped tokens are read through [PermissionColors], which still defers to the live scheme for
 * surfaces, outlines and error roles.
 */
internal object PermissionPalette {
    // Primary action: the deep teal the reference uses for every affirmative button.
    val ActionLight = Color(0xFF016277)
    val OnActionLight = Color(0xFFFFFFFF)
    // Dark keeps M3's contrast direction: a lighter fill with a dark label.
    val ActionDark = Color(0xFF5FD0E0)
    val OnActionDark = Color(0xFF00313C)

    val HeadingLight = Color(0xFF0B1B33)
    val HeadingDark = Color(0xFFE7ECF8)
    val BodyLight = Color(0xFF3E4863)
    val BodyDark = Color(0xFFC3CADB)

    val PageWashLight = Color(0xFFE7F1FF)
    val PageWashDark = Color(0xFF0E1A2B)
    val BottomGlowLight = Color(0xFF7FD3E6)
    val BottomGlowDark = Color(0xFF1C6E80)

    val AuroraCyanLight = Color(0xFF6FD2E6)
    val AuroraCyanDark = Color(0xFF1E6E85)
    val AuroraTealLight = Color(0xFF05B4C8)
    val AuroraTealDark = Color(0xFF0B5F72)
    val AuroraHighlightLight = Color(0xFFFFFFFF)
    val AuroraHighlightDark = Color(0xFF7FD3E6)

    val GrantedContainerLight = Color(0xFFB9E6EE)
    val GrantedContentLight = Color(0xFF00323E)
    val GrantedContainerDark = Color(0xFF10454F)
    val GrantedContentDark = Color(0xFFA9E4F0)

    val NotificationsBadgeLight = Color(0xFFCBEEF6)
    val NotificationsOnBadgeLight = Color(0xFF07576B)
    val NotificationsBadgeDark = Color(0xFF0E3F4B)
    val NotificationsOnBadgeDark = Color(0xFFA9E4F0)
    val NotificationsGlowLight = Color(0xFF25BFD8)
    val NotificationsGlowDark = Color(0xFF2AA8BE)

    val MicrophoneBadgeLight = Color(0xFFE4DEFB)
    val MicrophoneOnBadgeLight = Color(0xFF3B2A73)
    val MicrophoneBadgeDark = Color(0xFF2B2452)
    val MicrophoneOnBadgeDark = Color(0xFFCFC2F7)
    val MicrophoneGlowLight = Color(0xFF9A8AEC)
    val MicrophoneGlowDark = Color(0xFF7B6BD6)

    val CameraBadgeLight = Color(0xFFFCE1B6)
    val CameraOnBadgeLight = Color(0xFF6B4410)
    val CameraBadgeDark = Color(0xFF453014)
    val CameraOnBadgeDark = Color(0xFFF5CE95)
    val CameraGlowLight = Color(0xFFF2A63B)
    val CameraGlowDark = Color(0xFFD98B2A)

    /** A retryable denial is advisory, so it borrows the shared palette's warning pair. */
    val AttentionContainerLight = Color(SynapseColors.Light.warningContainer)
    val AttentionContentLight = Color(SynapseColors.Light.onWarningContainer)
    val AttentionContainerDark = Color(SynapseColors.Dark.warningContainer)
    val AttentionContentDark = Color(SynapseColors.Dark.onWarningContainer)

    /** Soft cast shadow: navy in light, neutral black in dark where a navy tint would read as haze. */
    val CardShadowLight = Color(0x1A0B1B33)
    val CardShadowDark = Color(0x59000000)
}

/**
 * Semantic roles for the permission screen. Surfaces, outlines and error colours come from the live
 * scheme; the accents come from [PermissionPalette]. Light/dark is decided by the scheme itself rather
 * than by the system setting, so it stays correct under a manual theme override and with dynamic colour.
 */
internal object PermissionColors {
    /** The scheme inverts its tonal scale in dark mode; its lowest container is the tell. */
    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceContainerLowest.luminance() < 0.5f

    val action: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) PermissionPalette.ActionDark else PermissionPalette.ActionLight

    val onAction: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) PermissionPalette.OnActionDark else PermissionPalette.OnActionLight

    val heading: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) PermissionPalette.HeadingDark else PermissionPalette.HeadingLight

    val body: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) PermissionPalette.BodyDark else PermissionPalette.BodyLight

    val pageWash: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) PermissionPalette.PageWashDark else PermissionPalette.PageWashLight

    val bottomGlow: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) PermissionPalette.BottomGlowDark else PermissionPalette.BottomGlowLight

    val auroraCyan: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) PermissionPalette.AuroraCyanDark else PermissionPalette.AuroraCyanLight

    val auroraTeal: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) PermissionPalette.AuroraTealDark else PermissionPalette.AuroraTealLight

    val auroraHighlight: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) PermissionPalette.AuroraHighlightDark else PermissionPalette.AuroraHighlightLight

    /** Dark mode keeps the same ribbons at lower strength; two compositions would be two designs. */
    val auroraOpacity: Float
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) 0.55f else 1f

    /** Accent glows are cast light: they read on a dark page only at reduced strength. */
    val glowStrength: Float
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) 0.55f else 1f

    val cardShadow: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) PermissionPalette.CardShadowDark else PermissionPalette.CardShadowLight

    val cardBorder: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.outlineVariant

    val grantedContainer: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) PermissionPalette.GrantedContainerDark else PermissionPalette.GrantedContainerLight

    val grantedContent: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) PermissionPalette.GrantedContentDark else PermissionPalette.GrantedContentLight

    val neutralContainer: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceContainerHigh

    val neutralContent: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onSurfaceVariant

    /** A denial the user can still retry is advisory, not an error; blocked states take the error role. */
    val attentionContainer: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) PermissionPalette.AttentionContainerDark else PermissionPalette.AttentionContainerLight

    val attentionContent: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) PermissionPalette.AttentionContentDark else PermissionPalette.AttentionContentLight

    val blockedContainer: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.errorContainer

    val blockedContent: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.onErrorContainer

    /** Skip is a quiet escape hatch: the cyan accent, not the teal action. */
    val skipContainer: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) PermissionPalette.NotificationsBadgeDark else PermissionPalette.NotificationsBadgeLight

    val skipContent: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isDark) PermissionPalette.NotificationsOnBadgeDark else PermissionPalette.NotificationsOnBadgeLight
}

/**
 * Component geometry for the permission screen, all of it from the existing scales.
 *
 * M3 references: corner scale (extra large 28dp, large 16dp, medium 12dp), chip container height 32dp
 * and ButtonMedium 56dp with the square-variant corner large. The ripple-free backdrop fractions are
 * this screen's own design decision: they are fractions of the canvas, so no device size is assumed.
 */
internal object PermissionMetrics {
    val cardShape: Shape = RoundedCornerShape(Sizes.CornerMassive)
    val cardPadding: Dp = Spacing.space16
    val cardElevation: Dp = Sizes.ElevationLow

    /** Nested corner = card corner (28dp) − card padding (16dp); never the parent's radius. */
    val badgeShape: Shape = RoundedCornerShape(Sizes.CornerDefault)
    val badgeSize: Dp = Sizes.AvatarDefault
    val badgeIconSize: Dp = Sizes.IconLarge

    val chipShape: Shape = CircleShape
    val chipHeight: Dp = Sizes.HeightChip
    val chipIconSize: Dp = Sizes.IconSemiMedium

    val buttonShape: Shape = RoundedCornerShape(Sizes.CornerLarge)
    val buttonHeight: Dp = Sizes.ButtonHeightM3Medium

    val skipShape: Shape = CircleShape
    val skipMinHeight: Dp = AccessibilitySizes.minimumTouchTarget

    val washFadeFraction: Float = 0.34f
    val auroraHeightFraction: Float = 0.22f
    val bottomGlowRadiusFraction: Float = 0.9f

    val revealOffset: Dp = Spacing.space16
    val revealStagger: Float = 0.12f
}

/**
 * Emphasis for this screen. Product Sans ships a regular and a bold face only, so emphasis is the
 * weight axis of the existing styles rather than a separate font.
 */
internal object PermissionTypography {
    val heroHeadline: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)

    val topBarTitle: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.titleLarge

    /** Card titles take the existing bold title style; the description stays regular body text. */
    val cardTitle: TextStyle
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography.titleMedium
}
