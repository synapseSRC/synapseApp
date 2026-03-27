package com.synapse.social.studioasinc.domain.repository

import com.synapse.social.studioasinc.domain.model.CreatePostRequest

interface ReelSubmissionHandler {
    suspend fun submitReel(request: CreatePostRequest, onProgress: (Float) -> Unit): Result<Unit>
}
