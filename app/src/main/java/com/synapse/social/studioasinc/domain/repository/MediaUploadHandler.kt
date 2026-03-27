package com.synapse.social.studioasinc.domain.repository

import com.synapse.social.studioasinc.domain.model.MediaItem

interface MediaUploadHandler {
    suspend fun uploadMedia(items: List<MediaItem>, onProgress: (Float) -> Unit): List<MediaItem>
}
