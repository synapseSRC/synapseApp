package com.synapse.social.studioasinc.feature.echo.lobby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.feature.echo.toEchoErrorKind
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoErrorKind
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoVisibility
import com.synapse.social.studioasinc.shared.domain.usecase.echo.CreateEchoRoomUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.echo.LoadEchoLobbyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.aakira.napier.Napier
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The lobby's one source of truth.
 *
 * The lobby is not realtime by design: rooms come from a single RPC that already expires stale
 * ones, and the screen reads again whenever it becomes visible or the user pulls. A room that goes
 * live while the user is staring at the list appears on the next pull, which is honest about the
 * cost of keeping a public list subscribed.
 */
@HiltViewModel
class EchoLobbyViewModel @Inject constructor(
    private val loadLobby: LoadEchoLobbyUseCase,
    private val createRoom: CreateEchoRoomUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EchoLobbyUiState())
    val uiState: StateFlow<EchoLobbyUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun refresh() {
        // A pull while the first read is still in flight replaces it; two interleaved responses
        // would let the older one land last.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val hadLoaded = _uiState.value.hasLoaded
            _uiState.update {
                it.copy(isLoading = !hadLoaded, isRefreshing = hadLoaded, error = null)
            }
            loadLobby()
                .onSuccess { rooms ->
                    _uiState.update {
                        it.copy(
                            rooms = rooms,
                            isLoading = false,
                            isRefreshing = false,
                            hasLoaded = true,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    Napier.w("Echo lobby load failed: ${error.message}", tag = TAG)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            hasLoaded = true,
                            error = error.toEchoErrorKind()
                        )
                    }
                }
        }
    }

    // ---- Create ------------------------------------------------------------

    fun openCreateSheet() {
        _uiState.update { it.copy(create = EchoCreateUiState(isOpen = true)) }
    }

    fun dismissCreateSheet() {
        if (_uiState.value.create.isSubmitting) return
        _uiState.update { it.copy(create = EchoCreateUiState()) }
    }

    fun updateCreateTitle(title: String) {
        val clean = title.take(EchoCreateUiState.MAX_TITLE_LENGTH)
        _uiState.update { it.copy(create = it.create.copy(title = clean, error = null)) }
    }

    fun updateCreateTopic(topic: String) {
        val clean = topic.take(EchoCreateUiState.MAX_TOPIC_LENGTH)
        _uiState.update { it.copy(create = it.create.copy(topic = clean, error = null)) }
    }

    fun updateCreateVisibility(visibility: EchoVisibility) {
        _uiState.update { it.copy(create = it.create.copy(visibility = visibility)) }
    }

    fun updateCreateMaxSpeakers(maxSpeakers: Int) {
        val clamped = maxSpeakers.coerceIn(EchoCreateUiState.MIN_SPEAKERS, EchoCreateUiState.MAX_SPEAKERS)
        _uiState.update { it.copy(create = it.create.copy(maxSpeakers = clamped)) }
    }

    fun submitCreate() {
        val create = _uiState.value.create
        if (create.isSubmitting) return
        if (create.title.isBlank()) {
            _uiState.update { it.copy(create = it.create.copy(error = EchoErrorKind.INVALID_INPUT)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(create = it.create.copy(isSubmitting = true, error = null)) }
            createRoom(
                title = create.title,
                topic = create.topic.takeIf { it.isNotBlank() },
                visibility = create.visibility,
                maxSpeakers = create.maxSpeakers
            )
                .onSuccess { room ->
                    // Membership is written by the RPC; the room screen joins idempotently and is
                    // where the microphone is asked for and audio starts.
                    _uiState.update {
                        it.copy(
                            create = EchoCreateUiState(),
                            pendingRoomId = room.id,
                            pendingSpeakOnEntry = true
                        )
                    }
                }
                .onFailure { error ->
                    Napier.w("Echo room create failed: ${error.message}", tag = TAG)
                    _uiState.update {
                        it.copy(create = it.create.copy(isSubmitting = false, error = error.toEchoErrorKind()))
                    }
                }
        }
    }

    fun consumePendingRoom() {
        _uiState.update { it.copy(pendingRoomId = null, pendingSpeakOnEntry = false) }
    }

    private companion object {
        const val TAG = "Echo"
    }
}
