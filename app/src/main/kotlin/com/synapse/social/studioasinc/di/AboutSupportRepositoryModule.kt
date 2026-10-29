package com.synapse.social.studioasinc.di

import com.synapse.social.studioasinc.data.repository.AssetOpenSourceLicenseRepository
import com.synapse.social.studioasinc.data.repository.FeedbackRepositoryImpl
import com.synapse.social.studioasinc.data.repository.GitHubAppUpdateRepository
import com.synapse.social.studioasinc.data.repository.GitHubReleaseRepository
import com.synapse.social.studioasinc.domain.repository.AppReleaseRepository
import com.synapse.social.studioasinc.domain.repository.AppUpdateRepository
import com.synapse.social.studioasinc.domain.repository.FeedbackRepository
import com.synapse.social.studioasinc.domain.repository.OpenSourceLicenseRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AboutSupportRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindFeedbackRepository(implementation: FeedbackRepositoryImpl): FeedbackRepository

    @Binds
    @Singleton
    abstract fun bindAppUpdateRepository(implementation: GitHubAppUpdateRepository): AppUpdateRepository

    @Binds
    @Singleton
    abstract fun bindOpenSourceLicenseRepository(
        implementation: AssetOpenSourceLicenseRepository
    ): OpenSourceLicenseRepository

    @Binds
    @Singleton
    abstract fun bindAppReleaseRepository(implementation: GitHubReleaseRepository): AppReleaseRepository
}
