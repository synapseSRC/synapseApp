package com.synapse.social.studioasinc.core.media

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class VoicePlaybackState(
    val activeUrl: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isCompleted: Boolean = false,
    val error: String? = null
)

@Singleton
@OptIn(UnstableApi::class)
class VoicePlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var player: ExoPlayer? = null
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow(VoicePlaybackState())
    val playbackState: StateFlow<VoicePlaybackState> = _playbackState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackStateInt: Int) {
            when (playbackStateInt) {
                Player.STATE_BUFFERING -> {
                    _playbackState.update { it.copy(isBuffering = true, error = null) }
                }
                Player.STATE_READY -> {
                    val duration = player?.duration?.coerceAtLeast(0L) ?: 0L
                    _playbackState.update {
                        it.copy(
                            isBuffering = false,
                            durationMs = if (duration > 0) duration else it.durationMs,
                            error = null
                        )
                    }
                }
                Player.STATE_ENDED -> {
                    _playbackState.update {
                        it.copy(
                            isPlaying = false,
                            isBuffering = false,
                            isCompleted = true,
                            currentPositionMs = 0L
                        )
                    }
                    stopProgressTracker()
                }
                Player.STATE_IDLE -> {
                    _playbackState.update { it.copy(isBuffering = false) }
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) {
                startProgressTracker()
            } else {
                stopProgressTracker()
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            _playbackState.update {
                it.copy(
                    isPlaying = false,
                    isBuffering = false,
                    error = error.localizedMessage ?: "Playback error"
                )
            }
            stopProgressTracker()
        }
    }

    private fun getOrCreatePlayer(): ExoPlayer {
        return player ?: ExoPlayer.Builder(context).build().also { newPlayer ->
            newPlayer.repeatMode = Player.REPEAT_MODE_OFF
            newPlayer.addListener(playerListener)
            player = newPlayer
        }
    }

    fun play(url: String, mediaPath: String, speed: Float = 1.0f) {
        val currentActive = _playbackState.value.activeUrl

        // If another voice message was playing, stop it first (Single Active Player)
        if (currentActive != null && currentActive != url) {
            pause()
        }

        val activePlayer = getOrCreatePlayer()

        if (currentActive != url) {
            activePlayer.stop()
            activePlayer.clearMediaItems()
            val uri = if (mediaPath.startsWith("http://") || mediaPath.startsWith("https://") || mediaPath.startsWith("content://") || mediaPath.startsWith("file://")) {
                Uri.parse(mediaPath)
            } else {
                Uri.fromFile(java.io.File(mediaPath))
            }
            val mediaItem = MediaItem.fromUri(uri)
            activePlayer.setMediaItem(mediaItem)
            activePlayer.prepare()

            _playbackState.value = VoicePlaybackState(
                activeUrl = url,
                isPlaying = false,
                isBuffering = true,
                playbackSpeed = speed
            )
        } else if (activePlayer.playbackState == Player.STATE_ENDED || _playbackState.value.isCompleted) {
            activePlayer.seekTo(0)
            _playbackState.update { it.copy(isCompleted = false, currentPositionMs = 0L) }
        }

        setPlaybackSpeed(speed)
        activePlayer.play()
    }

    fun pause() {
        player?.pause()
        _playbackState.update { it.copy(isPlaying = false) }
        stopProgressTracker()
    }

    fun seekTo(positionMs: Long) {
        player?.let { p ->
            p.seekTo(positionMs)
            val updatedPosition = p.currentPosition.coerceAtLeast(0L)
            _playbackState.update {
                it.copy(
                    currentPositionMs = updatedPosition,
                    isCompleted = false
                )
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        player?.let { p ->
            p.playbackParameters = PlaybackParameters(speed)
            _playbackState.update { it.copy(playbackSpeed = speed) }
        }
    }

    fun stop() {
        progressJob?.cancel()
        player?.stop()
        player?.clearMediaItems()
        _playbackState.value = VoicePlaybackState()
    }

    fun release() {
        stop()
        player?.removeListener(playerListener)
        player?.release()
        player = null
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            var lastSyncTime = 0L
            var syncFailures = 0
            
            while (true) {
                player?.let { p ->
                    if (p.isPlaying) {
                        val rawCurrent = p.currentPosition.coerceAtLeast(0L)
                        val duration = p.duration.coerceAtLeast(0L)
                        
                        // Detect and correct drift (sync every 2 seconds)
                        val now = System.currentTimeMillis()
                        if (now - lastSyncTime > 2000L) {
                            _playbackState.update { state ->
                                state.copy(currentPositionMs = rawCurrent.coerceAtLeast(0L))
                            }
                            lastSyncTime = now
                            syncFailures = 0
                        }
                        
                        // Cap exposed position at duration - 100ms to prevent
                        // premature seekbar completion while preserving full accuracy for
                        // completion detection
                        val cappedCurrent = if (duration > 0 && rawCurrent >= duration) {
                            duration - 100L
                        } else {
                            rawCurrent
                        }
                        
                        // Only mark completed when player's own position reaches the end
                        val shouldComplete = duration > 0 && rawCurrent >= duration
                        
                        _playbackState.update { state ->
                            state.copy(
                                currentPositionMs = cappedCurrent,
                                durationMs = if (duration > 0) duration else state.durationMs,
                                isCompleted = shouldComplete || state.isCompleted
                            )
                        }
                        
                        // Clear completion flag once playback catches up after overshoot
                        if (shouldComplete && !_playbackState.value.isCompleted) {
                            delay(300L) // Give ExoPlayer time to actually reach end
                            player?.let { realPlayer ->
                                val postDelayedRaw = realPlayer.currentPosition
                                if (postDelayedRaw < duration && !_playbackState.value.isCompleted) {
                                    _playbackState.update { it.copy(isCompleted = false) }
                                }
                            }
                        }
                        
                        if (duration > 0 && rawCurrent >= duration) {
                            syncFailures++
                            if (syncFailures > 3) {
                                println("Error: Player stuck at end of track")
                                pause()
                            }
                        }
                    }
                }
                delay(100)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }
}
