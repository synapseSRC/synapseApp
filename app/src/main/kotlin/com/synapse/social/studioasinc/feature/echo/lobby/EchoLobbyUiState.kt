package com.synapse.social.studioasinc.feature.echo.lobby

import com.synapse.social.studioasinc.shared.domain.model.echo.EchoErrorKind
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRoomSummary
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoVisibility

data class EchoLobbyUiState(
    val rooms: List<EchoRoomSummary> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasLoaded: Boolean = false,
    /** Set when the last read failed; a list already on screen is kept and this explains why. */
    val error: EchoErrorKind? = null,
    val create: EchoCreateUiState = EchoCreateUiState(),
    /** A room just created; the screen navigates into it and clears this. */
    val pendingRoomId: String? = null,
    /** The pending room is one the caller hosts, so entering it should ask for the microphone. */
    val pendingSpeakOnEntry: Boolean = false
)

data class EchoCreateUiState(
    val isOpen: Boolean = false,
    val title: String = "",
    val topic: String = "",
    val visibility: EchoVisibility = EchoVisibility.PUBLIC,
    val maxSpeakers: Int = DEFAULT_MAX_SPEAKERS,
    val isSubmitting: Boolean = false,
    val error: EchoErrorKind? = null
) {
    companion object {
        const val MAX_TITLE_LENGTH = 80
        const val MAX_TOPIC_LENGTH = 200
        const val MIN_SPEAKERS = 2
        const val MAX_SPEAKERS = 20
        const val DEFAULT_MAX_SPEAKERS = 8
    }
}
