package com.synapse.social.studioasinc.feature.inbox.inbox.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoicePlayerViewModel @Inject constructor(
    private val voiceDownloadCache: VoiceDownloadCache
) : ViewModel() {

    private val _localFilePath = MutableStateFlow<String?>(null)
    val localFilePath: StateFlow<String?> = _localFilePath.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    fun prepareVoiceMessage(url: String) {
        if (_localFilePath.value != null || _isDownloading.value) return
        _isDownloading.value = true
        viewModelScope.launch {
            val result = voiceDownloadCache.getLocalPath(url)
            result.onSuccess { path ->
                _localFilePath.value = path
            }.onFailure {
                // handle error silently for now
            }
            _isDownloading.value = false
        }
    }

    fun cyclePlaybackSpeed(): Float {
        val nextSpeed = when (_playbackSpeed.value) {
            1.0f -> 1.5f
            1.5f -> 2.0f
            else -> 1.0f
        }
        _playbackSpeed.value = nextSpeed
        return nextSpeed
    }
}
