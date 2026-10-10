package com.synapse.social.studioasinc.feature.shared.reels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.core.auth.AuthHelper
import com.synapse.social.studioasinc.shared.data.repository.ReelRepository
import com.synapse.social.studioasinc.shared.domain.model.ReelComment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CommentsUiState(
    val comments: List<ReelComment> = emptyList(),
    val isLoading: Boolean = false,
    val isPosting: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CommentsViewModel @Inject constructor(
    private val reelRepository: ReelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommentsUiState())
    val uiState: StateFlow<CommentsUiState> = _uiState.asStateFlow()

    fun loadComments(reelId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            reelRepository.getComments(reelId).onSuccess { comments ->
                _uiState.update { it.copy(comments = comments, isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun addComment(reelId: String, content: String) {
        val currentUserId = AuthHelper.getCurrentUserId()
        if (currentUserId == null) {
            _uiState.update { it.copy(error = "You must be logged in to comment.") }
            return
        }


        val tempComment = ReelComment(
            id = "temp_${System.currentTimeMillis()}",
            reelId = reelId,
            userId = currentUserId,
            content = content,
            createdAt = "Just now",
            updatedAt = "Just now",
            userUsername = "Me",
            userAvatarUrl = null
        )

        _uiState.update { it.copy(comments = listOf(tempComment) + it.comments, isPosting = true) }

        viewModelScope.launch {
            reelRepository.addComment(reelId, content).onSuccess {
                loadComments(reelId)
                _uiState.update { it.copy(isPosting = false) }
            }.onFailure { e ->
                _uiState.update { state ->
                    state.copy(
                        comments = state.comments.filter { it.id != tempComment.id },
                        isPosting = false,
                        error = e.message
                    )
                }
            }
        }
    }
}
