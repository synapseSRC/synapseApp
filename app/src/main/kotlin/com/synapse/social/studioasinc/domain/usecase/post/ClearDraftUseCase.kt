package com.synapse.social.studioasinc.domain.usecase.post

import com.synapse.social.studioasinc.domain.repository.DraftRepository
import javax.inject.Inject

class ClearDraftUseCase @Inject constructor(
    private val repository: DraftRepository
) {
    operator fun invoke() = repository.clearDraft()
}
