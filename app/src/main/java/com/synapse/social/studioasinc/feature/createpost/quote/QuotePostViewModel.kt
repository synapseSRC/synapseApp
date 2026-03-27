package com.synapse.social.studioasinc.feature.createpost.quote

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.usecase.post.GetPostUseCase
import com.synapse.social.studioasinc.domain.usecase.post.QuotePostUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuotePostUiState(
    val post: Post? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class QuotePostViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPostUseCase: GetPostUseCase,
    private val quotePostUseCase: QuotePostUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuotePostUiState())
    val uiState: StateFlow<QuotePostUiState> = _uiState.asStateFlow()

    private val postId: String = checkNotNull(savedStateHandle["postId"])

    init {
        loadPost()
    }

    private fun loadPost() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            getPostUseCase(postId)
                .onSuccess { post ->
                    _uiState.value = _uiState.value.copy(
                        post = post,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "Failed to load post",
                        isLoading = false
                    )
                }
        }
    }

    fun quotePost(text: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            quotePostUseCase(postId, text)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSuccess = true,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "Failed to quote post",
                        isLoading = false
                    )
                }
        }
    }
}
