package com.synapse.social.studioasinc.shared.domain.usecase.post

import com.synapse.social.studioasinc.shared.domain.usecase.auth.GetCurrentUserIdUseCase

class IsPostOwnerUseCase(
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) {
    operator fun invoke(postAuthorUid: String): Boolean {
        return getCurrentUserIdUseCase() == postAuthorUid
    }
}
