package com.synapse.social.studioasinc.di

import com.synapse.social.studioasinc.shared.domain.repository.AppRepository
import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.repository.LinkPreviewRepository
import com.synapse.social.studioasinc.shared.domain.repository.PlatformInfoProvider
import com.synapse.social.studioasinc.shared.domain.repository.SettingsRepository
import com.synapse.social.studioasinc.shared.domain.repository.UserPreferencesRepository
import com.synapse.social.studioasinc.shared.domain.usecase.CheckForUpdatesUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.GetLinkMetadataUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.settings.GetContextualHeroCardsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.settings.ObserveAccessibilitySettingsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.settings.ObserveChatSettingsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.settings.SearchSettingsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.settings.SyncAccessibilitySettingsUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.settings.SyncChatSettingsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SettingsUseCaseModule {

    @Provides
    @Singleton
    fun provideSearchSettingsUseCase(
        settingsRepository: SettingsRepository
    ): SearchSettingsUseCase = SearchSettingsUseCase(settingsRepository)

    @Provides
    @Singleton
    fun provideGetContextualHeroCardsUseCase(
        settingsRepository: SettingsRepository
    ): GetContextualHeroCardsUseCase = GetContextualHeroCardsUseCase(settingsRepository)

    @Provides
    @Singleton
    fun provideObserveAccessibilitySettingsUseCase(
        settingsRepository: SettingsRepository
    ): ObserveAccessibilitySettingsUseCase = ObserveAccessibilitySettingsUseCase(settingsRepository)

    @Provides
    @Singleton
    fun provideSyncAccessibilitySettingsUseCase(
        settingsRepository: SettingsRepository,
        userPreferencesRepository: UserPreferencesRepository,
        authRepository: AuthRepository
    ): SyncAccessibilitySettingsUseCase = SyncAccessibilitySettingsUseCase(settingsRepository, userPreferencesRepository, authRepository)

    @Provides
    @Singleton
    fun provideObserveChatSettingsUseCase(
        settingsRepository: SettingsRepository
    ): ObserveChatSettingsUseCase = ObserveChatSettingsUseCase(settingsRepository)

    @Provides
    @Singleton
    fun provideSyncChatSettingsUseCase(
        settingsRepository: SettingsRepository,
        userPreferencesRepository: UserPreferencesRepository,
        authRepository: AuthRepository
    ): SyncChatSettingsUseCase = SyncChatSettingsUseCase(settingsRepository, userPreferencesRepository, authRepository)

    @Provides
    @Singleton
    fun provideGetLinkMetadataUseCase(
        linkPreviewRepository: LinkPreviewRepository
    ): GetLinkMetadataUseCase = GetLinkMetadataUseCase(linkPreviewRepository)

    @Provides
    @Singleton
    fun provideCheckForUpdatesUseCase(
        appRepository: AppRepository,
        platformInfoProvider: PlatformInfoProvider
    ): CheckForUpdatesUseCase = CheckForUpdatesUseCase(appRepository, platformInfoProvider)
}
