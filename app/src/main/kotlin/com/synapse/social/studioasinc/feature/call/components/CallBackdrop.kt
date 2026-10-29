package com.synapse.social.studioasinc.feature.call.components

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.synapse.social.studioasinc.feature.auth.ui.util.AnimationUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import material.Hct
import material.SchemeTonalSpot
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The three tones the backdrop washes drift between. */
@Immutable
data class CallAtmosphereColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color
)

/**
 * Wash tones carried on the participant's avatar colours.
 *
 * Material publishes no image-to-role API, so the avatar is quantized with [Palette], its most
 * colorful swatch is fed in as a source color, and a tonal-spot scheme reads the accent roles from
 * it. The scheme performs the hue rotation Material specifies, which is what keeps the three washes
 * distinguishable — deriving all three from one swatch collapses them onto a single hue and the drift
 * stops reading. Tone comes from the theme rather than the photo, so a pale or greyscale avatar still
 * yields washes carrying the contrast the accent roles guarantee.
 *
 * The scheme roles stand in until extraction lands, or when there is no avatar at all, and the swap
 * cross-fades on an effects spring because colour must never overshoot.
 *
 * This carries the contact's hue; it does not carry their strength. Fixing the tone holds lightness
 * against the photo, so how visible the drift is stays [CallDimens.AtmosphereLightAlphaScale]'s job.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun rememberCallAtmosphereColors(avatarUrl: String?): CallAtmosphereColors {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val isDark = isDarkSurface(scheme)
    val derived by produceState<CallAtmosphereColors?>(initialValue = null, key1 = avatarUrl, key2 = isDark) {
        value = avatarUrl?.takeIf { it.isNotBlank() }
            ?.let { url -> withContext(Dispatchers.Default) { avatarWashColors(context, url, isDark) } }
    }
    val source = derived ?: CallAtmosphereColors(scheme.primary, scheme.secondary, scheme.tertiary)
    val colorSpec: AnimationSpec<Color> =
        if (AnimationUtil.rememberReducedMotion()) snap() else MaterialTheme.motionScheme.defaultEffectsSpec()
    return CallAtmosphereColors(
        primary = animateColorAsState(source.primary, colorSpec, label = "atmosphereWashPrimary").value,
        secondary = animateColorAsState(source.secondary, colorSpec, label = "atmosphereWashSecondary").value,
        tertiary = animateColorAsState(source.tertiary, colorSpec, label = "atmosphereWashTertiary").value
    )
}

private suspend fun avatarWashColors(
    context: Context,
    avatarUrl: String,
    isDark: Boolean
): CallAtmosphereColors? {
    val request = ImageRequest.Builder(context)
        .data(avatarUrl)
        .allowHardware(false)
        .build()
    val image = runCatching { context.imageLoader.execute(request).image }.getOrNull() ?: return null
    val sample = CallDimens.AtmosphereAvatarSamplePx
    val bitmap = runCatching { image.toBitmap(sample, sample) }.getOrNull() ?: return null
    val swatches = runCatching { Palette.from(bitmap).generate().swatches }.getOrNull().orEmpty()
    val sourceColor = swatches.maxByOrNull { Hct.fromInt(it.rgb).chroma }?.rgb ?: return null

    val tonalScheme = SchemeTonalSpot(Hct.fromInt(sourceColor), isDark, 0.0)
    val accentTone = if (isDark) CallDimens.AtmosphereAccentToneDark else CallDimens.AtmosphereAccentToneLight
    return CallAtmosphereColors(
        primary = Color(tonalScheme.primaryPalette.tone(accentTone)),
        secondary = Color(tonalScheme.secondaryPalette.tone(accentTone)),
        tertiary = Color(tonalScheme.tertiaryPalette.tone(accentTone))
    )
}

/**
 * The calm, cool backdrop the call screens sit on.
 *
 * A flat surface color made the portrait look pasted on. This draws the themed page tone plus three
 * low-alpha radial washes that drift on lissajous paths, breathe in size and opacity, and blend
 * toward their neighbouring tone, so the background keeps flowing instead of reading as a static
 * gradient. The washes carry the participant's avatar hues at the theme's accent tones — see
 * [rememberCallAtmosphereColors] — and fall back to the accent roles themselves.
 *
 * Every time term is a whole harmonic of [CallDimens.AtmosphereCycleMillis], so the loop restarts
 * without a visible jump. The phase is read inside the draw scope: each frame invalidates drawing
 * only, never composition. Reduced motion freezes the washes at their resting layout.
 */
@Composable
fun CallAtmosphere(
    colors: CallAtmosphereColors,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val base = scheme.surfaceContainerLow
    val primaryWash = colors.primary
    val secondaryWash = colors.secondary
    val tertiaryWash = colors.tertiary
    // A near-white page tone hides a low-alpha wash; the pale scheme needs the extra push.
    val alphaScale = if (isDarkSurface(scheme)) 1f else CallDimens.AtmosphereLightAlphaScale
    val phase = rememberAtmospherePhase()

    Canvas(modifier = modifier.fillMaxSize()) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        drawRect(base)

        val cycle = (phase?.value ?: 0f) * FULL_TURN
        val width = size.width
        val height = size.height

        drawWash(
            center = Offset(
                x = width * (0.50f + CallDimens.AtmospherePrimaryDriftX * sin(cycle)),
                y = height * (0.34f + CallDimens.AtmospherePrimaryDriftY * cos(cycle))
            ),
            radius = width * (0.82f + CallDimens.AtmosphereRadiusPulse * sin(cycle * 2f + 0.9f)),
            color = lerp(
                primaryWash,
                secondaryWash,
                CallDimens.AtmosphereTintBlend * (0.5f + 0.5f * sin(cycle + 0.6f))
            ),
            alpha = CallDimens.AtmosphereWashAlpha * alphaScale * atmosphereBreath(cycle, 0.4f)
        )
        drawWash(
            center = Offset(
                x = width * (0.15f + CallDimens.AtmosphereEdgeDriftX * cos(cycle)),
                y = height * (0.72f + CallDimens.AtmosphereEdgeDriftY * sin(cycle))
            ),
            radius = width * (0.66f + CallDimens.AtmosphereRadiusPulse * sin(cycle * 2f + 2.2f)),
            color = lerp(
                secondaryWash,
                tertiaryWash,
                CallDimens.AtmosphereTintBlend * (0.5f + 0.5f * sin(cycle + 2.4f))
            ),
            alpha = CallDimens.AtmosphereEdgeAlpha * alphaScale * atmosphereBreath(cycle, 1.9f)
        )
        drawWash(
            center = Offset(
                x = width * (0.85f + CallDimens.AtmosphereEdgeDriftX * sin(cycle * 2f + 0.7f)),
                y = height * (0.15f + CallDimens.AtmosphereEdgeDriftY * cos(cycle))
            ),
            radius = width * (0.56f + CallDimens.AtmosphereRadiusPulse * sin(cycle + 1.7f)),
            color = lerp(
                tertiaryWash,
                primaryWash,
                CallDimens.AtmosphereTintBlend * (0.5f + 0.5f * sin(cycle + 4.0f))
            ),
            alpha = CallDimens.AtmosphereEdgeAlpha * alphaScale * atmosphereBreath(cycle, 3.3f)
        )
    }
}

/** Which side of the wash the text sits on decides both the accent tone and the alpha push. */
private fun isDarkSurface(scheme: ColorScheme): Boolean = scheme.background.luminance() < 0.5f

private fun atmosphereBreath(cycle: Float, offset: Float): Float =
    1f + CallDimens.AtmosphereAlphaPulse * sin(cycle * 2f + offset)

private const val FULL_TURN = (2.0 * PI).toFloat()

@Composable
private fun rememberAtmospherePhase(): State<Float>? {
    if (AnimationUtil.rememberReducedMotion()) return null
    val transition = rememberInfiniteTransition(label = "callAtmosphere")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(CallDimens.AtmosphereCycleMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "callAtmospherePhase"
    )
}

private fun DrawScope.drawWash(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = alpha),
                color.copy(alpha = alpha * 0.45f),
                color.copy(alpha = 0f)
            ),
            center = center,
            radius = radius
        ),
        center = center,
        radius = radius
    )
}
