package com.synapse.social.studioasinc.shared.domain.usecase

import com.synapse.social.studioasinc.shared.domain.model.LinkPreview
import com.synapse.social.studioasinc.shared.domain.repository.LinkPreviewRepository

class GetLinkMetadataUseCase(
    private val repository: LinkPreviewRepository
) {
    suspend operator fun invoke(url: String): LinkPreview? {
        // Return null for invalid or blank URLs
        if (url.isBlank()) return null

        // Ensure the URL has a scheme before fetching
        val validUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }

        return repository.getLinkPreview(validUrl)
    }
}
