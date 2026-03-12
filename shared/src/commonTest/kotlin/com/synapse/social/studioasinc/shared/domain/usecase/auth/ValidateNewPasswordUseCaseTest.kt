package com.synapse.social.studioasinc.shared.domain.usecase.auth

import com.synapse.social.studioasinc.shared.domain.model.ValidationResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidateNewPasswordUseCaseTest {

    private val validatePasswordUseCase = ValidatePasswordUseCase()
    private val validateNewPassword = ValidateNewPasswordUseCase(validatePasswordUseCase)

    @Test
    fun testValidPasswordAndMatch() {
        val result = validateNewPassword("password123", "password123")
        assertTrue(result.isValid)
        assertEquals(null, result.passwordError)
        assertEquals(null, result.confirmPasswordError)
    }

    @Test
    fun testPasswordMismatch() {
        val result = validateNewPassword("password123", "different123")
        assertFalse(result.isValid)
        assertEquals(null, result.passwordError)
        assertEquals("Passwords do not match", result.confirmPasswordError)
    }

    @Test
    fun testInvalidPassword() {
        val result = validateNewPassword("short", "short")
        assertFalse(result.isValid)
        assertEquals("Password must be at least 8 characters", result.passwordError)
        assertEquals(null, result.confirmPasswordError)
    }

    @Test
    fun testInvalidPasswordAndMismatch() {
        val result = validateNewPassword("short", "other")
        assertFalse(result.isValid)
        assertEquals("Password must be at least 8 characters", result.passwordError)
        assertEquals("Passwords do not match", result.confirmPasswordError)
    }
}
