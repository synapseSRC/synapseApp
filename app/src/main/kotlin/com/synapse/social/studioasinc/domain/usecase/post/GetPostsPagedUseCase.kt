package com.synapse.social.studioasinc.domain.usecase.post

import androidx.paging.PagingData
import com.synapse.social.studioasinc.domain.repository.PostRepository
import com.synapse.social.studioasinc.domain.model.Post
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPostsPagedUseCase @Inject constructor(
    private val repository: PostRepository
) {
    operator fun invoke(): Flow<PagingData<Post>> = repository.getPostsPaged()
}
