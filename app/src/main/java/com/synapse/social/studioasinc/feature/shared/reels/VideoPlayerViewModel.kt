package com.synapse.social.studioasinc.feature.shared.reels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.synapse.social.studioasinc.core.media.VideoPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VideoPlayerUiState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val progress: Float = 0f,
    val duration: Long = 0L,
    val currentPosition: Long = 0L,
    val isMuted: Boolean = false
)

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    private val videoPlayerManager: VideoPlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoPlayerUiState())
    val uiState: StateFlow<VideoPlayerUiState> = _uiState.asStateFlow()

    private var player: ExoPlayer? = null
    private var videoUrl: String? = null
    private var progressJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _uiState.update {
                it.copy(isBuffering = playbackState == Player.STATE_BUFFERING)
            }
            if (playbackState == Player.STATE_READY) {
                _uiState.update { it.copy(duration = player?.duration ?: 0L) }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
        }
    }

    fun initializePlayer(url: String) {
        if (videoUrl == url && player != null) return

        videoUrl = url
        player = videoPlayerManager.getPlayer(url).apply {
            addListener(playerListener)
            volume = if (_uiState.value.isMuted) 0f else 1f
        }

        startProgressUpdate()
    }

    fun releasePlayer() {
        videoUrl?.let { url ->
            progressJob?.cancel()
            player?.removeListener(playerListener)
            videoPlayerManager.releasePlayer(url)
            player = null
        }
    }

    fun getPlayerInstance(): ExoPlayer? = player

    fun play() {
        player?.play()
    }

    fun pause() {
        player?.pause()
    }

    fun toggleMute() {
        _uiState.update {
            val newMuted = !it.isMuted
            player?.volume = if (newMuted) 0f else 1f
            it.copy(isMuted = newMuted)
        }
    }

    fun seekTo(position: Long) {
        player?.seekTo(position)
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                player?.let {
                    if (it.isPlaying) {
                        val current = it.currentPosition
                        val duration = it.duration.takeIf { d -> d > 0 } ?: 1L
                        _uiState.update { state ->
                            state.copy(
                                progress = current.toFloat() / duration.toFloat(),
                                currentPosition = current,
                                duration = duration
                            )
                        }
                    }
                }
                delay(100)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }
}
