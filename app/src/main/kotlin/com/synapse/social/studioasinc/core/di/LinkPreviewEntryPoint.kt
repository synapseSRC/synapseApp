package com.synapse.social.studioasinc.core.di

import com.synapse.social.studioasinc.shared.domain.usecase.GetLinkMetadataUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Lets UI code resolve the shared link metadata use case without every screen having to provide it.
 * [GetLinkMetadataUseCase] is already a Hilt singleton, so this entry point hands out exactly the
 * same instance the rest of the app uses.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface LinkPreviewEntryPoint {
    fun linkMetadataUseCase(): GetLinkMetadataUseCase
}
