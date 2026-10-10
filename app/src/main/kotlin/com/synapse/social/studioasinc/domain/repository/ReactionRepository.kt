package com.synapse.social.studioasinc.domain.repository

import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.ReactionType

enum class ReactionToggleResult { ADDED, REMOVED, UPDATED }

interface ReactionRepository {
    suspend fun toggleReaction(
        targetId: String,
        targetType: String,
        reactionType: ReactionType,
        oldReaction: ReactionType? = null,
        skipCheck: Boolean = false
    ): Result<ReactionToggleResult>

    suspend fun getReactionSummary(targetId: String, targetType: String): Result<Map<ReactionType, Int>>
    suspend fun getUserReaction(targetId: String, targetType: String): Result<ReactionType?>
    suspend fun populatePostReactions(posts: List<Post>): List<Post>
}
