package com.synapse.social.studioasinc.feature.call.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.text.format.DateUtils
import android.view.WindowManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.SignalCellularConnectedNoInternet4Bar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.auth.ui.util.AnimationUtil
import com.synapse.social.studioasinc.feature.shared.components.UserAvatar
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.call.CallConnectionQuality
import com.synapse.social.studioasinc.shared.domain.model.call.CallDirection
import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import com.synapse.social.studioasinc.shared.domain.model.call.CallPhase
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession
import androidx.core.view.WindowCompat

/** Text/icon role used for every piece of chrome drawn on top of call media. */
@Composable
fun callMediaTextColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return if (scheme.background.luminance() < 0.5f) scheme.onSurface else scheme.inverseOnSurface
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CallAvatar(
    session: CallSession,
    size: Dp,
    modifier: Modifier = Modifier,
    showHalo: Boolean = false,
    haloColor: Color? = null
) {
    val scheme = MaterialTheme.colorScheme
    val initialsStyle = if (size >= CallDimens.Avatar) {
        MaterialTheme.typography.headlineLarge
    } else {
        MaterialTheme.typography.headlineMedium
    }
    val halo = if (showHalo) ringingHaloState() else null
    val glowColor = haloColor ?: scheme.primary
    // M3 Expressive shape library: an organic cookie silhouette instead of a plain circle.
    val portraitShape = MaterialShapes.Cookie9Sided.toShape()

    Box(
        modifier = modifier.clearAndSetSemantics { },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(size + CallDimens.HaloGlow)
                .graphicsLayer {
                    val beat = halo?.value ?: 0f
                    val scale = 1f + (CallDimens.HaloMaxScale - 1f) * beat * 0.6f
                    scaleX = scale
                    scaleY = scale
                    alpha = if (halo != null) 1f - 0.55f * beat else 0.55f
                }
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = CallDimens.HaloAmbientAlpha),
                                Color.Transparent
                            ),
                            center = center,
                            radius = this.size.minDimension / 2f
                        )
                    )
                }
        )
        Box(
            Modifier
                .size(size + CallDimens.AvatarRingGap * 2)
                .border(CallDimens.BorderRing, scheme.outlineVariant, portraitShape)
                .padding(CallDimens.AvatarRingGap)
        ) {
            UserAvatar(
                avatarUrl = session.participant.avatarUrl,
                size = size,
                displayName = session.participant.displayName,
                shape = portraitShape,
                initialsTextStyle = initialsStyle
            )
        }
    }
}

@Composable
fun CallStatusText(
    session: CallSession,
    elapsedMs: Long,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    debugToolsEnabled: Boolean = false,
    onOpenDebug: () -> Unit = {}
) {
    val showDuration = session.connectedAtMillis != null && session.phase != CallPhase.ENDED
    val text = if (showDuration) formatCallDuration(elapsedMs) else callPhaseLabel(session)
    val description = if (showDuration) spokenDuration(elapsedMs) else text
    val debugLabel = stringResource(R.string.call_debug_open)

    Text(
        text = text,
        style = style.copy(fontFeatureSettings = TABULAR_FEATURE),
        color = color,
        textAlign = textAlign,
        maxLines = 1,
        modifier = modifier
            .callDebugGesture(debugToolsEnabled, debugLabel, onOpenDebug)
            .semantics {
                contentDescription = description
                if (!showDuration) liveRegion = LiveRegionMode.Polite
            }
    )
}

@Composable
fun callPhaseLabel(session: CallSession): String {
    val isVideo = session.mediaType == CallMediaType.VIDEO
    return when {
        session.direction == CallDirection.INCOMING && session.phase == CallPhase.RINGING ->
            stringResource(if (isVideo) R.string.call_incoming_video else R.string.call_incoming_voice)

        session.phase == CallPhase.DIALING -> stringResource(R.string.call_phase_calling)
        session.phase == CallPhase.RINGING -> stringResource(R.string.call_phase_ringing)
        session.phase == CallPhase.CONNECTING -> stringResource(R.string.call_phase_connecting)
        session.phase == CallPhase.RECONNECTING -> stringResource(R.string.call_phase_reconnecting)
        else -> stringResource(R.string.call_ended_title)
    }
}

@Composable
fun formatCallDuration(elapsedMs: Long): String =
    DateUtils.formatElapsedTime((elapsedMs / 1000L).coerceAtLeast(0L))

@Composable
fun spokenDuration(elapsedMs: Long): String {
    val totalSeconds = (elapsedMs / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0L -> stringResource(R.string.call_duration_spoken_hours, hours, minutes, seconds)
        minutes > 0L -> stringResource(R.string.call_duration_spoken_minutes, minutes, seconds)
        else -> stringResource(R.string.call_duration_spoken_seconds, seconds)
    }
}

@Composable
fun CallStatusChips(
    session: CallSession,
    modifier: Modifier = Modifier,
    overVideo: Boolean = false,
    showReconnecting: Boolean = true
) {
    val chips = buildList {
        if (session.isMuted) {
            add(
                CallChip(
                    icon = Icons.Rounded.MicOff,
                    label = stringResource(R.string.call_state_muted),
                    alarming = false
                )
            )
        }
        when (session.connectionQuality) {
            CallConnectionQuality.FAIR -> add(
                CallChip(
                    icon = Icons.Rounded.SignalCellularAlt,
                    label = stringResource(R.string.call_state_weak_connection),
                    alarming = false
                )
            )

            CallConnectionQuality.POOR -> add(
                CallChip(
                    icon = Icons.Rounded.SignalCellularConnectedNoInternet4Bar,
                    label = stringResource(R.string.call_state_poor_connection),
                    alarming = true
                )
            )

            CallConnectionQuality.GOOD -> Unit
        }
        if (showReconnecting && session.phase == CallPhase.RECONNECTING) {
            add(
                CallChip(
                    icon = null,
                    label = stringResource(R.string.call_phase_reconnecting),
                    alarming = true,
                    inProgress = true
                )
            )
        }
    }
    if (chips.isEmpty()) return

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.space8, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(Spacing.space8)
    ) {
        chips.forEach { chip -> CallChipView(chip = chip, overVideo = overVideo) }
    }
}

@Composable
fun CallProgressPill(
    text: String,
    modifier: Modifier = Modifier
) {
    val content = callMediaTextColor()
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = CallDimens.PillSurfaceAlpha))
            .padding(horizontal = Spacing.space16, vertical = Spacing.space8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.space8)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(CallDimens.ChipSpinner),
            color = content,
            strokeWidth = CallDimens.SpinnerStroke
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = content,
            maxLines = 1
        )
    }
}

/**
 * Keeps status and navigation bar icons readable for the active surface, restores the previous
 * appearance on dispose, and holds the screen on while a call is live.
 */
@Composable
fun CallSystemBarEffect(useLightIcons: Boolean, keepScreenOn: Boolean) {
    val context = LocalContext.current
    val view = LocalView.current
    val window = remember(context) { context.findActivity()?.window }

    DisposableEffect(window, view, useLightIcons) {
        if (window == null) {
            onDispose { }
        } else {
            val controller = WindowCompat.getInsetsController(window, view)
            val previousStatus = controller.isAppearanceLightStatusBars
            val previousNavigation = controller.isAppearanceLightNavigationBars
            controller.isAppearanceLightStatusBars = !useLightIcons
            controller.isAppearanceLightNavigationBars = !useLightIcons
            onDispose {
                controller.isAppearanceLightStatusBars = previousStatus
                controller.isAppearanceLightNavigationBars = previousNavigation
            }
        }
    }

    DisposableEffect(window, keepScreenOn) {
        val flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        if (keepScreenOn) window?.addFlags(flags) else window?.clearFlags(flags)
        onDispose { window?.clearFlags(flags) }
    }
}

/** Debug-only long-press plus an equivalent accessibility action on the call status. */
@Composable
fun Modifier.callDebugGesture(
    enabled: Boolean,
    label: String,
    onOpen: () -> Unit
): Modifier = if (!enabled) {
    this
} else {
    this
        .pointerInput(onOpen) { detectTapGestures(onLongPress = { onOpen() }) }
        .semantics {
            customActions = listOf(CustomAccessibilityAction(label) { onOpen(); true })
        }
}

@Composable
fun CallChipView(chip: CallChip, overVideo: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val mediaText = callMediaTextColor()
    val container = when {
        chip.alarming -> scheme.errorContainer
        overVideo -> scheme.scrim.copy(alpha = CallDimens.ControlSurfaceAlpha)
        else -> scheme.surfaceContainerHigh
    }
    val foreground = when {
        chip.alarming -> scheme.onErrorContainer
        overVideo -> mediaText
        else -> scheme.onSurface
    }

    Row(
        modifier = Modifier
            .heightIn(min = CallDimens.ChipMinHeight)
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = Spacing.space12, vertical = Spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.space4)
    ) {
        if (chip.inProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(CallDimens.ChipSpinner),
                color = foreground,
                strokeWidth = CallDimens.SpinnerStroke
            )
        } else {
            chip.icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.size(CallDimens.ChipIcon),
                    tint = foreground
                )
            }
        }
        Text(
            text = chip.label,
            style = MaterialTheme.typography.labelMedium,
            color = foreground,
            maxLines = 1
        )
    }
}

@Composable
private fun ringingHaloState(): State<Float>? {
    if (AnimationUtil.rememberReducedMotion()) return null
    val transition = rememberInfiniteTransition(label = "callRingingHalo")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(CallDimens.HaloCycleMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "callRingingHaloProgress"
    )
}

private fun Context.findActivity(): Activity? {
    var context: Context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

data class CallChip(
    val icon: ImageVector?,
    val label: String,
    val alarming: Boolean,
    val inProgress: Boolean = false
)

private const val TABULAR_FEATURE = "tnum"
