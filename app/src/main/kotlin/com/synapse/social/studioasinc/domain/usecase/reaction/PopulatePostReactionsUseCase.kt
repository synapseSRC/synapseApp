package com.synapse.social.studioasinc.domain.usecase.reaction

import com.synapse.social.studioasinc.domain.model.Post
import com.synapse.social.studioasinc.domain.repository.ReactionRepository
import javax.inject.Inject

class PopulatePostReactionsUseCase @Inject constructor(
    private val reactionRepository: ReactionRepository
) {
    suspend operator fun invoke(posts: List<Post>): List<Post> =
        reactionRepository.populatePostReactions(posts)
}
