package com.synapse.social.studioasinc.feature.shared.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle

/**
 * Liquid glass, in one place.
 *
 * Every floating surface that is meant to read as the same material - the chat header's back button
 * and its two pills, and now the composer at the bottom of the conversation - draws itself through
 * [liquidGlassSurface] with these numbers, so the app has one glass rather than a family of
 * approximations. The tint is what keeps text and icons readable over the sampled backdrop; the
 * blur, the refraction and the specular edge are what make the surface read as glass rather than as
 * a translucent panel. The blur stays deliberately low - a heavy frosted blur is the look this is
 * not.
 */
internal const val GlassTintAlphaLight = 0.45f
internal const val GlassTintAlphaDark = 0.5f
internal val GlassBlurRadius = 3.dp
internal val GlassHighlightWidth = 0.5.dp
internal val GlassHighlightBlur = 0.75.dp
internal const val GlassHighlightAlphaLight = 0.9f
internal const val GlassHighlightAlphaDark = 0.5f

/**
 * Refraction of a capsule whose short side is at least 48dp: the height stays inside the corner
 * radius and the amount inside the short side, which is the range the refraction shader is defined
 * over. Both the header's pills and the composer sit inside those bounds.
 */
internal val GlassLensHeight = 12.dp
internal val GlassLensAmount = 24.dp

/** How far a surface shrinks while it is held, without moving the glass it samples. */
internal const val GlassPressScale = 0.96f

/**
 * The two theme decisions every glass surface makes the same way: the colour it is tinted with, and
 * whether that colour means the light scheme. A light surface-container role means the light scheme,
 * which leans bright with a strong sheen; the dark scheme leans deep with a restrained one.
 */
internal data class LiquidGlassPaint(val tint: Color, val isLight: Boolean)

@Composable
internal fun rememberLiquidGlassPaint(): LiquidGlassPaint {
    val tint = MaterialTheme.colorScheme.surfaceContainerHigh
    return LiquidGlassPaint(tint = tint, isLight = tint.luminance() > 0.5f)
}

/**
 * How far a surface is pressed, 0 while untouched and 1 while held, on the one curve the surfaces
 * share. A surface that cannot be pressed - the composer's container, for instance - passes no
 * interaction source and stays still.
 */
@Composable
internal fun rememberGlassPressProgress(interactionSource: MutableInteractionSource?): Float {
    if (interactionSource == null) return 0f
    val held by interactionSource.collectIsPressedAsState()
    val progress by animateFloatAsState(
        targetValue = if (held) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "liquidGlassPress"
    )
    return progress
}

/**
 * The glass itself: the backdrop layer the caller captured is sampled through [shape], processed,
 * and drawn behind whatever the surface holds, which is why a glass surface takes no background of
 * its own.
 *
 * The press scale lives in the library's layerBlock rather than in a graphicsLayer above this
 * modifier: the node draws the backdrop through the inverse of that transform, so the surface can
 * shrink under a finger while the glass it samples stays exactly where it was.
 */
internal fun Modifier.liquidGlassSurface(
    backdrop: Backdrop,
    shape: Shape,
    paint: LiquidGlassPaint,
    pressProgress: Float = 0f
): Modifier = drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = {
        // The order the library prescribes: colour filter, then blur, then lens.
        vibrancy()
        blur(GlassBlurRadius.toPx())
        // Refraction is what separates this from frosted glass. lens() is a no-op below Android 13,
        // where the reflection shaders do not exist.
        lens(
            refractionHeight = GlassLensHeight.toPx(),
            refractionAmount = GlassLensAmount.toPx(),
            depthEffect = true,
            chromaticAberration = true
        )
    },
    // White is the colour of the specular edge itself, not a themed role; only its strength follows
    // the scheme.
    highlight = {
        Highlight(
            width = GlassHighlightWidth,
            blurRadius = GlassHighlightBlur,
            alpha = if (paint.isLight) GlassHighlightAlphaLight else GlassHighlightAlphaDark,
            style = HighlightStyle.Default(color = Color.White.copy(alpha = 0.5f))
        )
    },
    layerBlock = {
        val scale = 1f - (1f - GlassPressScale) * pressProgress
        scaleX = scale
        scaleY = scale
    },
    onDrawSurface = {
        drawRect(
            paint.tint.copy(alpha = if (paint.isLight) GlassTintAlphaLight else GlassTintAlphaDark)
        )
    }
)

/**
 * The specular sheen of a filled, circular action button.
 *
 * The composer's mic and send buttons keep Synapse's accent fill - they are the one place in this
 * conversation that is not glass - but they gain the same kind of edge the glass surfaces have, so
 * they read as raised beads of the same material family instead of flat colour chips sitting on top
 * of it. Drawn over the fill and behind the icon.
 */
internal fun Modifier.accentBead(): Modifier = drawBehind {
    drawCircle(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to Color.White.copy(alpha = 0.30f),
                0.62f to Color.Transparent
            )
        )
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.16f),
        style = Stroke(width = 1.dp.toPx())
    )
}
