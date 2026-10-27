package com.synapse.social.studioasinc.feature.call.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.data.call.CallSimulationAction
import com.synapse.social.studioasinc.feature.shared.theme.AccessibilitySizes
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.call.CallAudioRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallAudioRouteSheet(
    currentRoute: CallAudioRoute,
    onSelectRoute: (CallAudioRoute) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(Modifier.padding(bottom = Spacing.space24)) {
            Text(
                text = stringResource(R.string.call_audio_route_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = Spacing.space24,
                    end = Spacing.space24,
                    top = Spacing.space8,
                    bottom = Spacing.space16
                )
            )
            AudioRouteRow(
                route = CallAudioRoute.EARPIECE,
                selected = currentRoute == CallAudioRoute.EARPIECE,
                onClick = {
                    onSelectRoute(CallAudioRoute.EARPIECE)
                    onDismiss()
                }
            )
            AudioRouteRow(
                route = CallAudioRoute.SPEAKER,
                selected = currentRoute == CallAudioRoute.SPEAKER,
                onClick = {
                    onSelectRoute(CallAudioRoute.SPEAKER)
                    onDismiss()
                }
            )
            AudioRouteRow(
                route = CallAudioRoute.BLUETOOTH,
                selected = false,
                enabled = false,
                onClick = {}
            )
            AudioRouteRow(
                route = CallAudioRoute.WIRED_HEADSET,
                selected = false,
                enabled = false,
                onClick = {}
            )
        }
    }
}

@Composable
private fun AudioRouteRow(
    route: CallAudioRoute,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme
    val foreground = if (enabled) scheme.onSurface else scheme.onSurfaceVariant

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.ButtonHeightM3Medium)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Spacing.space24, vertical = Spacing.space8)
    ) {
        Icon(
            imageVector = callAudioRouteIcon(route),
            contentDescription = null,
            modifier = Modifier.size(Sizes.IconLarge),
            tint = foreground
        )
        Spacer(Modifier.width(Spacing.space16))
        Column(Modifier.weight(1f)) {
            Text(
                text = callAudioRouteLabel(route),
                style = MaterialTheme.typography.bodyLarge,
                color = foreground
            )
            if (!enabled) {
                Text(
                    text = stringResource(R.string.call_audio_route_not_connected),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
        RadioButton(selected = selected, onClick = null, enabled = enabled)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallDebugSheet(
    onSimulate: (CallSimulationAction) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(Modifier.navigationBarsPadding().padding(bottom = Spacing.space16)) {
            Text(
                text = stringResource(R.string.call_debug_tools_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = Spacing.space24,
                    end = Spacing.space24,
                    top = Spacing.space8
                )
            )
            Text(
                text = stringResource(R.string.call_debug_tools_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = Spacing.space24,
                    end = Spacing.space24,
                    top = Spacing.space4,
                    bottom = Spacing.space12
                )
            )
            DebugActionRow(CallIcons.qualityGood, stringResource(R.string.call_debug_quality_good)) {
                onSimulate(CallSimulationAction.GOOD_CONNECTION)
            }
            DebugActionRow(CallIcons.qualityWeak, stringResource(R.string.call_debug_quality_weak)) {
                onSimulate(CallSimulationAction.WEAK_CONNECTION)
            }
            DebugActionRow(CallIcons.qualityPoor, stringResource(R.string.call_debug_quality_poor)) {
                onSimulate(CallSimulationAction.POOR_CONNECTION)
            }
            DebugActionRow(CallIcons.reconnectNow, stringResource(R.string.call_debug_reconnect)) {
                onSimulate(CallSimulationAction.RECONNECT)
            }
            DebugActionRow(CallIcons.videoOff, stringResource(R.string.call_debug_remote_video_off)) {
                onSimulate(CallSimulationAction.REMOTE_VIDEO_OFF)
            }
            DebugActionRow(CallIcons.videoOn, stringResource(R.string.call_debug_remote_video_on)) {
                onSimulate(CallSimulationAction.REMOTE_VIDEO_ON)
            }
            DebugActionRow(CallIcons.endCall, stringResource(R.string.call_debug_remote_declines)) {
                onSimulate(CallSimulationAction.REMOTE_DECLINES)
            }
            DebugActionRow(CallIcons.missed, stringResource(R.string.call_debug_no_answer)) {
                onSimulate(CallSimulationAction.NO_ANSWER)
            }
            DebugActionRow(CallIcons.unavailable, stringResource(R.string.call_debug_unavailable)) {
                onSimulate(CallSimulationAction.UNAVAILABLE)
            }
            DebugActionRow(CallIcons.endCall, stringResource(R.string.call_debug_remote_ends)) {
                onSimulate(CallSimulationAction.REMOTE_ENDS)
            }
        }
    }
}

@Composable
private fun DebugActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AccessibilitySizes.minimumTouchTarget)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.space24, vertical = Spacing.space12)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(Sizes.IconLarge),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(Spacing.space16))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun CallEndConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.call_end_dialog_title)) },
        text = { Text(stringResource(R.string.call_end_dialog_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.call_end_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.call_end_dialog_dismiss))
            }
        }
    )
}
