package com.synapse.social.studioasinc.feature.permission.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import com.synapse.social.studioasinc.feature.permission.PermissionColors
import com.synapse.social.studioasinc.feature.permission.PermissionMetrics

/**
 * The screen's backdrop: a cool wash behind the header, the aurora ribbons that carry the accent
 * identity, and a pool of light under the primary action.
 *
 * Every coordinate is a fraction of the canvas, so the composition scales from a compact phone to a
 * tablet instead of assuming a device. It is static by design — the screen's motion is the cards
 * arriving and the buttons responding, and a moving background would compete with both.
 */
@Composable
internal fun PermissionBackdrop(modifier: Modifier = Modifier) {
    val wash = PermissionColors.pageWash
    val washFade = wash.copy(alpha = 0f)
    val glow = PermissionColors.bottomGlow
    val glowFade = glow.copy(alpha = 0f)
    val cyan = PermissionColors.auroraCyan
    val teal = PermissionColors.auroraTeal
    val highlight = PermissionColors.auroraHighlight
    val strength = PermissionColors.auroraOpacity

    Canvas(modifier = modifier.fillMaxSize().clipToBounds()) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        // Cool wash behind the header, fading into the page background before the cards begin.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(wash, washFade),
                startY = 0f,
                endY = height * PermissionMetrics.washFadeFraction
            )
        )

        // Light under the primary action, so Continue sits on a soft pool rather than a hard edge.
        val glowRadius = width * PermissionMetrics.bottomGlowRadiusFraction
        val glowCenter = Offset(width * 0.5f, height)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glow, glowFade),
                center = glowCenter,
                radius = glowRadius
            ),
            center = glowCenter,
            radius = glowRadius
        )

        val aurora = height * PermissionMetrics.auroraHeightFraction

        // Deepest teal: the crescent that hangs in the top-right corner of the reference.
        drawPath(
            path = Path().apply {
                moveTo(width * 0.40f, -aurora * 0.34f)
                cubicTo(width * 0.58f, aurora * 0.62f, width * 0.78f, aurora * 0.34f, width * 1.04f, aurora * 0.02f)
                lineTo(width * 1.04f, -aurora * 0.40f)
                close()
            },
            brush = Brush.linearGradient(
                colors = listOf(cyan.copy(alpha = 0.05f * strength), teal.copy(alpha = 0.85f * strength)),
                start = Offset(width * 0.40f, -aurora * 0.34f),
                end = Offset(width * 1.04f, -aurora * 0.10f)
            )
        )

        // Main ribbon: enters low on the left, rises across the header and leaves through the top-right.
        drawPath(
            path = Path().apply {
                moveTo(0f, aurora * 0.66f)
                cubicTo(width * 0.24f, aurora * 0.30f, width * 0.54f, aurora * 0.86f, width * 1.02f, aurora * 0.20f)
                lineTo(width * 1.02f, -aurora * 0.30f)
                cubicTo(width * 0.54f, aurora * 0.36f, width * 0.24f, -aurora * 0.10f, 0f, aurora * 0.22f)
                close()
            },
            brush = Brush.linearGradient(
                colors = listOf(
                    cyan.copy(alpha = 0f),
                    cyan.copy(alpha = 0.55f * strength),
                    teal.copy(alpha = 0.80f * strength)
                ),
                start = Offset(0f, aurora * 0.45f),
                end = Offset(width, aurora * 0.05f)
            )
        )

        // Specular ribbon riding the top edge of the band; fades at both ends so nothing is cut off.
        drawPath(
            path = Path().apply {
                moveTo(0f, aurora * 0.30f)
                cubicTo(width * 0.26f, -aurora * 0.02f, width * 0.56f, aurora * 0.44f, width * 1.02f, aurora * 0.14f)
                lineTo(width * 1.02f, aurora * 0.02f)
                cubicTo(width * 0.56f, aurora * 0.32f, width * 0.26f, -aurora * 0.14f, 0f, aurora * 0.18f)
                close()
            },
            brush = Brush.linearGradient(
                colors = listOf(
                    highlight.copy(alpha = 0f),
                    highlight.copy(alpha = 0.45f * strength),
                    highlight.copy(alpha = 0f)
                ),
                start = Offset(0f, aurora * 0.20f),
                end = Offset(width, aurora * 0.05f)
            )
        )
    }
}
