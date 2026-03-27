package com.synapse.social.studioasinc.core.di

import com.synapse.social.studioasinc.data.repository.UserRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface UtilsEntryPoint {
    fun userRepository(): UserRepository
}
