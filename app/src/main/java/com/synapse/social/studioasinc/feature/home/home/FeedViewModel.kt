package com.synapse.social.studioasinc.feature.home.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.ReactionType
import com.synapse.social.studioasinc.domain.usecase.post.BookmarkPostUseCase
import com.synapse.social.studioasinc.domain.usecase.post.GetPostsPagedUseCase
import com.synapse.social.studioasinc.domain.usecase.post.ReactToPostUseCase
import com.synapse.social.studioasinc.domain.usecase.post.RevokeVoteUseCase
import com.synapse.social.studioasinc.domain.usecase.post.VotePollUseCase
import com.synapse.social.studioasinc.domain.usecase.settings.GetAppearanceSettingsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.post.DeletePostUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.post.TogglePostCommentsUseCase
import com.synapse.social.studioasinc.feature.shared.components.post.PostCardState
import com.synapse.social.studioasinc.feature.shared.components.post.PostEvent
import com.synapse.social.studioasinc.feature.shared.components.post.PostEventBus
import com.synapse.social.studioasinc.feature.shared.components.post.PostUiMapper
import com.synapse.social.studioasinc.ui.settings.PostViewStyle
import com.synapse.social.studioasinc.core.util.ScrollPositionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting
import javax.inject.Inject

data class FeedUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val postViewStyle: PostViewStyle = PostViewStyle.SWIPE
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val getPostsPagedUseCase: GetPostsPagedUseCase,
    private val authRepository: AuthRepository,
    private val getAppearanceSettingsUseCase: GetAppearanceSettingsUseCase,
    private val reactToPostUseCase: ReactToPostUseCase,
    private val votePollUseCase: VotePollUseCase,
    private val revokeVoteUseCase: RevokeVoteUseCase,
    private val bookmarkPostUseCase: BookmarkPostUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    private val togglePostCommentsUseCase: TogglePostCommentsUseCase,
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private val _modifiedPosts = MutableStateFlow<Map<String, Post>>(emptyMap())
    private val MAX_MODIFIED_POSTS = 100

    private val refreshTrigger = MutableStateFlow(0)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val posts: Flow<PagingData<Post>> = refreshTrigger.flatMapLatest {
        getPostsPagedUseCase()
    }.cachedIn(viewModelScope)
    .combine(_modifiedPosts) { pagingData, modifications ->
        pagingData.map { post ->
            modifications[post.id] ?: post
        }
    }

    private var savedScrollPosition: ScrollPositionState? = null

    init {
        viewModelScope.launch {
            getAppearanceSettingsUseCase().collect { settings ->
                _uiState.update { it.copy(postViewStyle = settings.postViewStyle) }
            }
        }

        viewModelScope.launch {
            PostEventBus.events.collect { event ->
                when (event) {
                    is PostEvent.Liked -> {
                        // handled by updated
                    }
                    is PostEvent.Updated -> {
                        cacheModifiedPost(event.post)
                    }
                    is PostEvent.Deleted -> {
                        // handled elsewhere or ignored
                    }
                    is PostEvent.Created -> {
                        refresh()
                    }
                    is PostEvent.Deleted -> {
                        refresh()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun cacheModifiedPost(post: Post) {
        _modifiedPosts.update { currentMap ->
            val newMap = currentMap.toMutableMap()
            newMap.remove(post.id)
            newMap[post.id] = post

            while (newMap.size > MAX_MODIFIED_POSTS) {
                val iterator = newMap.iterator()
                if (iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                }
            }
            newMap
        }
    }

    @VisibleForTesting
    fun getModifiedPostsCount(): Int {
        return _modifiedPosts.value.size
    }

    fun likePost(post: Post) {
        performReaction(post, ReactionType.LIKE)
    }

    fun reactToPost(post: Post, reactionType: ReactionType) {
        performReaction(post, reactionType)
    }

    private fun performReaction(post: Post, reactionType: ReactionType) {
         viewModelScope.launch {
             reactToPostUseCase(post, reactionType).collect { result ->
                 result.onSuccess { updatedPost ->
                     cacheModifiedPost(updatedPost)
                     PostEventBus.emit(PostEvent.Updated(updatedPost))
                 }.onFailure {
                     // Error handling
                 }
             }
         }
    }

    fun votePoll(post: Post, optionIndex: Int) {
        viewModelScope.launch {
            votePollUseCase(post, optionIndex).collect { result ->
                result.onSuccess { updatedPost ->
                    cacheModifiedPost(updatedPost)
                    PostEventBus.emit(PostEvent.Updated(updatedPost))
                }.onFailure {
                    _modifiedPosts.update { it - post.id }
                }
            }
        }
    }

    fun revokeVote(post: Post) {
        viewModelScope.launch {
            revokeVoteUseCase(post).collect { result ->
                result.onSuccess { updatedPost ->
                    cacheModifiedPost(updatedPost)
                }.onFailure {
                    _modifiedPosts.update { it - post.id }
                }
            }
        }
    }

    fun bookmarkPost(post: Post) {
        viewModelScope.launch {
            val optimisticPost = post.copy(isBookmarked = !post.isBookmarked)
            cacheModifiedPost(optimisticPost)
            PostEventBus.emit(PostEvent.Updated(optimisticPost))
            val currentUserId = authRepository.getCurrentUserId() ?: return@launch
            bookmarkPostUseCase(post.id, currentUserId, post.isBookmarked).collect { result ->
                result.onFailure {
                    cacheModifiedPost(post)
                    PostEventBus.emit(PostEvent.Updated(post))
                }
            }
        }
    }

    fun resharePost(post: Post) {}

    fun refresh() {
        refreshTrigger.value += 1
    }

    fun mapPostToState(post: Post): PostCardState {
        return PostUiMapper.mapToState(post)
    }

    fun saveScrollPosition(position: Int, offset: Int) {
        savedScrollPosition = ScrollPositionState(position, offset)
    }

    fun restoreScrollPosition(): ScrollPositionState? {
        val position = savedScrollPosition
        return if (position != null && !position.isExpired()) {
            position
        } else {
            savedScrollPosition = null
            null
        }
    }

    fun isPostOwner(post: Post): Boolean {
        return authRepository.getCurrentUserId() == post.authorUid
    }

    fun areCommentsDisabled(post: Post): Boolean {
        return post.postDisableComments?.toBoolean() ?: false
    }

    fun editPost(post: Post) {
        // Navigation handled in UI
    }

    fun deletePost(post: Post) {
        viewModelScope.launch {
            deletePostUseCase(post.id).collect { result ->
                result.onFailure { it.printStackTrace() }
            }
        }
    }

    fun sharePost(post: Post) {
        // UI action
    }

    fun copyPostLink(post: Post) {
        // UI action
    }

    fun toggleComments(post: Post) {
        viewModelScope.launch {
            togglePostCommentsUseCase(post.id).collect { result ->
                result.onFailure { it.printStackTrace() }
            }
        }
    }

    fun reportPost(post: Post) {
        // UI action
    }

    fun blockUser(userId: String) {
        // UI action
    }
}
