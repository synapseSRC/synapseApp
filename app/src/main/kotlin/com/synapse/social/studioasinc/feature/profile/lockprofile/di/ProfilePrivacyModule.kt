package com.synapse.social.studioasinc.feature.profile.lockprofile.di

import com.synapse.social.studioasinc.data.repository.ProfileActionRepositoryImpl
import com.synapse.social.studioasinc.domain.repository.ProfilePrivacyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfilePrivacyModule {
    @Binds
    abstract fun bindProfilePrivacyRepository(implementation: ProfileActionRepositoryImpl): ProfilePrivacyRepository
}
