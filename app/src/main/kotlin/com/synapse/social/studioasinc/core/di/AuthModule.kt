package com.synapse.social.studioasinc.core.di

import com.synapse.social.studioasinc.shared.domain.repository.AuthRepository
import com.synapse.social.studioasinc.shared.domain.repository.UserRepository
import com.synapse.social.studioasinc.shared.domain.usecase.auth.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideSupabaseAuthenticationService(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context
    ): com.synapse.social.studioasinc.data.remote.services.SupabaseAuthenticationService {
        return com.synapse.social.studioasinc.data.remote.services.SupabaseAuthenticationService.getInstance(context)
    }
}

