package com.synapse.social.studioasinc.core.di.ai

import com.synapse.social.studioasinc.data.repository.ai.AiRepositoryImpl
import com.synapse.social.studioasinc.data.repository.ai.MultiProviderAiRepository
import com.synapse.social.studioasinc.domain.repository.ai.AiRepository
import com.synapse.social.studioasinc.domain.usecase.ai.GenerateSmartRepliesUseCase
import com.synapse.social.studioasinc.domain.usecase.ai.SummarizeChatUseCase
import com.synapse.social.studioasinc.domain.usecase.ai.SummarizeMessageUseCase
import com.synapse.social.studioasinc.domain.usecase.ai.SummarizePostUseCase
import com.synapse.social.studioasinc.domain.usecase.ai.SummarizeThreadUseCase
import com.synapse.social.studioasinc.settings.ApiKeySettingsService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideAiRepository(
        apiKeySettingsService: ApiKeySettingsService
    ): AiRepository {
        val platformRepository = AiRepositoryImpl(apiKeySettingsService)
        return MultiProviderAiRepository(platformRepository, apiKeySettingsService)
    }

    @Provides
    @Singleton
    fun provideGenerateSmartRepliesUseCase(
        aiRepository: AiRepository
    ): GenerateSmartRepliesUseCase {
        return GenerateSmartRepliesUseCase(aiRepository)
    }

    @Provides
    @Singleton
    fun provideSummarizeChatUseCase(aiRepository: AiRepository): SummarizeChatUseCase {
        return SummarizeChatUseCase(aiRepository)
    }

    @Provides
    @Singleton
    fun provideSummarizePostUseCase(aiRepository: AiRepository): SummarizePostUseCase {
        return SummarizePostUseCase(aiRepository)
    }

    @Provides
    @Singleton
    fun provideSummarizeMessageUseCase(aiRepository: AiRepository): SummarizeMessageUseCase {
        return SummarizeMessageUseCase(aiRepository)
    }

    @Provides
    @Singleton
    fun provideSummarizeThreadUseCase(aiRepository: AiRepository): SummarizeThreadUseCase {
        return SummarizeThreadUseCase(aiRepository)
    }
}
