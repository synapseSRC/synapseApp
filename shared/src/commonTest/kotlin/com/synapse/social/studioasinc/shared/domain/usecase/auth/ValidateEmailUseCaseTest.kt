package com.synapse.social.studioasinc.shared.domain.usecase.auth

import com.synapse.social.studioasinc.shared.domain.model.ValidationResult
import kotlin.test.Test
import kotlin.test.assertEquals

class ValidateEmailUseCaseTest {

    private val validateEmail = ValidateEmailUseCase()

    @Test
    fun testEmptyEmail() {
        val result = validateEmail("")
        assertEquals(ValidationResult.Invalid("Email cannot be empty"), result)
    }

    @Test
    fun testBlankEmail() {
        val result = validateEmail("   ")
        assertEquals(ValidationResult.Invalid("Email cannot be empty"), result)
    }

    @Test
    fun testEmailMissingAtSymbol() {
        val result = validateEmail("testexample.com")
        assertEquals(ValidationResult.Invalid("Invalid email format"), result)
    }

    @Test
    fun testEmailMissingLocalPart() {
        val result = validateEmail("@example.com")
        assertEquals(ValidationResult.Invalid("Invalid email format"), result)
    }

    @Test
    fun testEmailMissingDomainPart() {
        val result = validateEmail("test@")
        assertEquals(ValidationResult.Invalid("Invalid email format"), result)
    }

    @Test
    fun testEmailMissingTLD() {
        val result = validateEmail("test@example")
        assertEquals(ValidationResult.Invalid("Invalid email format"), result)
    }

    @Test
    fun testValidStandardEmail() {
        val result = validateEmail("test@example.com")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun testValidEmailWithSubdomains() {
        val result = validateEmail("test@mail.example.com")
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun testValidEmailWithSpecialCharacters() {
        val result = validateEmail("test.name+suffix-123_456@example.com")
        assertEquals(ValidationResult.Valid, result)
    }
}
