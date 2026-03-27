package com.synapse.social.studioasinc.shared.domain.usecase.auth

import com.synapse.social.studioasinc.shared.domain.model.ValidationResult
import kotlin.test.Test
import kotlin.test.assertEquals

class ValidateUsernameUseCaseTest {

    private val validateUsername = ValidateUsernameUseCase()

    @Test
    fun testEmptyUsername() {
        val result = validateUsername("")
        assertEquals(ValidationResult.Invalid("Username cannot be empty"), result)
    }

    @Test
    fun testBlankUsername() {
        val result = validateUsername("   ")
        assertEquals(ValidationResult.Invalid("Username cannot be empty"), result)
    }

    @Test
    fun testShortUsername() {
        val result = validateUsername("ab")
        assertEquals(ValidationResult.Invalid("Username must be at least 3 characters"), result)
    }

    @Test
    fun testLongUsername() {
        val result = validateUsername("abcdefghijklmnopqrstuvwxyz")
        assertEquals(ValidationResult.Invalid("Username must be at most 20 characters"), result)
    }

    @Test
    fun testInvalidCharacters() {
        val result = validateUsername("user@name")
        assertEquals(ValidationResult.Invalid("Username can only contain letters, numbers, and underscores"), result)
    }

    @Test
    fun testInvalidSpaces() {
        val result = validateUsername("user name")
        assertEquals(ValidationResult.Invalid("Username can only contain letters, numbers, and underscores"), result)
    }

    @Test
    fun testValidUsernameWithLetters() {
        val result = validateUsername("username")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun testValidUsernameWithLettersNumbersAndUnderscores() {
        val result = validateUsername("user_name_123")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun testValidUsernameExactMinLength() {
        val result = validateUsername("abc")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun testValidUsernameExactMaxLength() {
        val result = validateUsername("abcdefghijklmnopqrst")
        assertEquals(ValidationResult.Valid, result)
    }
}
