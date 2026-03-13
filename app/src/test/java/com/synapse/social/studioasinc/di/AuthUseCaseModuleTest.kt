package com.synapse.social.studioasinc.di

import com.synapse.social.studioasinc.shared.domain.usecase.auth.ValidatePasswordUseCase
import org.junit.Assert.assertNotNull
import org.junit.Test

class AuthUseCaseModuleTest {

    @Test
    fun testProvideValidateNewPasswordUseCase() {
        val validatePasswordUseCase = ValidatePasswordUseCase()
        val useCase = AuthUseCaseModule.provideValidateNewPasswordUseCase(validatePasswordUseCase)
        assertNotNull(useCase)
    }
}
