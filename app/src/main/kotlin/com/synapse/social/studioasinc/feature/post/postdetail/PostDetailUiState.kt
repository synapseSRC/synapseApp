package com.synapse.social.studioasinc.feature.post.postdetail

import com.synapse.social.studioasinc.domain.model.CommentWithUser
import com.synapse.social.studioasinc.domain.model.PostDetail
import com.synapse.social.studioasinc.domain.model.UserProfile

data class PostDetailUiState(
    val isLoading: Boolean = false,
    val post: PostDetail? = null,
    val rootComment: CommentWithUser? = null,
    val comments: List<CommentWithUser> = emptyList(),
    val isCommentsLoading: Boolean = false,
    val error: String? = null,
    val replyToComment: CommentWithUser? = null,
    val replyToParticipants: List<UserProfile> = emptyList(),
    val editingComment: CommentWithUser? = null,
    val hasMoreComments: Boolean = false,
    val currentUserId: String? = null,
    val commentActionsLoading: Set<String> = emptySet(),
    val currentUserAvatarUrl: String? = null,
    val currentUserDisplayName: String? = null,
    val refreshTrigger: Int = 0,
    val blockSuccess: Boolean = false,
    val blockError: String? = null,
    val ancestorComments: List<CommentWithUser> = emptyList(),
    val postSummary: String? = null,
    val isSummarizing: Boolean = false,
    val summaryError: String? = null
)
