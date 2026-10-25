package com.synapse.social.studioasinc.core.di

import com.synapse.social.studioasinc.shared.domain.repository.PostActionsRepository
import com.synapse.social.studioasinc.shared.domain.usecase.post.DeletePostUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.post.TogglePostCommentsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PostModule {

    @Provides
    @Singleton
    fun provideDeletePostUseCase(postActionsRepository: PostActionsRepository): DeletePostUseCase = DeletePostUseCase(postActionsRepository)

    @Provides
    @Singleton
    fun provideTogglePostCommentsUseCase(postActionsRepository: PostActionsRepository): TogglePostCommentsUseCase = TogglePostCommentsUseCase(postActionsRepository)
}
