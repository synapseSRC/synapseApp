package com.synapse.social.studioasinc.domain.usecase.post

import com.synapse.social.studioasinc.domain.repository.DraftRepository
import javax.inject.Inject

class GetDraftUseCase @Inject constructor(
    private val repository: DraftRepository
) {
    operator fun invoke(): String? = repository.getDraftText()
}
