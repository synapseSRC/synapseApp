package com.synapse.social.studioasinc.core.di.ai

import android.content.Context
import com.synapse.social.studioasinc.shared.data.repository.ai.AndroidLocalAiRepository
import com.synapse.social.studioasinc.shared.domain.repository.ai.AiRepository
import com.synapse.social.studioasinc.shared.domain.usecase.ai.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideAiRepository(@ApplicationContext context: Context): AiRepository {
        return AndroidLocalAiRepository(context)
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
    fun provideSummarizeChatUseCase(
        aiRepository: AiRepository
    ): SummarizeChatUseCase {
        return SummarizeChatUseCase(aiRepository)
    }

    @Provides
    @Singleton
    fun provideCheckMessageToxicityUseCase(
        aiRepository: AiRepository
    ): CheckMessageToxicityUseCase {
        return CheckMessageToxicityUseCase(aiRepository)
    }

    @Provides
    @Singleton
    fun provideDetectSensitiveMediaUseCase(
        aiRepository: AiRepository
    ): DetectSensitiveMediaUseCase {
        return DetectSensitiveMediaUseCase(aiRepository)
    }
}
