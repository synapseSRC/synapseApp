package com.synapse.social.studioasinc.domain.usecase.post

import com.synapse.social.studioasinc.data.repository.PollRepository
import com.synapse.social.studioasinc.domain.model.Post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class VotePollUseCase @Inject constructor(
    private val pollRepository: PollRepository
) {
    operator fun invoke(post: Post, optionIndex: Int): Flow<Result<Post>> = flow {
        val currentOptions = post.pollOptions ?: throw IllegalArgumentException("No poll options")
        if (post.userPollVote != null) {
            emit(Result.success(post))
            return@flow
        }

        val updatedOptions = currentOptions.mapIndexed { index, option ->
            if (index == optionIndex) option.copy(votes = option.votes + 1) else option
        }

        val updatedPost = post.copy(
            pollOptions = updatedOptions,
            userPollVote = optionIndex
        )

        emit(Result.success(updatedPost))

        try {
            pollRepository.submitVote(post.id, optionIndex)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
