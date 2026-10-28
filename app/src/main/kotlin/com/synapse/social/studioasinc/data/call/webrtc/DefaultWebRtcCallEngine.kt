package com.synapse.social.studioasinc.data.call.webrtc

import android.content.Context
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Handler
import com.synapse.social.studioasinc.shared.domain.model.call.CallMediaType
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.aakira.napier.Napier
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.CandidatePairChangeEvent
import org.webrtc.CameraVideoCapturer
import org.webrtc.CapturerObserver
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.IceCandidate
import org.webrtc.IceCandidateErrorEvent
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RTCStats
import org.webrtc.RTCStatsCollectorCallback
import org.webrtc.RTCStatsReport
import org.webrtc.RtpReceiver
import org.webrtc.RtpSender
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import org.webrtc.audio.JavaAudioDeviceModule

/**
 * The real WebRTC engine: one instance owns one `PeerConnectionFactory`, one `PeerConnection`, and
 * the capture pipeline for exactly one call.
 *
 * Threading model, all of it deliberate:
 *  - Every native signaling object is created, mutated and destroyed on [WebRtcRuntime]'s single
 *    signaling thread. WebRTC also fires every `PeerConnection.Observer` callback on that thread, so
 *    the negotiation state machine below is single-threaded by construction and needs no locks.
 *  - Camera calls run on the `SurfaceTextureHelper` thread, which is the thread WebRTC's capturers
 *    treat as the camera thread. They are posted and awaited with a timeout, and the wait happens on
 *    [Dispatchers.IO] so a wedged camera can never block the UI or the signaling thread.
 *  - Nothing here is ever touched from the UI thread.
 */
class DefaultWebRtcCallEngine(
    private val context: Context,
    private val runtime: WebRtcRuntime,
    private val mediaRegistry: CallMediaRegistry,
    private val engineScope: CoroutineScope
) : CallEngine {

    private val _events = Channel<EngineEvent>(Channel.BUFFERED)
    private val _stats = Channel<EngineStats>(Channel.CONFLATED)
    override val events: Flow<EngineEvent> = _events.receiveAsFlow()
    override val stats: Flow<EngineStats> = _stats.receiveAsFlow()

    private val capturerFactory = VideoCapturerFactory(context)

    // ---- Signaling-thread state ---------------------------------------------------

    @Volatile private var config: EngineConfig? = null
    @Volatile private var factory: PeerConnectionFactory? = null
    @Volatile private var audioDeviceModule: JavaAudioDeviceModule? = null
    @Volatile private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var audioTrack: AudioTrack? = null
    private var audioSender: RtpSender? = null
    private var videoSource: VideoSource? = null
    private var videoTrack: VideoTrack? = null
    private var videoSender: RtpSender? = null
    private var remoteVideoTrack: VideoTrack? = null
    private var negotiation: CompletableDeferred<EngineSdp>? = null
    private var gatheringPending: CompletableDeferred<EngineSdp>? = null

    /**
     * True once the local description has been handed to the repository. Candidates gathered after
     * that moment are not inside the SDP the peer already holds and must travel as their own message.
     */
    private var descriptionHandedOver = false

    // ---- Capture-thread state -----------------------------------------------------

    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var capturer: CameraVideoCapturer? = null

    @Volatile private var captureWidth = EngineConfig.DEFAULT_CAPTURE_WIDTH
    @Volatile private var captureHeight = EngineConfig.DEFAULT_CAPTURE_HEIGHT
    @Volatile private var captureFps = EngineConfig.DEFAULT_CAPTURE_FPS
    @Volatile private var isFrontCamera = true

    // ---- Engine flags -------------------------------------------------------------

    @Volatile private var opened = false
    @Volatile private var disposed = false
    private var statsJob: Job? = null

    // Written only from the stats coroutine; read nowhere else.
    private var previousInboundBytes = -1L
    private var previousSampleAtMillis = 0L

    override val isOpen: Boolean get() = opened && !disposed && peerConnection != null

    // ---- Lifecycle ----------------------------------------------------------------

    override suspend fun open(config: EngineConfig): Result<Unit> {
        if (disposed) return fail("ENGINE_DISPOSED", "This engine has already been disposed")
        if (opened) return fail("ENGINE_ALREADY_OPEN", "This engine is already open")
        this.config = config
        return runtime.onSignaling {
            runCatching {
                val audioDevice = JavaAudioDeviceModule.builder(context)
                    .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                    .setUseHardwareAcousticEchoCanceler(JavaAudioDeviceModule.isBuiltInAcousticEchoCancelerSupported())
                    .setUseHardwareNoiseSuppressor(JavaAudioDeviceModule.isBuiltInNoiseSuppressorSupported())
                    .setUseLowLatency(true)
                    .setAudioRecordErrorCallback(recordErrors)
                    .setAudioTrackErrorCallback(trackErrors)
                    .createAudioDeviceModule()
                audioDeviceModule = audioDevice

                val connectionFactory = PeerConnectionFactory.builder()
                    .setAudioDeviceModule(audioDevice)
                    .setVideoEncoderFactory(DefaultVideoEncoderFactory(runtime.eglContext, true, false))
                    .setVideoDecoderFactory(DefaultVideoDecoderFactory(runtime.eglContext))
                    .createPeerConnectionFactory()
                factory = connectionFactory

                val source = connectionFactory.createAudioSource(audioCaptureConstraints())
                val track = connectionFactory.createAudioTrack(AUDIO_TRACK_ID, source)
                track.setEnabled(true)
                audioSource = source
                audioTrack = track

                // createPeerConnection is @Nullable: a null here means the native stack refused the
                // configuration, which has to fail the call rather than leave a half-open engine.
                val connection = connectionFactory.createPeerConnection(
                    buildConfiguration(config.iceServers),
                    observer
                ) ?: throw EngineException("PC_CREATE_FAILED", "WebRTC could not create a peer connection")
                peerConnection = connection
                audioSender = connection.addTrack(track, listOf(streamId(config.callId)))
                opened = true
                startStatsCollection()
                Napier.i("Call engine opened (media=${config.mediaType}, initiator=${config.isInitiator})")
            }.fold(
                onSuccess = { Result.success(Unit) },
                onFailure = { error ->
                    Napier.e("Call engine could not open: ${error.message}", error)
                    teardownOnSignaling()
                    fail("ENGINE_OPEN_FAILED", error.message ?: "WebRTC initialization failed")
                }
            )
        }
    }

    override fun dispose() {
        if (disposed) return
        disposed = true
        opened = false
        statsJob?.cancel()
        statsJob = null
        config?.let { mediaRegistry.release(it.callId) }
        val abandoned = EngineException("ENGINE_DISPOSED", "The call ended while the engine was working")
        negotiation?.completeExceptionally(abandoned)
        gatheringPending?.completeExceptionally(abandoned)
        negotiation = null
        gatheringPending = null
        // Queued behind every other signaling operation, so teardown order stays deterministic even
        // when dispose() races an in-flight renegotiation.
        runtime.postToSignaling { teardownOnSignaling() }
    }

    /** Releases every native object in dependency order: capture → tracks → connection → factory. */
    private fun teardownOnSignaling() {
        // The camera must be handed back before anything else lets go: the in-call UI asserts the
        // indicator is off the moment a call ends, and nothing else needs the signaling thread now.
        stopCaptureAndRelease()
        val connection = peerConnection
        peerConnection = null
        runCatching { videoSender?.let { connection?.removeTrack(it) } }
        runCatching { audioSender?.let { connection?.removeTrack(it) } }
        videoSender = null
        audioSender = null
        remoteVideoTrack = null
        runCatching { connection?.close() }
        runCatching { connection?.dispose() }
        runCatching { videoTrack?.dispose() }
        runCatching { videoSource?.dispose() }
        videoTrack = null
        videoSource = null
        runCatching { audioTrack?.dispose() }
        runCatching { audioSource?.dispose() }
        audioTrack = null
        audioSource = null
        runCatching { factory?.dispose() }
        factory = null
        // The factory holds the module, so the module is released only after the factory is gone.
        runCatching { audioDeviceModule?.release() }
        audioDeviceModule = null
        _events.trySend(EngineEvent.ConnectionStateChanged(EngineConnectionState.CLOSED))
        _events.close()
        _stats.close()
        Napier.i("Call engine disposed")
    }

    // ---- Negotiation --------------------------------------------------------------

    override suspend fun createOffer(iceRestart: Boolean): Result<EngineSdp> {
        val pending = runtime.onSignaling { beginNegotiation(asOffer = true, iceRestart = iceRestart) }
        return awaitNegotiation(pending)
    }

    override suspend fun createAnswer(): Result<EngineSdp> {
        val pending = runtime.onSignaling { beginNegotiation(asOffer = false, iceRestart = false) }
        return awaitNegotiation(pending)
    }

    private fun beginNegotiation(
        asOffer: Boolean,
        iceRestart: Boolean
    ): CompletableDeferred<EngineSdp>? {
        val connection = peerConnection ?: return null
        negotiation?.takeIf { !it.isCompleted }?.let { return it }
        val pending = CompletableDeferred<EngineSdp>()
        negotiation = pending
        gatheringPending = pending
        descriptionHandedOver = false
        if (iceRestart) {
            runCatching { connection.restartIce() }
                .onFailure { Napier.w("restartIce() was refused: ${it.message}") }
        }
        val observer = SdpCallback(
            createSuccess = { description ->
                connection.setLocalDescription(
                    SdpCallback(
                        setSuccess = { armGatheringWait(connection, pending) },
                        failure = { reason ->
                            failNegotiation(pending, "SET_LOCAL_FAILED", reason)
                        }
                    ),
                    description
                )
            },
            failure = { reason ->
                failNegotiation(pending, if (asOffer) "OFFER_FAILED" else "ANSWER_FAILED", reason)
            }
        )
        if (asOffer) {
            connection.createOffer(observer, offerConstraints())
        } else {
            connection.createAnswer(observer, answerConstraints())
        }
        return pending
    }

    /**
     * Publishes the local description when ICE gathering finishes, or after a bounded wait.
     *
     * Waiting for the full list is what lets candidates ride inside the SDP, which keeps the
     * signaling table at a handful of rows per call. The deadline is what stops a black-holed TURN
     * from turning a call into a hang; whatever gathers later is emitted as [EngineEvent.LocalCandidate].
     */
    private fun armGatheringWait(connection: PeerConnection, pending: CompletableDeferred<EngineSdp>) {
        if (connection.iceGatheringState() == PeerConnection.IceGatheringState.COMPLETE) {
            handOverDescription(connection, pending)
            return
        }
        engineScope.launch {
            delay(ICE_GATHER_DEADLINE_MS)
            runtime.postToSignaling { handOver(connection, pending) }
        }
    }

    private fun failNegotiation(pending: CompletableDeferred<EngineSdp>, code: String, reason: String?) {
        if (gatheringPending === pending) gatheringPending = null
        if (negotiation === pending) negotiation = null
        pending.completeExceptionally(EngineException(code, reason ?: "SDP negotiation failed"))
    }

    private fun handOver(connection: PeerConnection, pending: CompletableDeferred<EngineSdp>) {
        if (!pending.isActive) return
        if (gatheringPending === pending) gatheringPending = null
        handOverDescription(connection, pending)
    }

    private fun handOverDescription(connection: PeerConnection, pending: CompletableDeferred<EngineSdp>) {
        descriptionHandedOver = true
        val local = connection.localDescription
        if (local == null) {
            failNegotiation(pending, "NO_LOCAL_DESCRIPTION", "WebRTC produced no local description")
            return
        }
        val sdp = EngineSdp(
            type = if (local.type == SessionDescription.Type.ANSWER) EngineSdpType.ANSWER else EngineSdpType.OFFER,
            sdp = local.description
        )
        pending.complete(sdp)
        if (negotiation === pending) negotiation = null
        _events.trySend(EngineEvent.LocalDescription(sdp))
    }

    private suspend fun awaitNegotiation(pending: CompletableDeferred<EngineSdp>?): Result<EngineSdp> {
        if (pending == null) return fail("ENGINE_NOT_OPEN", "The engine is not open")
        val sdp = withTimeoutOrNull(NEGOTIATION_TIMEOUT_MS) {
            runCatching { pending.await() }.getOrNull()
        }
        return sdp?.let { Result.success(it) }
            ?: fail("NEGOTIATION_TIMEOUT", "Timed out waiting for the local description")
    }

    override suspend fun setRemoteDescription(sdp: EngineSdp): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            runtime.postToSignaling {
                val connection = peerConnection
                if (connection == null) {
                    continuation.resumeResult(fail<Unit>("ENGINE_NOT_OPEN", "The engine is not open"))
                    return@postToSignaling
                }
                val type = when (sdp.type) {
                    EngineSdpType.OFFER -> SessionDescription.Type.OFFER
                    EngineSdpType.ANSWER -> SessionDescription.Type.ANSWER
                }
                connection.setRemoteDescription(
                    SdpCallback(
                        setSuccess = { continuation.resumeResult(Result.success(Unit)) },
                        failure = { reason ->
                            Napier.w("setRemoteDescription failed: $reason")
                            continuation.resumeResult(
                                fail("SET_REMOTE_FAILED", reason ?: "The remote description was rejected")
                            )
                        }
                    ),
                    SessionDescription(type, sdp.sdp)
                )
            }
        }

    override suspend fun addRemoteCandidates(candidates: List<EngineCandidate>): Result<Unit> {
        if (candidates.isEmpty()) return Result.success(Unit)
        return runtime.onSignaling {
            val connection = peerConnection
                ?: return@onSignaling fail("ENGINE_NOT_OPEN", "The engine is not open")
            val accepted = candidates.count { candidate ->
                runCatching {
                    connection.addIceCandidate(
                        IceCandidate(candidate.sdpMid.orEmpty(), candidate.sdpMLineIndex, candidate.sdp)
                    )
                }.getOrDefault(false)
            }
            if (accepted == 0) {
                fail("ICE_ADD_REJECTED", "WebRTC refused every remote candidate")
            } else {
                Result.success(Unit)
            }
        }
    }

    // ---- Media --------------------------------------------------------------------

    override suspend fun setMuted(muted: Boolean): Result<Unit> = runtime.onSignaling {
        val track = audioTrack ?: return@onSignaling fail("ENGINE_NOT_OPEN", "The engine is not open")
        track.setEnabled(!muted)
        // Muting the device module too means silence reaches the peer even if a renegotiation were
        // to re-enable the track — mute must be something the other side can hear, not just see.
        audioDeviceModule?.setMicrophoneMute(muted)
        Result.success(Unit)
    }

    override suspend fun startVideo(isFrontCamera: Boolean): Result<Unit> {
        val currentConfig = config ?: return fail("ENGINE_NOT_OPEN", "The engine is not open")
        if (currentConfig.mediaType != CallMediaType.VIDEO) {
            return fail("NOT_A_VIDEO_CALL", "Voice calls do not capture video")
        }
        val alreadyRunning = runtime.onSignaling { videoTrack != null }
        if (alreadyRunning) return Result.success(Unit)

        // Camera enumeration and format selection talk to the camera service; keep it off the caller.
        val selection = withContext(Dispatchers.IO) {
            capturerFactory.create(
                preferFront = isFrontCamera,
                requestedWidth = currentConfig.captureWidth,
                requestedHeight = currentConfig.captureHeight,
                requestedFps = currentConfig.captureFps,
                eventsHandler = cameraEvents
            )
        } ?: run {
            emit(EngineEvent.MediaError(EngineTrackKind.VIDEO, EngineMediaIssue.CAMERA_ERROR, "No usable camera"))
            return fail("NO_CAMERA", "This device exposes no usable camera")
        }
        captureWidth = selection.width
        captureHeight = selection.height
        captureFps = selection.fps
        this.isFrontCamera = selection.isFrontFacing

        val helper = withContext(Dispatchers.IO) {
            SurfaceTextureHelper.create(CAPTURE_THREAD_NAME, runtime.eglContext)
        }
        surfaceTextureHelper = helper

        val track = runtime.onSignaling {
            val connectionFactory = factory ?: return@onSignaling null
            val source = connectionFactory.createVideoSource(false)
            val created = connectionFactory.createVideoTrack(VIDEO_TRACK_ID, source)
            created.setEnabled(true)
            videoSource = source
            videoTrack = created
            created
        } ?: run {
            withContext(Dispatchers.IO) { stopCaptureAndRelease() }
            return fail("ENGINE_NOT_OPEN", "The engine is not open")
        }

        val observer: CapturerObserver = videoSource?.capturerObserver ?: run {
            withContext(Dispatchers.IO) { stopCaptureAndRelease() }
            return fail("ENGINE_NOT_OPEN", "The video source disappeared")
        }
        val started = withContext(Dispatchers.IO) {
            callOnCaptureThread("START_CAPTURE") {
                runCatching {
                    selection.capturer.initialize(helper, context, observer)
                    selection.capturer.startCapture(selection.width, selection.height, selection.fps)
                    capturer = selection.capturer
                }
            }
        }
        if (started == null || started.isFailure) {
            val reason = started?.exceptionOrNull()?.message ?: "The camera did not respond"
            Napier.e("Camera capture could not start: $reason")
            emit(EngineEvent.MediaError(EngineTrackKind.VIDEO, EngineMediaIssue.CAMERA_ERROR, "Camera start failed"))
            runtime.onSignaling { releaseVideoObjects() }
            withContext(Dispatchers.IO) { stopCaptureAndRelease() }
            return fail("CAMERA_START_FAILED", reason)
        }

        val attached = runtime.onSignaling {
            val connection = peerConnection ?: return@onSignaling false
            videoSender = connection.addTrack(track, listOf(streamId(currentConfig.callId)))
            mediaRegistry.setTrack(currentConfig.callId, CallMediaRegistry.Role.LOCAL, track)
            true
        }
        if (!attached) {
            runtime.onSignaling { releaseVideoObjects() }
            withContext(Dispatchers.IO) { stopCaptureAndRelease() }
            return fail("ENGINE_NOT_OPEN", "The engine is not open")
        }
        Napier.i("Video capture live: ${captureWidth}x$captureHeight @ ${captureFps}fps front=$isFrontCamera")
        return Result.success(Unit)
    }

    override suspend fun stopVideo(): Result<Unit> {
        val currentConfig = config ?: return fail("ENGINE_NOT_OPEN", "The engine is not open")
        withContext(Dispatchers.IO) { stopCaptureAndRelease() }
        return runtime.onSignaling {
            releaseVideoObjects()
            mediaRegistry.setTrack(currentConfig.callId, CallMediaRegistry.Role.LOCAL, null)
            _events.trySend(EngineEvent.LocalVideoEnabled(enabled = false))
            Napier.i("Video stopped, camera released to the system")
            Result.success(Unit)
        }
    }

    override suspend fun setLocalVideoEnabled(enabled: Boolean): Result<Unit> = runtime.onSignaling {
        val track = videoTrack ?: return@onSignaling fail("NO_VIDEO_TRACK", "Video is not running")
        track.setEnabled(enabled)
        _events.trySend(EngineEvent.LocalVideoEnabled(enabled))
        Result.success(Unit)
    }

    override suspend fun flipCamera(): Result<Unit> = selectCamera(isFrontCamera = !isFrontCamera)

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun selectCamera(isFrontCamera: Boolean): Result<Unit> {
        if (surfaceTextureHelper == null || capturer == null) {
            return fail("NO_VIDEO_TRACK", "Video is not running")
        }
        val target = capturerFactory.pickCamera(preferFront = isFrontCamera)
            ?: return fail("NO_ALTERNATE_CAMERA", "This device has no camera with the requested orientation")
        val switched = CompletableDeferred<Unit>()
        val posted = withContext(Dispatchers.IO) {
            callOnCaptureThread("SWITCH_CAMERA") {
                capturer?.switchCamera(
                    object : CameraVideoCapturer.CameraSwitchHandler {
                        override fun onCameraSwitchDone(isFront: Boolean) {
                            this@DefaultWebRtcCallEngine.isFrontCamera = isFront
                            Napier.i("Camera switched: front=$isFront")
                            switched.complete(Unit)
                            _events.trySend(EngineEvent.LocalCameraSwitched(isFront))
                        }

                        override fun onCameraSwitchError(message: String) {
                            Napier.w("Camera switch failed: $message")
                            switched.completeExceptionally(EngineException("CAMERA_SWITCH_FAILED", message))
                            _events.trySend(
                                EngineEvent.MediaError(EngineTrackKind.VIDEO, EngineMediaIssue.CAMERA_ERROR, message)
                            )
                        }
                    },
                    target
                )
            }
        }
        if (posted == null) return fail("CAMERA_UNAVAILABLE", "The camera is not reachable right now")
        val outcome = withTimeoutOrNull(CAMERA_SWITCH_TIMEOUT_MS) { runCatching { switched.await() }.getOrNull() }
        return if (outcome != null) {
            Result.success(Unit)
        } else {
            fail(
                "CAMERA_SWITCH_TIMEOUT",
                switched.getCompletionExceptionOrNull()?.message ?: "The camera did not report a switch"
            )
        }
    }

    override suspend fun selectInputDevice(deviceId: Int): Result<Unit> = runtime.onSignaling {
        val module = audioDeviceModule
            ?: return@onSignaling fail("ENGINE_NOT_OPEN", "The engine is not open")
        val manager = context.getSystemService(AudioManager::class.java)
        val device = manager?.availableCommunicationDevices?.firstOrNull { it.id == deviceId }
        if (device == null) {
            fail("AUDIO_DEVICE_UNAVAILABLE", "That audio input device is not available")
        } else {
            runCatching { module.setPreferredInputDevice(device) }
                .fold(
                    onSuccess = { Result.success(Unit) },
                    onFailure = { fail("AUDIO_DEVICE_UNAVAILABLE", it.message ?: "Audio input routing failed") }
                )
        }
    }

    override suspend fun updateIceServers(servers: List<IceServerSpec>): Result<Unit> = runtime.onSignaling {
        val connection = peerConnection
            ?: return@onSignaling fail("ENGINE_NOT_OPEN", "The engine is not open")
        if (connection.setConfiguration(buildConfiguration(servers))) {
            // No restartIce() here on purpose: the repository follows up with
            // createOffer(iceRestart = true), which stays the single place a restart is decided.
            Napier.i("ICE servers refreshed (${servers.count { it.isTurn }} TURN entry/entries)")
            Result.success(Unit)
        } else {
            fail("ICE_CONFIG_REJECTED", "WebRTC rejected the refreshed ICE configuration")
        }
    }

    // ---- Capture thread -----------------------------------------------------------

    /**
     * Runs [block] on the capture thread and waits for it. Returns null when the helper is gone or
     * the camera service stopped responding, so callers always get an answer, never a hang.
     * Must not be called from the capture thread itself.
     */
    private fun <T> callOnCaptureThread(tag: String, block: () -> T): T? {
        val handler: Handler = surfaceTextureHelper?.handler ?: return null
        val box = arrayOfNulls<Any>(1)
        var produced = false
        val done = CountDownLatch(1)
        if (!handler.post {
                try {
                    box[0] = block()
                    produced = true
                } catch (error: Throwable) {
                    Napier.w("Capture operation $tag threw: ${error.message}")
                } finally {
                    done.countDown()
                }
            }
        ) {
            return null
        }
        if (!done.await(CAMERA_OP_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            Napier.w("Capture operation $tag timed out after ${CAMERA_OP_TIMEOUT_MS}ms")
            return null
        }
        if (!produced) return null
        @Suppress("UNCHECKED_CAST")
        return box[0] as T
    }

    /**
     * Stops capture, frees the capturer and disposes the helper. Blocks for at most
     * [CAMERA_OP_TIMEOUT_MS]; callers that are not the teardown path run this on [Dispatchers.IO].
     * Safe to call when nothing is capturing, and never throws.
     */
    private fun stopCaptureAndRelease() {
        val helper = surfaceTextureHelper ?: return
        val released = callOnCaptureThread("RELEASE_CAPTURE") { releaseCaptureOnCaptureThread() }
        if (released == null) {
            // The capture thread never answered. Drop our references anyway and ask the helper to
            // release itself on its own thread later; a leaked camera handle is worse than a late one.
            Napier.w("Capture release unconfirmed; scheduling self-dispose on the capture thread")
            helper.handler.post { releaseCaptureOnCaptureThread() }
        }
        surfaceTextureHelper = null
    }

    /** Runs on the capture thread. */
    private fun releaseCaptureOnCaptureThread() {
        runCatching { capturer?.stopCapture() }
        runCatching { capturer?.dispose() }
        capturer = null
        surfaceTextureHelper?.let { helper ->
            runCatching { helper.stopListening() }
            // dispose() has to happen on the helper's own thread, which is exactly where we are.
            runCatching { helper.dispose() }
        }
        surfaceTextureHelper = null
    }

    /** Must run on the signaling thread: unpublishes and disposes the local video objects. */
    private fun releaseVideoObjects() {
        val connection = peerConnection
        videoSender?.let { sender -> runCatching { connection?.removeTrack(sender) } }
        videoSender = null
        runCatching { videoTrack?.dispose() }
        runCatching { videoSource?.dispose() }
        videoTrack = null
        videoSource = null
    }

    // ---- Stats --------------------------------------------------------------------

    private fun startStatsCollection() {
        statsJob?.cancel()
        statsJob = engineScope.launch {
            while (isOpen) {
                val report = collectStatsReport() ?: break
                parseReport(report)?.let { _stats.trySend(it) }
                delay(STATS_INTERVAL_MS)
            }
        }
    }

    private suspend fun collectStatsReport(): RTCStatsReport? = suspendCancellableCoroutine { continuation ->
        val connection = peerConnection
        if (connection == null || disposed) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        runtime.postToSignaling {
            runCatching {
                connection.getStats(RTCStatsCollectorCallback { report ->
                    if (continuation.isActive) continuation.resume(report)
                })
            }.onFailure {
                Napier.w("getStats failed: ${it.message}")
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }

    private fun parseReport(report: RTCStatsReport): EngineStats? {
        var selected: RTCStats? = null
        var selectedScore = -1
        val localCandidateTypes = HashMap<String, String>()
        var inboundBytes = 0L
        var audioReceived = 0L
        var audioLost = 0L
        var audioJitterSeconds: Double? = null
        var videoFramesPerSecond: Int? = null

        report.statsMap.values.forEach { stat ->
            val members = stat.members
            when (stat.type) {
                "local-candidate" -> members.stringValue("candidateType")?.let { localCandidateTypes[stat.id] = it }

                "candidate-pair" -> {
                    val nominated = members["nominated"] == true
                    val state = members.stringValue("state")
                    val usable = state == "succeeded" || state == "in-use" || state == "active"
                    val score = when {
                        nominated && usable -> 3
                        nominated -> 2
                        usable -> 1
                        else -> 0
                    }
                    if (score > selectedScore) {
                        selectedScore = score
                        selected = stat
                    }
                }

                "inbound-rtp" -> {
                    inboundBytes += members.longValue("bytesReceived")
                    when (members.stringValue("kind") ?: members.stringValue("mediaType")) {
                        "audio" -> {
                            audioReceived += members.longValue("packetsReceived")
                            audioLost += members.longValue("packetsLost")
                            audioJitterSeconds = members.doubleValue("jitter") ?: audioJitterSeconds
                        }

                        "video" -> videoFramesPerSecond = members.intValue("framesPerSecond") ?: videoFramesPerSecond
                    }
                }
            }
        }

        val now = System.currentTimeMillis()
        val incomingBitrate = if (previousInboundBytes < 0L || previousSampleAtMillis == 0L) {
            null
        } else {
            val elapsedMs = (now - previousSampleAtMillis).coerceAtLeast(1L)
            ((inboundBytes - previousInboundBytes).coerceAtLeast(0L) * 8L * 1000L) / elapsedMs
        }
        previousInboundBytes = inboundBytes
        previousSampleAtMillis = now

        val pairMembers = selected?.members
        val localCandidateId = pairMembers?.stringValue("localCandidateId")
        val relayed = (localCandidateId?.let { localCandidateTypes[it] } == "relay") || pairMembers?.get("relay") == true
        val audioPackets = audioReceived + audioLost

        // Remote camera on/off is deliberately not inferred here: a stalled frame counter is not
        // proof that the peer switched off, and the UI must never claim something the network did not
        // confirm. That state comes from signaling, with hysteresis, in the connection-quality layer.
        return EngineStats(
            roundTripTimeMs = pairMembers?.doubleValue("currentRoundTripTime")?.times(1000.0),
            availableOutgoingBitrateBps = pairMembers?.doubleValue("availableOutgoingBitrate")?.toLong(),
            incomingBitrateBps = incomingBitrate,
            audioPacketLossRatio = if (audioPackets > 0L) audioLost.toFloat() / audioPackets.toFloat() else null,
            audioJitterMs = audioJitterSeconds?.times(1000.0),
            videoFramesPerSecond = videoFramesPerSecond,
            viaRelay = relayed,
            sampledAtMillis = now
        )
    }

    // ---- Callbacks ----------------------------------------------------------------

    private val observer = object : PeerConnection.Observer {
        override fun onSignalingChange(state: PeerConnection.SignalingState) {
            Napier.d("Signaling state: $state")
        }

        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
            val mapped = mapConnectionState(state)
            Napier.i("ICE connection state: $state")
            descriptionHandedOver = true
            _events.trySend(EngineEvent.ConnectionStateChanged(mapped))
        }

        override fun onIceConnectionReceivingChange(receiving: Boolean) {
            Napier.d("ICE receiving changed: $receiving")
        }

        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {
            if (state != PeerConnection.IceGatheringState.COMPLETE) return
            _events.trySend(EngineEvent.GatheringComplete)
            gatheringPending?.let { pending -> peerConnection?.let { connection -> handOver(connection, pending) } }
        }

        override fun onIceCandidate(candidate: IceCandidate) {
            // Before the hand-over the candidate is already inside the description we are about to
            // publish, so sending it separately would only add signaling rows and duplicates.
            if (!descriptionHandedOver) return
            _events.trySend(
                EngineEvent.LocalCandidate(
                    EngineCandidate(
                        sdp = candidate.sdp,
                        sdpMid = candidate.sdpMid,
                        sdpMLineIndex = candidate.sdpMLineIndex
                    )
                )
            )
        }

        override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {
            Napier.d("ICE candidates invalidated: ${candidates.size}")
        }

        override fun onIceCandidateError(event: IceCandidateErrorEvent) {
            // A rejected TURN allocation lands here. It is not fatal while another server works, but
            // it is never swallowed: the repository decides whether the call becomes TURN_UNAVAILABLE.
            Napier.w("ICE candidate error (${event.url}): ${event.errorText}")
            _events.trySend(EngineEvent.IceError(event.errorText ?: event.url ?: "ICE candidate error"))
        }

        override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent) = Unit

        override fun onRenegotiationNeeded() {
            _events.trySend(EngineEvent.RenegotiationNeeded)
        }

        override fun onAddStream(stream: MediaStream) = Unit

        override fun onRemoveStream(stream: MediaStream) = Unit

        override fun onDataChannel(channel: DataChannel) = Unit

        override fun onAddTrack(receiver: RtpReceiver, streams: Array<MediaStream>) {
            val callId = config?.callId ?: return
            when (val track = receiver.track()) {
                is VideoTrack -> {
                    remoteVideoTrack = track
                    track.setEnabled(true)
                    track.setShouldReceive(true)
                    mediaRegistry.setTrack(callId, CallMediaRegistry.Role.REMOTE, track)
                    _events.trySend(EngineEvent.RemoteVideoTrack(available = true, enabled = true))
                    Napier.i("Remote video track received")
                }

                is AudioTrack -> track.setEnabled(true)
            }
        }

        override fun onRemoveTrack(receiver: RtpReceiver) {
            val callId = config?.callId ?: return
            if (receiver.track() is VideoTrack) {
                remoteVideoTrack = null
                mediaRegistry.setTrack(callId, CallMediaRegistry.Role.REMOTE, null)
                _events.trySend(EngineEvent.RemoteVideoTrack(available = false, enabled = false))
            }
        }
    }

    private val cameraEvents = object : CameraVideoCapturer.CameraEventsHandler {
        override fun onCameraError(description: String) {
            Napier.w("Camera error: $description")
            emit(EngineEvent.MediaError(EngineTrackKind.VIDEO, EngineMediaIssue.CAMERA_ERROR, description))
        }

        override fun onCameraDisconnected() {
            // The system reclaimed the camera (another app, an incoming GSM call). Report it honestly
            // instead of keeping a frozen frame on screen.
            Napier.w("Camera taken by the system")
            emit(EngineEvent.MediaError(EngineTrackKind.VIDEO, EngineMediaIssue.CAMERA_DISCONNECTED, "Camera was released"))
        }

        override fun onCameraFreezed(message: String) {
            emit(EngineEvent.MediaError(EngineTrackKind.VIDEO, EngineMediaIssue.CAMERA_FROZEN, message))
        }

        override fun onCameraOpening(deviceName: String) = Unit

        override fun onFirstFrameAvailable() {
            Napier.i("First local camera frame rendered")
        }

        override fun onCameraClosed() = Unit
    }

    private val recordErrors = object : JavaAudioDeviceModule.AudioRecordErrorCallback {
        override fun onWebRtcAudioRecordInitError(message: String) =
            audioFailure("The microphone could not be initialised", message)

        override fun onWebRtcAudioRecordStartError(
            code: JavaAudioDeviceModule.AudioRecordStartErrorCode,
            message: String
        ) = audioFailure("The microphone could not start", message)

        override fun onWebRtcAudioRecordError(message: String) =
            audioFailure("Microphone failure", message)
    }

    private val trackErrors = object : JavaAudioDeviceModule.AudioTrackErrorCallback {
        override fun onWebRtcAudioTrackInitError(message: String) =
            audioFailure("The speaker could not be initialised", message)

        override fun onWebRtcAudioTrackStartError(
            code: JavaAudioDeviceModule.AudioTrackStartErrorCode,
            message: String
        ) = audioFailure("The speaker could not start", message)

        override fun onWebRtcAudioTrackError(message: String) =
            audioFailure("Speaker failure", message)
    }

    private fun audioFailure(summary: String, detail: String) {
        Napier.w("$summary: $detail")
        emit(EngineEvent.MediaError(EngineTrackKind.AUDIO, EngineMediaIssue.AUDIO_ERROR, summary))
    }

    private fun emit(event: EngineEvent) {
        _events.trySend(event)
    }

    // ---- Configuration helpers ----------------------------------------------------

    private fun buildConfiguration(servers: List<IceServerSpec>): PeerConnection.RTCConfiguration {
        val iceServers = servers.map { spec ->
            val builder = PeerConnection.IceServer.builder(spec.urls)
            spec.username?.let { builder.setUsername(it) }
            spec.credential?.let { builder.setPassword(it) }
            if (spec.isTurn) builder.setTlsCertPolicy(PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_SECURE)
            builder.createIceServer()
        }
        return PeerConnection.RTCConfiguration(ArrayList(iceServers)).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            // GATHER_CONTINUALLY keeps late candidates (network change, slow TURN relay) flowing so a
            // call that is already up can migrate without a full renegotiation.
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            iceTransportsType = PeerConnection.IceTransportsType.ALL
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
            keyType = PeerConnection.KeyType.ECDSA
            enableDscp = true
            // A relay-only path is still a real call: do not wait for a direct candidate to declare it
            // writable, otherwise symmetric-NAT users sit in CONNECTING for no reason.
            presumeWritableWhenFullyRelayed = true
            pruneTurnPorts = false
            iceCandidatePoolSize = ICE_CANDIDATE_POOL_SIZE
        }
    }

    private fun mapConnectionState(state: PeerConnection.IceConnectionState): EngineConnectionState = when (state) {
        PeerConnection.IceConnectionState.NEW -> EngineConnectionState.NEW
        PeerConnection.IceConnectionState.CHECKING -> EngineConnectionState.CONNECTING
        PeerConnection.IceConnectionState.CONNECTED,
        PeerConnection.IceConnectionState.COMPLETED -> EngineConnectionState.CONNECTED
        PeerConnection.IceConnectionState.DISCONNECTED -> EngineConnectionState.DISCONNECTED
        PeerConnection.IceConnectionState.FAILED -> EngineConnectionState.FAILED
        PeerConnection.IceConnectionState.CLOSED -> EngineConnectionState.CLOSED
    }

    private fun offerConstraints(): MediaConstraints = MediaConstraints().apply {
        mandatory.add(pair("OfferToReceiveAudio", "true"))
        mandatory.add(pair("OfferToReceiveVideo", if (config?.mediaType == CallMediaType.VIDEO) "true" else "false"))
    }

    private fun answerConstraints(): MediaConstraints = MediaConstraints().apply {
        mandatory.add(pair("AnswerToReceiveAudio", "true"))
        mandatory.add(pair("AnswerToReceiveVideo", if (config?.mediaType == CallMediaType.VIDEO) "true" else "false"))
    }

    private fun audioCaptureConstraints(): MediaConstraints = MediaConstraints().apply {
        mandatory.add(pair("echoCancellation", "true"))
        mandatory.add(pair("noiseSuppression", "true"))
        mandatory.add(pair("autoGainControl", "true"))
    }

    private fun pair(key: String, value: String) = MediaConstraints.KeyValuePair(key, value)

    private fun streamId(callId: String) = "synapse-$callId"

    private fun <T> fail(code: String, message: String): Result<T> = Result.failure(EngineException(code, message))

    companion object {
        private const val AUDIO_TRACK_ID = "synapse-mic"
        private const val VIDEO_TRACK_ID = "synapse-camera"
        private const val CAPTURE_THREAD_NAME = "SynapseCameraCapture"

        /** How long we let ICE gather before publishing the description anyway. */
        private const val ICE_GATHER_DEADLINE_MS = 2_500L
        private const val NEGOTIATION_TIMEOUT_MS = 15_000L
        private const val CAMERA_OP_TIMEOUT_MS = 3_000L
        private const val CAMERA_SWITCH_TIMEOUT_MS = 4_000L
        private const val STATS_INTERVAL_MS = 2_000L
        private const val ICE_CANDIDATE_POOL_SIZE = 2
    }
}

/** Every engine-level failure carries a machine-readable code the repository maps to a UI state. */
class EngineException(
    val code: String,
    override val message: String
) : IllegalStateException("$code: $message")

/** Bridges WebRTC's four-method `SdpObserver` into the callbacks the engine actually cares about. */
private class SdpCallback(
    val createSuccess: (SessionDescription) -> Unit = { },
    val setSuccess: () -> Unit = { },
    val failure: (String?) -> Unit = { }
) : SdpObserver {
    override fun onCreateSuccess(description: SessionDescription) = createSuccess(description)
    override fun onSetSuccess() = setSuccess()
    override fun onCreateFailure(reason: String?) = failure(reason)
    override fun onSetFailure(reason: String?) = failure(reason)
}

/**
 * Produces one engine per call. Engines are stateful and must never be shared, so this is the only
 * type bound to the object graph — the repository owns the instance and disposes it.
 */
@Singleton
class DefaultCallEngineFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val runtime: WebRtcRuntime,
    private val mediaRegistry: CallMediaRegistry,
    @Named("ApplicationScope") private val appScope: CoroutineScope
) : CallEngineFactory {
    override fun create(): CallEngine = DefaultWebRtcCallEngine(context, runtime, mediaRegistry, appScope)
}

private fun Map<String, Any>.stringValue(key: String): String? = this[key] as? String
private fun Map<String, Any>.doubleValue(key: String): Double? = (this[key] as? Number)?.toDouble()
private fun Map<String, Any>.intValue(key: String): Int? = (this[key] as? Number)?.toInt()
private fun Map<String, Any>.longValue(key: String): Long = (this[key] as? Number)?.toLong() ?: 0L
private fun <T> CancellableContinuation<Result<T>>.resumeResult(value: Result<T>) {
    if (isActive) resume(value)
}
