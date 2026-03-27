package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.SearchAccount
import com.synapse.social.studioasinc.shared.domain.model.SearchHashtag
import com.synapse.social.studioasinc.shared.domain.model.SearchNews
import com.synapse.social.studioasinc.shared.domain.model.SearchPost

interface ISearchRepository {
    suspend fun searchPosts(query: String): Result<List<SearchPost>>
    suspend fun searchHashtags(query: String): Result<List<SearchHashtag>>
    suspend fun searchNews(query: String): Result<List<SearchNews>>
    suspend fun getSuggestedAccounts(query: String): Result<List<SearchAccount>>
    suspend fun getTrendingHashtags(): Result<List<SearchHashtag>>
}
