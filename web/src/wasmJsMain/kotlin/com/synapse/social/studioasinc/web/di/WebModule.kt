package com.synapse.social.studioasinc.web.di

import com.synapse.social.studioasinc.shared.data.repository.SupabaseAuthRepository
import com.synapse.social.studioasinc.shared.data.repository.SearchRepositoryImpl
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.repository.ISearchRepository
import com.synapse.social.studioasinc.shared.domain.usecase.auth.SignInUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.search.SearchPostsUseCase
import org.koin.dsl.module

val webModule = module {
    single<AuthRepository> { SupabaseAuthRepository() }
    single { SignInUseCase(get()) }

    single<ISearchRepository> { SearchRepositoryImpl() }
    single { SearchPostsUseCase(get()) }
}
