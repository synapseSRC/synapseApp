package com.synapse.social.studioasinc.domain.usecase.post

import com.synapse.social.studioasinc.domain.repository.DraftRepository
import javax.inject.Inject

class SaveDraftUseCase @Inject constructor(
    private val repository: DraftRepository
) {
    operator fun invoke(text: String) = repository.saveDraftText(text)
}
