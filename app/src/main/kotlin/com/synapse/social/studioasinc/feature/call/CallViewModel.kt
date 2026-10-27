package com.synapse.social.studioasinc.feature.call

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.synapse.social.studioasinc.data.call.CallSimulationAction
import com.synapse.social.studioasinc.data.call.CallSimulationController
import com.synapse.social.studioasinc.shared.domain.model.call.CallAudioRoute
import com.synapse.social.studioasinc.shared.domain.model.call.CallDirection
import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import com.synapse.social.studioasinc.shared.domain.model.call.CallParticipant
import com.synapse.social.studioasinc.shared.domain.model.call.CallPhase
import com.synapse.social.studioasinc.shared.domain.model.call.CallRequest
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession
import com.synapse.social.studioasinc.shared.domain.repository.CallRepository
import com.synapse.social.studioasinc.ui.navigation.AppDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class CallViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val callRepository: CallRepository,
    private val simulationController: CallSimulationController
) : ViewModel() {

    private val route = savedStateHandle.toRoute<AppDestination.Call>()

    private val _uiState = MutableStateFlow(CallUiState())
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    private var callId: String? = null
    private var sessionJob: Job? = null
    private var tickerJob: Job? = null

    init {
        startCall(
            CallRequest(
                participant = CallParticipant(
                    userId = route.peerUserId,
                    displayName = route.peerName,
                    avatarUrl = route.peerAvatarUrl
                ),
                chatId = route.chatId,
                direction = if (route.isIncoming) CallDirection.INCOMING else CallDirection.OUTGOING,
                mediaType = if (route.isVideo) CallMediaType.VIDEO else CallMediaType.VOICE
            )
        )
    }

    fun accept() = mutate { callRepository.accept(it) }

    fun decline() = mutate { callRepository.decline(it) }

    fun end() = mutate { callRepository.end(it) }

    fun toggleMute() {
        val session = currentSession() ?: return
        mutate { callRepository.setMuted(it, !session.isMuted) }
    }

    fun toggleSpeaker() {
        val session = currentSession() ?: return
        selectAudioRoute(
            if (session.audioRoute == CallAudioRoute.SPEAKER) CallAudioRoute.EARPIECE
            else CallAudioRoute.SPEAKER
        )
    }

    fun selectAudioRoute(audioRoute: CallAudioRoute) =
        mutate { callRepository.setAudioRoute(it, audioRoute) }

    fun toggleCamera() {
        val session = currentSession() ?: return
        mutate { callRepository.setLocalVideoEnabled(it, !session.isLocalVideoEnabled) }
    }

    fun flipCamera() = mutate { callRepository.flipCamera(it) }

    fun switchMediaType(mediaType: CallMediaType) {
        val session = currentSession() ?: return
        val dialingOut = session.direction == CallDirection.OUTGOING &&
            (session.phase == CallPhase.DIALING || session.phase == CallPhase.RINGING)
        if (dialingOut) {
            // Nothing is connected yet, so re-dial as the other media type instead of no-oping.
            startCall(
                CallRequest(
                    participant = session.participant,
                    chatId = session.chatId,
                    direction = CallDirection.OUTGOING,
                    mediaType = mediaType
                )
            )
            return
        }
        mutate { callRepository.switchMediaType(it, mediaType) }
    }

    fun simulate(action: CallSimulationAction) = simulationController.simulate(action)

    fun redial() {
        val session = currentSession() ?: return
        startCall(
            CallRequest(
                participant = session.participant,
                chatId = session.chatId,
                direction = CallDirection.OUTGOING,
                mediaType = session.mediaType
            )
        )
    }

    /** Ends the live session before leaving so no ghost call survives the screen. */
    fun endAndClose() {
        val id = callId
        viewModelScope.launch {
            if (id != null) callRepository.end(id)
            _uiState.update { it.copy(closeRequested = true) }
        }
    }

    private fun currentSession(): CallSession? = _uiState.value.session

    private fun mutate(action: suspend (String) -> Result<Unit>) {
        val id = callId ?: return
        viewModelScope.launch { action(id) }
    }

    private fun startCall(request: CallRequest) {
        // Drop the previous subscription first so a replaced session can't publish a null frame.
        sessionJob?.cancel()
        tickerJob?.cancel()
        viewModelScope.launch {
            callRepository.start(request)
                .onSuccess { session ->
                    callId = session.id
                    _uiState.value = CallUiState(session = session)
                    observe(session.id)
                }
                .onFailure {
                    _uiState.value = CallUiState(startFailed = true)
                }
        }
    }

    private fun observe(id: String) {
        sessionJob?.cancel()
        tickerJob?.cancel()
        tickerJob = null
        sessionJob = viewModelScope.launch {
            callRepository.observeSession(id).collect { session ->
                _uiState.update { it.copy(session = session) }
                syncTicker(session)
            }
        }
    }

    private fun syncTicker(session: CallSession?) {
        val connectedAt = session?.connectedAtMillis
        if (connectedAt == null) {
            tickerJob?.cancel()
            tickerJob = null
            _uiState.update { it.copy(elapsedMs = 0L) }
            return
        }
        if (session.phase == CallPhase.ENDED) {
            tickerJob?.cancel()
            tickerJob = null
            val endedAt = session.endedAtMillis ?: connectedAt
            _uiState.update { it.copy(elapsedMs = (endedAt - connectedAt).coerceAtLeast(0L)) }
            return
        }
        if (tickerJob?.isActive == true) return
        tickerJob = viewModelScope.launch {
            while (isActive) {
                val startedAt = _uiState.value.session?.connectedAtMillis ?: break
                _uiState.update {
                    it.copy(elapsedMs = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L))
                }
                delay(TICK_INTERVAL_MS)
            }
        }
    }

    private companion object {
        const val TICK_INTERVAL_MS = 1_000L
    }
}
