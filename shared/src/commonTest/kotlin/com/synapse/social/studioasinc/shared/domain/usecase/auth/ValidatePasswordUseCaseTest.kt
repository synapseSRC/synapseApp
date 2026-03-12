package com.synapse.social.studioasinc.shared.domain.usecase.auth

import com.synapse.social.studioasinc.shared.domain.model.ValidationResult
import kotlin.test.Test
import kotlin.test.assertEquals

class ValidatePasswordUseCaseTest {

    private val validatePassword = ValidatePasswordUseCase()

    @Test
    fun testPasswordTooShort() {
        val result = validatePassword("1234567")
        assertEquals(ValidationResult.Invalid("Password must be at least 8 characters"), result)
    }

    @Test
    fun testPasswordExactlyMinimumLength() {
        val result = validatePassword("12345678")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun testPasswordLongerThanMinimum() {
        val result = validatePassword("password123")
        assertEquals(ValidationResult.Valid, result)
    }
}
