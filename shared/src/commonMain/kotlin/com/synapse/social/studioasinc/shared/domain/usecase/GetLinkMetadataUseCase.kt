package com.synapse.social.studioasinc.shared.domain.usecase

import com.synapse.social.studioasinc.shared.core.util.LinkMetadataParser
import com.synapse.social.studioasinc.shared.domain.model.LinkPreview
import com.synapse.social.studioasinc.shared.domain.repository.LinkPreviewRepository

class GetLinkMetadataUseCase(
    private val repository: LinkPreviewRepository
) {
    suspend operator fun invoke(url: String): LinkPreview? {
        // Blank, malformed and non-http(s) input is rejected before any network work happens.
        val normalized = LinkMetadataParser.normalizeUrl(url) ?: return null
        return repository.getLinkPreview(normalized)
    }
}
