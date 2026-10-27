package com.synapse.social.studioasinc.core.di

import com.synapse.social.studioasinc.data.call.CallSimulationController
import com.synapse.social.studioasinc.data.call.MockCallRepository
import com.synapse.social.studioasinc.shared.domain.repository.CallRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CallModule {

    @Binds
    @Singleton
    abstract fun bindCallRepository(repository: MockCallRepository): CallRepository

    @Binds
    @Singleton
    abstract fun bindCallSimulationController(repository: MockCallRepository): CallSimulationController
}
