package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.core.media.VoicePlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class VoicePlayerUiState(
    val mediaUrl: String = "",
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadError: String? = null,
    val localPath: String? = null,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val waveformAmplitudes: List<Float> = emptyList()
)

@HiltViewModel
class VoiceMessagePlayerViewModel @Inject constructor(
    private val voicePlayerManager: VoicePlayerManager,
    private val voiceDownloadCache: VoiceDownloadCache
) : ViewModel() {

    var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val _uiState = MutableStateFlow(VoicePlayerUiState())
    val uiState: StateFlow<VoicePlayerUiState> = _uiState.asStateFlow()

    private var playbackObserveJob: Job? = null

    init {
        observePlaybackState()
    }

    fun initialize(mediaUrl: String, initialDurationMs: Long = 0L) {
        if (_uiState.value.mediaUrl == mediaUrl) {
            if (initialDurationMs > 0 && _uiState.value.durationMs <= 0) {
                _uiState.update { it.copy(durationMs = initialDurationMs) }
            }
            return
        }

        val localFile = File(mediaUrl)
        val isLocalFile = localFile.exists() && localFile.isFile

        _uiState.update {
            VoicePlayerUiState(
                mediaUrl = mediaUrl,
                localPath = if (isLocalFile) mediaUrl else null,
                durationMs = initialDurationMs,
                waveformAmplitudes = VoiceWaveformExtractor.generateFallbackAmplitudes(mediaUrl)
            )
        }

        if (isLocalFile) {
            loadWaveformForLocalPath(mediaUrl)
        } else {
            loadLocalPathAndWaveform(mediaUrl)
        }
    }

    private fun observePlaybackState() {
        playbackObserveJob?.cancel()
        playbackObserveJob = viewModelScope.launch {
            voicePlayerManager.playbackState.collect { playback ->
                val currentMediaUrl = _uiState.value.mediaUrl
                if (currentMediaUrl.isNotBlank() && playback.activeUrl == currentMediaUrl) {
                    _uiState.update { state ->
                        state.copy(
                            isPlaying = playback.isPlaying,
                            isBuffering = playback.isBuffering,
                            playbackSpeed = playback.playbackSpeed,
                            durationMs = if (playback.durationMs > 0) playback.durationMs else state.durationMs,
                            currentPositionMs = if (playback.isPlaying || playback.currentPositionMs > 0) {
                                playback.currentPositionMs
                            } else if (playback.isCompleted) {
                                0L
                            } else {
                                state.currentPositionMs
                            },
                            downloadError = playback.error ?: state.downloadError
                        )
                    }
                } else if (playback.activeUrl != null && playback.activeUrl != currentMediaUrl) {
                    // Another audio file is playing, pause state for this player while preserving position
                    _uiState.update { state ->
                        state.copy(
                            isPlaying = false,
                            isBuffering = false
                        )
                    }
                }
            }
        }
    }

    private fun loadWaveformForLocalPath(filePath: String) {
        viewModelScope.launch(ioDispatcher) {
            val amplitudes = VoiceWaveformExtractor.extractAmplitudes(filePath, dispatcher = ioDispatcher)
            _uiState.update { state ->
                state.copy(
                    isDownloading = false,
                    downloadError = null,
                    waveformAmplitudes = amplitudes
                )
            }
        }
    }

    private fun loadLocalPathAndWaveform(mediaUrl: String) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(isDownloading = true, downloadError = null) }
            val result = voiceDownloadCache.getLocalPath(mediaUrl)
            result.fold(
                onSuccess = { path ->
                    val amplitudes = VoiceWaveformExtractor.extractAmplitudes(path, dispatcher = ioDispatcher)
                    _uiState.update { state ->
                        state.copy(
                            localPath = path,
                            isDownloading = false,
                            downloadError = null,
                            waveformAmplitudes = amplitudes
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            isDownloading = false,
                            downloadError = error.localizedMessage ?: "Download failed"
                        )
                    }
                }
            )
        }
    }

    fun onPlayPauseClicked() {
        val state = _uiState.value
        val url = state.mediaUrl
        if (url.isBlank()) return

        if (state.localPath == null) {
            onRetryClicked()
            return
        }

        if (state.isPlaying) {
            voicePlayerManager.pause()
        } else {
            voicePlayerManager.play(
                url = url,
                mediaPath = state.localPath,
                speed = state.playbackSpeed
            )
        }
    }

    fun onSeek(fraction: Float) {
        val state = _uiState.value
        val duration = state.durationMs
        if (duration <= 0) return

        val targetPosition = (fraction.coerceIn(0f, 1f) * duration).toLong()
        _uiState.update { it.copy(currentPositionMs = targetPosition) }

        if (voicePlayerManager.playbackState.value.activeUrl == state.mediaUrl) {
            voicePlayerManager.seekTo(targetPosition)
        }
    }

    fun onSpeedToggle() {
        val currentSpeed = _uiState.value.playbackSpeed
        val nextSpeed = when (currentSpeed) {
            1.0f -> 1.5f
            1.5f -> 2.0f
            else -> 1.0f
        }

        _uiState.update { it.copy(playbackSpeed = nextSpeed) }

        if (voicePlayerManager.playbackState.value.activeUrl == _uiState.value.mediaUrl) {
            voicePlayerManager.setPlaybackSpeed(nextSpeed)
        }
    }

    fun onRetryClicked() {
        val url = _uiState.value.mediaUrl
        if (url.isNotBlank()) {
            if (File(url).exists()) {
                loadWaveformForLocalPath(url)
            } else {
                loadLocalPathAndWaveform(url)
            }
        }
    }

    fun pause() {
        if (_uiState.value.isPlaying) {
            voicePlayerManager.pause()
        }
    }
}
