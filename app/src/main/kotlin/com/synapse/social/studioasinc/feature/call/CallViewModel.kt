package com.synapse.social.studioasinc.feature.call

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.synapse.social.studioasinc.data.call.CallSimulationAction
import com.synapse.social.studioasinc.data.call.CallSimulationController
import com.synapse.social.studioasinc.shared.domain.model.call.CallAudioRoute
import com.synapse.social.studioasinc.shared.domain.model.call.CallBackendException
import com.synapse.social.studioasinc.shared.domain.model.call.CallDirection
import com.synapse.social.studioasinc.shared.domain.model.call.CallErrorKind
import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaChange
import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import com.synapse.social.studioasinc.shared.domain.model.call.CallParticipant
import com.synapse.social.studioasinc.shared.domain.model.call.CallPermission
import com.synapse.social.studioasinc.shared.domain.model.call.CallPermissionOutcome
import com.synapse.social.studioasinc.shared.domain.model.call.CallPermissionPlan
import com.synapse.social.studioasinc.shared.domain.model.call.CallPhase
import com.synapse.social.studioasinc.shared.domain.model.call.CallRequest
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession
import com.synapse.social.studioasinc.shared.domain.model.sound.SoundEffect
import com.synapse.social.studioasinc.shared.domain.usecase.call.AcceptCallUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.call.DeclineCallUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.call.EndCallUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.call.FlipCallCameraUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.call.ObserveCallSessionUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.call.PrepareCallPermissionsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.call.RecordCallPermissionOutcomeUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.call.SelectCallAudioRouteUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.call.StartCallUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.call.SwitchCallMediaTypeUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.call.ToggleCallMuteUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.call.ToggleLocalVideoUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.sound.PlaySoundEffectUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.sound.ResolveCallSoundUseCase
import com.synapse.social.studioasinc.ui.navigation.AppDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.aakira.napier.Napier
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The call screen's only source of truth.
 *
 * It talks to UseCases, never to a repository: which phases a call may pass through, what a control is
 * allowed to do, and what a refusal means are all decided below the ViewModel, so this class holds the
 * screen's *conversation* with those decisions — the elapsed timer, the close request, and the
 * bookkeeping for a call that is waiting on a permission.
 *
 * Nothing here can put a connected call on screen. The frames it renders come from the repository, and
 * the repository will not produce [CallPhase.ACTIVE] without a backend row and its own connected ICE
 * candidate pair.
 */
@HiltViewModel
class CallViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val startCall: StartCallUseCase,
    private val acceptCall: AcceptCallUseCase,
    private val declineCall: DeclineCallUseCase,
    private val endCall: EndCallUseCase,
    private val observeCallSession: ObserveCallSessionUseCase,
    private val toggleCallMute: ToggleCallMuteUseCase,
    private val selectCallAudioRoute: SelectCallAudioRouteUseCase,
    private val toggleLocalVideo: ToggleLocalVideoUseCase,
    private val flipCallCamera: FlipCallCameraUseCase,
    private val switchCallMediaType: SwitchCallMediaTypeUseCase,
    private val prepareCallPermissions: PrepareCallPermissionsUseCase,
    private val recordPermissionOutcome: RecordCallPermissionOutcomeUseCase,
    private val simulationController: CallSimulationController,
    private val playSoundEffect: PlaySoundEffectUseCase,
    private val resolveCallSound: ResolveCallSoundUseCase
) : ViewModel() {

    private val route = savedStateHandle.toRoute<AppDestination.Call>()

    /** Set when the user answered from the notification; consumed by the first ringing frame. */
    private var autoAcceptPending = route.autoAccept

    private val _uiState = MutableStateFlow(CallUiState())
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    private var callId: String? = null
    private var sessionJob: Job? = null
    private var tickerJob: Job? = null

    /** The frame cues were last decided from: a cue is a difference between two frames, not a phase name. */
    private var lastSoundedSession: CallSession? = null

    private var ringbackWatchdog: Job? = null

    /** What the user was trying to do when a permission got in the way, so granting it can finish it. */
    private var pendingOp: PendingCallOp? = null

    init {
        dial(
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

    // ---- Call lifecycle ----------------------------------------------------------

    fun accept() {
        val session = currentSession() ?: return
        gateOnPermissions(session.mediaType, PendingCallOp.Accept) { runControl { acceptCall(session) } }
    }

    fun decline() {
        val session = currentSession() ?: return
        runControl { declineCall(session) }
    }

    fun end() {
        val session = currentSession() ?: return
        runControl { endCall(session) }
    }

    /** Ends the live session before leaving so no ghost call survives the screen. */
    fun endAndClose() {
        val session = currentSession()
        viewModelScope.launch {
            endCall(session)
            _uiState.update { it.copy(closeRequested = true) }
        }
    }

    fun redial() {
        val session = currentSession() ?: return
        dial(
            CallRequest(
                participant = session.participant,
                chatId = session.chatId,
                direction = CallDirection.OUTGOING,
                mediaType = session.mediaType
            )
        )
    }

    // ---- Controls ----------------------------------------------------------------

    fun toggleMute() {
        val session = currentSession() ?: return
        runControl { toggleCallMute(session) }
    }

    fun toggleSpeaker() {
        val session = currentSession() ?: return
        selectAudioRoute(
            if (session.audioRoute == CallAudioRoute.SPEAKER) CallAudioRoute.EARPIECE
            else CallAudioRoute.SPEAKER
        )
    }

    fun selectAudioRoute(audioRoute: CallAudioRoute) {
        val session = currentSession() ?: return
        runControl { selectCallAudioRoute(session, audioRoute) }
    }

    fun toggleCamera() {
        val session = currentSession() ?: return
        runControl { toggleLocalVideo(session) }
    }

    fun flipCamera() {
        val session = currentSession() ?: return
        runControl { flipCallCamera(session) }
    }

    fun switchMediaType(mediaType: CallMediaType) {
        val session = currentSession() ?: return
        // Gate on the *target* media type: upgrading to video needs a camera this call may never have
        // been granted, and downgrading to voice needs nothing beyond the microphone it is already using.
        gateOnPermissions(mediaType, PendingCallOp.Upgrade(mediaType)) {
            runControl { applyMediaChange(session, mediaType) }
        }
    }

    fun simulate(action: CallSimulationAction) = simulationController.simulate(action)

    // ---- Permissions -------------------------------------------------------------

    /**
     * Files the user's answer and either finishes what they asked for or tells them what is left.
     *
     * Called by the screen with the outcomes it read off the platform, because only an activity knows
     * whether a denial can still be asked about. Nothing here re-opens the dialog: an answer that has
     * just been given gets buttons, not a second prompt.
     */
    fun permissionsAnswered(outcomes: Map<CallPermission, CallPermissionOutcome>) {
        val prompt = _uiState.value.permissionPrompt ?: return
        recordPermissionOutcome(prompt.plan.requestable(), outcomes)
        val plan = prepareCallPermissions(prompt.plan.mediaType)
        if (plan.isReady) {
            val op = pendingOp
            pendingOp = null
            _uiState.update { it.copy(permissionPrompt = null) }
            when (op) {
                is PendingCallOp.Dial -> launchCall(op.request)
                PendingCallOp.Accept -> currentSession()?.let { runControl { acceptCall(it) } }
                is PendingCallOp.Upgrade ->
                    currentSession()?.let { runControl { applyMediaChange(it, op.mediaType) } }

                // Nothing was parked behind the question — the user granted a permission the screen had
                // put up on its own, so the call is free to be dialled again.
                null -> Unit
            }
        } else {
            publishPlan(plan, reask = false)
        }
    }

    /** Re-asks a plan the user declined once — the only path that can open the system dialog again. */
    fun askPermissions() {
        val prompt = _uiState.value.permissionPrompt ?: return
        if (!prompt.plan.canAsk) return
        publishPlan(prompt.plan, reask = true)
    }

    /**
     * Runs [action] immediately, or parks it behind whatever [mediaType] still needs.
     *
     * The gate is here rather than only in the repository so a tap can lead to the system dialog
     * instead of a silent refusal: the repository's own check still stands behind this, and its refusal
     * arrives at [runControl], which re-publishes the plan.
     */
    private fun gateOnPermissions(mediaType: CallMediaType, op: PendingCallOp, action: () -> Unit) {
        val plan = prepareCallPermissions(mediaType)
        if (plan.isReady) {
            pendingOp = null
            _uiState.update { it.copy(permissionPrompt = null) }
            action()
        } else {
            pendingOp = op
            publishPlan(plan, reask = true)
        }
    }

    /**
     * Publishes [plan] as the thing standing between the user and their call.
     *
     * [reask] is what makes the system dialog appear. Bumping the key is the only signal the screen has
     * that this is a question the user is choosing to ask again; an answer that was just given keeps its
     * key, so a denial cannot produce a dialog that reopens itself.
     */
    private fun publishPlan(plan: CallPermissionPlan, reask: Boolean) {
        val current = _uiState.value.permissionPrompt?.requestKey ?: 0
        val key = if (reask || current == 0) current + 1 else current
        _uiState.update { it.copy(permissionPrompt = CallPermissionPrompt(plan, key)) }
    }

    private fun dial(request: CallRequest) =
        gateOnPermissions(request.mediaType, PendingCallOp.Dial(request)) { launchCall(request) }

    // ---- Session -----------------------------------------------------------------

    private fun launchCall(request: CallRequest) {
        // Drop the previous subscription first so a replaced session can't publish a null frame.
        sessionJob?.cancel()
        tickerJob?.cancel()
        viewModelScope.launch {
            startCall(request)
                .onSuccess { session ->
                    callId = session.id
                    _uiState.value = CallUiState(session = session)
                    // A dialled call is a fresh sequence: without this, a redial that lands on the same
                    // phase it started on would look like no change at all and never ring again.
                    lastSoundedSession = null
                    observe(session.id)
                }
                .onFailure { error ->
                    Napier.w("Call could not be placed: ${error.message}")
                    pendingOp = null
                    _uiState.value = CallUiState(startFailed = true)
                    // No session was ever published, so no phase edge can report this; the failure is
                    // only ever visible here.
                    val cue = if (error is CallBackendException && error.kind == CallErrorKind.BUSY) {
                        SoundEffect.CallBusy
                    } else {
                        SoundEffect.CallFailed
                    }
                    viewModelScope.launch { playSoundEffect(cue) }
                }
        }
    }

    private suspend fun applyMediaChange(session: CallSession, mediaType: CallMediaType): Result<Unit> =
        switchCallMediaType(session, mediaType).map { change ->
            when (change) {
                // The repository renegotiated on a live call; the session stream will show the result.
                is CallMediaChange.Renegotiated -> Unit

                // Nothing was connected yet, so the only honest way to change media is to place the
                // call again as that media type — the original ring was cancelled by the same call.
                is CallMediaChange.Redial -> dial(change.request)
            }
        }

    private fun observe(id: String) {
        sessionJob?.cancel()
        tickerJob?.cancel()
        tickerJob = null
        sessionJob = viewModelScope.launch {
            observeCallSession(id).collect { session ->
                val cues = resolveCallSound(lastSoundedSession, session)
                lastSoundedSession = session
                _uiState.update { it.copy(session = session) }
                syncTicker(session)
                armRingbackWatchdog(session)
                maybeAutoAccept(session)
                cues.forEach { playSoundEffect(it.effect, it.mode) }
            }
        }
    }

    /**
     * Kills the caller's own ringback.
     *
     * The only cue this screen can leave running without meaning to: by the time the platform clears a
     * ViewModel its scope is already cancelled, so a suspend call to stop the loop would be dropped with
     * the scope and the tone would outlive the screen it belonged to.
     */
    override fun onCleared() {
        playSoundEffect.stop(SoundEffect.CallRingbackLoop)
        super.onCleared()
    }

    /**
     * Stops the ringback at the backend's own ring deadline, whatever the session stream does next.
     *
     * The phase machine usually ends the loop by moving out of dialling; this is the backstop for the
     * frame that never arrives, because a stalled stream would otherwise leave the caller listening to
     * a ringer for a call that has already stopped existing. Stopping an idle loop is a no-op, so an
     * extra firing costs nothing.
     */
    private fun armRingbackWatchdog(session: CallSession?) {
        val isRinging = session?.direction == CallDirection.OUTGOING &&
            (session.phase == CallPhase.DIALING || session.phase == CallPhase.RINGING)
        val deadline = session?.serverRingTimeoutAtMillis
        if (!isRinging || deadline == null) {
            ringbackWatchdog?.cancel()
            ringbackWatchdog = null
            return
        }
        if (ringbackWatchdog?.isActive == true) return
        val waitMs = (deadline - System.currentTimeMillis() + RINGBACK_WATCHDOG_GRACE_MS).coerceAtLeast(0L)
        ringbackWatchdog = viewModelScope.launch {
            delay(waitMs)
            playSoundEffect.stop(SoundEffect.CallRingbackLoop)
        }
    }

    /**
     * Answers a ring the user already agreed to answer.
     *
     * The accept button on the incoming-call notification set [autoAcceptPending]: the decision was the
     * user's, but it was made without a screen to make it on, so it is honoured here — through the same
     * [accept] a tap would have used, which still refuses to open a microphone that has not been granted.
     * Waiting for a ringing session rather than answering in [init] is what keeps this honest: there is no
     * call to accept until the backend's row has been adopted as one.
     */
    private fun maybeAutoAccept(session: CallSession?) {
        if (!autoAcceptPending) return
        if (session == null) return
        if (session.direction != CallDirection.INCOMING) {
            autoAcceptPending = false
            return
        }
        if (session.phase != CallPhase.RINGING) return
        autoAcceptPending = false
        accept()
    }

    private fun currentSession(): CallSession? = _uiState.value.session

    private fun runControl(action: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            action().onFailure { error ->
                if (error is CallBackendException && error.kind == CallErrorKind.PERMISSION_DENIED) {
                    // A grant was revoked, or the platform's answer changed after the gate ran. The
                    // control did nothing, so say so by putting the question back on screen — this is a
                    // response to a tap, which is what makes the dialog itself the right answer.
                    val mediaType = currentSession()?.mediaType
                    pendingOp = null
                    if (mediaType != null) publishPlan(prepareCallPermissions(mediaType), reask = true)
                } else {
                    Napier.w("Call control refused: ${error.message}")
                }
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

    /** A call intent that a permission can interrupt, remembered so granting can finish it. */
    private sealed interface PendingCallOp {
        data class Dial(val request: CallRequest) : PendingCallOp
        data object Accept : PendingCallOp
        data class Upgrade(val mediaType: CallMediaType) : PendingCallOp
    }

    private companion object {
        const val TICK_INTERVAL_MS = 1_000L

        // The watchdog fires just after the backend's deadline, so a session frame that arrives with the
        // answer at the same moment still gets to pick the closing cue.
        const val RINGBACK_WATCHDOG_GRACE_MS = 2_000L
    }
}
