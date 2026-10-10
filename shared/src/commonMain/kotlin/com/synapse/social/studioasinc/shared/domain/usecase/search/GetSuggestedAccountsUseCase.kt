package com.synapse.social.studioasinc.shared.domain.usecase.search

import com.synapse.social.studioasinc.shared.domain.model.SearchAccount
import com.synapse.social.studioasinc.shared.domain.repository.ISearchRepository

class GetSuggestedAccountsUseCase(private val repository: ISearchRepository) {
    suspend operator fun invoke(query: String): Result<List<SearchAccount>> {
        return repository.getSuggestedAccounts(query)
    }
}
