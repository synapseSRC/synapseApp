package com.synapse.social.studioasinc.shared.domain.usecase.search

import com.synapse.social.studioasinc.shared.domain.model.SearchPost
import com.synapse.social.studioasinc.shared.domain.repository.ISearchRepository

class SearchPostsUseCase(private val repository: ISearchRepository) {
    suspend operator fun invoke(query: String): Result<List<SearchPost>> {
        return repository.searchPosts(query)
    }
}
