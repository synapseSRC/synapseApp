package com.synapse.social.studioasinc.data.call.webrtc

import javax.inject.Inject
import javax.inject.Singleton
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

/**
 * The side channel that keeps Android video types out of the domain layer.
 *
 * `CallSession` only carries booleans (`isRemoteVideoEnabled`, `isLocalVideoEnabled`, ...) because
 * `VideoTrack` and `SurfaceViewRenderer` are platform types. The engine publishes tracks here, and
 * the Compose views attach renderers here, keyed by call id. Neither side needs to know the other
 * exists, and the negotiation order no longer matters: whichever arrives first waits for the other.
 */
@Singleton
class CallMediaRegistry @Inject constructor() {

    enum class Role { LOCAL, REMOTE }

    private val lock = Any()
    private val tracks = mutableMapOf<Key, VideoTrack>()
    private val renderers = mutableMapOf<Key, MutableList<SurfaceViewRenderer>>()

    private data class Key(val callId: String, val role: Role)

    /**
     * Publishes the track for [callId]/[role]. Pass null when the track goes away
     * (remote video ended, camera switched, call disposed).
     */
    fun setTrack(callId: String, role: Role, track: VideoTrack?) {
        val key = Key(callId, role)
        val current: MutableSet<SurfaceViewRenderer>
        val previous: VideoTrack?
        val next: VideoTrack?
        synchronized(lock) {
            previous = tracks[key]
            if (previous === track) return
            if (track == null) tracks.remove(key) else tracks[key] = track
            next = track
            current = renderers[key]?.toMutableSet() ?: mutableSetOf()
        }
        // addSink/removeSink are safe off the UI thread; the previous track must stop rendering.
        previous?.let { old -> current.forEach { renderer -> runCatching { old.removeSink(renderer) } } }
        next?.let { fresh -> current.forEach { renderer -> runCatching { fresh.addSink(renderer) } } }
    }

    /**
     * Attaches an already-initialized [renderer] to the current track for this call and keeps it
     * attached across track changes. Returns the detach callback for `DisposableEffect`.
     */
    fun attach(callId: String, role: Role, renderer: SurfaceViewRenderer): () -> Unit {
        val key = Key(callId, role)
        val existing: VideoTrack? = synchronized(lock) {
            renderers.getOrPut(key) { mutableListOf() }.add(renderer)
            tracks[key]
        }
        existing?.let { track -> runCatching { track.addSink(renderer) } }
        return {
            val track: VideoTrack? = synchronized(lock) {
                val list = renderers[key]
                list?.remove(renderer)
                if (list.isNullOrEmpty()) renderers.remove(key)
                tracks[key]
            }
            runCatching { track?.removeSink(renderer) }
        }
    }

    /** Drops everything held for a finished call so no native track outlives the session. */
    fun release(callId: String) {
        synchronized(lock) {
            Role.entries.forEach { role ->
                val key = Key(callId, role)
                renderers[key]?.forEach { renderer ->
                    tracks[key]?.let { track -> runCatching { track.removeSink(renderer) } }
                }
                renderers.remove(key)
                tracks.remove(key)
            }
        }
    }

    /** Debug aid: asserts nothing was leaked after a call ends. */
    fun isEmpty(): Boolean = synchronized(lock) { tracks.isEmpty() && renderers.isEmpty() }
}
