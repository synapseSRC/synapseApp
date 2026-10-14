package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import android.graphics.Paint as PlatformPaint
import android.graphics.Path as PlatformPath
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.AccentBlue
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.StoryRingEnd
import com.synapse.social.studioasinc.feature.shared.theme.StoryRingMiddle
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import kotlin.math.abs
import kotlin.math.ceil

private const val VOICE_LIQUID_AGSL = """
    uniform float2 uResolution;
    uniform float4 uPrimary;
    uniform float4 uSecondary;
    uniform float4 uTertiary;
    uniform float uEnergy;

    half4 main(float2 coord) {
        float2 uv = coord / uResolution;
        half3 firstBlend = mix(uTertiary.rgb, uPrimary.rgb, smoothstep(0.04, 0.56, uv.x));
        half3 pigment = mix(firstBlend, uSecondary.rgb, smoothstep(0.38, 0.86, uv.x));
        pigment = mix(pigment, uTertiary.rgb, smoothstep(0.78, 1.0, uv.x) * 0.72);
        float upperHighlight = exp(-pow((uv.y - 0.70), 2.0) * 190.0);
        float lowerHighlight = exp(-pow((uv.y - 0.84), 2.0) * 62.0);
        half3 highlight = uPrimary.rgb * (upperHighlight * (0.16 + uEnergy * 0.26)) +
            half3(lowerHighlight * (0.08 + uEnergy * 0.12));
        return half4(clamp(pigment + highlight, 0.0, 1.0), 1.0);
    }
"""

private object GlowDesign {
    const val MIN_AMPLITUDE = 0.08f
    const val MAX_AMPLITUDE = 1f
    const val WAVE_NODE_COUNT = 44
    const val MIN_ACTIVE_LIFT = 0.055f
    const val MAX_ACTIVE_LIFT = 0.34f
    const val EDGE_TAPER = 9f
    const val SPRING_STIFFNESS = 30f
    const val NEIGHBOR_COUPLING = 25f
    const val VELOCITY_DAMPING = 8.5f
    const val MAX_RIPPLE_DISPLACEMENT = 0.24f
    const val IMPULSE_THRESHOLD = 0.008f

    // My design decision (not an M3 token): the custom liquid geometry and spring constants tune
    // the recording pill as a compact 1D fluid surface, rather than an M3 component animation.
}

/** A damped, neighbor-coupled surface. Audio-envelope changes inject impulses; the springs propagate and settle them. */
internal class VoiceLiquidWaveSimulation(
    private val nodeCount: Int = GlowDesign.WAVE_NODE_COUNT
) {
    private val displacement = FloatArray(nodeCount)
    private val velocity = FloatArray(nodeCount)
    private val nextVelocity = FloatArray(nodeCount)
    private var previousEnergy = GlowDesign.MIN_AMPLITUDE
    private var impulseNode = nodeCount / 2

    fun reset() {
        displacement.fill(0f)
        velocity.fill(0f)
        nextVelocity.fill(0f)
        previousEnergy = GlowDesign.MIN_AMPLITUDE
        impulseNode = nodeCount / 2
    }

    fun offsets(): FloatArray = displacement

    fun advance(amplitude: Float, elapsedSeconds: Float, paused: Boolean) {
        val energy = amplitude.coerceIn(GlowDesign.MIN_AMPLITUDE, GlowDesign.MAX_AMPLITUDE)
        val delta = energy - previousEnergy
        if (!paused && abs(delta) >= GlowDesign.IMPULSE_THRESHOLD) {
            // Advance through the surface by the amount the input changed; no repeating phase or canned wave is used.
            val stride = 3 + (abs(delta) * 12f).toInt()
            impulseNode = 3 + ((impulseNode - 3 + stride) % (nodeCount - 6))
            val impulse = (abs(delta) * 3.8f + energy * 0.58f).coerceIn(0.12f, 2.8f)
            val direction = if (delta >= 0f) 1f else -0.62f
            for (distance in -2..2) {
                val index = (impulseNode + distance).coerceIn(1, nodeCount - 2)
                val weight = 1f - abs(distance) * 0.22f
                velocity[index] += direction * impulse * weight
            }
        }
        previousEnergy = energy

        val dt = elapsedSeconds.coerceIn(0f, 0.05f)
        if (dt <= 0f) return
        val substeps = ceil(dt / (1f / 120f)).toInt().coerceIn(1, 6)
        val step = dt / substeps
        repeat(substeps) {
            for (i in 0 until nodeCount) {
                if (i == 0 || i == nodeCount - 1) {
                    nextVelocity[i] = 0f
                    continue
                }
                val laplacian = displacement[i - 1] + displacement[i + 1] - 2f * displacement[i]
                val acceleration =
                    -GlowDesign.SPRING_STIFFNESS * displacement[i] +
                    GlowDesign.NEIGHBOR_COUPLING * laplacian -
                    GlowDesign.VELOCITY_DAMPING * velocity[i]
                nextVelocity[i] = (velocity[i] + acceleration * step).coerceIn(-4f, 4f)
            }
            for (i in 1 until nodeCount - 1) {
                velocity[i] = nextVelocity[i]
                displacement[i] = (displacement[i] + velocity[i] * step)
                    .coerceIn(-GlowDesign.MAX_RIPPLE_DISPLACEMENT, GlowDesign.MAX_RIPPLE_DISPLACEMENT)
            }
            displacement[0] = 0f
            displacement[nodeCount - 1] = 0f
        }
    }
}

/**
 * Audio-reactive liquid recording pill with an independently clipped backdrop-glass layer.
 * The backdrop is blurred below this Canvas; timer, hint, and action controls are drawn by the host above it.
 */
@Composable
fun VoiceRecordingGlowSurface(
    voiceState: VoiceState,
    reducedMotionOverride: Boolean,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    val active = voiceState.isGlowActive()
    val paused = (voiceState as? VoiceState.Locked)?.isPaused == true
    val reducedMotion = reducedMotionOverride
    val colorScheme = MaterialTheme.colorScheme
    val surface = colorScheme.surfaceContainerHigh
    // My design decision (not in M3): reuse the app's brand-blue and story-ring cyan/violet accents for liquid pigment.
    val liquidBlue = AccentBlue
    val liquidCyan = StoryRingEnd
    val liquidViolet = StoryRingMiddle
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(Sizes.CornerMassive)
    val amplitude = if (active) voiceState.glowAmplitude() else GlowDesign.MIN_AMPLITUDE
    val latestAmplitude by rememberUpdatedState(amplitude)
    val simulation = remember { VoiceLiquidWaveSimulation() }
    val liquidShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) RuntimeShader(VOICE_LIQUID_AGSL) else null
    }
    val nativeLiquidPath = remember { PlatformPath() }
    val nativePaint = remember { PlatformPaint(PlatformPaint.ANTI_ALIAS_FLAG) }
    var frameNanos by remember { mutableLongStateOf(0L) }
    val edgePath = remember { Path() }
    val fillPath = remember { Path() }

    // My design decision (not an M3 token): a localized 16dp backdrop blur keeps chat details soft
    // while the Compose controls in VoiceInputHost remain crisp and readable.
    val glassStyle = remember(surface) {
        HazeBlurStyle {
            blurRadius(16.dp)
            blurEnabled(true)
            backgroundColor(surface.copy(alpha = 0.40f))
            colorEffects(listOf(HazeColorEffect.tint(surface.copy(alpha = 0.12f))))
            fallbackColorEffect(HazeColorEffect.tint(surface.copy(alpha = 0.76f)))
        }
    }

    LaunchedEffect(active, reducedMotion, paused) {
        simulation.reset()
        frameNanos = 0L
        if (!active || reducedMotion) return@LaunchedEffect
        var previousFrame = 0L
        while (true) {
            val now = withFrameNanos { it }
            if (previousFrame != 0L) {
                simulation.advance(
                    amplitude = latestAmplitude,
                    elapsedSeconds = (now - previousFrame) / 1_000_000_000f,
                    paused = paused
                )
            }
            previousFrame = now
            frameNanos = now
        }
    }

    Box(modifier.clip(shape)) {
        if (hazeState != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .hazeBlur(
                        input = HazeInput.Sources(hazeState),
                        style = glassStyle
                    )
            )
        }

        Canvas(Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            if (width <= 0f || height <= 0f) return@Canvas
            @Suppress("UNUSED_VARIABLE")
            val frame = frameNanos

            val round = CornerRadius(height / 2f)
            val baseAlpha = if (hazeState == null) 1f else 0.16f
            drawRoundRect(
                color = surface.copy(alpha = baseAlpha),
                cornerRadius = round
            )

            if (!active) {
                drawRoundRect(
                    color = colorScheme.outlineVariant.copy(alpha = 0.10f),
                    topLeft = Offset(0f, 0f),
                    size = Size(width, 1.dp.toPx()),
                    cornerRadius = round
                )
                return@Canvas
            }

            val normalizedEnergy = ((amplitude - GlowDesign.MIN_AMPLITUDE) /
                (GlowDesign.MAX_AMPLITUDE - GlowDesign.MIN_AMPLITUDE)).coerceIn(0f, 1f)
            val baseLift = GlowDesign.MIN_ACTIVE_LIFT +
                (GlowDesign.MAX_ACTIVE_LIFT - GlowDesign.MIN_ACTIVE_LIFT) * normalizedEnergy
            val offsets = simulation.offsets()
            val count = offsets.size

            edgePath.reset()
            fillPath.reset()
            nativeLiquidPath.reset()
            for (i in 0 until count) {
                val xRatio = i.toFloat() / (count - 1)
                val edgeFactor = (kotlin.math.min(xRatio, 1f - xRatio) * GlowDesign.EDGE_TAPER).coerceIn(0f, 1f)
                val lift = (baseLift + offsets[i] * 0.42f).coerceIn(0f, 0.54f) * edgeFactor
                val x = xRatio * width
                val y = height * (1f - lift)
                if (i == 0) {
                    edgePath.moveTo(x, y)
                    nativeLiquidPath.moveTo(x, y)
                } else {
                    val previousRatio = (i - 1).toFloat() / (count - 1)
                    val previousEdgeFactor = (kotlin.math.min(previousRatio, 1f - previousRatio) * GlowDesign.EDGE_TAPER).coerceIn(0f, 1f)
                    val previousLift = (baseLift + offsets[i - 1] * 0.42f).coerceIn(0f, 0.54f) * previousEdgeFactor
                    val previousX = previousRatio * width
                    val previousY = height * (1f - previousLift)
                    val middleX = (previousX + x) / 2f
                    val middleY = (previousY + y) / 2f
                    edgePath.quadraticTo(previousX, previousY, middleX, middleY)
                    nativeLiquidPath.quadTo(previousX, previousY, middleX, middleY)
                }
            }
            val lastRatio = 1f
            val lastEdgeFactor = (kotlin.math.min(lastRatio, 1f - lastRatio) * GlowDesign.EDGE_TAPER).coerceIn(0f, 1f)
            val endY = height * (1f - (baseLift + offsets[count - 1] * 0.42f).coerceIn(0f, 0.54f) * lastEdgeFactor)
            edgePath.lineTo(width, endY)
            nativeLiquidPath.lineTo(width, endY)
            fillPath.addPath(edgePath)
            fillPath.lineTo(width, height)
            fillPath.lineTo(0f, height)
            fillPath.close()
            nativeLiquidPath.lineTo(width, height)
            nativeLiquidPath.lineTo(0f, height)
            nativeLiquidPath.close()

            // Material accent roles supply the cyan/blue/violet palette while adapting to the active color scheme.
            val liquidBrush = Brush.horizontalGradient(
                colors = listOf(
                    liquidCyan.copy(alpha = 0.96f),
                    liquidBlue.copy(alpha = 0.98f),
                    liquidViolet.copy(alpha = 0.98f),
                    liquidCyan.copy(alpha = 0.98f),
                    liquidBlue.copy(alpha = 0.96f)
                ),
                startX = 0f,
                endX = width
            )
            if (liquidShader != null) {
                liquidShader.setFloatUniform("uResolution", width, height)
                liquidShader.setFloatUniform("uPrimary", liquidBlue.red, liquidBlue.green, liquidBlue.blue, 1f)
                liquidShader.setFloatUniform("uSecondary", liquidCyan.red, liquidCyan.green, liquidCyan.blue, 1f)
                liquidShader.setFloatUniform("uTertiary", liquidViolet.red, liquidViolet.green, liquidViolet.blue, 1f)
                liquidShader.setFloatUniform("uEnergy", normalizedEnergy)
                nativePaint.shader = liquidShader
                nativePaint.color = android.graphics.Color.WHITE
                drawContext.canvas.nativeCanvas.drawPath(nativeLiquidPath, nativePaint)
            } else {
                drawPath(path = fillPath, brush = liquidBrush)
            }
            drawPath(
                path = edgePath,
                brush = Brush.horizontalGradient(
                    listOf(
                        liquidCyan.copy(alpha = 0.95f),
                        liquidBlue.copy(alpha = 0.98f),
                        liquidViolet.copy(alpha = 0.98f),
                        liquidCyan.copy(alpha = 0.96f)
                    ),
                    startX = 0f,
                    endX = width
                ),
                style = Stroke(width = 1.7.dp.toPx())
            )
            drawPath(
                path = edgePath,
                color = colorScheme.onSurface.copy(alpha = 0.40f + normalizedEnergy * 0.18f),
                style = Stroke(width = 0.7.dp.toPx())
            )
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(
                        colorScheme.primary.copy(alpha = 0.34f),
                        colorScheme.secondary.copy(alpha = 0.42f),
                        colorScheme.tertiary.copy(alpha = 0.34f)
                    ),
                    startX = 0f,
                    endX = width
                ),
                topLeft = Offset(0.6.dp.toPx(), 0.6.dp.toPx()),
                size = Size(width - 1.2.dp.toPx(), height - 1.2.dp.toPx()),
                cornerRadius = CornerRadius((height - 1.2.dp.toPx()).coerceAtLeast(0f) / 2f),
                style = Stroke(width = 1.2.dp.toPx())
            )
        }
    }
}

@Composable
fun VoiceRecordingAmbientGlow(
    voiceState: VoiceState,
    @Suppress("UNUSED_PARAMETER") reducedMotionOverride: Boolean,
    modifier: Modifier = Modifier
) {
    if (!voiceState.isGlowActive()) return
    val energy = voiceState.glowAmplitude()
    val normalizedEnergy = ((energy - GlowDesign.MIN_AMPLITUDE) /
        (GlowDesign.MAX_AMPLITUDE - GlowDesign.MIN_AMPLITUDE)).coerceIn(0f, 1f)
    val colors = MaterialTheme.colorScheme
    Canvas(modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas
        val radius = maxOf(width * 0.70f, height * 1.05f)
        val alpha = 0.08f + 0.12f * normalizedEnergy
        val left = Offset(width * 0.26f, height * 0.91f)
        val center = Offset(width * 0.50f, height * 0.98f)
        val right = Offset(width * 0.75f, height * 0.89f)
        drawCircle(
            brush = Brush.radialGradient(listOf(colors.primary.copy(alpha = alpha), colors.secondary.copy(alpha = alpha * 0.42f), Color.Transparent), left, radius),
            center = left,
            radius = radius
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(colors.secondary.copy(alpha = alpha * 0.75f), Color.Transparent), center, radius * 0.76f),
            center = center,
            radius = radius * 0.76f
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(colors.tertiary.copy(alpha = alpha), colors.primary.copy(alpha = alpha * 0.42f), Color.Transparent), right, radius),
            center = right,
            radius = radius
        )
    }
}

private fun VoiceState.isGlowActive(): Boolean = this is VoiceState.Recording || this is VoiceState.Locked

/** A paused capture settles to its idle floor; active recordings keep following their newest real amplitude sample. */
internal fun VoiceState.glowAmplitude(): Float = when (this) {
    is VoiceState.Recording -> (amplitudeHistory.lastOrNull() ?: GlowDesign.MIN_AMPLITUDE)
        .coerceIn(GlowDesign.MIN_AMPLITUDE, GlowDesign.MAX_AMPLITUDE)
    is VoiceState.Locked -> if (isPaused) GlowDesign.MIN_AMPLITUDE else
        (amplitudeHistory.lastOrNull() ?: GlowDesign.MIN_AMPLITUDE)
            .coerceIn(GlowDesign.MIN_AMPLITUDE, GlowDesign.MAX_AMPLITUDE)
    else -> 0f
}
