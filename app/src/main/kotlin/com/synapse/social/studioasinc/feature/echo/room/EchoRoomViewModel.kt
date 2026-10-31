package com.synapse.social.studioasinc.feature.echo.room

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.synapse.social.studioasinc.core.util.EchoNotificationContent
import com.synapse.social.studioasinc.data.echo.EchoMediaHold
import com.synapse.social.studioasinc.feature.echo.toEchoErrorKind
import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import com.synapse.social.studioasinc.shared.domain.model.call.CallPermission
import com.synapse.social.studioasinc.shared.domain.model.call.CallPermissionOutcome
import com.synapse.social.studioasinc.shared.domain.model.call.CallPermissionPlan
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoErrorKind
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoReaction
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRole
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRoomDetail
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoRoomStatus
import com.synapse.social.studioasinc.shared.domain.service.EchoAudioConnection
import com.synapse.social.studioasinc.shared.domain.service.EchoAudioEngine
import com.synapse.social.studioasinc.shared.domain.usecase.call.PrepareCallPermissionsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.call.RecordCallPermissionOutcomeUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.echo.EchoHeartbeatUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.echo.EndEchoRoomUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.echo.JoinEchoRoomUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.echo.LeaveEchoRoomUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.echo.ObserveEchoRoomUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.echo.ReconnectEchoAudioUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.echo.SetEchoHandRaisedUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.echo.SetEchoRoleUseCase
import com.synapse.social.studioasinc.ui.navigation.AppDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.aakira.napier.Napier
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The room screen's one source of truth.
 *
 * Membership, the token and the connection are started in that order because each one authorises
 * the next; after that the screen is driven by two streams — the server's roster and the audio
 * transport's own state — and by a heartbeat that notices a room ending when realtime goes quiet.
 *
 * The ViewModel never trusts its own role to publish: the publish grant lives in the token, and a
 * promotion only takes effect through a freshly minted one. What the UI can do is derived from the
 * server's `myRole`, so a client cannot put itself on stage.
 */
@HiltViewModel
class EchoRoomViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val joinRoom: JoinEchoRoomUseCase,
    private val leaveRoom: LeaveEchoRoomUseCase,
    private val endRoom: EndEchoRoomUseCase,
    private val heartbeat: EchoHeartbeatUseCase,
    private val setRole: SetEchoRoleUseCase,
    private val setHandRaised: SetEchoHandRaisedUseCase,
    private val observeRoom: ObserveEchoRoomUseCase,
    private val reconnectAudio: ReconnectEchoAudioUseCase,
    private val audioEngine: EchoAudioEngine,
    private val mediaHold: EchoMediaHold,
    private val preparePermissions: PrepareCallPermissionsUseCase,
    private val recordPermissionOutcome: RecordCallPermissionOutcomeUseCase,
    @Named("ApplicationScope") private val appScope: CoroutineScope
) : ViewModel() {

    private val route = savedStateHandle.toRoute<AppDestination.EchoRoom>()
    private val roomId: String = route.roomId

    private val _uiState = MutableStateFlow(EchoRoomUiState())
    val uiState: StateFlow<EchoRoomUiState> = _uiState.asStateFlow()

    private var detailJob: Job? = null
    private var heartbeatJob: Job? = null
    private var recoveryJob: Job? = null
    private var joinJob: Job? = null

    /** Set once the membership is over for this screen: left, ended, or already torn down. */
    private var closed = false

    /** Whether the live token carries the publish grant, i.e. whether a mic can be opened at all. */
    private var tokenAllowsPublish = false

    /** True while this ViewModel is itself reconnecting, so its own teardown is not read as a dropout. */
    private var audioChangeInFlight = false

    private var reactionSequence = 0L

    init {
        observeAudio()

        if (route.speakOnEntry) {
            requestMicrophone(EchoMicPurpose.JOIN)
        } else {
            beginJoin()
        }
    }

    // ---- Joining ----------------------------------------------------------------

    private fun beginJoin() {
        if (joinJob?.isActive == true) return
        val micReady = microphonePlan().isReady
        joinJob = viewModelScope.launch {
            _uiState.update { it.copy(phase = EchoRoomPhase.JOINING, error = null) }
            joinRoom(roomId) { role ->
                // The hold is taken before the microphone is opened, so the OS decides whether a
                // publisher may keep capturing based on a notification that already exists.
                mediaHold.acquire(
                    EchoNotificationContent(
                        roomId = roomId,
                        title = _uiState.value.title.orEmpty(),
                        isPublishing = role.canPublish && micReady
                    )
                )
            }
                .onSuccess { role ->
                    tokenAllowsPublish = role.canPublish
                    if (role.canPublish && !micReady) {
                        // The join connected with a publish-capable token; keep the mic shut until
                        // the user grants it, instead of relying on the OS to refuse capture.
                        audioEngine.setMicrophoneEnabled(false)
                    }
                    _uiState.update {
                        it.copy(
                            phase = EchoRoomPhase.LIVE,
                            micEnabled = role.canPublish && micReady,
                            micBlocked = role.canPublish && !micReady && !microphonePlan().canAsk,
                            hasUsedMicrophone = role.canPublish && micReady,
                            error = null
                        )
                    }
                    startDetailObservation()
                    startHeartbeat()
                }
                .onFailure { error ->
                    Napier.w("Echo join failed: ${error.message}", tag = TAG)
                    mediaHold.release()
                    _uiState.update {
                        it.copy(phase = EchoRoomPhase.FAILED, error = error.toEchoErrorKind())
                    }
                }
        }
    }

    /** Re-reads the room immediately; a recreated channel is cheaper than trusting a silent one. */
    fun refresh() {
        if (_uiState.value.phase != EchoRoomPhase.LIVE) return
        startDetailObservation()
    }

    private fun startDetailObservation() {
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            observeRoom(roomId)
                .catch { error -> onDetailLost(error) }
                .collect { detail -> applyDetail(detail) }
        }
    }

    private fun applyDetail(detail: EchoRoomDetail) {
        val previousRole = _uiState.value.myRole
        val viewer = detail.viewerId
        _uiState.update { state ->
            state.copy(
                detail = detail,
                handRaised = detail.participants
                    .firstOrNull { viewer != null && it.userId == viewer }
                    ?.hasRaisedHand
                    ?: false,
                micBlocked = state.micBlocked && detail.myRole?.canPublish == true,
                // An audience member carries no microphone history: a later promotion is a fresh
                // invitation, even if the previous stay on stage ended in a mute.
                hasUsedMicrophone = if (detail.myRole?.canPublish == true) state.hasUsedMicrophone else false
            )
        }
        if (detail.room.status == EchoRoomStatus.ENDED) {
            finishEnded()
            return
        }
        refreshHold()

        // A demotion has to reach the transport, not just the label: the new role arrives with no
        // publish grant, and staying on the old token would keep a speaker the host just moved down.
        if (previousRole != null &&
            previousRole.canPublish &&
            detail.myRole == EchoRole.AUDIENCE &&
            tokenAllowsPublish
        ) {
            demoteConnection()
        }
    }

    private fun onDetailLost(error: Throwable) {
        val kind = error.toEchoErrorKind()
        Napier.w("Echo room read ended: ${error.message}", tag = TAG)
        if (kind == EchoErrorKind.ENDED || kind == EchoErrorKind.NOT_FOUND || kind == EchoErrorKind.FORBIDDEN) {
            finishEnded()
        } else {
            _uiState.update { it.copy(error = kind) }
        }
    }

    // ---- Audio ------------------------------------------------------------------

    private fun observeAudio() {
        viewModelScope.launch {
            audioEngine.state.collect { connection -> applyAudioState(connection) }
        }
        viewModelScope.launch {
            audioEngine.activeSpeakerIds.collect { speakers ->
                _uiState.update { it.copy(activeSpeakerIds = speakers) }
            }
        }
        viewModelScope.launch {
            audioEngine.reactions.collect { reaction -> showReaction(reaction) }
        }
    }

    private fun applyAudioState(connection: EchoAudioConnection) {
        _uiState.update {
            it.copy(
                audio = connection,
                hasConnected = it.hasConnected || connection == EchoAudioConnection.CONNECTED
            )
        }
        when (connection) {
            EchoAudioConnection.CONNECTED -> clearRecovery()
            EchoAudioConnection.DISCONNECTED -> {
                if (!closed && !audioChangeInFlight && _uiState.value.phase == EchoRoomPhase.LIVE) {
                    startRecovery()
                }
            }
            else -> Unit
        }
    }

    /**
     * Rebuilds a lost connection with a freshly minted token.
     *
     * Attempts are spaced rather than immediate: a token mint that is already failing will usually
     * keep failing for as long as the radio is down, and hammering it would only add load to a
     * reconnect the SFU is already attempting on its own.
     */
    private fun startRecovery() {
        if (recoveryJob?.isActive == true) return
        if (audioEngine.state.value != EchoAudioConnection.DISCONNECTED) return
        recoveryJob = viewModelScope.launch {
            for (attempt in RECOVERY_DELAYS_MS.indices) {
                delay(RECOVERY_DELAYS_MS[attempt])
                if (closed || _uiState.value.phase != EchoRoomPhase.LIVE) return@launch
                val role = _uiState.value.myRole ?: return@launch
                val publish = role.canPublish && _uiState.value.micEnabled
                audioChangeInFlight = true
                val result = reconnectAudio(roomId, publish)
                audioChangeInFlight = false
                if (result.isSuccess) {
                    tokenAllowsPublish = role.canPublish
                    return@launch
                }
                Napier.w("Echo audio recovery attempt ${attempt + 1} failed", tag = TAG)
            }
            if (!closed) {
                _uiState.update { it.copy(error = EchoErrorKind.AUDIO_FAILED) }
            }
        }
    }

    private fun clearRecovery() {
        recoveryJob?.cancel()
        recoveryJob = null
    }

    /** Manual retry from the connection banner: skips the first delay but keeps the backoff. */
    fun retryAudio() {
        if (closed || _uiState.value.phase != EchoRoomPhase.LIVE) return
        clearRecovery()
        _uiState.update { it.copy(error = null) }
        startRecovery()
    }

    private fun demoteConnection() {
        viewModelScope.launch {
            audioChangeInFlight = true
            val result = reconnectAudio(roomId, publish = false)
            audioChangeInFlight = false
            if (result.isSuccess) {
                tokenAllowsPublish = false
                _uiState.update { it.copy(micEnabled = false, error = null) }
                refreshHold()
            } else {
                Napier.w("Echo demotion reconnect failed: ${result.exceptionOrNull()?.message}", tag = TAG)
            }
        }
    }

    // ---- Microphone -------------------------------------------------------------

    private fun microphonePlan() = preparePermissions(CallMediaType.VOICE)

    /** The screen's request to open the microphone, either for the first time or after a refusal. */
    fun requestMicrophone() {
        if (_uiState.value.phase != EchoRoomPhase.LIVE) return
        requestMicrophone(EchoMicPurpose.UNMUTE)
    }

    private fun requestMicrophone(purpose: EchoMicPurpose) {
        val plan = microphonePlan()
        if (plan.isReady) {
            when (purpose) {
                EchoMicPurpose.JOIN -> beginJoin()
                EchoMicPurpose.SPEAK, EchoMicPurpose.UNMUTE -> enableMicrophone()
            }
            return
        }
        publishPrompt(plan, purpose)
    }

    private fun publishPrompt(plan: CallPermissionPlan, purpose: EchoMicPurpose) {
        val key = (_uiState.value.permissionPrompt?.requestKey ?: 0L) + 1
        _uiState.update {
            it.copy(
                permissionPrompt = EchoPermissionPrompt(plan, purpose, key),
                micBlocked = !plan.canAsk
            )
        }
    }

    /**
     * Files the user's answer and finishes whatever the question interrupted.
     *
     * A refusal never traps the user outside the room: a join continues muted, and a speaker who
     * said no stays on the roster with the microphone control offering a second try.
     */
    fun permissionsAnswered(outcomes: Map<CallPermission, CallPermissionOutcome>) {
        val prompt = _uiState.value.permissionPrompt ?: return
        recordPermissionOutcome(prompt.plan.requestable(), outcomes)
        val plan = microphonePlan()
        _uiState.update {
            it.copy(permissionPrompt = null, micBlocked = !plan.isReady && !plan.canAsk)
        }
        when (prompt.purpose) {
            EchoMicPurpose.JOIN -> beginJoin()
            EchoMicPurpose.SPEAK, EchoMicPurpose.UNMUTE -> if (plan.isReady) enableMicrophone()
        }
    }

    private fun enableMicrophone() {
        val state = _uiState.value
        if (closed || state.phase != EchoRoomPhase.LIVE) return
        val role = state.myRole ?: return
        if (!role.canPublish) return
        if (!microphonePlan().isReady) {
            requestMicrophone(EchoMicPurpose.UNMUTE)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, error = null) }
            val result = if (tokenAllowsPublish) {
                audioEngine.setMicrophoneEnabled(true)
            } else {
                // Promoted after connecting: the grant only exists in a new token.
                audioChangeInFlight = true
                val reconnected = reconnectAudio(roomId, publish = true)
                audioChangeInFlight = false
                if (reconnected.isSuccess) tokenAllowsPublish = true
                reconnected
            }
            result
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            busy = false,
                            micEnabled = true,
                            micBlocked = false,
                            hasUsedMicrophone = true
                        )
                    }
                    refreshHold()
                }
                .onFailure { error ->
                    Napier.w("Echo microphone could not be opened: ${error.message}", tag = TAG)
                    _uiState.update { it.copy(busy = false, error = error.toEchoErrorKind()) }
                }
        }
    }

    fun toggleMicrophone() {
        val state = _uiState.value
        if (state.phase != EchoRoomPhase.LIVE || !state.canSpeak) return
        if (!state.micEnabled) {
            requestMicrophone(EchoMicPurpose.UNMUTE)
            return
        }
        viewModelScope.launch {
            audioEngine.setMicrophoneEnabled(false)
                .onSuccess {
                    _uiState.update { it.copy(micEnabled = false, micBlocked = false) }
                    refreshHold()
                }
                .onFailure { error ->
                    Napier.w("Echo microphone could not be muted: ${error.message}", tag = TAG)
                }
        }
    }

    // ---- Audience + host controls ----------------------------------------------

    fun toggleHand() {
        val state = _uiState.value
        if (state.phase != EchoRoomPhase.LIVE || state.myRole != EchoRole.AUDIENCE) return
        val raised = !state.handRaised
        viewModelScope.launch {
            setHandRaised(roomId, raised)
                .onSuccess { _uiState.update { it.copy(handRaised = raised) } }
                .onFailure { error ->
                    Napier.w("Echo hand could not be lowered or raised: ${error.message}", tag = TAG)
                    _uiState.update { it.copy(error = error.toEchoErrorKind()) }
                }
        }
    }

    fun openParticipantMenu(userId: String) {
        val detail = _uiState.value.detail ?: return
        if (!detail.isHost) return
        if (userId == detail.room.hostId || detail.isViewer(userId)) return
        _uiState.update { it.copy(participantMenu = userId) }
    }

    fun dismissParticipantMenu() {
        _uiState.update { it.copy(participantMenu = null) }
    }

    fun setParticipantRole(userId: String, role: EchoRole) {
        if (closed || _uiState.value.phase != EchoRoomPhase.LIVE || !_uiState.value.isHost) return
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, participantMenu = null, error = null) }
            setRole(roomId, userId, role)
                .onSuccess { startDetailObservation() }
                .onFailure { error ->
                    Napier.w("Echo role change failed: ${error.message}", tag = TAG)
                    _uiState.update { it.copy(error = error.toEchoErrorKind()) }
                }
            _uiState.update { it.copy(busy = false) }
        }
    }

    fun sendReaction(emoji: String) {
        if (_uiState.value.phase != EchoRoomPhase.LIVE) return
        if (audioEngine.state.value != EchoAudioConnection.CONNECTED) return
        viewModelScope.launch {
            // The SFU does not echo a member's own data packet back, so the sender's bubble is
            // drawn here; remote bubbles arrive through the engine's reaction flow.
            showReaction(
                EchoReaction(
                    userId = _uiState.value.detail?.viewerId.orEmpty(),
                    emoji = emoji
                )
            )
            audioEngine.sendReaction(emoji).onFailure { error ->
                Napier.w("Echo reaction could not be sent: ${error.message}", tag = TAG)
            }
        }
    }

    // ---- Ending and leaving -----------------------------------------------------

    fun endRoomForEveryone() {
        val state = _uiState.value
        if (closed || state.phase != EchoRoomPhase.LIVE || !state.isHost) return
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, error = null) }
            endRoom(roomId)
                .onSuccess { finishEnded() }
                .onFailure { error ->
                    Napier.w("Echo room could not be ended: ${error.message}", tag = TAG)
                    _uiState.update {
                        it.copy(busy = false, error = error.toEchoErrorKind())
                    }
                }
        }
    }

    /** Leaves the room. Audio stops immediately; the membership write survives the screen. */
    fun leave() {
        if (closed) return
        closed = true
        mediaHold.release()
        _uiState.update { it.copy(closeRequested = true) }
        appScope.launch {
            leaveRoom(roomId).onFailure { error ->
                Napier.w("Echo leave was refused: ${error.message}", tag = TAG)
            }
        }
    }

    /** Closes an ended room without writing a leave: the membership is already over. */
    fun closeEnded() {
        _uiState.update { it.copy(closeRequested = true) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun retryJoin() {
        if (_uiState.value.phase != EchoRoomPhase.FAILED) return
        closed = false
        beginJoin()
    }

    /**
     * A remote end: the room is gone, so the transport and the notification go with it.
     *
     * No leave is written — the membership belongs to a room that has already ended, and asking
     * the backend to leave it would be a refusal dressed up as cleanup.
     */
    private fun finishEnded() {
        if (closed) return
        closed = true
        clearRecovery()
        mediaHold.release()
        viewModelScope.launch { audioEngine.disconnect() }
        _uiState.update {
            it.copy(
                phase = EchoRoomPhase.ENDED,
                micEnabled = false,
                permissionPrompt = null,
                participantMenu = null,
                busy = false
            )
        }
    }

    private fun showReaction(reaction: EchoReaction) {
        val emoji = reaction.emoji.trim()
        if (emoji.isEmpty()) return
        val displayName = _uiState.value.participant(reaction.userId)
            ?.let { it.displayName ?: it.username }
        val floating = EchoFloatingReaction(
            id = ++reactionSequence,
            emoji = emoji,
            userId = reaction.userId,
            displayName = displayName
        )
        _uiState.update {
            it.copy(reactions = (it.reactions + floating).takeLast(MAX_FLOATING_REACTIONS))
        }
        viewModelScope.launch {
            delay(REACTION_LIFETIME_MS)
            _uiState.update { state ->
                state.copy(reactions = state.reactions.filterNot { item -> item.id == floating.id })
            }
        }
    }

    private fun refreshHold() {
        val state = _uiState.value
        val detail = state.detail ?: return
        if (state.phase != EchoRoomPhase.LIVE) return
        mediaHold.acquire(
            EchoNotificationContent(
                roomId = roomId,
                title = detail.room.title,
                isPublishing = state.onAir
            )
        )
    }

    // ---- Heartbeat --------------------------------------------------------------

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)
                if (closed) return@launch
                if (_uiState.value.phase != EchoRoomPhase.LIVE) continue
                heartbeat(roomId)
                    .onSuccess { status -> if (status == EchoRoomStatus.ENDED) finishEnded() }
                    .onFailure { error ->
                        val kind = error.toEchoErrorKind()
                        Napier.w("Echo heartbeat failed: ${error.message}", tag = TAG)
                        if (kind == EchoErrorKind.ENDED ||
                            kind == EchoErrorKind.NOT_FOUND ||
                            kind == EchoErrorKind.FORBIDDEN
                        ) {
                            finishEnded()
                        }
                    }
            }
        }
    }

    override fun onCleared() {
        heartbeatJob?.cancel()
        detailJob?.cancel()
        recoveryJob?.cancel()
        mediaHold.release()
        if (!closed) {
            closed = true
            // The ViewModel's own scope dies with the screen; a leave must outlive it or the
            // membership would linger until expiry.
            appScope.launch { leaveRoom(roomId) }
        }
        super.onCleared()
    }

    private companion object {
        const val TAG = "Echo"
        const val HEARTBEAT_INTERVAL_MS = 30_000L
        const val REACTION_LIFETIME_MS = 3_000L
        const val MAX_FLOATING_REACTIONS = 12
        val RECOVERY_DELAYS_MS = longArrayOf(2_000L, 5_000L, 12_000L, 20_000L)
    }
}
