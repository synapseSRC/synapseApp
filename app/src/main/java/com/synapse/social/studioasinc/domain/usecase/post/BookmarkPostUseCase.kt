package com.synapse.social.studioasinc.domain.usecase.post

import com.synapse.social.studioasinc.data.repository.PostInteractionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class BookmarkPostUseCase @Inject constructor(
    private val repository: PostInteractionRepository
) {
    operator fun invoke(postId: String, userId: String, isBookmarked: Boolean): Flow<Result<Boolean>> = flow {
        val result = if (isBookmarked) {
            repository.unsavePost(postId, userId)
        } else {
            repository.savePost(postId, userId)
        }

        if (result.isSuccess) {
            emit(Result.success(!isBookmarked))
        } else {
            emit(Result.failure(result.exceptionOrNull() ?: Exception("Unknown error")))
        }
    }
}
