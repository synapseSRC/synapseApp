package com.synapse.social.studioasinc.feature.call

import com.synapse.social.studioasinc.data.call.CallSimulationAction
import com.synapse.social.studioasinc.shared.domain.model.call.CallAudioRoute
import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession

data class CallUiState(
    val session: CallSession? = null,
    val elapsedMs: Long = 0L,
    val startFailed: Boolean = false,
    val closeRequested: Boolean = false
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
    val onRedial: () -> Unit = {}
)
