package com.synapse.social.studioasinc.di

import com.synapse.social.studioasinc.shared.data.repository.SupabaseGroupCreationRepository
import com.synapse.social.studioasinc.shared.domain.repository.GroupCreationRepository
import com.synapse.social.studioasinc.shared.data.datasource.SupabaseChatDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GroupCreationModule {
    @Provides
    @Singleton
    fun provideSupabaseChatDataSource(): SupabaseChatDataSource = SupabaseChatDataSource()

    @Provides
    @Singleton
    fun provideGroupCreationRepository(dataSource: SupabaseChatDataSource): GroupCreationRepository =
        SupabaseGroupCreationRepository(dataSource)
}
