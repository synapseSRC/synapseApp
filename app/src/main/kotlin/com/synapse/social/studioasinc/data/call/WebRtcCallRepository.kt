package com.synapse.social.studioasinc.data.call

import com.synapse.social.studioasinc.core.util.toNotificationContent
import com.synapse.social.studioasinc.data.call.audio.CallAudioRouter
import com.synapse.social.studioasinc.data.call.webrtc.CallEngine
import com.synapse.social.studioasinc.data.call.webrtc.CallEngineFactory
import com.synapse.social.studioasinc.data.call.webrtc.EngineCandidate
import com.synapse.social.studioasinc.data.call.webrtc.EngineConfig
import com.synapse.social.studioasinc.data.call.webrtc.EngineConnectionState
import com.synapse.social.studioasinc.data.call.webrtc.EngineEvent
import com.synapse.social.studioasinc.data.call.webrtc.EngineMediaIssue
import com.synapse.social.studioasinc.data.call.webrtc.EngineSdp
import com.synapse.social.studioasinc.data.call.webrtc.EngineSdpType
import com.synapse.social.studioasinc.data.call.webrtc.EngineTrackKind
import com.synapse.social.studioasinc.data.call.webrtc.IceServerSpec
import com.synapse.social.studioasinc.shared.domain.model.call.CallAudioRoute
import com.synapse.social.studioasinc.shared.domain.model.call.CallBackendException
import com.synapse.social.studioasinc.shared.domain.model.call.CallBackendStatus
import com.synapse.social.studioasinc.shared.domain.model.call.CallDirection
import com.synapse.social.studioasinc.shared.domain.model.call.CallEndReason
import com.synapse.social.studioasinc.shared.domain.model.call.CallErrorKind
import com.synapse.social.studioasinc.shared.domain.model.call.CallIceCandidate
import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import com.synapse.social.studioasinc.shared.domain.model.call.CallPermissionPlan
import com.synapse.social.studioasinc.shared.domain.model.call.CallPhase
import com.synapse.social.studioasinc.shared.domain.model.call.CallRequest
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession
import com.synapse.social.studioasinc.shared.domain.model.call.CallSignal
import com.synapse.social.studioasinc.shared.domain.model.call.CallSignalKind
import com.synapse.social.studioasinc.shared.domain.model.call.CallSignalRecord
import com.synapse.social.studioasinc.shared.domain.model.call.IceServerEndpoint
import com.synapse.social.studioasinc.shared.domain.model.call.TurnCredentials
import com.synapse.social.studioasinc.shared.domain.repository.CallPermissionRepository
import com.synapse.social.studioasinc.shared.domain.repository.CallRepository
import com.synapse.social.studioasinc.shared.domain.repository.CallSessionRepository
import com.synapse.social.studioasinc.shared.domain.usecase.call.MintTurnCredentialsUseCase
import io.github.aakira.napier.Napier
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The real call: WebRTC media, backend-authoritative state, and the signaling mailbox between them.
 *
 * It implements the same ten-method [CallRepository] the mock did, because the screen asks for call
 * *actions*, not for a transport. Below that boundary, three rules decide everything:
 *
 *  - **a phase comes from the backend.** Every transition is a row the server wrote after checking
 *    `auth.uid()` against the role that transition needs. A local timer can make the UI wait; it can
 *    never make the UI connected.
 *  - **media comes from ICE.** [CallStateMachine.phaseFor] needs the server's `active` *and* this
 *    device's own connected link before it will show an active call.
 *  - **one engine per call, released deterministically.** See [teardownLocked].
 *
 * The negotiation rule explains most of what follows: **only the caller creates an offer.** Both
 * sides learn about a media change from the backend row, so there is one ordered trigger for
 * renegotiation instead of two peers racing to offer — glare is designed out rather than patched
 * with rollbacks and timeouts. The callee answers whatever the caller sends.
 */
@Singleton
class WebRtcCallRepository @Inject constructor(
    private val engineFactory: CallEngineFactory,
    private val sessions: CallSessionRepository,
    private val mintTurnCredentials: MintTurnCredentialsUseCase,
    private val audioRouter: CallAudioRouter,
    private val permissions: CallPermissionRepository,
    private val mediaHold: CallMediaHold,
    @Named("ApplicationScope") private val appScope: CoroutineScope
) : CallRepository {

    private val mutex = Mutex()
    private val _session = MutableStateFlow<CallSession?>(null)

    private var live: LiveCall? = null

    /** Observer tokens by call id, so a screen closing can be told apart from a call ending. */
    private val observers = mutableMapOf<Any, String>()

    // ---- Observation -------------------------------------------------------------

    /**
     * The frame source for the call screen.
     *
     * Teardown is not triggered by cancellation alone: the stream has to survive the screen being
     * recomposed or handed to another host mid-call, so a cancellation only starts the grace period
     * in [checkAbandoned].
     */
    override fun observeSession(callId: String): Flow<CallSession?> = channelFlow {
        val token = Any()
        synchronized(observers) { observers[token] = callId }
        try {
            _session.asStateFlow()
                .map { current -> current?.takeIf { it.id == callId } }
                .collect { send(it) }
        } finally {
            withContext(NonCancellable) {
                synchronized(observers) { observers.remove(token) }
                appScope.launch { checkAbandoned(callId) }
            }
        }
    }

    /** Nothing is watching and the call is still up: the microphone must not outlive the UI. */
    private suspend fun checkAbandoned(callId: String) {
        delay(ABANDON_GRACE_MS)
        val watched = synchronized(observers) { observers.containsValue(callId) }
        val stuck = _session.value?.takeIf { it.id == callId && it.phase != CallPhase.ENDED }
        if (watched || stuck == null) return
        Napier.w("Call $callId lost its screen while ${stuck.phase}; releasing media instead of leaking it")
        runCatching { end(callId) }
            .onFailure { Napier.w("Abandoned call could not be ended cleanly: ${it.message}") }
    }

    // ---- Lifecycle ---------------------------------------------------------------

    override suspend fun start(request: CallRequest): Result<CallSession> {
        val mediaType = request.mediaType
        val isIncoming = request.direction == CallDirection.INCOMING

        // A ringing screen captures nothing, so an incoming call is never blocked on a permission
        // answer — [accept] is where this device starts using a microphone, and it checks there. An
        // outgoing call is different: capture begins the moment the invitation is stored.
        if (!isIncoming) {
            val plan = permissions.planFor(mediaType)
            if (!plan.isReady) return failStart(permissionFailure(plan))
        }

        // A placeholder with a face and a phase, so the dial is on screen during the round trips
        // below rather than blank. It has no backend row, so nothing can be mistaken for progress.
        _session.value = CallSession(
            id = PENDING_ID,
            participant = request.participant,
            chatId = request.chatId,
            direction = request.direction,
            mediaType = mediaType,
            phase = if (isIncoming) CallPhase.RINGING else CallPhase.DIALING,
            audioRoute = if (mediaType == CallMediaType.VIDEO) CallAudioRoute.SPEAKER else CallAudioRoute.EARPIECE,
            isLocalVideoEnabled = mediaType == CallMediaType.VIDEO && !isIncoming,
            isRemoteVideoEnabled = mediaType == CallMediaType.VIDEO,
            startedAtMillis = System.currentTimeMillis()
        )

        // Traversal credentials before anything else: without a relay, two peers behind symmetric
        // NATs fail half-way through negotiation, which reads as a flaky product instead of missing
        // configuration. STUN-only is therefore refused here, in every build.
        val credentials = mintTurnCredentials(requireTurn = true)
            .getOrElse { return failStart(it) }

        val row = if (isIncoming) {
            resolveInvitation(request).getOrElse { return failStart(it) }
        } else {
            val chatId = request.chatId
            if (chatId.isNullOrBlank()) {
                return failStart(
                    CallBackendException(
                        CallErrorKind.STATE_INVALID,
                        "A call has to come from the conversation it belongs to"
                    )
                )
            }
            sessions.create(chatId, request.participant.userId, mediaType).getOrElse { return failStart(it) }
        }

        return mutex.withLock {
            // Superseding an existing call must not leave the previous hardware open.
            live?.let { previous -> teardownLocked(previous, "superseded by a new call") }

            val session = initialView(row)
            _session.value = session
            val call = LiveCall(
                id = row.id,
                direction = row.direction,
                isInitiator = row.direction == CallDirection.OUTGOING,
                mediaType = row.mediaType,
                engine = engineFactory.create(),
                iceServers = credentials.toSpecs(),
                turnExpiresAtMillis = credentials.expiresAtMillis,
                deadlineMillis = row.serverRingTimeoutAtMillis
            )
            live = call
            startWatch(call)
            // Capture and negotiation happen off the lock: the invitation is already stored (the row
            // says `ringing` and the notification trigger has fired), so the screen may show that
            // while the camera is still coming up.
            //
            // Only the caller does this now. An incoming call must leave the microphone and camera
            // closed until the user answers — the caller's offer waits in [LiveCall.pendingSignals]
            // instead, and `bringUpMedia` runs from [accept].
            if (call.isInitiator) call.scope.launch { bringUpMedia(call) }
            Result.success(session)
        }
    }

    override suspend fun accept(callId: String): Result<Unit> = mutex.withLock {
        val call = liveOrNull(callId)
            ?: return@withLock Result.failure(IllegalStateException("No incoming call to accept"))
        // Answering is where this device starts capturing: the ring itself needed no permission, the
        // microphone does. Refusing here leaves the backend row ringing, so the caller still hears
        // ringing and can give up honestly rather than staring at a connected call with no audio.
        val plan = permissions.planFor(call.mediaType)
        if (!plan.isReady) return@withLock Result.failure(permissionFailure(plan))
        val row = sessions.accept(callId).getOrElse { return@withLock Result.failure(it) }
        applyRowLocked(call, row)
        call.scope.launch { bringUpMedia(call) }
        Result.success(Unit)
    }

    override suspend fun decline(callId: String): Result<Unit> = mutex.withLock {
        val call = liveOrNull(callId)
            ?: return@withLock Result.failure(IllegalStateException("No incoming call to decline"))
        val row = sessions.decline(callId).getOrElse { return@withLock Result.failure(it) }
        applyRowLocked(call, row)
        teardownLocked(call, "declined")
        Result.success(Unit)
    }

    /**
     * Ends the call. The reason is a *claim*: `end_call` re-reads the row and overrides it when its
     * own view disagrees, so a client cannot log a ring nobody answered as a completed call.
     */
    override suspend fun end(callId: String): Result<Unit> = mutex.withLock {
        val call = liveOrNull(callId)
            ?: return@withLock Result.failure(IllegalStateException("No call to end"))
        val claim = _session.value?.let(CallStateMachine::claimEndReason)
        val row = sessions.end(callId, claim).getOrElse { error ->
            // The row may be stale, but the hardware still has to come back. A call the user hung up
            // on is hung up on; backend housekeeping resolves the row.
            Napier.w("Backend refused the end of $callId (${error.message}); releasing media anyway")
            moveLocalToEnd(CallErrorKind.BACKEND_UNREACHABLE)
            teardownLocked(call, "end refused")
            return@withLock Result.failure(error)
        }
        applyRowLocked(call, row)
        teardownLocked(call, "ended")
        Result.success(Unit)
    }

    // ---- Controls ----------------------------------------------------------------

    override suspend fun setMuted(callId: String, muted: Boolean): Result<Unit> =
        withLiveCall(callId) { call ->
            call.engine.setMuted(muted).also { outcome ->
                if (outcome.isSuccess) reduce(call) { it.copy(isMuted = muted, errorKind = null) }
            }
        }

    /**
     * Asks for [route], then reports what the device settled on — which may not be what was asked.
     *
     * The router is the authority: an icon showing Bluetooth while audio is on the earpiece is a lie
     * the user can only discover by holding the phone to their ear.
     */
    override suspend fun setAudioRoute(callId: String, route: CallAudioRoute): Result<Unit> =
        withLiveCall(callId) { call ->
            val settled = audioRouter.select(route)
            reduce(call) { it.copy(audioRoute = settled) }
            if (settled == route) Result.success(Unit)
            else Result.failure(IllegalStateException("Audio route '$route' is not available; using '$settled'"))
        }

    override suspend fun setLocalVideoEnabled(callId: String, enabled: Boolean): Result<Unit> =
        withLiveCall(callId) { call ->
            if (call.mediaType != CallMediaType.VIDEO) {
                return@withLiveCall Result.failure(IllegalStateException("Call is not a video call"))
            }
            // Switching the preview back on can mean starting capture again: turning video off
            // releases the camera to the system, so there may be no track left to enable.
            if (enabled && !call.captureRunning) {
                val started = startCaptureLocked(call)
                if (started.isFailure) return@withLiveCall started
            }
            call.engine.setLocalVideoEnabled(enabled).also { outcome ->
                if (outcome.isSuccess) reduce(call) { it.copy(isLocalVideoEnabled = enabled, errorKind = null) }
            }
        }

    /** The engine reports which lens actually became live, so the icon follows the hardware. */
    override suspend fun flipCamera(callId: String): Result<Unit> =
        withLiveCall(callId) { call ->
            if (call.mediaType != CallMediaType.VIDEO) {
                return@withLiveCall Result.failure(IllegalStateException("Call is not a video call"))
            }
            call.engine.flipCamera()
        }

    /**
     * Changes the media of a live call.
     *
     * The row is written first, because the row is what tells the *other* device a change is coming.
     * Writing it last would have the peer renegotiate against a description this device has not
     * changed yet, and the caller's offer would race the callee's camera.
     */
    override suspend fun switchMediaType(callId: String, mediaType: CallMediaType): Result<Unit> =
        withLiveCall(callId) { call ->
            if (call.mediaType == mediaType) return@withLiveCall Result.success(Unit)
            val phase = _session.value?.phase
            if (phase != CallPhase.ACTIVE && phase != CallPhase.RECONNECTING) {
                return@withLiveCall Result.failure(IllegalStateException("Call is not active"))
            }
            // Upgrading to video means capturing from a second device, and the user may never have
            // granted the camera. Checking before the row flips keeps the peer from being told about
            // a video stream this device cannot produce.
            if (mediaType == CallMediaType.VIDEO) {
                val plan = permissions.planFor(CallMediaType.VIDEO)
                if (!plan.isReady) return@withLiveCall Result.failure(permissionFailure(plan))
            }
            val row = sessions.switchMediaType(callId, mediaType)
                .getOrElse { return@withLiveCall Result.failure(it) }
            applyRowLocked(call, row)
            Result.success(Unit)
        }

    // ---- Media bring-up ----------------------------------------------------------

    /**
     * Opens the engine, starts capture for video calls, answers or offers, then replays whatever the
     * peer sent while the camera was still waking up.
     *
     * Any failure ends the call visibly. Half a call — ringing, mute controls, no audio — is worse
     * than an honest failure, and the screen has nothing to show for a peer connection that does not
     * exist.
     */
    private suspend fun bringUpMedia(call: LiveCall) {
        // Call audio mode begins here, not when the row was created: a ringing screen is still an app
        // playing its own ringtone, and MODE_IN_COMMUNICATION would move that audio to the earpiece.
        audioRouter.start(video = call.mediaType == CallMediaType.VIDEO)
        val opened = call.engine.open(
            EngineConfig(
                callId = call.id,
                iceServers = call.iceServers,
                mediaType = call.mediaType,
                isInitiator = call.isInitiator
            )
        )
        if (opened.isFailure) {
            mutex.withLock {
                Napier.e("Call media could not open: ${opened.exceptionOrNull()?.message}")
                failCallLocked(call, opened.exceptionOrNull(), CallErrorKind.MEDIA_START_FAILED)
            }
            return
        }
        if (call.mediaType == CallMediaType.VIDEO) {
            val capture = startCaptureLocked(call)
            if (capture.isFailure) {
                mutex.withLock {
                    failCallLocked(call, capture.exceptionOrNull(), CallErrorKind.MEDIA_START_FAILED)
                }
                return
            }
        }
        mutex.withLock {
            if (call.closed) return@withLock
            call.mediaOpen = true
            // Capture is running, so the process may now ask to keep it running. Acquired here rather
            // than when the row appeared: a ringing screen has no hardware to license, and a hold that
            // predates the microphone would be a notification claiming a call that has not started.
            acquireHoldLocked(call)
            if (call.isInitiator) negotiateLocked(call, iceRestart = false)
            flushPendingSignalsLocked(call)
        }
    }

    /**
     * Tells the platform which call this device is holding hardware for.
     *
     * Re-entrant on purpose: the service re-promotes with the media type the call has now, which is how a
     * voice call that became a video call is allowed to keep using the camera. This reads the published
     * session rather than the live call, so the notification and the screen cannot disagree about who is
     * on the call.
     */
    private fun acquireHoldLocked(call: LiveCall) {
        val session = _session.value?.takeIf { it.id == call.id } ?: return
        mediaHold.acquire(session.toNotificationContent())
    }

    private suspend fun startCaptureLocked(call: LiveCall): Result<Unit> =
        call.engine.startVideo(isFrontCamera = true).also { outcome ->
            if (outcome.isSuccess) {
                call.captureRunning = true
                reduce(call) { it.copy(isLocalVideoEnabled = true) }
            }
        }

    /**
     * Creates the local description and hands it to the peer.
     *
     * The engine waits out ICE gathering before returning, so candidates ride inside the SDP and the
     * signaling table stays at a handful of rows per call. Only the initiator offers — the glare-free
     * rule described on the class.
     */
    private suspend fun negotiateLocked(call: LiveCall, iceRestart: Boolean = false): Result<EngineSdp> {
        val sdp = call.engine.createOffer(iceRestart).getOrElse { error ->
            Napier.w("Could not create an offer (restart=$iceRestart): ${error.message}")
            return Result.failure(error)
        }
        val signal = CallSignal.description(
            kind = CallSignalKind.OFFER,
            sdpType = sdp.type.wireValue(),
            sdp = sdp.sdp
        )
        return if (publishLocked(call, signal)) Result.success(sdp)
        else Result.failure(
            CallBackendException(CallErrorKind.SIGNALLING_FAILED, "The offer could not be delivered")
        )
    }

    private suspend fun answerLocked(call: LiveCall) {
        val sdp = call.engine.createAnswer().getOrElse { error ->
            Napier.w("Could not answer the caller's offer: ${error.message}")
            return
        }
        val signal = CallSignal.description(
            kind = CallSignalKind.ANSWER,
            sdpType = sdp.type.wireValue(),
            sdp = sdp.sdp
        )
        publishLocked(call, signal)
    }

    // ---- Watchers ----------------------------------------------------------------

    /** One subscription per source, all cancelled together when the call ends. */
    private fun startWatch(call: LiveCall) {
        call.scope.launch { collectBackendRow(call) }
        call.scope.launch { collectSignals(call) }
        call.scope.launch { collectEngineEvents(call) }
        call.scope.launch { drainCandidateOutbox(call) }
        call.scope.launch { watchRingDeadline(call) }
        call.scope.launch { rotateTurnCredentials(call) }
    }

    private suspend fun collectBackendRow(call: LiveCall) {
        sessions.observe(call.id).collect { row ->
            mutex.withLock {
                if (call.closed) return@withLock
                val wasEnded = row.backendStatus == CallBackendStatus.ENDED
                applyRowLocked(call, row)
                if (wasEnded) teardownLocked(call, "ended by the backend")
            }
        }
    }

    /**
     * Applies everything the peer sent, in backend order.
     *
     * Two windows are handled here, and both are ordinary rather than exotic. Messages that arrive
     * before this device has an engine are buffered and replayed by [flushPendingSignalsLocked] —
     * accepting a call and the caller's offer landing at the same instant is the common case.
     * Candidates that arrive before the remote description are buffered separately, because WebRTC
     * rejects them outright, and dropping one can leave a call that never connects.
     */
    private suspend fun collectSignals(call: LiveCall) {
        sessions.observeSignals(call.id).collect { record: CallSignalRecord ->
            mutex.withLock {
                if (call.closed) return@withLock
                if (!call.mediaOpen) {
                    call.pendingSignals.add(record)
                    return@withLock
                }
                applySignalLocked(call, record)
            }
        }
    }

    private suspend fun applySignalLocked(call: LiveCall, record: CallSignalRecord) {
        when (record.signal.kind) {
            CallSignalKind.OFFER, CallSignalKind.ANSWER -> {
                val description = record.signal.toEngineSdp() ?: run {
                    Napier.w("Signaling row ${record.id} carries an unreadable description type")
                    return
                }
                if (call.engine.setRemoteDescription(description).isFailure) return
                call.remoteDescriptionSet = true
                val buffered = call.inboundCandidates.toList()
                call.inboundCandidates.clear()
                if (buffered.isNotEmpty()) call.engine.addRemoteCandidates(buffered.toEngineCandidates())
                if (description.type == EngineSdpType.OFFER && !call.isInitiator) answerLocked(call)
            }

            CallSignalKind.ICE, CallSignalKind.ICE_BATCH -> {
                val candidates = record.signal.candidates
                if (candidates.isEmpty()) return
                if (call.remoteDescriptionSet) call.engine.addRemoteCandidates(candidates.toEngineCandidates())
                else call.inboundCandidates.addAll(candidates)
            }
        }
    }

    private suspend fun flushPendingSignalsLocked(call: LiveCall) {
        val queued = call.pendingSignals.toList()
        call.pendingSignals.clear()
        queued.forEach { record -> applySignalLocked(call, record) }
    }

    private suspend fun collectEngineEvents(call: LiveCall) {
        call.engine.events.collect { event ->
            mutex.withLock {
                if (call.closed) return@withLock
                handleEngineEventLocked(call, event)
            }
        }
    }

    private suspend fun handleEngineEventLocked(call: LiveCall, event: EngineEvent) {
        when (event) {
            // The description this side asked for is published by negotiateLocked. Re-publishing it
            // from the event would put the same SDP in the mailbox twice.
            is EngineEvent.LocalDescription -> Unit

            is EngineEvent.LocalCandidate -> call.outbox.trySend(event.candidate.toDomain())

            EngineEvent.GatheringComplete -> drainOutboxNow(call)

            is EngineEvent.ConnectionStateChanged -> onLinkChangedLocked(call, event.state)

            is EngineEvent.RemoteVideoTrack ->
                reduce(call) { it.copy(isRemoteVideoEnabled = event.available && event.enabled) }

            is EngineEvent.LocalVideoEnabled ->
                reduce(call) { it.copy(isLocalVideoEnabled = event.enabled) }

            is EngineEvent.LocalCameraSwitched ->
                reduce(call) { it.copy(isFrontCamera = event.isFront) }

            EngineEvent.RenegotiationNeeded -> when {
                call.isInitiator && call.mediaOpen -> negotiateLocked(call)
                // A polite peer does not offer: it waits for the caller's description and answers it.
                else -> Napier.d("Responder asked for renegotiation; the caller drives it")
            }

            is EngineEvent.IceError ->
                // Non-fatal while another server still works. Surfacing it as a call error would
                // flash a failure on a call that is connecting normally.
                Napier.w("ICE candidate rejected: ${event.message}")

            is EngineEvent.MediaError -> onMediaErrorLocked(call, event)
        }
    }

    private suspend fun onLinkChangedLocked(call: LiveCall, state: EngineConnectionState) {
        call.link = state
        recomputePhase(call)
        when (state) {
            EngineConnectionState.CONNECTED -> markActiveLocked(call)

            EngineConnectionState.DISCONNECTED -> {
                Napier.w("ICE link dropped; asking the network stack to rebuild it")
                if (call.reconnectAttempts < MAX_ICE_RESTARTS) recoverLinkLocked(call)
                else failCallLocked(
                    call,
                    CallBackendException(CallErrorKind.ICE_FAILED, "The connection to the other device was lost"),
                    CallErrorKind.ICE_FAILED
                )
            }

            EngineConnectionState.FAILED -> {
                if (call.reconnectAttempts < MAX_ICE_RESTARTS) {
                    Napier.w("ICE failed; restarting candidate collection")
                    recoverLinkLocked(call)
                } else {
                    failCallLocked(
                        call,
                        CallBackendException(
                            CallErrorKind.ICE_FAILED,
                            "The two devices could not reach each other"
                        ),
                        CallErrorKind.ICE_FAILED
                    )
                }
            }

            EngineConnectionState.CLOSED -> if (call.mediaOpen) teardownLocked(call, "peer connection closed")

            EngineConnectionState.NEW, EngineConnectionState.CONNECTING -> Unit
        }
    }

    private suspend fun onMediaErrorLocked(call: LiveCall, event: EngineEvent.MediaError) {
        val kind = when (event.issue) {
            EngineMediaIssue.CAMERA_PERMISSION -> CallErrorKind.PERMISSION_DENIED
            EngineMediaIssue.CAMERA_DISCONNECTED,
            EngineMediaIssue.CAMERA_FROZEN,
            EngineMediaIssue.CAMERA_ERROR -> CallErrorKind.MEDIA_START_FAILED

            EngineMediaIssue.AUDIO_ERROR -> CallErrorKind.MEDIA_START_FAILED
        }
        Napier.w("Media failure (${event.kind}): ${event.message}")
        if (event.kind == EngineTrackKind.VIDEO) {
            // The camera is gone. Say so instead of keeping a frozen self-view on screen; the audio
            // half of the call is still real, so the call is not ended over a lost lens.
            call.captureRunning = false
            reduce(call) { it.copy(isLocalVideoEnabled = false, errorKind = kind) }
            return
        }
        reduce(call) { it.copy(errorKind = kind) }
        // Silence in both directions is not a call.
        failCallLocked(call, CallBackendException(kind, event.message), kind)
    }

    /**
     * Asks for `active` once this device's own link is up.
     *
     * Either participant may write it and both do: each is proving a different half of "this call
     * works", and the row only needs to move once. A refusal means the peer has not answered yet,
     * which is correct, and the row stream retries it when the status does change.
     */
    private suspend fun markActiveLocked(call: LiveCall) {
        if (call.markedActive) return
        val row = sessions.markActive(call.id).getOrElse {
            Napier.d("Not allowed to activate the call yet: ${it.message}")
            return
        }
        call.markedActive = true
        applyRowLocked(call, row)
    }

    /**
     * Rebuilds a dropped link: fresh traversal credentials, then a restart from the caller.
     *
     * The credential mint and the settle delay happen under the repository lock, which serialises
     * controls for a second. That is the right trade: restarting ICE while a mute or a camera flip is
     * in flight would interleave two renegotiations, and the row already says the call is reconnecting.
     */
    private suspend fun recoverLinkLocked(call: LiveCall) {
        call.reconnectAttempts++
        reduce(call) { it.copy(errorKind = CallErrorKind.ICE_FAILED) }
        sessions.markReconnecting(call.id)
            .onFailure { Napier.w("Backend could not record the reconnect: ${it.message}") }
        mintTurnCredentials(requireTurn = true).onSuccess { fresh ->
            call.iceServers = fresh.toSpecs()
            call.turnExpiresAtMillis = fresh.expiresAtMillis
            call.engine.updateIceServers(call.iceServers)
                .onFailure { Napier.w("Fresh ICE configuration was refused: ${it.message}") }
        }
        if (call.isInitiator) {
            delay(ICE_RESTART_SETTLE_MS)
            negotiateLocked(call, iceRestart = true)
        }
    }

    // ---- TURN rotation -----------------------------------------------------------

    /**
     * Re-mints credentials at half their lifetime and restarts ICE with them.
     *
     * An expired credential does not drop a running call — the selected candidate pair keeps working
     * — but every *new* allocation after it fails, including one after a network change. Refreshing
     * early is what keeps a long call able to recover. A refused refresh is reported, not fatal.
     */
    private suspend fun rotateTurnCredentials(call: LiveCall) {
        while (true) {
            val expiresAt = call.turnExpiresAtMillis ?: return
            val halfway = ((expiresAt - System.currentTimeMillis()) / 2L).coerceAtLeast(REFRESH_MIN_DELAY_MS)
            delay(halfway)
            if (call.closed) return
            val fresh = mintTurnCredentials(requireTurn = true).getOrNull()
            if (fresh == null) {
                Napier.w("TURN rotation was refused; the live call continues on its existing path")
                reduce(call) { it.copy(errorKind = CallErrorKind.TURN_UNAVAILABLE) }
                delay(REFRESH_RETRY_DELAY_MS)
                continue
            }
            mutex.withLock {
                if (call.closed) return
                call.iceServers = fresh.toSpecs()
                call.turnExpiresAtMillis = fresh.expiresAtMillis
                call.engine.updateIceServers(call.iceServers)
                    .onFailure { Napier.w("Rotated ICE configuration was refused: ${it.message}") }
                if (call.isInitiator && call.mediaOpen) negotiateLocked(call, iceRestart = true)
            }
        }
    }

    // ---- Ring deadline -----------------------------------------------------------

    /**
     * Reads the backend's own `ringing_expires_at` rather than a client-side constant, so a device
     * with a wrong clock can neither end a call early nor keep ringing forever.
     *
     * When it passes, nothing is decided locally. The caller writes the end it is allowed to write;
     * both sides then ask the server to expire its own stale rings and re-read the row, so what
     * appears on screen is what the backend recorded. Only when the backend cannot be reached at all
     * does this device stop ringing on its own — with the failure visible rather than hidden.
     */
    private suspend fun watchRingDeadline(call: LiveCall) {
        val deadline = call.deadlineMillis ?: return
        delay((deadline - System.currentTimeMillis()).coerceAtLeast(0L) + RING_GRACE_MS)
        val stillRinging = _session.value?.takeIf { it.id == call.id && it.phase == CallPhase.RINGING }
            ?: return
        Napier.i("Ring deadline passed with no answer on ${stillRinging.id}")
        mutex.withLock {
            if (call.closed) return@withLock
            if (_session.value?.phase != CallPhase.RINGING) return@withLock
            if (call.isInitiator) sessions.end(call.id, CallEndReason.NO_ANSWER)
            sessions.resolveStaleSessions()
                .onFailure { Napier.w("Stale rings could not be resolved: ${it.message}") }
            val row = sessions.fetch(call.id).getOrNull()
            if (row == null) {
                // No verdict available. Releasing the hardware is still right — the media never
                // started, so nothing is being cut — and the reason goes in the session, not under
                // the rug.
                moveLocalToEnd(CallErrorKind.BACKEND_UNREACHABLE)
                teardownLocked(call, "no answer, backend unreachable")
            } else {
                applyRowLocked(call, row)
                if (_session.value?.phase == CallPhase.RINGING) {
                    // The server still insists the call is live. Then it is live, and this device
                    // waits for the row instead of inventing an ending.
                    Napier.w("Backend still reports the ring as open after its own deadline")
                } else {
                    teardownLocked(call, "no answer")
                }
            }
        }
    }

    // ---- Candidate outbox --------------------------------------------------------

    /**
     * Publishes the candidates that arrive after the description went out, batched into one row.
     *
     * Runs outside the repository lock: it only moves bytes, and holding the lock through a quiet
     * period would stall every control the user taps.
     */
    private suspend fun drainCandidateOutbox(call: LiveCall) {
        val batch = mutableListOf<CallIceCandidate>()
        while (true) {
            val first = call.outbox.receiveCatching()
            if (first.isClosed) break
            val candidate = first.getOrNull() ?: continue
            batch.add(candidate)
            // Keep collecting while candidates are still arriving, and publish as soon as the
            // engine goes quiet: one envelope for a late relay batch, not one per candidate.
            while (true) {
                val extra = withTimeoutOrNull(CANDIDATE_QUIET_MS) {
                    call.outbox.receiveCatching().getOrNull()
                } ?: break
                batch.add(extra)
            }
            publish(call, CallSignal.batch(batch.toList()))
            batch.clear()
        }
    }

    /** Gathering finished, so anything still queued is all there is for this round. */
    private fun drainOutboxNow(call: LiveCall) {
        val queued = mutableListOf<CallIceCandidate>()
        while (true) {
            val candidate = call.outbox.tryReceive().getOrNull() ?: break
            queued.add(candidate)
        }
        if (queued.isNotEmpty()) call.scope.launch { publish(call, CallSignal.batch(queued)) }
    }

    private suspend fun publish(call: LiveCall, signal: CallSignal): Boolean =
        sessions.publish(call.id, signal).fold(
            onSuccess = { true },
            onFailure = { error ->
                Napier.w("Signaling '${signal.kind}' could not be published: ${error.message}")
                false
            }
        )

    private suspend fun publishLocked(call: LiveCall, signal: CallSignal): Boolean =
        sessions.publish(call.id, signal).fold(
            onSuccess = { true },
            onFailure = { error ->
                Napier.w("Signaling '${signal.kind}' could not be published: ${error.message}")
                reduce(call) { it.copy(errorKind = CallErrorKind.SIGNALLING_FAILED) }
                false
            }
        )

    // ---- State reduction ---------------------------------------------------------

    /**
     * Merges the authoritative row into what the screen sees.
     *
     * Only the fields the backend owns are taken from it. Mute, audio route, camera state and quality
     * stay local, because the server has no idea what this device's hardware is doing — overwriting
     * them from a row would flip the mute icon back on with every realtime message.
     */
    private fun applyRowLocked(call: LiveCall, row: CallSession) {
        call.deadlineMillis = row.serverRingTimeoutAtMillis
        val previousMedia = call.mediaType
        call.mediaType = row.mediaType
        _session.update { current ->
            val base = current?.takeIf { it.id == row.id } ?: return@update current
            base.copy(
                participant = row.participant,
                chatId = row.chatId ?: base.chatId,
                direction = row.direction,
                mediaType = row.mediaType,
                backendStatus = row.backendStatus,
                endReason = row.endReason ?: base.endReason,
                endedAtMillis = row.endedAtMillis ?: base.endedAtMillis,
                startedAtMillis = row.startedAtMillis,
                serverRingTimeoutAtMillis = row.serverRingTimeoutAtMillis
            )
        }
        recomputePhase(call)
        if (previousMedia != row.mediaType) reactToMediaChange(call)
    }

    /**
     * The anti-simulation rule, applied: the server's status **and** this device's link, never either
     * alone.
     *
     * The duration is stamped from the same evidence. A row that says `answered` proves the peer
     * tapped accept, not that audio is flowing, so showing a running clock from `answered_at` would
     * time a call that has not connected yet.
     */
    private fun recomputePhase(call: LiveCall) {
        _session.update { current ->
            val base = current?.takeIf { it.id == call.id } ?: return@update current
            val phase = CallStateMachine.phaseFor(base.backendStatus, base.direction, call.link)
            val moved = CallStateMachine.move(base, phase)
            val mediaProven = phase == CallPhase.ACTIVE || phase == CallPhase.RECONNECTING ||
                (phase == CallPhase.ENDED && call.link == EngineConnectionState.CONNECTED)
            if (moved.connectedAtMillis != null && !mediaProven) moved.copy(connectedAtMillis = null) else moved
        }
        reconcileHeartbeatLocked(call)
    }

    /**
     * Starts the presence beat once this device considers the call connected, and stops it when the
     * call is over.
     *
     * The backend closes a live session whose last beat is older than its stale window, so a call
     * that is quietly working has to keep saying so. It starts from the local phase rather than from
     * [markActiveLocked] alone: the peer may have marked the row active first, in which case this
     * side's own activation call is a no-op that still has to be followed by beats.
     */
    private fun reconcileHeartbeatLocked(call: LiveCall) {
        val phase = _session.value?.takeIf { it.id == call.id }?.phase
        val connected = phase == CallPhase.ACTIVE || phase == CallPhase.RECONNECTING
        if (connected && call.heartbeatJob == null) {
            call.heartbeatJob = call.scope.launch { runHeartbeat(call) }
        } else if (phase == CallPhase.ENDED) {
            call.heartbeatJob?.cancel()
            call.heartbeatJob = null
        }
    }

    /**
     * The beat itself. A refusal other than "the call is already over" is logged and retried: a
     * transient network failure must not be read as a reason to drop a working call, and the row
     * observer is what actually closes the UI when the server has ended it.
     */
    private suspend fun runHeartbeat(call: LiveCall) {
        while (true) {
            delay(HEARTBEAT_INTERVAL_MS)
            if (call.closed) return
            val error = sessions.heartbeat(call.id).exceptionOrNull() ?: continue
            if ((error as? CallBackendException)?.kind == CallErrorKind.ALREADY_ENDED) {
                Napier.i("Heartbeat found ${call.id} already ended; the row observer will close the call")
                return
            }
            Napier.w("Heartbeat for ${call.id} failed: ${error.message}")
        }
    }

    private fun reduce(call: LiveCall, block: (CallSession) -> CallSession) {
        _session.update { current ->
            val base = current?.takeIf { it.id == call.id } ?: return@update current
            block(base)
        }
    }

    private fun moveLocalToEnd(kind: CallErrorKind?) {
        _session.update { current ->
            current ?: return@update current
            CallStateMachine.move(current, CallPhase.ENDED).copy(errorKind = kind ?: current.errorKind)
        }
    }

    /**
     * Runs a row's media change on the call's own scope instead of inline.
     *
     * Following the row means starting or stopping capture and renegotiating, and an offer waits for ICE
     * gathering — a second or two of work that must not stall the collector that noticed the change, and
     * must not be attempted while the caller still holds the repository lock. The job takes the lock
     * itself and reads `call.mediaType` when it runs, so two rows in quick succession converge on the
     * newest media instead of fighting over the camera.
     */
    private fun reactToMediaChange(call: LiveCall) {
        call.scope.launch {
            mutex.withLock {
                if (!call.closed) applyMediaChangeLocked(call)
            }
        }
    }

    /**
     * Reacts to a media change on the row, which is how both sides learn about it — including the
     * side that did not ask. Capture follows the row, and only the caller renegotiates afterwards.
     */
    private suspend fun applyMediaChangeLocked(call: LiveCall) {
        val video = call.mediaType == CallMediaType.VIDEO
        // The router is asked outside the state update: `update` may replay its block under
        // contention, and a hardware switch must never run twice because of a compare-and-set retry.
        val settled = audioRouter.select(if (video) CallAudioRoute.SPEAKER else CallAudioRoute.EARPIECE)
        reduce(call) { it.copy(isRemoteVideoEnabled = video, audioRoute = settled) }
        // The hold carries the media type, so an upgrade that starts using the camera has to be
        // re-declared or the platform takes the camera away mid-call.
        if (call.mediaOpen) acquireHoldLocked(call)
        when {
            video && !call.captureRunning && call.mediaOpen ->
                if (startCaptureLocked(call).isSuccess && call.isInitiator) negotiateLocked(call)

            !video && call.captureRunning -> {
                call.captureRunning = false
                call.engine.stopVideo()
                reduce(call) { it.copy(isLocalVideoEnabled = false) }
                if (call.isInitiator) negotiateLocked(call)
            }
        }
    }

    private inline suspend fun withLiveCall(
        callId: String,
        action: (LiveCall) -> Result<Unit>
    ): Result<Unit> = mutex.withLock {
        val call = liveOrNull(callId)
            ?: return@withLock Result.failure(IllegalStateException("No active call"))
        action(call)
    }

    private fun liveOrNull(callId: String): LiveCall? = live?.takeIf { it.id == callId && !it.closed }

    // ---- Teardown ----------------------------------------------------------------

    /**
     * Deterministic release, in dependency order, safe to call twice.
     *
     * Watchers go first, so no late event can resurrect state mid-teardown; then the engine, which
     * unwinds capture, tracks, peer connection and factory in that order internally; then the audio
     * mode. The UI's renderers detach themselves through `DisposableEffect` — nothing here can reach
     * a view, which is exactly why a leaked surface cannot originate from this side either.
     */
    private fun teardownLocked(call: LiveCall, why: String) {
        if (call.closed) return
        call.closed = true
        call.heartbeatJob?.cancel()
        call.heartbeatJob = null
        call.scope.cancel()
        call.outbox.close()
        runCatching { call.engine.dispose() }
            .onFailure { Napier.w("Engine dispose threw: ${it.message}") }
        runCatching { audioRouter.stop() }
        // Last, after the hardware is actually back: the hold is the platform's permission to have been
        // using that hardware, and releasing it early would leave a notification claiming a call that is
        // still, for a moment, live.
        mediaHold.release()
        if (live === call) live = null
        Napier.i("Call ${call.id} torn down ($why)")
    }

    /** Ends the call with [kind] recorded, then releases the hardware. */
    private suspend fun failCallLocked(call: LiveCall, error: Throwable?, kind: CallErrorKind) {
        if (call.closed) return
        val message = error?.message ?: "The call could not be connected"
        Napier.w("Failing call ${call.id} as $kind: $message")
        reduce(call) { it.copy(errorKind = kind) }
        val row = sessions.end(call.id, CallEndReason.FAILED).getOrNull()
        if (row != null) applyRowLocked(call, row) else moveLocalToEnd(kind)
        teardownLocked(call, "failed")
    }

    // ---- Start failures ----------------------------------------------------------

    private fun failStart(error: Throwable): Result<CallSession> {
        Napier.w("Call could not start: ${error.message}")
        _session.value = null
        runCatching { audioRouter.stop() }
        return Result.failure(error)
    }

    /**
     * A dial that cannot be made because the user declined something.
     *
     * The UI needs the difference between "ask again" and "only Settings can undo this", which is why
     * this reads the plan rather than a boolean.
     */
    private fun permissionFailure(plan: CallPermissionPlan): CallBackendException = CallBackendException(
        kind = CallErrorKind.PERMISSION_DENIED,
        message = if (plan.needsSettings.isNotEmpty()) {
            "A permission this ${plan.mediaType} call needs was declined permanently; turn it on in system settings."
        } else {
            "This ${plan.mediaType} call needs permission that has not been granted yet."
        }
    )

    /**
     * Finds the invitation this screen was opened for.
     *
     * The route into a call carries the peer, not the row id, so the ringing row is resolved from the
     * backend rather than trusted from the navigation argument — an intent that names a call nobody
     * offered simply finds nothing.
     */
    private suspend fun resolveInvitation(request: CallRequest): Result<CallSession> {
        val peer = request.participant.userId
        val invitation = withTimeoutOrNull(INVITATION_LOOKUP_MS) {
            sessions.observeIncomingInvitations().firstOrNull { row ->
                row.backendStatus == CallBackendStatus.RINGING &&
                    row.participant.userId == peer &&
                    (row.chatId == null || request.chatId == null || row.chatId == request.chatId)
            }
        }
        return invitation?.let { Result.success(it) } ?: Result.failure(
            CallBackendException(
                CallErrorKind.NOT_FOUND,
                "That invitation is no longer waiting for an answer"
            )
        )
    }

    private fun initialView(row: CallSession): CallSession = CallSession(
        id = row.id,
        participant = row.participant,
        chatId = row.chatId,
        direction = row.direction,
        mediaType = row.mediaType,
        // `link` is null until the engine reports one, which is exactly why this reads CONNECTING
        // for an answered row and never ACTIVE: media is not proven yet.
        phase = CallStateMachine.phaseFor(row.backendStatus, row.direction, null),
        endReason = row.endReason,
        audioRoute = if (row.mediaType == CallMediaType.VIDEO) CallAudioRoute.SPEAKER else CallAudioRoute.EARPIECE,
        isLocalVideoEnabled = row.mediaType == CallMediaType.VIDEO && row.direction == CallDirection.OUTGOING,
        isRemoteVideoEnabled = row.mediaType == CallMediaType.VIDEO,
        startedAtMillis = row.startedAtMillis,
        backendStatus = row.backendStatus,
        serverRingTimeoutAtMillis = row.serverRingTimeoutAtMillis
    )

    private companion object {
        /** How long a call may live with no UI watching it before the hardware is reclaimed. */
        const val ABANDON_GRACE_MS = 4_000L

        const val ICE_RESTART_SETTLE_MS = 800L
        const val MAX_ICE_RESTARTS = 2
        const val REFRESH_MIN_DELAY_MS = 30_000L
        const val REFRESH_RETRY_DELAY_MS = 20_000L

        /** Late candidates are published once the engine goes quiet, not one row per candidate. */
        const val CANDIDATE_QUIET_MS = 700L

        /** The backend's expiry wins; this only covers the gap before the row arrives. */
        const val RING_GRACE_MS = 3_000L

        const val INVITATION_LOOKUP_MS = 6_000L

        /**
         * Ten beats fit in the server's five-minute stale window, so Doze batching or a short dead
         * zone cannot make a working call look abandoned.
         */
        const val HEARTBEAT_INTERVAL_MS = 30_000L

        /** The id of a dial that has no backend row yet, so a failed start leaves nothing to show. */
        const val PENDING_ID = "call-pending"
    }
}

/**
 * Everything one live call owns.
 *
 * A class rather than a bag of repository fields because these must move together: an engine without
 * its watchers, or watchers without a call id, is a leaked microphone.
 */
private class LiveCall(
    val id: String,
    val direction: CallDirection,
    val isInitiator: Boolean,
    var mediaType: CallMediaType,
    val engine: CallEngine,
    var iceServers: List<IceServerSpec>,
    var turnExpiresAtMillis: Long?,
    var deadlineMillis: Long?
) {
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Late local candidates, drained in batches by the repository's outbox coroutine. */
    val outbox = Channel<CallIceCandidate>(Channel.UNLIMITED)

    /** Envelopes that arrived before this device had an engine, replayed once it exists. */
    val pendingSignals = mutableListOf<CallSignalRecord>()

    /** Candidates from the peer that arrived before the remote description they belong to. */
    val inboundCandidates = mutableListOf<CallIceCandidate>()

    var link: EngineConnectionState? = null
    var remoteDescriptionSet: Boolean = false
    var captureRunning: Boolean = false
    var mediaOpen: Boolean = false
    var markedActive: Boolean = false
    var reconnectAttempts: Int = 0
    var closed: Boolean = false

    /** The presence beat for this call; null until the call is connected. */
    var heartbeatJob: Job? = null
}

private fun CallSignal.toEngineSdp(): EngineSdp? {
    val body = sdp?.takeIf { it.isNotBlank() } ?: return null
    val type = when (sdpType?.lowercase()) {
        "offer" -> EngineSdpType.OFFER
        "answer" -> EngineSdpType.ANSWER
        // A description whose type cannot be read is dropped, not guessed: handing WebRTC a
        // description of the wrong kind fails the negotiation and looks like a network fault.
        else -> return null
    }
    return EngineSdp(type = type, sdp = body)
}

private fun EngineSdpType.wireValue(): String = when (this) {
    EngineSdpType.OFFER -> "offer"
    EngineSdpType.ANSWER -> "answer"
}

private fun CallIceCandidate.toEngineCandidate(): EngineCandidate =
    EngineCandidate(sdp = candidate, sdpMid = sdpMid, sdpMLineIndex = sdpMLineIndex)

private fun List<CallIceCandidate>.toEngineCandidates(): List<EngineCandidate> = map { it.toEngineCandidate() }

private fun EngineCandidate.toDomain(): CallIceCandidate =
    CallIceCandidate(candidate = sdp, sdpMid = sdpMid, sdpMLineIndex = sdpMLineIndex)

private fun IceServerEndpoint.toSpec(): IceServerSpec =
    IceServerSpec(urls = urls, username = username, credential = credential)

private fun TurnCredentials.toSpecs(): List<IceServerSpec> = servers.map(IceServerEndpoint::toSpec)
