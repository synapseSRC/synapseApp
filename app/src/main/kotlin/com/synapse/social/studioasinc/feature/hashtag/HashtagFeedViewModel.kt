package com.synapse.social.studioasinc.feature.hashtag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.ReactionType
import com.synapse.social.studioasinc.domain.usecase.post.BookmarkPostUseCase
import com.synapse.social.studioasinc.domain.usecase.post.ReactToPostUseCase
import com.synapse.social.studioasinc.domain.usecase.post.RepostPostUseCase
import com.synapse.social.studioasinc.domain.usecase.post.UnrepostPostUseCase
import com.synapse.social.studioasinc.domain.usecase.post.VotePollUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.post.DeletePostUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.post.TogglePostCommentsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.search.GetPostsByHashtagUseCase
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.domain.usecase.post.PopulatePostPollsUseCase
import com.synapse.social.studioasinc.domain.usecase.reaction.PopulatePostReactionsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.blocking.BlockUserUseCase
import com.synapse.social.studioasinc.domain.usecase.post.ReportPostUseCase
import com.synapse.social.studioasinc.domain.usecase.post.RevokeVoteUseCase
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
    private val getPostsByHashtagUseCase: GetPostsByHashtagUseCase,
    private val reactToPostUseCase: ReactToPostUseCase,
    private val bookmarkPostUseCase: BookmarkPostUseCase,
    private val votePollUseCase: VotePollUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    private val repostPostUseCase: RepostPostUseCase,
    private val unrepostPostUseCase: UnrepostPostUseCase,
    private val populatePostPollsUseCase: PopulatePostPollsUseCase,
    private val populatePostReactionsUseCase: PopulatePostReactionsUseCase,
    private val authRepository: AuthRepository,
    private val togglePostCommentsUseCase: TogglePostCommentsUseCase,
    private val blockUserUseCase: BlockUserUseCase,
    private val reportPostUseCase: ReportPostUseCase,
    private val revokeVoteUseCase: RevokeVoteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HashtagFeedUiState())
    val uiState: StateFlow<HashtagFeedUiState> = _uiState.asStateFlow()

    /** Default reason used when reporting a post from the hashtag feed (no reason picker is shown there). */
    private val defaultReportReason = "Reported from hashtag feed"

    fun init(tag: String) {
        if (_uiState.value.tag == tag) return
        _uiState.update { it.copy(tag = tag) }
        loadPosts(tag)
    }

    private fun loadPosts(tag: String) {
        if (tag.isBlank()) {
            _uiState.update { it.copy(posts = emptyList(), isLoading = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getPostsByHashtagUseCase(tag).onSuccess { searchPosts ->
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
                }.onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
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
                }.onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
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
                }.onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            }
        }
    }

    fun deletePost(post: Post) {
        viewModelScope.launch {
            deletePostUseCase(post.id).collect { result ->
                result.onSuccess {
                    _uiState.update { state -> state.copy(posts = state.posts.filter { it.id != post.id }) }
                }.onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            }
        }
    }

    fun resharePost(post: Post) {
        val isCurrentlyReshared = post.isReshared
        val optimisticPost = post.copy(
            isReshared = !isCurrentlyReshared,
            resharesCount = if (isCurrentlyReshared) maxOf(0, post.resharesCount - 1) else post.resharesCount + 1
        )
        _uiState.update { state ->
            state.copy(posts = state.posts.map { if (it.id == post.id) optimisticPost else it })
        }

        viewModelScope.launch {
            val result = if (isCurrentlyReshared) {
                unrepostPostUseCase(post.id)
            } else {
                repostPostUseCase(post.id)
            }

            result.onSuccess {
                val currentTag = _uiState.value.tag
                if (currentTag.isNotBlank()) {
                    loadPosts(currentTag)
                }
            }.onFailure {
                _uiState.update { state ->
                    state.copy(posts = state.posts.map { if (it.id == post.id) post else it })
                }
            }
        }
    }

    fun isPostOwner(post: Post): Boolean {
        return authRepository.getCurrentUserId() == post.authorUid
    }

    fun toggleComments(post: Post) {
        viewModelScope.launch {
            togglePostCommentsUseCase(post.id).collect { result ->
                result.onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to toggle comments") }
                }
            }
        }
    }

    fun blockUser(userId: String) {
        viewModelScope.launch {
            blockUserUseCase(userId).onSuccess {
                _uiState.update { it.copy(error = null) }
                loadPosts(_uiState.value.tag)
            }.onFailure { error ->
                _uiState.update { it.copy(error = error.message ?: "Failed to block user") }
            }
        }
    }

    fun reportPost(postId: String) {
        viewModelScope.launch {
            reportPostUseCase(postId, defaultReportReason, null).collect { result ->
                result.onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to report post") }
                }
            }
        }
    }

    fun revokeVote(post: Post) {
        viewModelScope.launch {
            revokeVoteUseCase(post).collect { result ->
                result.onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to revoke vote") }
                }
            }
        }
    }
}
