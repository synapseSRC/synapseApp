package com.synapse.social.studioasinc.core.di

import com.synapse.social.studioasinc.shared.data.datasource.SupabaseBlockDataSource
import com.synapse.social.studioasinc.shared.data.repository.BlockRepositoryImpl
import com.synapse.social.studioasinc.shared.domain.repository.BlockRepository
import com.synapse.social.studioasinc.shared.domain.usecase.auth.GetCurrentUserIdUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.blocking.BlockUserUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.blocking.GetBlockedUsersUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.blocking.IsUserBlockedUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.blocking.UnblockUserUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BlockingModule {

    @Provides
    @Singleton
    fun provideSupabaseBlockDataSource(client: SupabaseClient): SupabaseBlockDataSource {
        return SupabaseBlockDataSource(client)
    }

    @Provides
    @Singleton
    fun provideBlockRepository(dataSource: SupabaseBlockDataSource): BlockRepository {
        return BlockRepositoryImpl(dataSource)
    }

    @Provides
    @Singleton
    fun provideBlockUserUseCase(
        blockRepository: BlockRepository,
        getCurrentUserIdUseCase: GetCurrentUserIdUseCase
    ): BlockUserUseCase {
        return BlockUserUseCase(blockRepository, getCurrentUserIdUseCase)
    }

    @Provides
    @Singleton
    fun provideUnblockUserUseCase(blockRepository: BlockRepository): UnblockUserUseCase {
        return UnblockUserUseCase(blockRepository)
    }

    @Provides
    @Singleton
    fun provideGetBlockedUsersUseCase(blockRepository: BlockRepository): GetBlockedUsersUseCase {
        return GetBlockedUsersUseCase(blockRepository)
    }

    @Provides
    @Singleton
    fun provideIsUserBlockedUseCase(blockRepository: BlockRepository): IsUserBlockedUseCase {
        return IsUserBlockedUseCase(blockRepository)
    }
}
