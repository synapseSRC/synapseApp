package com.synapse.social.studioasinc.feature.call.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.components.pressScale
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.raisedControlColor
import com.synapse.social.studioasinc.shared.domain.model.call.CallAudioRoute

/**
 * A call control: 56dp circle with a two-line label that is part of the same touch target and
 * accessibility node. Project design decision (not in M3): labels stay visible at all times so a
 * control never relies on an icon alone.
 */
@Composable
fun CallControlButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    stateDescription: String? = null,
    containerColor: Color = raisedControlColor(),
    selectedContainerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    selectedContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    labelColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val interactionSource = remember { MutableInteractionSource() }
    val background = if (selected) selectedContainerColor else containerColor
    val foreground = if (selected) selectedContentColor else contentColor

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .widthIn(min = CallDimens.ControlLabelWidth, max = CallDimens.ControlLabelMaxWidth)
            .pressScale(interactionSource)
            .clip(RoundedCornerShape(Sizes.CornerDefault))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                onClick = onClick
            )
            .semanticsMerge(label, stateDescription, selected)
            .padding(vertical = Spacing.space4)
    ) {
        Box(
            modifier = Modifier
                .size(CallDimens.Control)
                .clip(CircleShape)
                .background(background),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(CallDimens.ControlIcon),
                tint = foreground
            )
        }
        Spacer(Modifier.height(Spacing.space4))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(hyphens = Hyphens.None),
            color = labelColor,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
fun CallControlRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.space8, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(Spacing.space4),
        maxItemsInEachRow = 4
    ) {
        content()
    }
}

/** The deliberately distinct, always-destructive end control. */
@Composable
fun EndCallButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val label = stringResource(R.string.call_control_end)
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.space8, Alignment.CenterHorizontally),
        modifier = modifier
            .heightIn(min = CallDimens.EndPillHeight)
            .widthIn(min = CallDimens.EndPillMinWidth)
            .pressScale(interactionSource)
            .clip(CircleShape)
            .background(scheme.error)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                onClick = onClick
            )
            .semanticsMerge(label, stateDescription = null, selected = false)
            .padding(horizontal = Spacing.space24, vertical = Spacing.space12)
    ) {
        Icon(
            imageVector = CallIcons.endCall,
            contentDescription = null,
            modifier = Modifier.size(CallDimens.ControlIcon),
            tint = scheme.onError
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = scheme.onError,
            maxLines = 1
        )
    }
}

/**
 * The rounded bottom sheet the active-call controls sit on, so the control circles read as buttons
 * on a surface instead of floating loose over the page or the video frame.
 */
@Composable
fun CallControlPanel(
    modifier: Modifier = Modifier,
    overMedia: Boolean = false,
    content: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Sizes.CornerExtraLarge),
        color = if (overMedia) {
            scheme.scrim.copy(alpha = CallDimens.ControlSurfaceAlpha)
        } else {
            scheme.surfaceContainerHigh
        }
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.space16, vertical = Spacing.space8),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

/** Compact pill used for the debug-only media switch over video chrome. */
@Composable
fun CallSwitchChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val content = callMediaTextColor()
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.space4, Alignment.CenterHorizontally),
        modifier = modifier
            .heightIn(min = CallDimens.DebugChipMinHeight)
            .pressScale(interactionSource)
            .clip(CircleShape)
            .background(scheme.scrim.copy(alpha = CallDimens.ControlSurfaceAlpha))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                onClick = onClick
            )
            .semanticsMerge(label, stateDescription = null, selected = false)
            .padding(horizontal = Spacing.space12, vertical = Spacing.space4)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(CallDimens.DebugChipIcon),
            tint = content
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            maxLines = 1
        )
    }
}

@Composable
fun callAudioRouteIcon(route: CallAudioRoute): ImageVector = when (route) {
    CallAudioRoute.EARPIECE -> CallIcons.earpiece
    CallAudioRoute.SPEAKER -> CallIcons.speakerOn
    CallAudioRoute.BLUETOOTH -> CallIcons.bluetooth
    CallAudioRoute.WIRED_HEADSET -> CallIcons.wiredHeadset
}

@Composable
fun callAudioRouteLabel(route: CallAudioRoute): String = stringResource(
    when (route) {
        CallAudioRoute.EARPIECE -> R.string.call_audio_route_earpiece
        CallAudioRoute.SPEAKER -> R.string.call_audio_route_speaker
        CallAudioRoute.BLUETOOTH -> R.string.call_audio_route_bluetooth
        CallAudioRoute.WIRED_HEADSET -> R.string.call_audio_route_wired
    }
)

private fun Modifier.semanticsMerge(
    label: String,
    stateDescription: String?,
    selected: Boolean
): Modifier = semantics(mergeDescendants = true) {
    this.contentDescription = label
    stateDescription?.let { this.stateDescription = it }
    this.selected = selected
    this.role = Role.Button
}
