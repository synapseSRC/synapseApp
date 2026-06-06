package com.synapse.social.studioasinc.shared.domain.usecase.search

import com.synapse.social.studioasinc.shared.domain.model.SearchPost
import com.synapse.social.studioasinc.shared.domain.repository.ISearchRepository

class GetPostsByHashtagUseCase(private val repository: ISearchRepository) {
    suspend operator fun invoke(tag: String): Result<List<SearchPost>> = repository.getPostsByHashtag(tag)
}
