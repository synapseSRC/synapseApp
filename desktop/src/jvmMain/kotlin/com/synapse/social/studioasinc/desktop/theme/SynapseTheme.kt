package com.synapse.social.studioasinc.desktop.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.synapse.social.studioasinc.shared.theme.SynapseColorScheme
import com.synapse.social.studioasinc.shared.theme.SynapseTheme as SharedTheme

// The `ColorScheme` constructor these replaced is deprecated because it has no defaults: it
// demands a value for every role, so it cannot grow with Material. The factory functions below
// take the same roles and fill in the newer ones, which is also what the Android theme does with
// this palette (see app/.../feature/shared/theme/SynapseTheme.kt). The shared palette has no M3
// "fixed" container roles, so those keep the factory defaults here exactly as they do on Android.
private fun SynapseColorScheme.toLightComposeColorScheme(): ColorScheme {
    return lightColorScheme(
        primary = Color(this.primary),
        onPrimary = Color(this.onPrimary),
        primaryContainer = Color(this.primaryContainer),
        onPrimaryContainer = Color(this.onPrimaryContainer),
        secondary = Color(this.secondary),
        onSecondary = Color(this.onSecondary),
        secondaryContainer = Color(this.secondaryContainer),
        onSecondaryContainer = Color(this.onSecondaryContainer),
        tertiary = Color(this.tertiary),
        onTertiary = Color(this.onTertiary),
        tertiaryContainer = Color(this.tertiaryContainer),
        onTertiaryContainer = Color(this.onTertiaryContainer),
        error = Color(this.error),
        onError = Color(this.onError),
        errorContainer = Color(this.errorContainer),
        onErrorContainer = Color(this.onErrorContainer),
        background = Color(this.background),
        onBackground = Color(this.onBackground),
        surface = Color(this.surface),
        onSurface = Color(this.onSurface),
        surfaceVariant = Color(this.surfaceVariant),
        onSurfaceVariant = Color(this.onSurfaceVariant),
        outline = Color(this.outline),
        outlineVariant = Color(this.outlineVariant),
        scrim = Color(this.scrim),
        inverseSurface = Color(this.inverseSurface),
        inverseOnSurface = Color(this.inverseOnSurface),
        inversePrimary = Color(this.inversePrimary),
        surfaceTint = Color(this.surfaceTint),
        surfaceContainer = Color(this.surfaceContainer),
        surfaceContainerLow = Color(this.surfaceContainerLow),
        surfaceContainerHigh = Color(this.surfaceContainerHigh),
        surfaceContainerHighest = Color(this.surfaceContainerHighest),
        surfaceContainerLowest = Color(this.surfaceContainerLowest),
        surfaceDim = Color(this.surfaceContainerLow),
        surfaceBright = Color(this.surfaceContainerHigh)
    )
}

private fun SynapseColorScheme.toDarkComposeColorScheme(): ColorScheme {
    return darkColorScheme(
        primary = Color(this.primary),
        onPrimary = Color(this.onPrimary),
        primaryContainer = Color(this.primaryContainer),
        onPrimaryContainer = Color(this.onPrimaryContainer),
        secondary = Color(this.secondary),
        onSecondary = Color(this.onSecondary),
        secondaryContainer = Color(this.secondaryContainer),
        onSecondaryContainer = Color(this.onSecondaryContainer),
        tertiary = Color(this.tertiary),
        onTertiary = Color(this.onTertiary),
        tertiaryContainer = Color(this.tertiaryContainer),
        onTertiaryContainer = Color(this.onTertiaryContainer),
        error = Color(this.error),
        onError = Color(this.onError),
        errorContainer = Color(this.errorContainer),
        onErrorContainer = Color(this.onErrorContainer),
        background = Color(this.background),
        onBackground = Color(this.onBackground),
        surface = Color(this.surface),
        onSurface = Color(this.onSurface),
        surfaceVariant = Color(this.surfaceVariant),
        onSurfaceVariant = Color(this.onSurfaceVariant),
        outline = Color(this.outline),
        outlineVariant = Color(this.outlineVariant),
        scrim = Color(this.scrim),
        inverseSurface = Color(this.inverseSurface),
        inverseOnSurface = Color(this.inverseOnSurface),
        inversePrimary = Color(this.inversePrimary),
        surfaceTint = Color(this.surfaceTint),
        surfaceContainer = Color(this.surfaceContainer),
        surfaceContainerLow = Color(this.surfaceContainerLow),
        surfaceContainerHigh = Color(this.surfaceContainerHigh),
        surfaceContainerHighest = Color(this.surfaceContainerHighest),
        surfaceContainerLowest = Color(this.surfaceContainerLowest),
        surfaceDim = Color(this.surfaceContainerLow),
        surfaceBright = Color(this.surfaceContainerHigh)
    )
}

private val LightColorScheme = SharedTheme.colors.Light.toLightComposeColorScheme()
private val DarkColorScheme = SharedTheme.colors.Dark.toDarkComposeColorScheme()

@Composable
fun SynapseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
