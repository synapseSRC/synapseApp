package com.synapse.social.studioasinc.feature.auth.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.SynapseBrand
import com.synapse.social.studioasinc.feature.shared.theme.SynapseBrandPalette
import com.synapse.social.studioasinc.feature.shared.theme.WelcomeHeroMetrics

/**
 * Hero artwork for the welcome screen: flowing ribbon illustration drawn natively with Bezier
 * paths, plus the Synapse mark inside a dimensional plate.
 *
 * Everything is vector/Canvas based - no bitmap background - and every coordinate is expressed as a
 * fraction of the measured hero box, so the composition re-flows with the display size, the font
 * scale and the window insets instead of pinning to screenshot coordinates.
 */
@Composable
fun WelcomeHero(
    heroHeight: Dp,
    modifier: Modifier = Modifier
) {
    val palette = SynapseBrand.palette
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(heroHeight),
        contentAlignment = Alignment.Center
    ) {
        // The artwork is pure geometry: build the paths, brushes and scalar values once per size
        // instead of allocating them on every draw pass, so the Canvas lambda only issues draw
        // calls and never triggers GC work on the render thread.
        val artwork = remember(maxWidth, maxHeight, palette, density) {
            with(density) {
                buildHeroArtwork(
                    width = maxWidth.toPx(),
                    height = maxHeight.toPx(),
                    hairlineWidth = RibbonHairlineWidth.toPx(),
                    palette = palette
                )
            }
        }

        Canvas(modifier = Modifier.matchParentSize()) {
            drawHeroArtwork(artwork)
        }

        WelcomeLogoPlate(
            palette = palette,
            modifier = Modifier.align(Alignment.Center).offset(
                y = heroHeight * (WelcomeHeroMetrics.LogoCentreFraction - 0.5f)
            )
        )
    }
}

/**
 * The Synapse mark on a soft dimensional plate.
 *
 * The [R.drawable.ic_launcher_foreground] vector is reused as-is, so its original gradient identity
 * is preserved; only the surrounding plate is drawn here.
 */
@Composable
fun WelcomeLogoPlate(
    modifier: Modifier = Modifier,
    palette: SynapseBrandPalette = SynapseBrand.palette
) {
    val shape = RoundedCornerShape(WelcomeHeroMetrics.PlateCorner)

    Box(
        modifier = modifier
            .size(WelcomeHeroMetrics.PlateSize)
            .shadow(
                elevation = WelcomeHeroMetrics.PlateElevation,
                shape = shape,
                clip = false,
                ambientColor = palette.plateShadow,
                spotColor = palette.plateShadow
            )
            .clip(shape)
            .background(palette.plate.copy(alpha = PlateFillAlpha))
            .border(
                width = Sizes.BorderHairline,
                color = palette.plateBorder.copy(alpha = PlateBorderAlpha),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = stringResource(id = R.string.app_name),
            modifier = Modifier.requiredSize(WelcomeHeroMetrics.MarkSize)
        )
    }
}

private const val PlateFillAlpha = 0.94f
private const val PlateBorderAlpha = 0.75f

/** Number of line segments used to approximate one ribbon edge; smooth at any hero size. */
private const val RibbonSamples = 56

private val RibbonHairlineWidth = 1.5.dp

/**
 * Pre-built hero geometry for one hero size.
 *
 * Every [Path], [Brush] and scalar is computed once by [buildHeroArtwork]; the draw pass only reads
 * them, which keeps the render thread allocation-free.
 */
private class HeroArtwork(
    val petalPath: Path,
    val petalBrush: Brush,
    val cornerBloomCenter: Offset,
    val cornerBloomRadius: Float,
    val cornerBloomBrush: Brush,
    val mistPath: Path,
    val mistBrush: Brush,
    val ribbonShadowColor: Color,
    val ribbonShadowOffset: Float,
    val ribbonPath: Path,
    val ribbonBrush: Brush,
    val sheenPath: Path,
    val sheenBrush: Brush,
    val ribbonEdgeColor: Color,
    val ribbonEdgeWidth: Float,
    val haloCenter: Offset,
    val haloRadius: Float,
    val haloBrush: Brush,
)

/**
 * Builds the whole hero composition for a given box size. Pure: no composable state, no draw calls,
 * so it can be cached with [remember] and re-used by every draw pass.
 */
private fun buildHeroArtwork(
    width: Float,
    height: Float,
    hairlineWidth: Float,
    palette: SynapseBrandPalette
): HeroArtwork {
    val w = width
    val h = height

    // Soft directional lighting plus the large frosted petal that opens the composition top-left.
    val petal = Path().apply {
        moveTo(-0.10f * w, -0.06f * h)
        cubicTo(0.26f * w, 0.01f * h, 0.43f * w, 0.19f * h, 0.47f * w, 0.44f * h)
        cubicTo(0.30f * w, 0.30f * h, 0.04f * w, 0.21f * h, -0.12f * w, 0.18f * h)
        close()
    }
    val petalBrush = Brush.linearGradient(
        colors = listOf(
            palette.bloom.copy(alpha = 0.85f),
            palette.bloom.copy(alpha = 0.28f),
            palette.bloom.copy(alpha = 0f)
        ),
        start = Offset(0.12f * w, 0f),
        end = Offset(0.44f * w, 0.48f * h)
    )

    val cornerBloomCenter = Offset(0.86f * w, 0.12f * h)
    val cornerBloomRadius = 0.55f * w
    val cornerBloomBrush = Brush.radialGradient(
        colors = listOf(
            palette.ribbonLight.copy(alpha = 0.28f),
            palette.ribbonLight.copy(alpha = 0f)
        ),
        center = cornerBloomCenter,
        radius = cornerBloomRadius
    )

    // The flowing Synapse ribbon: a wide band sweeping from the top-right corner to the lower left.
    val ribbon = ribbonPath(
        p0 = Offset(1.15f * w, 0.16f * h),
        p1 = Offset(0.90f * w, 0.30f * h),
        p2 = Offset(0.52f * w, 0.66f * h),
        p3 = Offset(-0.08f * w, 0.94f * h),
        startHalfWidth = 0.100f * h,
        endHalfWidth = 0.072f * h
    )
    val ribbonBrush = Brush.linearGradient(
        colors = listOf(
            palette.ribbonDeep,
            palette.ribbonMid,
            palette.ribbonLight,
            palette.ribbonSheen
        ),
        start = Offset(0.80f * w, 0.16f * h),
        end = Offset(0.12f * w, 0.86f * h)
    )

    // Diffused band crossing the top-right corner, giving the ribbons depth of field.
    val mist = ribbonPath(
        p0 = Offset(0.30f * w, -0.12f * h),
        p1 = Offset(0.60f * w, 0.02f * h),
        p2 = Offset(0.88f * w, 0.04f * h),
        p3 = Offset(1.12f * w, 0.30f * h),
        startHalfWidth = 0.150f * h,
        endHalfWidth = 0.190f * h
    )
    val mistBrush = Brush.linearGradient(
        colors = listOf(
            palette.ribbonLight.copy(alpha = 0.45f),
            palette.ribbonSheen.copy(alpha = 0.70f)
        ),
        start = Offset(0.34f * w, 0f),
        end = Offset(1f * w, 0.30f * h)
    )

    val sheen = ribbonPath(
        p0 = Offset(1.12f * w, 0.09f * h),
        p1 = Offset(0.88f * w, 0.24f * h),
        p2 = Offset(0.50f * w, 0.60f * h),
        p3 = Offset(-0.10f * w, 0.86f * h),
        startHalfWidth = 0.030f * h,
        endHalfWidth = 0.018f * h
    )
    val sheenBrush = Brush.linearGradient(
        colors = listOf(
            palette.ribbonSheen.copy(alpha = 0.85f),
            palette.ribbonLight.copy(alpha = 0.45f)
        ),
        start = Offset(0.80f * w, 0.10f * h),
        end = Offset(0.10f * w, 0.80f * h)
    )

    // Cool violet halo behind the plate; keeps the mark crisply separated from the ribbon band.
    val haloCenter = Offset(0.5f * w, WelcomeHeroMetrics.LogoCentreFraction * h)
    val haloRadius = 0.34f * w
    val haloBrush = Brush.radialGradient(
        colors = listOf(
            palette.halo.copy(alpha = 0.38f),
            palette.halo.copy(alpha = 0.12f),
            palette.halo.copy(alpha = 0f)
        ),
        center = haloCenter,
        radius = haloRadius
    )

    return HeroArtwork(
        petalPath = petal,
        petalBrush = petalBrush,
        cornerBloomCenter = cornerBloomCenter,
        cornerBloomRadius = cornerBloomRadius,
        cornerBloomBrush = cornerBloomBrush,
        mistPath = mist,
        mistBrush = mistBrush,
        ribbonShadowColor = palette.ribbonDeep.copy(alpha = 0.16f),
        ribbonShadowOffset = 0.012f * h,
        ribbonPath = ribbon,
        ribbonBrush = ribbonBrush,
        sheenPath = sheen,
        sheenBrush = sheenBrush,
        ribbonEdgeColor = palette.ribbonEdge.copy(alpha = 0.14f),
        ribbonEdgeWidth = hairlineWidth,
        haloCenter = haloCenter,
        haloRadius = haloRadius,
        haloBrush = haloBrush
    )
}

/** Draws a pre-built [HeroArtwork]; allocates nothing per frame. */
private fun DrawScope.drawHeroArtwork(artwork: HeroArtwork) {
    // Backdrop: lighting petal and the corner bloom.
    drawPath(path = artwork.petalPath, brush = artwork.petalBrush)
    drawCircle(
        brush = artwork.cornerBloomBrush,
        radius = artwork.cornerBloomRadius,
        center = artwork.cornerBloomCenter
    )

    // Diffused band behind the main ribbon.
    drawPath(path = artwork.mistPath, brush = artwork.mistBrush)

    // Contact shadow so the band sits in front of the petal instead of bleeding into it.
    translate(top = artwork.ribbonShadowOffset) {
        drawPath(path = artwork.ribbonPath, color = artwork.ribbonShadowColor)
    }

    drawPath(path = artwork.ribbonPath, brush = artwork.ribbonBrush)
    drawPath(path = artwork.sheenPath, brush = artwork.sheenBrush)
    drawPath(
        path = artwork.ribbonPath,
        color = artwork.ribbonEdgeColor,
        style = Stroke(width = artwork.ribbonEdgeWidth)
    )

    // Halo last, so it separates the mark from the ribbon underneath it.
    drawCircle(
        brush = artwork.haloBrush,
        radius = artwork.haloRadius,
        center = artwork.haloCenter
    )
}

/**
 * Builds a tapered band around a cubic Bezier centre line. The outline is sampled from the curve so
 * the band keeps a constant, controllable thickness instead of the pinch a naive offset produces.
 */
private fun ribbonPath(
    p0: Offset,
    p1: Offset,
    p2: Offset,
    p3: Offset,
    startHalfWidth: Float,
    endHalfWidth: Float
): Path {
    val leading = ArrayList<Offset>(RibbonSamples + 1)
    val trailing = ArrayList<Offset>(RibbonSamples + 1)
    var previous = p0

    for (index in 0..RibbonSamples) {
        val t = index / RibbonSamples.toFloat()
        val point = cubicPointAt(p0, p1, p2, p3, t)
        val tangent = if (index == 0) {
            cubicPointAt(p0, p1, p2, p3, 0.002f) - p0
        } else {
            point - previous
        }
        val length = tangent.getDistance()
        val normal = if (length < 0.0001f) Offset(0f, 1f) else Offset(-tangent.y / length, tangent.x / length)
        val halfWidth = startHalfWidth + (endHalfWidth - startHalfWidth) * t

        leading.add(point + normal * halfWidth)
        trailing.add(point - normal * halfWidth)
        previous = point
    }

    return Path().apply {
        moveTo(leading.first().x, leading.first().y)
        for (index in 1..RibbonSamples) lineTo(leading[index].x, leading[index].y)
        for (index in RibbonSamples downTo 0) lineTo(trailing[index].x, trailing[index].y)
        close()
    }
}

private fun cubicPointAt(p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float): Offset {
    val u = 1f - t
    val w0 = u * u * u
    val w1 = 3f * u * u * t
    val w2 = 3f * u * t * t
    val w3 = t * t * t
    return Offset(
        x = w0 * p0.x + w1 * p1.x + w2 * p2.x + w3 * p3.x,
        y = w0 * p0.y + w1 * p1.y + w2 * p2.y + w3 * p3.y
    )
}
