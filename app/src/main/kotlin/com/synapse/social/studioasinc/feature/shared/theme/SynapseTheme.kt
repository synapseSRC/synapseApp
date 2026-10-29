package com.synapse.social.studioasinc.feature.shared.theme

import android.os.Build
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.core.content.ContextCompat

val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    outline = LightOutline,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightInverseOnSurface,
    inversePrimary = LightInversePrimary,
    surfaceTint = LightSurfaceTint,
    outlineVariant = LightOutlineVariant,
    scrim = LightScrim,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
)

val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    outline = DarkOutline,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    inversePrimary = DarkInversePrimary,
    surfaceTint = DarkSurfaceTint,
    outlineVariant = DarkOutlineVariant,
    scrim = DarkScrim,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SynapseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    increaseContrast: Boolean = false,
    highContrastText: Boolean = false,
    atmosphereState: UiAtmosphereState = remember { UiAtmosphereState() },
    content: @Composable () -> Unit
) {
    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val contrastEnabled = increaseContrast || highContrastText || rememberSystemHighContrastTextEnabled()
    val finalColorScheme = if (contrastEnabled) {
        baseScheme.copy(
            outline = baseScheme.onSurface,
            outlineVariant = baseScheme.onSurfaceVariant,
            onBackground = baseScheme.onSurface,
            onSurfaceVariant = baseScheme.onSurface
        )
    } else {
        baseScheme
    }

    CompositionLocalProvider(LocalUiAtmosphere provides atmosphereState) {
        MaterialExpressiveTheme(
            colorScheme = finalColorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = SynapseTypography,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(finalColorScheme.background)
            ) {
                content()
            }
        }
    }
}


@Composable
private fun rememberSystemHighContrastTextEnabled(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return false

    val context = LocalContext.current
    val accessibilityManager = remember(context) {
        context.getSystemService(AccessibilityManager::class.java)
    } ?: return false
    val enabled = remember(accessibilityManager) {
        mutableStateOf(accessibilityManager.isHighContrastTextEnabled)
    }

    DisposableEffect(accessibilityManager, context) {
        val listener = AccessibilityManager.HighContrastTextStateChangeListener { isEnabled ->
            enabled.value = isEnabled
        }
        accessibilityManager.addHighContrastTextStateChangeListener(
            ContextCompat.getMainExecutor(context),
            listener
        )
        onDispose {
            accessibilityManager.removeHighContrastTextStateChangeListener(listener)
        }
    }

    return enabled.value
}
