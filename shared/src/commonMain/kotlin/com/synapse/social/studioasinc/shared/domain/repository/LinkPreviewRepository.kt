package com.synapse.social.studioasinc.shared.domain.repository

import com.synapse.social.studioasinc.shared.domain.model.LinkPreview

interface LinkPreviewRepository {
    suspend fun getLinkPreview(url: String): LinkPreview?
}
