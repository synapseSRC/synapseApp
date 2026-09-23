package com.synapse.social.studioasinc.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.remote.services.SupabaseAuthenticationService
import com.synapse.social.studioasinc.domain.model.ScheduledPost
import com.synapse.social.studioasinc.domain.repository.ScheduledPostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScheduledPostsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val posts: List<ScheduledPost> = emptyList(),
    val isOffline: Boolean = false
)

@HiltViewModel
class ScheduledPostsViewModel @Inject constructor(
    private val scheduledPostRepository: ScheduledPostRepository
) : ViewModel() {

    private val authService = SupabaseAuthenticationService()

    private val _uiState = MutableStateFlow(ScheduledPostsUiState())
    val uiState: StateFlow<ScheduledPostsUiState> = _uiState.asStateFlow()

    init {
        loadScheduledPosts()
    }

    fun loadScheduledPosts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val userId = authService.getCurrentUserId()
            if (userId == null) {
                _uiState.update { it.copy(isLoading = false, error = "Not authenticated") }
                return@launch
            }

            scheduledPostRepository.getScheduledPosts(userId)
                .onSuccess { list ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            posts = list.sortedBy { post -> post.scheduledAt },
                            error = null,
                            isOffline = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load scheduled posts",
                            isOffline = error is java.io.IOException
                        )
                    }
                }
        }
    }

    fun cancelScheduledPost(id: String) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(posts = state.posts.filter { it.id != id })
            }
            scheduledPostRepository.cancelScheduledPost(id)
                .onFailure {
                    loadScheduledPosts()
                }
        }
    }

    fun retryScheduledPost(id: String) {
        viewModelScope.launch {
            scheduledPostRepository.retryScheduledPost(id)
                .onSuccess {
                    loadScheduledPosts()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = "Retry failed: ${error.message}") }
                }
        }
    }

    fun reschedulePost(id: String, newScheduledAt: String) {
        viewModelScope.launch {
            val post = _uiState.value.posts.find { it.id == id } ?: return@launch
            scheduledPostRepository.updateScheduledPost(id, newScheduledAt, post.postRequest)
                .onSuccess {
                    loadScheduledPosts()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = "Reschedule failed: ${error.message}") }
                }
        }
    }
}
