package com.synapse.social.studioasinc.domain.usecase.post

import com.synapse.social.studioasinc.domain.repository.PollRepository
import com.synapse.social.studioasinc.domain.model.Post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.math.max

import com.synapse.social.studioasinc.shared.domain.repository.PostActionsRepository

class RevokeVoteUseCase @Inject constructor(
    private val pollRepository: PollRepository,
    private val postActionsRepository: PostActionsRepository
) {
    operator fun invoke(post: Post): Flow<Result<Post>> = flow {
        val currentVoteIndex = post.userPollVote
        if (currentVoteIndex == null) {
            emit(Result.success(post))
            return@flow
        }
        val currentOptions = post.pollOptions ?: throw IllegalArgumentException("No poll options")

        val updatedOptions = currentOptions.mapIndexed { index, option ->
            if (index == currentVoteIndex) option.copy(votes = maxOf(0, option.votes - 1)) else option
        }

        val updatedPost = post.copy(
            pollOptions = updatedOptions,
            userPollVote = null
        )

        emit(Result.success(updatedPost))

        try {
            pollRepository.revokeVote(post.id)
            postActionsRepository.updateLocalPost(updatedPost)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
