package com.synapse.social.studioasinc.shared.domain.usecase.search

import com.synapse.social.studioasinc.shared.domain.model.SearchAccount
import com.synapse.social.studioasinc.shared.domain.model.SearchHashtag
import com.synapse.social.studioasinc.shared.domain.model.SearchNews
import com.synapse.social.studioasinc.shared.domain.model.SearchPost
import com.synapse.social.studioasinc.shared.domain.repository.ISearchRepository

class SearchPostsUseCase(private val repository: ISearchRepository) {
    suspend operator fun invoke(query: String): Result<List<SearchPost>> {
        return repository.searchPosts(query)
    }
}

class SearchHashtagsUseCase(private val repository: ISearchRepository) {
    suspend operator fun invoke(query: String): Result<List<SearchHashtag>> {
        return if (query.isBlank()) {
            repository.getTrendingHashtags()
        } else {
            repository.searchHashtags(query)
        }
    }
}

class SearchNewsUseCase(private val repository: ISearchRepository) {
    suspend operator fun invoke(query: String): Result<List<SearchNews>> {
        return repository.searchNews(query)
    }
}

class GetSuggestedAccountsUseCase(private val repository: ISearchRepository) {
    suspend operator fun invoke(query: String): Result<List<SearchAccount>> {
        return repository.getSuggestedAccounts(query)
    }
}
