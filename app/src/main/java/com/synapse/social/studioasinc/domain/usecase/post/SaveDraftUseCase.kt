package com.synapse.social.studioasinc.domain.usecase.post

import com.synapse.social.studioasinc.shared.domain.model.Draft
import com.synapse.social.studioasinc.shared.domain.model.DraftType
import com.synapse.social.studioasinc.shared.domain.repository.DraftRepository
import javax.inject.Inject

class SaveDraftUseCase @Inject constructor(
    private val repository: DraftRepository
) {
    suspend operator fun invoke(id: String, type: DraftType, content: String) {
        val draft = Draft(
            id = id,
            type = type,
            content = content,
            updatedAt = System.currentTimeMillis()
        )
        repository.saveDraft(draft)
    }
}
