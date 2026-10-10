package com.synapse.social.studioasinc.shared.domain.usecase.search

import com.synapse.social.studioasinc.shared.domain.model.SearchHashtag
import com.synapse.social.studioasinc.shared.domain.repository.ISearchRepository

class GetTrendingHashtagsUseCase(private val repository: ISearchRepository) {
    suspend operator fun invoke(): Result<List<SearchHashtag>> = repository.getTrendingHashtags()
}
