package com.synapse.social.studioasinc.core.auth

import com.synapse.social.studioasinc.shared.data.repository.SupabaseAuthRepository
import com.synapse.social.studioasinc.shared.domain.usecase.auth.GetCurrentUserIdUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.auth.GetCurrentUserEmailUseCase

object AuthHelper {
    private val authRepository by lazy { SupabaseAuthRepository() }
    
    private val getCurrentUserIdUseCase by lazy {
        GetCurrentUserIdUseCase(authRepository)
    }
    
    private val getCurrentUserEmailUseCase by lazy {
        GetCurrentUserEmailUseCase(authRepository)
    }
    
    fun getCurrentUserId(): String? = getCurrentUserIdUseCase()
    
    fun getCurrentUserEmail(): String? = getCurrentUserEmailUseCase()
}
