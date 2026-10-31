package com.synapse.social.studioasinc.shared.domain.service

import com.synapse.social.studioasinc.shared.domain.model.echo.EchoMediaCredentials
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoReaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class EchoAudioConnection {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}

/**
 * The audio transport, with no SFU types in the signature.
 *
 * A room member is either subscribed or also publishing; `publish` is fixed for the life of a
 * connection because changing it means a new token, and a new token means a reconnect.
 */
interface EchoAudioEngine {
    val state: StateFlow<EchoAudioConnection>
    val activeSpeakerIds: StateFlow<Set<String>>
    val reactions: Flow<EchoReaction>

    suspend fun connect(credentials: EchoMediaCredentials, publish: Boolean): Result<Unit>
    suspend fun reconnect(credentials: EchoMediaCredentials, publish: Boolean): Result<Unit>
    suspend fun disconnect()

    suspend fun setMicrophoneEnabled(enabled: Boolean): Result<Unit>
    suspend fun sendReaction(emoji: String): Result<Unit>
}
