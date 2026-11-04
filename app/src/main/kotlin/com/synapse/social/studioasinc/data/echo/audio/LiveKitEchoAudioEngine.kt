package com.synapse.social.studioasinc.data.echo.audio

import android.content.Context
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoBackendException
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoErrorKind
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoMediaCredentials
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoReaction
import com.synapse.social.studioasinc.shared.domain.service.EchoAudioConnection
import com.synapse.social.studioasinc.shared.domain.service.EchoAudioEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.aakira.napier.Napier
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.track.DataPublishReliability
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * The LiveKit-backed transport for one Echo Room at a time.
 *
 * One `Room` exists per connection and is released on disconnect: LiveKit rooms carry the local
 * participant, publications and the peer connection, and reusing a torn-down one is how a role
 * change ends up renegotiating stale state. Role changes and token refreshes mint a new token and
 * therefore go through [connect] again rather than mutating the live grant.
 *
 * LiveKit owns audio focus, the audio device and echo cancellation; the call stack's audio router
 * is deliberately not touched, because two owners of the same focus would fight.
 */
@Singleton
class LiveKitEchoAudioEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("ApplicationScope") private val scope: CoroutineScope
) : EchoAudioEngine {

    private val _state = MutableStateFlow(EchoAudioConnection.DISCONNECTED)
    override val state: StateFlow<EchoAudioConnection> = _state.asStateFlow()

    private val _activeSpeakerIds = MutableStateFlow<Set<String>>(emptySet())
    override val activeSpeakerIds: StateFlow<Set<String>> = _activeSpeakerIds.asStateFlow()

    private val _reactions = MutableSharedFlow<EchoReaction>(
        replay = 0,
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val reactions: Flow<EchoReaction> = _reactions.asSharedFlow()

    private val connectMutex = Mutex()
    private var room: Room? = null
    private var eventJob: Job? = null
    private var liveKitInitialized = false

    override suspend fun connect(
        credentials: EchoMediaCredentials,
        publish: Boolean
    ): Result<Unit> = withContext(Dispatchers.Main.immediate) {
        connectMutex.withLock {
            try {
                disconnectInternal()
                val newRoom = ensureRoom()
                observeEvents(newRoom)
                _state.value = EchoAudioConnection.CONNECTING
                newRoom.connect(credentials.serverUrl, credentials.token)
                if (publish) {
                    // The gate asked for the microphone before this point; a refusal here still
                    // leaves a listening connection, which is better than dropping the member.
                    runCatching { newRoom.localParticipant.setMicrophoneEnabled(true) }
                        .onFailure { Napier.w("Echo microphone could not be enabled: ${it.message}") }
                }
                _state.value = EchoAudioConnection.CONNECTED
                Result.success(Unit)
            } catch (e: CancellationException) {
                disconnectInternal()
                throw e
            } catch (e: Exception) {
                Napier.w("Echo audio connect failed: ${e.message}")
                disconnectInternal()
                Result.failure(
                    EchoBackendException(
                        EchoErrorKind.AUDIO_FAILED,
                        e.message ?: "Could not connect to the room audio"
                    )
                )
            }
        }
    }

    /** A reconnect is a fresh token and a fresh room; [connect] already tears the old one down. */
    override suspend fun reconnect(
        credentials: EchoMediaCredentials,
        publish: Boolean
    ): Result<Unit> = connect(credentials, publish)

    override suspend fun disconnect() = withContext(Dispatchers.Main.immediate) {
        disconnectInternal()
    }

    override suspend fun setMicrophoneEnabled(enabled: Boolean): Result<Unit> {
        val localParticipant = room?.localParticipant
            ?: return Result.failure(
                EchoBackendException(EchoErrorKind.AUDIO_FAILED, "Not connected to a room")
            )
        return try {
            withContext(Dispatchers.Main.immediate) {
                localParticipant.setMicrophoneEnabled(enabled)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w("Echo microphone could not be set to $enabled: ${e.message}")
            Result.failure(
                EchoBackendException(
                    EchoErrorKind.AUDIO_FAILED,
                    e.message ?: "The microphone could not be changed"
                )
            )
        }
    }

    override suspend fun sendReaction(emoji: String): Result<Unit> {
        val clean = emoji.trim().take(MAX_EMOJI_LENGTH)
        if (clean.isEmpty()) {
            return Result.failure(IllegalArgumentException("A reaction needs an emoji"))
        }
        val localParticipant = room?.localParticipant
            ?: return Result.failure(
                EchoBackendException(EchoErrorKind.AUDIO_FAILED, "Not connected to a room")
            )
        val payload = buildJsonObject { put("emoji", clean) }.toString().encodeToByteArray()
        return try {
            localParticipant.publishData(
                data = payload,
                reliability = DataPublishReliability.LOSSY,
                topic = REACTION_TOPIC
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w("Echo reaction could not be sent: ${e.message}")
            Result.failure(
                EchoBackendException(
                    EchoErrorKind.AUDIO_FAILED,
                    e.message ?: "The reaction could not be sent"
                )
            )
        }
    }

    private fun ensureRoom(): Room {
        room?.let { return it }
        if (!liveKitInitialized) {
            LiveKit.init(context)
            liveKitInitialized = true
        }
        return LiveKit.create(context).also { room = it }
    }

    private fun observeEvents(room: Room) {
        eventJob?.cancel()
        eventJob = scope.launch {
            room.events.collect { event ->
                when (event) {
                    is RoomEvent.Connected -> _state.value = EchoAudioConnection.CONNECTED
                    is RoomEvent.Reconnecting -> _state.value = EchoAudioConnection.RECONNECTING
                    is RoomEvent.Reconnected -> _state.value = EchoAudioConnection.CONNECTED
                    is RoomEvent.Disconnected, is RoomEvent.FailedToConnect -> {
                        _state.value = EchoAudioConnection.DISCONNECTED
                        _activeSpeakerIds.value = emptySet()
                    }

                    is RoomEvent.ActiveSpeakersChanged -> {
                        _activeSpeakerIds.value = event.speakers
                            .mapNotNull { speaker -> runCatching { speaker.identity?.value }.getOrNull() }
                            .toSet()
                    }

                    is RoomEvent.DataReceived -> handleReaction(event)
                    else -> Unit
                }
            }
        }
    }

    private fun handleReaction(event: RoomEvent.DataReceived) {
        if (event.topic != REACTION_TOPIC) return
        val userId = event.participant?.identity?.value ?: return
        val emoji = runCatching {
            Json.parseToJsonElement(event.data.decodeToString())
                .jsonObject["emoji"]
                ?.jsonPrimitive
                ?.contentOrNull
        }.getOrNull()?.trim()?.take(MAX_EMOJI_LENGTH)
        if (emoji.isNullOrEmpty()) return
        _reactions.tryEmit(
            EchoReaction(
                userId = userId,
                emoji = emoji,
                sentAtMillis = System.currentTimeMillis()
            )
        )
    }

    /**
     * Tears the connection down without suspending on the caller's behalf.
     *
     * Called both by explicit disconnects and at the top of [connect], so a failed reconnect cannot
     * leave the previous room's participants and speakers visible in the domain flows.
     */
    private fun disconnectInternal() {
        eventJob?.cancel()
        eventJob = null
        room?.let { current ->
            room = null
            runCatching { current.disconnect() }
            runCatching { current.release() }
        }
        _state.value = EchoAudioConnection.DISCONNECTED
        _activeSpeakerIds.value = emptySet()
    }

    private companion object {
        const val REACTION_TOPIC = "echo.reaction"
        const val MAX_EMOJI_LENGTH = 8
    }
}
