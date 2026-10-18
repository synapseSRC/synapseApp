package com.synapse.social.studioasinc.domain.usecase.about

import com.synapse.social.studioasinc.domain.model.FeedbackSubmission
import com.synapse.social.studioasinc.domain.repository.FeedbackRepository
import javax.inject.Inject

class SubmitFeedbackUseCase @Inject constructor(
    private val repository: FeedbackRepository
) {
    suspend operator fun invoke(submission: FeedbackSubmission): Result<Unit> =
        repository.submitFeedback(submission)
}
