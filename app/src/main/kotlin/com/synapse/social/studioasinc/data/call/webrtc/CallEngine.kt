package com.synapse.social.studioasinc.data.call.webrtc

import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import kotlinx.coroutines.flow.Flow

/**
 * Platform-neutral description of an ICE server handed to the WebRTC engine.
 * Credentials are minted per call by the backend; nothing here is persisted or logged.
 */
data class IceServerSpec(
    val urls: List<String>,
    val username: String? = null,
    val credential: String? = null
) {
    val isTurn: Boolean get() = urls.any { it.startsWith("turn", ignoreCase = true) }

    // Never include the credential: this can reach a log or a crash report.
    override fun toString(): String = "IceServerSpec(urls=$urls, authenticated=${username != null})"
}

data class EngineSdp(
    val type: EngineSdpType,
    val sdp: String
)

enum class EngineSdpType { OFFER, ANSWER }

data class EngineCandidate(
    val sdp: String,
    val sdpMid: String?,
    val sdpMLineIndex: Int
)

/** Everything the engine needs to build one peer connection. */
data class EngineConfig(
    val callId: String,
    val iceServers: List<IceServerSpec>,
    val mediaType: CallMediaType,
    val isInitiator: Boolean,
    val captureWidth: Int = DEFAULT_CAPTURE_WIDTH,
    val captureHeight: Int = DEFAULT_CAPTURE_HEIGHT,
    val captureFps: Int = DEFAULT_CAPTURE_FPS
) {
    companion object {
        const val DEFAULT_CAPTURE_WIDTH = 720
        const val DEFAULT_CAPTURE_HEIGHT = 1280
        const val DEFAULT_CAPTURE_FPS = 30
    }
}

/** Connection state as the engine observes it, decoupled from WebRTC's own enums. */
enum class EngineConnectionState { NEW, CONNECTING, CONNECTED, DISCONNECTED, FAILED, CLOSED }

enum class EngineTrackKind { AUDIO, VIDEO }

/** Capture failure reasons, surfaced to the UI instead of raw exceptions. */
enum class EngineMediaIssue { CAMERA_ERROR, CAMERA_DISCONNECTED, CAMERA_FROZEN, CAMERA_PERMISSION, AUDIO_ERROR }

sealed interface EngineEvent {
    /** A local description is ready to be published to the peer. */
    data class LocalDescription(val sdp: EngineSdp) : EngineEvent

    /**
     * A locally gathered candidate. Candidates gathered before the SDP is handed over are already
     * embedded in it; this only fires for late arrivals (relay after a slow TURN, network change).
     */
    data class LocalCandidate(val candidate: EngineCandidate) : EngineEvent

    /** ICE gathering finished for the current negotiation round. */
    data object GatheringComplete : EngineEvent

    data class ConnectionStateChanged(val state: EngineConnectionState) : EngineEvent

    /** The peer's video track appeared, vanished, or changed enabled state. */
    data class RemoteVideoTrack(val available: Boolean, val enabled: Boolean) : EngineEvent

    /** Our own video track changed enabled state — this is exactly what the peer now sees. */
    data class LocalVideoEnabled(val enabled: Boolean) : EngineEvent

    /** The front/rear lens actually changed. */
    data class LocalCameraSwitched(val isFront: Boolean) : EngineEvent

    /** Renegotiate: emit a new offer (or answer) when this arrives. */
    data object RenegotiationNeeded : EngineEvent

    /** Non-fatal ICE error, e.g. a TURN allocation rejection. */
    data class IceError(val message: String) : EngineEvent

    /** Capture failed (camera released by the system, permission revoked mid-call, ...). */
    data class MediaError(val kind: EngineTrackKind, val issue: EngineMediaIssue, val message: String) : EngineEvent
}

/**
 * Periodic snapshot of the real connection, used to drive the quality indicator.
 * Every field stays null until a real sample exists — quality is never guessed or defaulted.
 */
data class EngineStats(
    val roundTripTimeMs: Double? = null,
    val availableOutgoingBitrateBps: Long? = null,
    val incomingBitrateBps: Long? = null,
    val audioPacketLossRatio: Float? = null,
    val audioJitterMs: Double? = null,
    val videoFramesPerSecond: Int? = null,
    /** True when the selected candidate pair goes through TURN, i.e. direct P2P was impossible. */
    val viaRelay: Boolean = false,
    val sampledAtMillis: Long = 0L
)

/**
 * One engine instance == one call == one `PeerConnection`.
 *
 * The engine owns WebRTC completely: every call is marshalled onto the WebRTC signaling thread, so
 * callers never have to know about WebRTC's threading rules. Implementations must release every
 * native resource in [dispose] and must tolerate being disposed more than once.
 */
interface CallEngine {

    /** Cold start: factory, audio device module, peer connection, and local audio track. */
    suspend fun open(config: EngineConfig): Result<Unit>

    /** Deterministic teardown, in dependency order. Idempotent; never throws. */
    fun dispose()

    val events: Flow<EngineEvent>
    val stats: Flow<EngineStats>
    val isOpen: Boolean

    // ---- Negotiation ------------------------------------------------------------

    suspend fun createOffer(iceRestart: Boolean = false): Result<EngineSdp>
    suspend fun createAnswer(): Result<EngineSdp>
    suspend fun setRemoteDescription(sdp: EngineSdp): Result<Unit>
    suspend fun addRemoteCandidates(candidates: List<EngineCandidate>): Result<Unit>

    // ---- Media ------------------------------------------------------------------

    suspend fun setMuted(muted: Boolean): Result<Unit>

    /** Starts camera capture and adds the video track. Voice calls have nothing to start. */
    suspend fun startVideo(isFrontCamera: Boolean): Result<Unit>

    /** Stops capture and releases the camera, keeping the call alive as audio-only. */
    suspend fun stopVideo(): Result<Unit>

    /** Mutes/unmutes the video track. The camera is kept warm so re-enabling is instant. */
    suspend fun setLocalVideoEnabled(enabled: Boolean): Result<Unit>

    /** Switches lens; emits [EngineEvent.LocalCameraSwitched] with the real result. */
    suspend fun flipCamera(): Result<Unit>

    /** Points capture at a specific lens (used when the peer requests a different one). */
    suspend fun selectCamera(isFrontCamera: Boolean): Result<Unit>

    /** Redirects the microphone input to a platform audio device; no-op when unsupported. */
    suspend fun selectInputDevice(deviceId: Int): Result<Unit>

    /** Applies freshly minted TURN credentials mid-call and restarts ICE. */
    suspend fun updateIceServers(servers: List<IceServerSpec>): Result<Unit>
}

/** Creates engines; kept separate from [CallEngine] so repositories stay swappable. */
interface CallEngineFactory {
    fun create(): CallEngine
}
