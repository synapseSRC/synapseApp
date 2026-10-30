package com.synapse.social.studioasinc.feature.post.postdetail

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.synapse.social.studioasinc.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.synapse.social.studioasinc.data.repository.*
import com.synapse.social.studioasinc.domain.model.*
import com.synapse.social.studioasinc.feature.createpost.createpost.handlers.MediaUploadHandler
import com.synapse.social.studioasinc.feature.shared.components.post.PostEvent
import com.synapse.social.studioasinc.feature.shared.components.post.PostEventBus
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.repository.PostActionsRepository
import com.synapse.social.studioasinc.shared.domain.usecase.blocking.BlockUserUseCase
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
import com.synapse.social.studioasinc.domain.usecase.ai.SummarizePostUseCase
import com.synapse.social.studioasinc.domain.usecase.ai.SummarizeThreadUseCase
import com.synapse.social.studioasinc.domain.usecase.post.RepostPostUseCase
import com.synapse.social.studioasinc.domain.usecase.post.UnrepostPostUseCase

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val application: Application,
    private val postDetailRepository: PostDetailRepositoryImpl,
    private val commentRepository: CommentRepositoryImpl,
    private val reactionRepository: ReactionRepositoryImpl,
    private val pollRepository: PollRepositoryImpl,
    private val bookmarkRepository: BookmarkRepositoryImpl,
    private val repostPostUseCase: RepostPostUseCase,
    private val unrepostPostUseCase: UnrepostPostUseCase,
    private val reportRepository: ReportRepositoryImpl,
    private val userRepository: UserRepositoryImpl,
    private val authRepository: AuthRepository,
    private val mediaUploadHandler: MediaUploadHandler,
    private val postActionsRepository: PostActionsRepository,
    private val blockUserUseCase: BlockUserUseCase,
    private val summarizePostUseCase: SummarizePostUseCase,
    private val summarizeThreadUseCase: SummarizeThreadUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val commentsPagingFlow: Flow<PagingData<CommentWithUser>> = _uiState
        .map { Pair(it.post?.post?.id, it.rootComment?.id) }
        .distinctUntilChanged()
        .filter { it.first != null }
        .flatMapLatest { (postId, rootCommentId) ->
            Pager(
                config = PagingConfig(pageSize = 20),
                pagingSourceFactory = { CommentPagingSource(commentRepository, postId ?: "", rootCommentId) }
            ).flow
        }
        .cachedIn(viewModelScope)

    private var currentPostId: String? = null
    private var rootCommentId: String? = null

    init {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId()
            _uiState.update { it.copy(currentUserId = currentUserId) }
            
            currentUserId?.let { uid ->
                userRepository.getUserById(uid).onSuccess { user ->
                    _uiState.update { it.copy(
                        currentUserAvatarUrl = user?.avatar,
                        currentUserDisplayName = user?.displayName ?: user?.username
                    ) }
                }
            }
        }
    }

    fun loadPost(postId: String, rootCommentId: String? = null) {
        if (currentPostId == postId && this.rootCommentId == rootCommentId) return
        currentPostId = postId
        this.rootCommentId = rootCommentId

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                postDetailRepository.getPostWithDetails(postId)
            }.onSuccess { result ->
                result.fold(
                    onSuccess = { post ->
                        _uiState.update { it.copy(post = post) }

                        if (rootCommentId != null) {
                            runCatching { commentRepository.getComment(rootCommentId) }
                                .getOrNull()
                                ?.onSuccess { comment ->
                                    _uiState.update { it.copy(rootComment = comment) }
                                    runCatching { commentRepository.getCommentAncestors(rootCommentId) }
                                        .getOrNull()
                                        ?.onSuccess { ancestors ->
                                            _uiState.update { it.copy(ancestorComments = ancestors) }
                                        }
                                }
                                ?.onFailure { e ->
                                    _uiState.update { it.copy(error = e.message ?: "Failed to load comment") }
                                }
                        } else {
                            _uiState.update { it.copy(rootComment = null, ancestorComments = emptyList()) }
                        }
                        _uiState.update { it.copy(isLoading = false) }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isLoading = false, error = error.message ?: "Failed to load post") }
                    }
                )
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load post") }
            }
            runCatching { postDetailRepository.incrementViewCount(postId) }
            if (rootCommentId != null) {
                runCatching { postDetailRepository.incrementCommentViewCount(rootCommentId) }
            }
        }
    }

    private val pendingViewedCommentIds = mutableSetOf<String>()
    private var flushViewsJob: kotlinx.coroutines.Job? = null

    fun trackCommentView(commentId: String) {
        pendingViewedCommentIds.add(commentId)
        flushViewsJob?.cancel()
        flushViewsJob = viewModelScope.launch {
            kotlinx.coroutines.delay(2_000)
            val batch = pendingViewedCommentIds.toList()
            pendingViewedCommentIds.clear()
            batch.forEach { postDetailRepository.incrementCommentViewCount(it) }
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
                    likesCount = maxOf(0, currentPost.post.likesCount + countChange),
                    reactions = updatedReactions,
                    userReaction = newReaction
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

    fun addComment(content: String, mediaUri: Uri? = null) {
        if (isSubmittingComment) return
        val postId = currentPostId ?: return
        val parentId = _uiState.value.replyToComment?.id

        isSubmittingComment = true
        viewModelScope.launch {
            try {
                var mediaUrl: String? = null
                if (mediaUri != null) {
                    val mediaItem = MediaItem(id = java.util.UUID.randomUUID().toString(), url = mediaUri.toString(), type = MediaType.IMAGE)
                    val uploadedItems = mediaUploadHandler.uploadMedia(listOf(mediaItem)) { }
                    mediaUrl = uploadedItems.firstOrNull()?.url
                }

                commentRepository.createComment(postId, content, mediaUrl, parentId).onSuccess {
                    refreshComments()
                    invalidateComments()
                    setReplyTo(null)

                }.onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Failed to add comment") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to upload media") }
            } finally {
                isSubmittingComment = false
            }
        }
    }

    fun setReplyTo(comment: CommentWithUser?) {
        _uiState.update { it.copy(replyToComment = comment, replyToParticipants = emptyList(), editingComment = null) }
        if (comment == null) return
        viewModelScope.launch {
            val currentUserId = _uiState.value.currentUserId
            val postAuthor = _uiState.value.post?.author
            val participants = mutableListOf<com.synapse.social.studioasinc.domain.model.UserProfile>()

            // Add the direct comment's author
            comment.user?.let { participants.add(it) }

            // Walk ancestors and collect their authors
            commentRepository.getCommentAncestors(comment.id).onSuccess { ancestors ->
                ancestors.forEach { ancestor -> ancestor.user?.let { participants.add(it) } }
            }

            // Add post author
            if (postAuthor != null) {
                participants.add(
                    com.synapse.social.studioasinc.domain.model.UserProfile(
                        uid = _uiState.value.post?.post?.authorUid ?: "",
                        username = postAuthor.username ?: "",
                        displayName = postAuthor.displayName,
                        avatar = postAuthor.avatar
                    )
                )
            }

            // Dedupe by uid, exclude self
            val unique = participants
                .filter { it.uid.isNotBlank() && it.uid != currentUserId }
                .distinctBy { it.uid }

            _uiState.update { it.copy(replyToParticipants = unique) }
        }
    }

    fun setEditingComment(comment: CommentWithUser?) {
        _uiState.update { it.copy(editingComment = comment, replyToComment = null) }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
             _uiState.update { it.copy(commentActionsLoading = it.commentActionsLoading + commentId) }
            commentRepository.deleteComment(commentId).onSuccess {
                refreshComments()
                invalidateComments()
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

    fun toggleReshare() {
        val postId = currentPostId ?: return
        val currentPostDetail = _uiState.value.post ?: return
        val currentPost = currentPostDetail.post
        val isCurrentlyReshared = currentPostDetail.hasReshared

        viewModelScope.launch {
            // Optimistic update
            val newResharesCount = if (isCurrentlyReshared) maxOf(0, currentPost.resharesCount - 1) else currentPost.resharesCount + 1
            val optimisticPost = currentPost.copy(
                isReshared = !isCurrentlyReshared,
                resharesCount = newResharesCount
            )
            val optimisticPostDetail = currentPostDetail.copy(
                post = optimisticPost,
                hasReshared = !isCurrentlyReshared
            )
            _uiState.update { it.copy(post = optimisticPostDetail) }
            com.synapse.social.studioasinc.feature.shared.components.post.PostEventBus.emit(com.synapse.social.studioasinc.feature.shared.components.post.PostEvent.Updated(optimisticPost))

            val result = if (isCurrentlyReshared) {
                unrepostPostUseCase(postId)
            } else {
                repostPostUseCase(postId)
            }

            result.onSuccess {
                postDetailRepository.getPostWithDetails(postId).onSuccess { updatedPostDetail ->
                    _uiState.update { it.copy(post = updatedPostDetail) }
                }
            }.onFailure {
                // Revert on failure
                _uiState.update { it.copy(post = currentPostDetail) }
                com.synapse.social.studioasinc.feature.shared.components.post.PostEventBus.emit(com.synapse.social.studioasinc.feature.shared.components.post.PostEvent.Updated(currentPost))
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
            _uiState.update { it.copy(blockSuccess = false, blockError = null) }
            
            blockUserUseCase(userId)
                .onSuccess {
                    _uiState.update { it.copy(blockSuccess = true) }
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(blockError = error.message ?: "Failed to block user")
                    }
                }
        }
    }
    
    fun clearBlockStatus() {
        _uiState.update { it.copy(blockSuccess = false, blockError = null) }
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
        val clip = ClipData.newPlainText(application.getString(R.string.clip_label_post_link), "synapse://post/$postId")
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
    }

    fun summarizePost() {
        val post = _uiState.value.post?.post ?: return
        val content = post.postText.orEmpty().trim()
        if (content.isBlank()) {
            _uiState.update { it.copy(summaryError = "No text content to summarize.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSummarizing = true, postSummary = null, summaryError = null) }
            summarizePostUseCase(content, emptyList())
                .onSuccess { summary -> _uiState.update { it.copy(isSummarizing = false, postSummary = summary) } }
                .onFailure { e -> _uiState.update { it.copy(isSummarizing = false, summaryError = e.message ?: "Failed to summarize post.") } }
        }
    }

    fun clearPostSummary() {
        _uiState.update { it.copy(postSummary = null, summaryError = null) }
    }
}
