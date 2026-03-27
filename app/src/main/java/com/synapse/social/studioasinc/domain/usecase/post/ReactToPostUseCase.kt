package com.synapse.social.studioasinc.domain.usecase.post

import com.synapse.social.studioasinc.domain.repository.ReactionRepository
import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.model.ReactionType
import com.synapse.social.studioasinc.shared.domain.repository.PostActionsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ReactToPostUseCase @Inject constructor(
    private val reactionRepository: ReactionRepository,
    private val postActionsRepository: PostActionsRepository
) {
    operator fun invoke(post: Post, reactionType: ReactionType): Flow<Result<Post>> = flow {
        val currentReaction = post.userReaction
        val isRemoving = currentReaction == reactionType
        val newReaction = if (isRemoving) null else reactionType

        val countChange = when {
            isRemoving -> -1
            currentReaction == null -> 1
            else -> 0
        }

        val newCount = post.likesCount + countChange

        val updatedReactions = post.reactions?.toMutableMap() ?: mutableMapOf()
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

        val updatedPost = post.copy(
            likesCount = maxOf(0, newCount),
            userReaction = newReaction,
            reactions = updatedReactions
        )

        emit(Result.success(updatedPost))

        postActionsRepository.updateLocalPost(updatedPost)

        reactionRepository.toggleReaction(
            targetId = post.id,
            targetType = "post",
            reactionType = reactionType,
            oldReaction = currentReaction,
            skipCheck = true
        ).onFailure {
            // Revert optimistic update by re-emitting the original post
            postActionsRepository.updateLocalPost(post)
            emit(Result.success(post))
            emit(Result.failure(it))
        }
    }
}
