package com.synapse.social.studioasinc.shared.domain.usecase

import com.synapse.social.studioasinc.shared.domain.model.MediaType
import com.synapse.social.studioasinc.shared.domain.model.StorageProvider
import com.synapse.social.studioasinc.shared.domain.repository.StorageRepository

class UpdateStorageProviderUseCase(private val repository: StorageRepository) {
    suspend operator fun invoke(mediaType: MediaType, provider: StorageProvider) {
        when (mediaType) {
            MediaType.PHOTO, MediaType.IMAGE -> repository.updatePhotoProvider(provider)
            MediaType.VIDEO -> repository.updateVideoProvider(provider)
            MediaType.OTHER -> repository.updateOtherProvider(provider)
            else -> {

            }
        }
    }
}
