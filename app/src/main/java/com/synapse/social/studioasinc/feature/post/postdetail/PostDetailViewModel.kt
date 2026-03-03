package com.synapse.social.studioasinc.feature.post.postdetail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapse.social.studioasinc.data.repository.*
import com.synapse.social.studioasinc.domain.model.*
import com.synapse.social.studioasinc.feature.shared.components.post.PostEvent
import com.synapse.social.studioasinc.feature.shared.components.post.PostEventBus
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.repository.PostActionsRepository
import com.synapse.social.studioasinc.data.paging.CommentPagingSource
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val postDetailRepository: PostDetailRepository,
    private val commentRepository: CommentRepository,
    private val reactionRepository: ReactionRepository,
    private val pollRepository: PollRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val reshareRepository: ReshareRepository,
    private val reportRepository: ReportRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val postActionsRepository: PostActionsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val commentsPagingFlow: Flow<PagingData<CommentWithUser>> = _uiState
        .map { it.post?.post?.id }
        .distinctUntilChanged()
        .filterNotNull()
        .flatMapLatest { postId ->
            Pager(
                config = PagingConfig(pageSize = 20),
                pagingSourceFactory = { CommentPagingSource(commentRepository, postId) }
            ).flow
        }
        .cachedIn(viewModelScope)

    private var currentPostId: String? = null

    init {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId()
            _uiState.update { it.copy(currentUserId = currentUserId) }
        }
    }

    fun loadPost(postId: String) {
        if (currentPostId == postId) return
        currentPostId = postId

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            postDetailRepository.getPostWithDetails(postId).fold(
                onSuccess = { post ->
                    _uiState.update { it.copy(post = post, isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message ?: "Failed to load post") }
                }
            )
            postDetailRepository.incrementViewCount(postId)
        }
    }

    fun refreshComments() {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            postDetailRepository.getPostWithDetails(postId).onSuccess { updatedPost ->
                _uiState.update { it.copy(post = updatedPost) }
                PostEventBus.emit(PostEvent.Updated(updatedPost.post))
            }
        }
    }

    fun invalidateComments() {
        _uiState.update { it.copy(refreshTrigger = it.refreshTrigger + 1) }
    }

    fun toggleReaction(reactionType: ReactionType) {
        val postId = currentPostId ?: return
        val currentPost = _uiState.value.post ?: return
        
        viewModelScope.launch {
            val currentReaction = currentPost.userReaction
            val isRemoving = currentReaction == reactionType
            val newReaction = if (isRemoving) null else reactionType
            
            val countChange = when {
                isRemoving -> -1
                currentReaction == null -> 1
                else -> 0
            }
            
            val updatedReactions = currentPost.reactionSummary.toMutableMap()
            if (isRemoving) {
                val currentCount = updatedReactions[reactionType] ?: 1
                updatedReactions[reactionType] = maxOf(0, currentCount - 1)
            } else {
                if (currentReaction != null) {
                    val oldTypeCount = updatedReactions[currentReaction] ?: 1
                    updatedReactions[currentReaction] = maxOf(0, oldTypeCount - 1)
                }
                val newTypeCount = updatedReactions[reactionType] ?: 0
                updatedReactions[reactionType] = newTypeCount + 1
            }
            
            val optimisticPost = currentPost.copy(
                post = currentPost.post.copy(
                    likesCount = maxOf(0, currentPost.post.likesCount + countChange)
                ),
                userReaction = newReaction,
                reactionSummary = updatedReactions
            )
            
            _uiState.update { it.copy(post = optimisticPost) }
            PostEventBus.emit(PostEvent.Updated(optimisticPost.post))
            
            postActionsRepository.updateLocalPost(optimisticPost.post)

            reactionRepository.toggleReaction(postId, "post", reactionType, currentReaction, skipCheck = true).onFailure {
                _uiState.update { it.copy(post = currentPost) }
                PostEventBus.emit(PostEvent.Updated(currentPost.post))
            }
        }
    }

    fun toggleCommentReaction(commentId: String, reactionType: ReactionType) {
        viewModelScope.launch {
            reactionRepository.toggleReaction(commentId, "comment", reactionType).onSuccess {
                 invalidateComments()
            }
        }
    }

    private var isSubmittingComment = false

    fun addComment(content: String) {
        if (isSubmittingComment) return
        val postId = currentPostId ?: return
        val parentId = _uiState.value.replyToComment?.id

        isSubmittingComment = true
        viewModelScope.launch {
            commentRepository.addComment(postId, content, parentId).onSuccess {
                refreshComments()
                setReplyTo(null)
            }.also {
                isSubmittingComment = false
            }
        }
    }

    fun setReplyTo(comment: CommentWithUser?) {
        _uiState.update { it.copy(replyToComment = comment, editingComment = null) }
    }

    fun setEditingComment(comment: CommentWithUser?) {
        _uiState.update { it.copy(editingComment = comment, replyToComment = null) }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
             _uiState.update { it.copy(commentActionsLoading = it.commentActionsLoading + commentId) }
            commentRepository.deleteComment(commentId).onSuccess {
                refreshComments()
            }.also {
                _uiState.update { it.copy(commentActionsLoading = it.commentActionsLoading - commentId) }
            }
        }
    }

    fun editComment(commentId: String, content: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(commentActionsLoading = it.commentActionsLoading + commentId) }
            commentRepository.updateComment(commentId, content).onSuccess {
                invalidateComments()
                setEditingComment(null)
            }.also {
                _uiState.update { it.copy(commentActionsLoading = it.commentActionsLoading - commentId) }
            }
        }
    }

    fun votePoll(optionIndex: Int) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            pollRepository.submitVote(postId, optionIndex).onSuccess {
                postDetailRepository.getPostWithDetails(postId).onSuccess { updatedPost ->
                     _uiState.update { it.copy(post = updatedPost) }
                 }
            }
        }
    }

    fun revokeVote() {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            pollRepository.revokeVote(postId).onSuccess {
                postDetailRepository.getPostWithDetails(postId).onSuccess { updatedPost ->
                     _uiState.update { it.copy(post = updatedPost) }
                 }
            }
        }
    }

    fun toggleBookmark() {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            bookmarkRepository.toggleBookmark(postId, null).onSuccess {
                postDetailRepository.getPostWithDetails(postId).onSuccess { updatedPost ->
                     _uiState.update { it.copy(post = updatedPost) }
                 }
            }
        }
    }

    fun createReshare(commentary: String?) {
        val postId = currentPostId ?: return
        viewModelScope.launch {
            reshareRepository.createReshare(postId, commentary).onSuccess {
                postDetailRepository.getPostWithDetails(postId).onSuccess { updatedPost ->
                    _uiState.update { it.copy(post = updatedPost) }
                }
            }
        }
    }

    fun reportPost(reason: String) {
        val postId = currentPostId ?: return
        viewModelScope.launch { reportRepository.createReport(postId, reason, null) }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            postDetailRepository.deletePost(postId)
        }
    }

    fun toggleComments() {

    }

    fun blockUser(userId: String) {
        viewModelScope.launch {

        }
    }

    fun hideComment(commentId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(commentActionsLoading = it.commentActionsLoading + commentId) }
            commentRepository.hideComment(commentId).onSuccess {
                invalidateComments()
            }.also {
                _uiState.update { it.copy(commentActionsLoading = it.commentActionsLoading - commentId) }
            }
        }
    }

    fun pinComment(commentId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(commentActionsLoading = it.commentActionsLoading + commentId) }
            commentRepository.pinComment(commentId).onSuccess {
                invalidateComments()
            }.also {
                _uiState.update { it.copy(commentActionsLoading = it.commentActionsLoading - commentId) }
            }
        }
    }

    fun reportComment(commentId: String, reason: String, description: String?) {
        viewModelScope.launch {
            commentRepository.reportComment(commentId, reason)
        }
    }

    fun copyLink(postId: String, context: Context) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val clip = ClipData.newPlainText("Post Link", "synapse://post/$postId")
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
    }

    fun loadReplies(commentId: String) {
        if (_uiState.value.replyLoading.contains(commentId)) return

        viewModelScope.launch {
            _uiState.update { it.copy(replyLoading = it.replyLoading + commentId) }
            commentRepository.getReplies(commentId).fold(
                onSuccess = { replies ->
                    _uiState.update {
                        it.copy(
                            replies = it.replies + (commentId to replies),
                            replyLoading = it.replyLoading - commentId
                        )
                    }
                },
                onFailure = {
                    _uiState.update { it.copy(replyLoading = it.replyLoading - commentId) }
                }
            )
        }
    }
}
