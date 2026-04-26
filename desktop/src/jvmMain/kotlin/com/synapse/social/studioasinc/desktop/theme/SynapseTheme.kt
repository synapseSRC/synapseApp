package com.synapse.social.studioasinc.desktop.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.synapse.social.studioasinc.shared.theme.SynapseColorScheme
import com.synapse.social.studioasinc.shared.theme.SynapseTheme as SharedTheme

private fun SynapseColorScheme.toComposeColorScheme(): ColorScheme {
    return ColorScheme(
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
        surfaceContainerLowest = Color(this.surface), // Fallback
        surfaceDim = Color(this.surface), // Fallback
        surfaceBright = Color(this.surface) // Fallback
    )
}

private val LightColorScheme = SharedTheme.colors.Light.toComposeColorScheme()
private val DarkColorScheme = SharedTheme.colors.Dark.toComposeColorScheme()

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
