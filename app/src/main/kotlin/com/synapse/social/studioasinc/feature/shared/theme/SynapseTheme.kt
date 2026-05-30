package com.synapse.social.studioasinc.feature.shared.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme

private val LightColorScheme = lightColorScheme(
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
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
)

private val DarkColorScheme = darkColorScheme(
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
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SynapseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    atmosphereState: UiAtmosphereState = remember { UiAtmosphereState() },
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val dominantColor by animateColorAsState(
        targetValue = atmosphereState.colors.dominantColor,
        animationSpec = tween(durationMillis = 2000),
        label = "DominantColorAnimation"
    )

    val vibrantColor by animateColorAsState(
        targetValue = atmosphereState.colors.vibrantColor,
        animationSpec = tween(durationMillis = 2500, easing = LinearOutSlowInEasing),
        label = "VibrantColorAnimation"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "AtmosphereInfinite")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AtmospherePulse"
    )

    CompositionLocalProvider(LocalUiAtmosphere provides atmosphereState) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = SynapseTypography,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorScheme.background)
                    .drawBehind {
                        val brush = Brush.radialGradient(
                            colors = listOf(
                                dominantColor.copy(alpha = if (darkTheme) 0.2f else 0.15f),
                                vibrantColor.copy(alpha = pulseAlpha),
                                Color.Transparent
                            ),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.2f),
                            radius = size.maxDimension * 0.8f
                        )
                        drawRect(brush)
                    }
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf<Color>(
                                dominantColor.copy(alpha = if (darkTheme) 0.15f else 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                content()
            }
        }
    }
}

fun Modifier.glassmorphic(
    blurRadius: Float = 30f,
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
    strokeWidth: androidx.compose.ui.unit.Dp = 1.dp
): Modifier = this.then(
    Modifier
        .graphicsLayer {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                renderEffect = android.graphics.RenderEffect
                    .createBlurEffect(blurRadius, blurRadius, android.graphics.Shader.TileMode.CLAMP)
                    .asComposeRenderEffect()
            }
        }
        .clip(shape)
        .drawWithCache {
            onDrawBehind {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    // Fallback for older versions
                    drawRect(Color.White.copy(alpha = 0.1f))
                }
            }
        }
        .border(
            width = strokeWidth,
            brush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.15f),
                    Color.White.copy(alpha = 0.05f)
                )
            ),
            shape = shape
        )
)
