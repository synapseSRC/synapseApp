package com.synapse.social.studioasinc.domain.usecase.post

import androidx.paging.PagingData
import com.synapse.social.studioasinc.domain.repository.PostRepository
import com.synapse.social.studioasinc.domain.model.FeedItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFeedPagedUseCase @Inject constructor(
    private val repository: PostRepository
) {
    operator fun invoke(): Flow<PagingData<FeedItem>> = repository.getFeedPaged()
}
