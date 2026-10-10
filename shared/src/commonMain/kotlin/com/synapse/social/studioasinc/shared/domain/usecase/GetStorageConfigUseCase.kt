
package com.synapse.social.studioasinc.shared.domain.usecase

import com.synapse.social.studioasinc.shared.domain.repository.StorageRepository

class GetStorageConfigUseCase(private val repository: StorageRepository) {
    operator fun invoke() = repository.getStorageConfig()
}
