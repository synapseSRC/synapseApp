package com.synapse.social.studioasinc.domain.repository

import com.synapse.social.studioasinc.domain.model.FeedbackSubmission

interface FeedbackRepository {
    suspend fun submitFeedback(submission: FeedbackSubmission): Result<Unit>
}
