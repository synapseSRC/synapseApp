package com.synapse.social.studioasinc.domain.usecase.post

import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.repository.PollRepository
import javax.inject.Inject

class PopulatePostPollsUseCase @Inject constructor(
    private val pollRepository: PollRepository
) {
    suspend operator fun invoke(posts: List<Post>): List<Post> {
        val pollPosts = posts.filter { it.hasPoll == true }
        if (pollPosts.isEmpty()) return posts

        val postIds = pollPosts.map { it.id }
        val userVotes = pollRepository.getBatchUserVotes(postIds).getOrNull() ?: emptyMap()
        val pollCounts = pollRepository.getBatchPollVotes(postIds).getOrNull() ?: emptyMap()

        return posts.map { post ->
            if (post.hasPoll == true) {
                val updatedOptions = post.pollOptions?.mapIndexed { index, option ->
                    option.copy(votes = pollCounts[post.id]?.get(index) ?: 0)
                }
                val updatedPost = post.copy(pollOptions = updatedOptions)
                updatedPost.userPollVote = userVotes[post.id]
                updatedPost
            } else {
                post
            }
        }
    }
}
