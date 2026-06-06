package com.synapse.social.studioasinc.feature.hashtag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.ReactionType
import com.synapse.social.studioasinc.domain.usecase.post.BookmarkPostUseCase
import com.synapse.social.studioasinc.domain.usecase.post.ReactToPostUseCase
import com.synapse.social.studioasinc.domain.usecase.post.VotePollUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.post.DeletePostUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.search.SearchPostsUseCase
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.domain.usecase.post.PopulatePostPollsUseCase
import com.synapse.social.studioasinc.domain.usecase.reaction.PopulatePostReactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HashtagFeedUiState(
    val tag: String = "",
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HashtagFeedViewModel @Inject constructor(
    private val searchPostsUseCase: SearchPostsUseCase,
    private val reactToPostUseCase: ReactToPostUseCase,
    private val bookmarkPostUseCase: BookmarkPostUseCase,
    private val votePollUseCase: VotePollUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    private val populatePostPollsUseCase: PopulatePostPollsUseCase,
    private val populatePostReactionsUseCase: PopulatePostReactionsUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HashtagFeedUiState())
    val uiState: StateFlow<HashtagFeedUiState> = _uiState.asStateFlow()

    fun init(tag: String) {
        if (_uiState.value.tag == tag) return
        _uiState.update { it.copy(tag = tag) }
        loadPosts(tag)
    }

    private fun loadPosts(tag: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            searchPostsUseCase("#$tag").onSuccess { searchPosts ->
                val posts = searchPosts.map { searchPost ->
                    Post(
                        id = searchPost.id,
                        authorUid = searchPost.authorId,
                        postText = searchPost.content,
                        publishDate = searchPost.createdAt,
                        likesCount = searchPost.likesCount,
                        replyCount = searchPost.commentsCount,
                        resharesCount = searchPost.boostCount,
                        username = searchPost.authorHandle,
                        avatarUrl = searchPost.authorAvatar,
                        mediaItems = searchPost.mediaUrls?.map { url ->
                            com.synapse.social.studioasinc.domain.model.MediaItem(id = url, url = url, type = com.synapse.social.studioasinc.domain.model.MediaType.IMAGE)
                        }?.toMutableList() ?: mutableListOf()
                    )
                }
                val enrichedPosts = populatePostReactionsUseCase(posts)
                val fullyEnrichedPosts = populatePostPollsUseCase(enrichedPosts)
                _uiState.update { it.copy(posts = fullyEnrichedPosts, isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun reactToPost(post: Post, reactionType: ReactionType) {
        viewModelScope.launch {
            reactToPostUseCase(post, reactionType).collect { result ->
                result.onSuccess { updatedPost ->
                    _uiState.update { state ->
                        state.copy(posts = state.posts.map { if (it.id == updatedPost.id) updatedPost else it })
                    }
                }
            }
        }
    }

    fun bookmarkPost(post: Post) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            val isBookmarked = post.isBookmarked == true
            bookmarkPostUseCase(post.id, userId, isBookmarked).collect { result ->
                result.onSuccess {
                    _uiState.update { state ->
                        state.copy(posts = state.posts.map {
                            if (it.id == post.id) it.copy(isBookmarked = !isBookmarked) else it
                        })
                    }
                }
            }
        }
    }

    fun votePoll(post: Post, optionIndex: Int) {
        viewModelScope.launch {
            votePollUseCase(post, optionIndex).collect { result ->
                result.onSuccess { updatedPost ->
                    _uiState.update { state ->
                        state.copy(posts = state.posts.map { if (it.id == updatedPost.id) updatedPost else it })
                    }
                }
            }
        }
    }

    fun deletePost(post: Post) {
        viewModelScope.launch {
            deletePostUseCase(post.id).collect { result ->
                result.onSuccess {
                    _uiState.update { state -> state.copy(posts = state.posts.filter { it.id != post.id }) }
                }
            }
        }
    }

    fun isPostOwner(post: Post): Boolean {
        return authRepository.getCurrentUserId() == post.authorUid
    }
}
