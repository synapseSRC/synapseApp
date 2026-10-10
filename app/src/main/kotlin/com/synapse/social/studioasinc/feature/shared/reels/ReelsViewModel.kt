package com.synapse.social.studioasinc.feature.shared.reels

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.core.media.VideoPlayerManager
import com.synapse.social.studioasinc.shared.data.repository.ReelRepository
import com.synapse.social.studioasinc.shared.domain.model.Reel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max

data class ReelsUiState(
    val reels: List<Reel> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadMoreLoading: Boolean = false,
    val isEndReached: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ReelsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val videoPlayerManager: VideoPlayerManager,
    private val reelRepository: ReelRepository
) : ViewModel() {
    private var currentPage = 0
    private val pageSize = 10

    private val _uiState = MutableStateFlow(ReelsUiState())
    val uiState: StateFlow<ReelsUiState> = _uiState.asStateFlow()

    init {
        loadReels()
    }

    fun preloadReels(urls: List<String>) {
        videoPlayerManager.preload(urls)
    }

    fun pauseAllPlayers() {
        videoPlayerManager.pauseAll()
    }

    fun releaseAllPlayers() {
        videoPlayerManager.releaseAll()
    }

    fun loadReels() {
        viewModelScope.launch {
            currentPage = 0
            _uiState.update { it.copy(isLoading = true, isEndReached = false, error = null) }
            val result = reelRepository.getReels(currentPage, pageSize)
            result.onSuccess { reels ->
                _uiState.update { it.copy(
                    reels = reels,
                    isLoading = false,
                    isEndReached = reels.size < pageSize
                ) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun loadMoreReels() {
        val currentState = _uiState.value
        if (currentState.isLoadMoreLoading || currentState.isEndReached || currentState.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadMoreLoading = true) }
            val nextPage = currentPage + 1
            val result = reelRepository.getReels(nextPage, pageSize)
            result.onSuccess { newReels ->
                if (newReels.isEmpty()) {
                    _uiState.update { it.copy(isEndReached = true, isLoadMoreLoading = false) }
                } else {
                    currentPage = nextPage
                    _uiState.update { state ->
                        val existingIds = state.reels.map { it.id }.toSet()
                        val uniqueNewReels = newReels.filter { it.id !in existingIds }
                        state.copy(
                            reels = state.reels + uniqueNewReels,
                            isLoadMoreLoading = false,
                            isEndReached = newReels.size < pageSize
                        )
                    }
                }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoadMoreLoading = false) }
            }
        }
    }

    fun likeReel(reelId: String) {
        _uiState.update { state ->
            val updatedReels = state.reels.map { reel ->
                if (reel.id == reelId) {
                    val newLiked = !reel.isLikedByCurrentUser
                    val newCount = if (newLiked) reel.likesCount + 1 else max(0, reel.likesCount - 1)
                    reel.copy(isLikedByCurrentUser = newLiked, likesCount = newCount)
                } else reel
            }
            state.copy(reels = updatedReels)
        }

        viewModelScope.launch {
             reelRepository.likeReel(reelId).onFailure {

                 _uiState.update { state ->
                    val updatedReels = state.reels.map { reel ->
                        if (reel.id == reelId) {
                            val oldLiked = !reel.isLikedByCurrentUser
                            val oldCount = if (oldLiked) reel.likesCount + 1 else max(0, reel.likesCount - 1)
                            reel.copy(isLikedByCurrentUser = oldLiked, likesCount = oldCount)
                        } else reel
                    }
                    state.copy(reels = updatedReels, error = "Failed to update like")
                 }
             }
        }
    }

    fun opposeReel(reelId: String) {
        _uiState.update { state ->
            val updatedReels = state.reels.map { reel ->
                if (reel.id == reelId) {
                    val newOpposed = !reel.isOpposedByCurrentUser
                    val newCount = if (newOpposed) reel.opposeCount + 1 else max(0, reel.opposeCount - 1)
                    reel.copy(isOpposedByCurrentUser = newOpposed, opposeCount = newCount)
                } else reel
            }
            state.copy(reels = updatedReels)
        }

        viewModelScope.launch {
            reelRepository.opposeReel(reelId).onFailure {

                 _uiState.update { state ->
                    val updatedReels = state.reels.map { reel ->
                        if (reel.id == reelId) {
                            val oldOpposed = !reel.isOpposedByCurrentUser
                            val oldCount = if (oldOpposed) reel.opposeCount + 1 else max(0, reel.opposeCount - 1)
                            reel.copy(isOpposedByCurrentUser = oldOpposed, opposeCount = oldCount)
                        } else reel
                    }
                    state.copy(reels = updatedReels, error = "Failed to update oppose")
                 }
            }
        }
    }

    fun reportReel(reelId: String, reason: String) {
        viewModelScope.launch {
            reelRepository.reportReel(reelId, reason).onFailure { e ->
                _uiState.update { it.copy(error = "Failed to report: ${e.message}") }
            }
        }
    }

    fun blockCreator(creatorId: String) {
        viewModelScope.launch {
            reelRepository.blockCreator(creatorId).onSuccess {

                loadReels()
            }.onFailure { e ->
                _uiState.update { it.copy(error = "Failed to block: ${e.message}") }
            }
        }
    }

    fun downloadReel(videoUrl: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(videoUrl))
                .setTitle("Downloading Reel")
                .setDescription("Saving video to your device")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, "Synapse/reel_${System.currentTimeMillis()}.mp4")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Download failed: ${e.message}") }
        }
    }

    fun shareReel(videoUrl: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Check out this reel on Synapse: $videoUrl")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }
}
