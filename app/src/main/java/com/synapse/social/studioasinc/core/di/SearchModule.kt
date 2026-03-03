package com.synapse.social.studioasinc.core.di

import com.synapse.social.studioasinc.shared.data.repository.SearchRepositoryImpl
import com.synapse.social.studioasinc.shared.domain.repository.ISearchRepository
import com.synapse.social.studioasinc.shared.domain.usecase.search.GetSuggestedAccountsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.search.SearchHashtagsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.search.SearchNewsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.search.SearchPostsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SearchModule {

    @Provides
    @Singleton
    fun provideSearchRepository(): ISearchRepository {
        return SearchRepositoryImpl()
    }

    @Provides
    @Singleton
    fun provideSearchPostsUseCase(repository: ISearchRepository): SearchPostsUseCase {
        return SearchPostsUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideSearchHashtagsUseCase(repository: ISearchRepository): SearchHashtagsUseCase {
        return SearchHashtagsUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideSearchNewsUseCase(repository: ISearchRepository): SearchNewsUseCase {
        return SearchNewsUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideGetSuggestedAccountsUseCase(repository: ISearchRepository): GetSuggestedAccountsUseCase {
        return GetSuggestedAccountsUseCase(repository)
    }
}
