package com.synapse.social.studioasinc.feature.call

import com.synapse.social.studioasinc.data.call.CallSimulationAction
import com.synapse.social.studioasinc.shared.domain.model.call.CallAudioRoute
import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import com.synapse.social.studioasinc.shared.domain.model.call.CallPermission
import com.synapse.social.studioasinc.shared.domain.model.call.CallPermissionOutcome
import com.synapse.social.studioasinc.shared.domain.model.call.CallPermissionPlan
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession

data class CallUiState(
    val session: CallSession? = null,
    val elapsedMs: Long = 0L,
    val startFailed: Boolean = false,
    val closeRequested: Boolean = false,
    /**
     * A call that is stopped at a permission, or null when nothing is standing in the way.
     *
     * Held as state rather than surfaced as a dialog because the answer may need the system dialog,
     * may need Settings, or may need neither — and the screen has to be able to say which after the
     * user has already turned the phone back over.
     */
    val permissionPrompt: CallPermissionPrompt? = null
)

/**
 * The permission question the screen owes the platform.
 *
 * [requestKey] exists because a retry has to look like a new event: the same plan asking the same
 * permissions again would otherwise be a state the screen has already seen, and Compose would not
 * re-run the launcher.
 */
data class CallPermissionPrompt(
    val plan: CallPermissionPlan,
    val requestKey: Int
)

/** Every intent the call surface can raise. Sheet visibility stays in the composable layer. */
data class CallActions(
    val onAccept: () -> Unit = {},
    val onDecline: () -> Unit = {},
    val onEnd: () -> Unit = {},
    val onEndAndClose: () -> Unit = {},
    val onToggleMute: () -> Unit = {},
    val onToggleSpeaker: () -> Unit = {},
    val onOpenAudioSheet: () -> Unit = {},
    val onSelectAudioRoute: (CallAudioRoute) -> Unit = {},
    val onToggleCamera: () -> Unit = {},
    val onFlipCamera: () -> Unit = {},
    val onSwitchMediaType: (CallMediaType) -> Unit = {},
    val onOpenDebugSheet: () -> Unit = {},
    val onSimulate: (CallSimulationAction) -> Unit = {},
    val onRedial: () -> Unit = {},
    val onAskPermissions: () -> Unit = {},
    val onPermissionsAnswered: (Map<CallPermission, CallPermissionOutcome>) -> Unit = {},
    val onOpenAppSettings: () -> Unit = {}
)
