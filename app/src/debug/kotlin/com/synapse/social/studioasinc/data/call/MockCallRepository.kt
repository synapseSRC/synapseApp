package com.synapse.social.studioasinc.data.call

import com.synapse.social.studioasinc.shared.domain.model.call.CallAudioRoute
import com.synapse.social.studioasinc.shared.domain.model.call.CallConnectionQuality
import com.synapse.social.studioasinc.shared.domain.model.call.CallDirection
import com.synapse.social.studioasinc.shared.domain.model.call.CallEndReason
import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import com.synapse.social.studioasinc.shared.domain.model.call.CallPhase
import com.synapse.social.studioasinc.shared.domain.model.call.CallRequest
import com.synapse.social.studioasinc.shared.domain.model.call.CallSession
import com.synapse.social.studioasinc.shared.domain.repository.CallRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A timer-driven call, for debug builds only.
 *
 * This file lives in `app/src/debug`, so no release binary contains it, and the release build cannot
 * select it even by accident: only `DebugCallModule` can bind it, and that module is compiled into the
 * debug variant alone. The binding is additionally opt-in — `call.simulation.enabled=true` in
 * `gradle.properties` — so a debug build dials for real unless a developer asks for the simulator.
 *
 * What it is for: exercising the call *UI* — every phase, control, error surface and teardown path —
 * without two devices, a signalling database or a TURN server. What it is not for: pretending a call
 * happened. Every phase change goes through [CallStateMachine], so a simulated state has to be a legal
 * state; the mock cannot reach [CallPhase.ACTIVE] except along an edge the production reducer accepts.
 *
 * Known limits, left in place because they are honest rather than cosmetic:
 *  - audio routes other than earpiece and speaker are refused, because there is no hardware here to
 *    route to (the real repository drives the actual device and accepts Bluetooth and wired headsets);
 *  - nothing is persisted, so a simulated call leaves no backend row and no call history.
 */
@Singleton
class MockCallRepository @Inject constructor(
    @Named("ApplicationScope") private val appScope: CoroutineScope
) : CallRepository, CallSimulationController {

    private val mutex = Mutex()
    private val sessionState = MutableStateFlow<CallSession?>(null)

    private var progressionJob: Job? = null
    private var noAnswerJob: Job? = null
    private var reconnectJob: Job? = null

    override fun observeSession(callId: String): Flow<CallSession?> =
        sessionState.asStateFlow().map { session -> session?.takeIf { it.id == callId } }

    override suspend fun start(request: CallRequest): Result<CallSession> = mutex.withLock {
        cancelTimers()

        val isIncoming = request.direction == CallDirection.INCOMING
        val isVideo = request.mediaType == CallMediaType.VIDEO
        val session = CallSession(
            id = UUID.randomUUID().toString(),
            participant = request.participant,
            chatId = request.chatId,
            direction = request.direction,
            mediaType = request.mediaType,
            phase = if (isIncoming) CallPhase.RINGING else CallPhase.DIALING,
            audioRoute = if (isVideo) CallAudioRoute.SPEAKER else CallAudioRoute.EARPIECE,
            isLocalVideoEnabled = isVideo && !isIncoming,
            isRemoteVideoEnabled = isVideo,
            startedAtMillis = System.currentTimeMillis()
        )
        sessionState.value = session

        if (isIncoming) {
            noAnswerJob = appScope.launch {
                delay(NO_ANSWER_DURATION_MS)
                finish(session.id, CallEndReason.NO_ANSWER)
            }
        } else {
            progressionJob = appScope.launch {
                delay(DIALING_DURATION_MS)
                transition(session.id, CallPhase.RINGING)
                delay(RINGING_DURATION_MS)
                transition(session.id, CallPhase.CONNECTING)
                delay(CONNECTING_DURATION_MS)
                connect(session.id)
            }
        }

        Result.success(session)
    }

    override suspend fun accept(callId: String): Result<Unit> = mutex.withLock {
        val current = sessionState.value
        if (current?.id != callId || current.direction != CallDirection.INCOMING) {
            return@withLock Result.failure(IllegalStateException("No incoming call to accept"))
        }
        if (current.phase == CallPhase.CONNECTING) {
            return@withLock Result.success(Unit)
        }
        if (current.phase != CallPhase.RINGING) {
            return@withLock Result.failure(IllegalStateException("Call cannot be accepted"))
        }

        noAnswerJob?.cancel()
        noAnswerJob = null
        val isVideo = current.mediaType == CallMediaType.VIDEO
        sessionState.value = current.copy(
            phase = CallPhase.CONNECTING,
            isLocalVideoEnabled = isVideo,
            isRemoteVideoEnabled = isVideo
        )
        progressionJob = appScope.launch {
            delay(CONNECTING_DURATION_MS)
            connect(callId)
        }
        Result.success(Unit)
    }

    override suspend fun decline(callId: String): Result<Unit> = mutex.withLock {
        val current = sessionState.value
        if (current?.id != callId || current.direction != CallDirection.INCOMING ||
            current.phase !in setOf(CallPhase.RINGING, CallPhase.CONNECTING)
        ) {
            return@withLock Result.failure(IllegalStateException("Call cannot be declined"))
        }
        sessionState.value = endSession(current, CallEndReason.DECLINED)
        Result.success(Unit)
    }

    override suspend fun end(callId: String): Result<Unit> = mutex.withLock {
        val current = sessionState.value
        if (current?.id != callId || current.phase == CallPhase.ENDED) {
            return@withLock Result.failure(IllegalStateException("No active call to end"))
        }
        sessionState.value = endSession(current, CallStateMachine.claimEndReason(current))
        Result.success(Unit)
    }

    override suspend fun setMuted(callId: String, muted: Boolean): Result<Unit> = mutex.withLock {
        val current = usableSession(callId)
            ?: return@withLock Result.failure(IllegalStateException("No active call"))
        sessionState.value = current.copy(isMuted = muted)
        Result.success(Unit)
    }

    /**
     * Simulated routing, limited on purpose.
     *
     * There is no audio device behind this repository, so a route it cannot demonstrate would be a
     * checkbox rather than a test. The real repository reads the actual hardware and answers with
     * whatever device the system settled on, including Bluetooth and wired headsets.
     */
    override suspend fun setAudioRoute(callId: String, route: CallAudioRoute): Result<Unit> = mutex.withLock {
        if (route != CallAudioRoute.EARPIECE && route != CallAudioRoute.SPEAKER) {
            return@withLock Result.failure(IllegalArgumentException("Audio route is not connected"))
        }
        val current = usableSession(callId)
            ?: return@withLock Result.failure(IllegalStateException("No active call"))
        sessionState.value = current.copy(audioRoute = route)
        Result.success(Unit)
    }

    override suspend fun setLocalVideoEnabled(callId: String, enabled: Boolean): Result<Unit> = mutex.withLock {
        val current = usableSession(callId)
            ?: return@withLock Result.failure(IllegalStateException("No active call"))
        if (current.mediaType != CallMediaType.VIDEO) {
            return@withLock Result.failure(IllegalStateException("Call is not a video call"))
        }
        sessionState.value = current.copy(isLocalVideoEnabled = enabled)
        Result.success(Unit)
    }

    override suspend fun flipCamera(callId: String): Result<Unit> = mutex.withLock {
        val current = usableSession(callId)
            ?: return@withLock Result.failure(IllegalStateException("No active call"))
        if (current.mediaType != CallMediaType.VIDEO) {
            return@withLock Result.failure(IllegalStateException("Call is not a video call"))
        }
        sessionState.value = current.copy(isFrontCamera = !current.isFrontCamera)
        Result.success(Unit)
    }

    override suspend fun switchMediaType(callId: String, mediaType: CallMediaType): Result<Unit> = mutex.withLock {
        val current = usableSession(callId)
            ?: return@withLock Result.failure(IllegalStateException("No active call"))
        if (current.phase !in setOf(CallPhase.ACTIVE, CallPhase.RECONNECTING)) {
            return@withLock Result.failure(IllegalStateException("Call is not active"))
        }
        if (current.mediaType == mediaType) {
            return@withLock Result.success(Unit)
        }
        sessionState.value = if (mediaType == CallMediaType.VIDEO) {
            current.copy(
                mediaType = CallMediaType.VIDEO,
                isLocalVideoEnabled = true,
                isRemoteVideoEnabled = true,
                audioRoute = CallAudioRoute.SPEAKER
            )
        } else {
            current.copy(
                mediaType = CallMediaType.VOICE,
                isLocalVideoEnabled = false,
                isRemoteVideoEnabled = false,
                audioRoute = CallAudioRoute.EARPIECE
            )
        }
        Result.success(Unit)
    }

    override fun simulate(action: CallSimulationAction) {
        appScope.launch {
            mutex.withLock {
                val current = sessionState.value ?: return@withLock
                if (current.phase == CallPhase.ENDED) return@withLock

                when (action) {
                    CallSimulationAction.GOOD_CONNECTION ->
                        if (CallStateMachine.isLiveConnection(current.phase)) {
                            sessionState.value = current.copy(connectionQuality = CallConnectionQuality.GOOD)
                        }

                    CallSimulationAction.WEAK_CONNECTION ->
                        if (CallStateMachine.isLiveConnection(current.phase)) {
                            sessionState.value = current.copy(connectionQuality = CallConnectionQuality.FAIR)
                        }

                    CallSimulationAction.POOR_CONNECTION ->
                        if (CallStateMachine.isLiveConnection(current.phase)) {
                            sessionState.value = current.copy(connectionQuality = CallConnectionQuality.POOR)
                        }

                    CallSimulationAction.RECONNECT -> {
                        val moved = CallStateMachine.move(
                            current.copy(connectionQuality = CallConnectionQuality.POOR),
                            CallPhase.RECONNECTING
                        )
                        if (moved.phase == CallPhase.RECONNECTING) {
                            sessionState.value = moved
                            reconnectJob?.cancel()
                            reconnectJob = appScope.launch {
                                delay(RECONNECT_DURATION_MS)
                                mutex.withLock {
                                    val reconnecting = sessionState.value
                                    if (reconnecting?.id == current.id &&
                                        reconnecting.phase == CallPhase.RECONNECTING
                                    ) {
                                        sessionState.value = CallStateMachine.move(
                                            reconnecting.copy(connectionQuality = CallConnectionQuality.GOOD),
                                            CallPhase.ACTIVE
                                        )
                                    }
                                }
                            }
                        }
                    }

                    CallSimulationAction.REMOTE_VIDEO_OFF ->
                        if (current.mediaType == CallMediaType.VIDEO &&
                            CallStateMachine.isLiveConnection(current.phase)
                        ) {
                            sessionState.value = current.copy(isRemoteVideoEnabled = false)
                        }

                    CallSimulationAction.REMOTE_VIDEO_ON ->
                        if (current.mediaType == CallMediaType.VIDEO &&
                            CallStateMachine.isLiveConnection(current.phase)
                        ) {
                            sessionState.value = current.copy(isRemoteVideoEnabled = true)
                        }

                    CallSimulationAction.REMOTE_DECLINES ->
                        if (current.direction == CallDirection.OUTGOING &&
                            current.phase in setOf(CallPhase.DIALING, CallPhase.RINGING)
                        ) {
                            sessionState.value = endSession(current, CallEndReason.DECLINED)
                        }

                    CallSimulationAction.NO_ANSWER ->
                        if (current.direction == CallDirection.OUTGOING &&
                            current.connectedAtMillis == null &&
                            current.phase in setOf(CallPhase.DIALING, CallPhase.RINGING, CallPhase.CONNECTING)
                        ) {
                            sessionState.value = endSession(current, CallEndReason.NO_ANSWER)
                        }

                    CallSimulationAction.UNAVAILABLE ->
                        if (current.connectedAtMillis == null) {
                            sessionState.value = endSession(current, CallEndReason.UNAVAILABLE)
                        }

                    CallSimulationAction.REMOTE_ENDS ->
                        if (current.connectedAtMillis != null) {
                            sessionState.value = endSession(current, CallEndReason.REMOTE_ENDED)
                        }
                }
            }
        }
    }

    private suspend fun transition(callId: String, to: CallPhase) = mutex.withLock {
        val current = sessionState.value
        if (current?.id == callId) {
            sessionState.value = CallStateMachine.move(current, to)
        }
    }

    private suspend fun connect(callId: String) = mutex.withLock {
        val current = sessionState.value
        if (current?.id == callId) {
            sessionState.value = CallStateMachine.move(current, CallPhase.ACTIVE)
        }
    }

    private suspend fun finish(callId: String, reason: CallEndReason) = mutex.withLock {
        val current = sessionState.value
        if (current?.id == callId && current.phase != CallPhase.ENDED) {
            sessionState.value = endSession(current, reason)
        }
    }

    private fun usableSession(callId: String): CallSession? =
        sessionState.value?.takeIf { it.id == callId && it.phase != CallPhase.ENDED }

    private fun endSession(current: CallSession, reason: CallEndReason): CallSession {
        cancelTimers()
        return CallStateMachine.move(current.copy(endReason = reason), CallPhase.ENDED)
    }

    private fun cancelTimers() {
        progressionJob?.cancel()
        progressionJob = null
        noAnswerJob?.cancel()
        noAnswerJob = null
        reconnectJob?.cancel()
        reconnectJob = null
    }

    private companion object {
        const val DIALING_DURATION_MS = 1_000L
        const val RINGING_DURATION_MS = 1_800L
        const val CONNECTING_DURATION_MS = 800L
        const val NO_ANSWER_DURATION_MS = 30_000L
        const val RECONNECT_DURATION_MS = 2_500L
    }
}
