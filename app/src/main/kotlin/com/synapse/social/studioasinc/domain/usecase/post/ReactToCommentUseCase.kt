package com.synapse.social.studioasinc.domain.usecase.post

import com.synapse.social.studioasinc.domain.repository.ReactionRepository
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.ReactionType
import javax.inject.Inject

class ReactToCommentUseCase @Inject constructor(
    private val reactionRepository: ReactionRepository
) {
    suspend operator fun invoke(commentId: String, reactionType: ReactionType): Result<Post> {
        return reactionRepository.toggleReaction(commentId, "comment", reactionType).map {
            val summary = reactionRepository.getReactionSummary(commentId, "comment").getOrDefault(emptyMap())
            val userReact = reactionRepository.getUserReaction(commentId, "comment").getOrNull()

            Post(
                id = commentId,
                authorUid = "",
                likesCount = summary[ReactionType.LIKE] ?: 0,
                userReaction = userReact
            )
        }
    }
}
