package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val GroupLightScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF635A75),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFEF7FF),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFEF7FF),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE6E0E9),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    inverseSurface = Color(0xFF322F35),
    inverseOnSurface = Color(0xFFF5EFF7),
    inversePrimary = Color(0xFFD0BCFF),
    surfaceContainerLow = Color(0xFFF7F2FA),
    surfaceContainer = Color(0xFFF3EDF7),
    surfaceContainerHigh = Color(0xFFECE6F0),
    surfaceContainerHighest = Color(0xFFE6E0E9)
)

private val GroupDarkScheme = darkColorScheme(
    primary = Color(0xFFD2BCFC),
    onPrimary = Color(0xFF32226F),
    primaryContainer = Color(0xFF4C3889),
    onPrimaryContainer = Color(0xFFE9DDFF),
    secondary = Color(0xFFCDC1E1),
    secondaryContainer = Color(0xFF4B425D),
    onSecondaryContainer = Color(0xFFE9DDFD),
    tertiaryContainer = Color(0xFF6C3644),
    onTertiaryContainer = Color(0xFFFDDAE1),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF141317),
    onBackground = Color(0xFFE4E1E7),
    surface = Color(0xFF141317),
    onSurface = Color(0xFFE4E1E7),
    surfaceVariant = Color(0xFF363438),
    onSurfaceVariant = Color(0xFFC9C4D1),
    outline = Color(0xFF938F9B),
    outlineVariant = Color(0xFF494550),
    inverseSurface = Color(0xFFE4E1E7),
    inverseOnSurface = Color(0xFF313034),
    inversePrimary = Color(0xFF6750A4),
    surfaceContainerLow = Color(0xFF1C1B1F),
    surfaceContainer = Color(0xFF201F23),
    surfaceContainerHigh = Color(0xFF2B292D),
    surfaceContainerHighest = Color(0xFF363438)
)

/** The group flow uses the requested purple static palette and follows the device appearance. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun GroupCreationTheme(content: @Composable () -> Unit) {
    MaterialExpressiveTheme(
        colorScheme = if (isSystemInDarkTheme()) GroupDarkScheme else GroupLightScheme,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}


internal object GroupCreationDimensions {
    // Project-specific QR rendering size; M3 does not prescribe a QR code size.
    val qrCodeSize = 160.dp
}
