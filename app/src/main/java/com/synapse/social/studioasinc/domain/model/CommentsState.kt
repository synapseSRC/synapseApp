package com.synapse.social.studioasinc.domain.model



sealed class CommentsState {


    object Loading : CommentsState()



    data class Success(
        val comments: List<CommentWithUser>,
        val hasMore: Boolean = false
    ) : CommentsState()



    data class Error(val message: String, val throwable: Throwable? = null) : CommentsState()



    object Empty : CommentsState()
}
