package com.synapse.social.studioasinc.core.di

import com.synapse.social.studioasinc.feature.inbox.inbox.voice.VoiceUploadService
import com.synapse.social.studioasinc.shared.domain.repository.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ChatEntryPoint {
    fun voiceUploadService(): VoiceUploadService
    fun settingsRepository(): SettingsRepository
}
