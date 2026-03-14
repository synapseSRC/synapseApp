package com.synapse.social.studioasinc.domain.usecase.post

import com.synapse.social.studioasinc.shared.domain.model.DraftType
import com.synapse.social.studioasinc.shared.domain.repository.DraftRepository
import javax.inject.Inject

class ClearDraftUseCase @Inject constructor(
    private val repository: DraftRepository
) {
    suspend operator fun invoke(type: DraftType) = repository.deleteDraftByType(type)
}
