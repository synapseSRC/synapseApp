package com.synapse.social.studioasinc.feature.shared.theme

import androidx.compose.ui.graphics.Color
import com.synapse.social.studioasinc.shared.theme.SynapseColors

// M3 role and shared preset values are sourced from the common palette.
private fun Long.toComposeColor(): Color = Color(this)

val LightPrimary = SynapseColors.Light.primary.toComposeColor()
val LightOnPrimary = SynapseColors.Light.onPrimary.toComposeColor()
val LightPrimaryContainer = SynapseColors.Light.primaryContainer.toComposeColor()
val LightOnPrimaryContainer = SynapseColors.Light.onPrimaryContainer.toComposeColor()
val LightSecondary = SynapseColors.Light.secondary.toComposeColor()
val LightOnSecondary = SynapseColors.Light.onSecondary.toComposeColor()
val LightSecondaryContainer = SynapseColors.Light.secondaryContainer.toComposeColor()
val LightOnSecondaryContainer = SynapseColors.Light.onSecondaryContainer.toComposeColor()
val LightTertiary = SynapseColors.Light.tertiary.toComposeColor()
val LightOnTertiary = SynapseColors.Light.onTertiary.toComposeColor()
val LightTertiaryContainer = SynapseColors.Light.tertiaryContainer.toComposeColor()
val LightOnTertiaryContainer = SynapseColors.Light.onTertiaryContainer.toComposeColor()
val LightError = SynapseColors.Light.error.toComposeColor()
val LightOnError = SynapseColors.Light.onError.toComposeColor()
val LightErrorContainer = SynapseColors.Light.errorContainer.toComposeColor()
val LightOnErrorContainer = SynapseColors.Light.onErrorContainer.toComposeColor()
val LightOutline = SynapseColors.Light.outline.toComposeColor()
val LightBackground = SynapseColors.Light.background.toComposeColor()
val LightOnBackground = SynapseColors.Light.onBackground.toComposeColor()
val LightSurface = SynapseColors.Light.surface.toComposeColor()
val LightOnSurface = SynapseColors.Light.onSurface.toComposeColor()
val LightSurfaceVariant = SynapseColors.Light.surfaceVariant.toComposeColor()
val LightOnSurfaceVariant = SynapseColors.Light.onSurfaceVariant.toComposeColor()
val LightInverseSurface = SynapseColors.Light.inverseSurface.toComposeColor()
val LightInverseOnSurface = SynapseColors.Light.inverseOnSurface.toComposeColor()
val LightInversePrimary = SynapseColors.Light.inversePrimary.toComposeColor()
val LightShadow = Color(0xFF000000)
val LightSurfaceTint = SynapseColors.Light.surfaceTint.toComposeColor()
val LightOutlineVariant = SynapseColors.Light.outlineVariant.toComposeColor()
val LightScrim = SynapseColors.Light.scrim.toComposeColor()
val LightSurfaceContainerLowest = SynapseColors.Light.surfaceContainerLowest.toComposeColor()
val LightSurfaceContainerLow = SynapseColors.Light.surfaceContainerLow.toComposeColor()
val LightSurfaceContainer = SynapseColors.Light.surfaceContainer.toComposeColor()
val LightSurfaceContainerHigh = SynapseColors.Light.surfaceContainerHigh.toComposeColor()
val LightSurfaceContainerHighest = SynapseColors.Light.surfaceContainerHighest.toComposeColor()

val SynapseBlue = SynapseColors.Blue.toComposeColor()
val SynapseDarkBlue = SynapseColors.DarkBlue.toComposeColor()
val SynapseLightBlue = SynapseColors.LightBlue.toComposeColor()

val DarkPrimary = SynapseColors.Dark.primary.toComposeColor()
val DarkOnPrimary = SynapseColors.Dark.onPrimary.toComposeColor()
val DarkPrimaryContainer = SynapseColors.Dark.primaryContainer.toComposeColor()
val DarkOnPrimaryContainer = SynapseColors.Dark.onPrimaryContainer.toComposeColor()
val DarkSecondary = SynapseColors.Dark.secondary.toComposeColor()
val DarkOnSecondary = SynapseColors.Dark.onSecondary.toComposeColor()
val DarkSecondaryContainer = SynapseColors.Dark.secondaryContainer.toComposeColor()
val DarkOnSecondaryContainer = SynapseColors.Dark.onSecondaryContainer.toComposeColor()
val DarkTertiary = SynapseColors.Dark.tertiary.toComposeColor()
val DarkOnTertiary = SynapseColors.Dark.onTertiary.toComposeColor()
val DarkTertiaryContainer = SynapseColors.Dark.tertiaryContainer.toComposeColor()
val DarkOnTertiaryContainer = SynapseColors.Dark.onTertiaryContainer.toComposeColor()
val DarkError = SynapseColors.Dark.error.toComposeColor()
val DarkOnError = SynapseColors.Dark.onError.toComposeColor()
val DarkErrorContainer = SynapseColors.Dark.errorContainer.toComposeColor()
val DarkOnErrorContainer = SynapseColors.Dark.onErrorContainer.toComposeColor()
val DarkOutline = SynapseColors.Dark.outline.toComposeColor()
val DarkBackground = SynapseColors.Dark.background.toComposeColor()
val DarkOnBackground = SynapseColors.Dark.onBackground.toComposeColor()
val DarkSurface = SynapseColors.Dark.surface.toComposeColor()
val DarkOnSurface = SynapseColors.Dark.onSurface.toComposeColor()
val DarkSurfaceVariant = SynapseColors.Dark.surfaceVariant.toComposeColor()
val DarkOnSurfaceVariant = SynapseColors.Dark.onSurfaceVariant.toComposeColor()
val DarkInverseSurface = SynapseColors.Dark.inverseSurface.toComposeColor()
val DarkInverseOnSurface = SynapseColors.Dark.inverseOnSurface.toComposeColor()
val DarkInversePrimary = SynapseColors.Dark.inversePrimary.toComposeColor()
val DarkShadow = Color(0xFF000000)
val DarkSurfaceTint = SynapseColors.Dark.surfaceTint.toComposeColor()
val DarkOutlineVariant = SynapseColors.Dark.outlineVariant.toComposeColor()
val DarkScrim = SynapseColors.Dark.scrim.toComposeColor()
val DarkSurfaceContainerLowest = SynapseColors.Dark.surfaceContainerLowest.toComposeColor()
val DarkSurfaceContainerLow = SynapseColors.Dark.surfaceContainerLow.toComposeColor()
val DarkSurfaceContainer = SynapseColors.Dark.surfaceContainer.toComposeColor()
val DarkSurfaceContainerHigh = SynapseColors.Dark.surfaceContainerHigh.toComposeColor()
val DarkSurfaceContainerHighest = SynapseColors.Dark.surfaceContainerHighest.toComposeColor()

// App-specific semantic roles are also sourced from the shared scheme.
val StatusOnline = SynapseColors.StatusOnline.toComposeColor()
val StatusOffline = SynapseColors.StatusOffline.toComposeColor()
val StatusRead = SynapseColors.StatusRead.toComposeColor()
val LightSuccess = SynapseColors.Light.success.toComposeColor()
val LightOnSuccess = SynapseColors.Light.onSuccess.toComposeColor()
val LightSuccessContainer = SynapseColors.Light.successContainer.toComposeColor()
val LightOnSuccessContainer = SynapseColors.Light.onSuccessContainer.toComposeColor()
val DarkSuccess = SynapseColors.Dark.success.toComposeColor()
val DarkOnSuccess = SynapseColors.Dark.onSuccess.toComposeColor()
val DarkSuccessContainer = SynapseColors.Dark.successContainer.toComposeColor()
val DarkOnSuccessContainer = SynapseColors.Dark.onSuccessContainer.toComposeColor()
val LightWarning = SynapseColors.Light.warning.toComposeColor()
val LightOnWarning = SynapseColors.Light.onWarning.toComposeColor()
val LightWarningContainer = SynapseColors.Light.warningContainer.toComposeColor()
val LightOnWarningContainer = SynapseColors.Light.onWarningContainer.toComposeColor()
val DarkWarning = SynapseColors.Dark.warning.toComposeColor()
val DarkOnWarning = SynapseColors.Dark.onWarning.toComposeColor()
val DarkWarningContainer = SynapseColors.Dark.warningContainer.toComposeColor()
val DarkOnWarningContainer = SynapseColors.Dark.onWarningContainer.toComposeColor()

val OceanDarkContainer = SynapseColors.OceanDarkContainer.toComposeColor()
val OceanLightContainer = SynapseColors.OceanLightContainer.toComposeColor()
val OceanDarkOnContainer = SynapseColors.OceanDarkOnContainer.toComposeColor()
val OceanLightOnContainer = SynapseColors.OceanLightOnContainer.toComposeColor()
val ForestDarkContainer = SynapseColors.ForestDarkContainer.toComposeColor()
val ForestLightContainer = SynapseColors.ForestLightContainer.toComposeColor()
val ForestDarkOnContainer = SynapseColors.ForestDarkOnContainer.toComposeColor()
val ForestLightOnContainer = SynapseColors.ForestLightOnContainer.toComposeColor()
val SunsetDarkContainer = SynapseColors.SunsetDarkContainer.toComposeColor()
val SunsetLightContainer = SynapseColors.SunsetLightContainer.toComposeColor()
val SunsetDarkOnContainer = SynapseColors.SunsetDarkOnContainer.toComposeColor()
val SunsetLightOnContainer = SynapseColors.SunsetLightOnContainer.toComposeColor()
val MonochromeDarkContainer = SynapseColors.MonochromeDarkContainer.toComposeColor()
val MonochromeLightContainer = SynapseColors.MonochromeLightContainer.toComposeColor()
val MonochromeDarkOnContainer = SynapseColors.MonochromeDarkOnContainer.toComposeColor()
val MonochromeLightOnContainer = SynapseColors.MonochromeLightOnContainer.toComposeColor()

val ForestBubbleBackground = ForestLightContainer
val ForestBubbleText = ForestLightOnContainer
val SunsetBubbleBackground = SunsetLightContainer
val SunsetBubbleText = SunsetLightOnContainer
val SunsetAccent = Color(0xFFFF5722)

val InteractionIconDefault = SynapseColors.InteractionIconDefault.toComposeColor()
val InteractionLikeActive = SynapseColors.InteractionLikeActive.toComposeColor()
val InteractionRepostActive = SynapseColors.InteractionRepostActive.toComposeColor()

// Accent & Content Accents (Harmonized with M3 Brand Roles)
val AccentOrange = Color(0xFFD97706)
val AccentBlue = LightPrimary
val AccentYellow = Color(0xFFCA8A04)

// Neutral Grays
val Gray200 = Color(0xFFE0E0E0)
val Gray300 = Color(0xFFBDBDBD)
val Gray500 = Color(0xFF9E9E9E)
val Gray700 = Color(0xFF616161)
val Gray900 = Color(0xFF212121)

// Content / Editor / Story Accents (Explicitly Scoped Content Colors)
val OnlineGreenLight = StatusOnline
val PinnedBackground = Color(0xFFFFF8E1)
val PinnedBackgroundDark = Color(0xFF3E2723)
val StoryGradientStart = Color(0xFFE040FB)
val StoryGradientMiddle = Color(0xFFFF5722)
val StoryGradientEnd = Color(0xFFFFEB3B)
val StoryRingMiddle = Color(0xFF7C4DFF)
val StoryRingEnd = Color(0xFF00BCD4)

val StoryColorOrange = Color(0xFFFF9800)
val StoryColorGreen = Color(0xFF4CAF50)
val StoryColorPurple = Color(0xFF9C27B0)

// Shimmer
val ShimmerBase = Color(0xFFE0E0E0)
val ShimmerHighlight = Color(0xFFF5F5F5)

/**
 * Safely parses a hex color string (e.g. "#0061A4") and returns a fallback Color if parsing fails or input is null.
 */
fun safeParseColorHex(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        val clean = hex.trim()
        if (clean.startsWith("#")) {
            Color(android.graphics.Color.parseColor(clean))
        } else {
            fallback
        }
    } catch (e: Exception) {
        fallback
    }
}
