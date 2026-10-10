
package com.synapse.social.studioasinc.shared.domain.usecase

import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import com.synapse.social.studioasinc.shared.domain.model.StorageProvider

class ValidateProviderConfigUseCase {
    operator fun invoke(config: StorageConfig, provider: StorageProvider): Boolean {
        return config.isProviderConfigured(provider)
    }
}
