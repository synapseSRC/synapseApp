package com.synapse.social.studioasinc.domain.usecase.post

import com.synapse.social.studioasinc.domain.repository.PollRepository
import com.synapse.social.studioasinc.domain.model.Post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

import com.synapse.social.studioasinc.shared.domain.repository.PostActionsRepository

class VotePollUseCase @Inject constructor(
    private val pollRepository: PollRepository,
    private val postActionsRepository: PostActionsRepository
) {
    operator fun invoke(post: Post, optionIndex: Int): Flow<Result<Post>> = flow {
        val currentOptions = post.pollOptions ?: throw IllegalArgumentException("No poll options")
        if (post.userPollVote == optionIndex) {
            emit(Result.success(post))
            return@flow
        }

        val previousVote = post.userPollVote

        val updatedOptions = currentOptions.mapIndexed { index, option ->
            when (index) {
                optionIndex -> option.copy(votes = option.votes + 1)
                previousVote -> option.copy(votes = maxOf(0, option.votes - 1))
                else -> option
            }
        }

        val updatedPost = post.copy(
            pollOptions = updatedOptions,
            userPollVote = optionIndex
        )

        emit(Result.success(updatedPost))

        try {
            pollRepository.submitVote(post.id, optionIndex)
            postActionsRepository.updateLocalPost(updatedPost)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
