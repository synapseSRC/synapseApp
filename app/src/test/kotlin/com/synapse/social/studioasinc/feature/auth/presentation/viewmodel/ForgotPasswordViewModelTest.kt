package com.synapse.social.studioasinc.feature.auth.presentation.viewmodel

import com.synapse.social.studioasinc.feature.auth.ui.models.AuthField
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthNavigationEvent
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthUiState
import com.synapse.social.studioasinc.shared.domain.model.ValidationResult
import com.synapse.social.studioasinc.shared.domain.usecase.auth.SendPasswordResetUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.auth.ValidateEmailUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.auth.ValidateResetPasswordEmailUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val validateEmailUseCase: ValidateEmailUseCase = mock()
    private val validateResetPasswordEmailUseCase: ValidateResetPasswordEmailUseCase = mock()
    private val sendPasswordResetUseCase: SendPasswordResetUseCase = mock()

    private lateinit var viewModel: ForgotPasswordViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ForgotPasswordViewModel(
            validateEmailUseCase,
            validateResetPasswordEmailUseCase,
            sendPasswordResetUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onSubmitNewPassword with invalid email updates validationErrors`() = runTest {
        val testEmail = "invalid-email"
        whenever(validateEmailUseCase(testEmail)).thenReturn(ValidationResult.Invalid("Invalid email"))
        whenever(validateResetPasswordEmailUseCase(testEmail)).thenReturn(
            ValidateResetPasswordEmailUseCase.ValidationResultGroup(isValid = false, emailError = "Invalid email format")
        )

        viewModel.onEmailChanged(testEmail)
        viewModel.onSubmitNewPassword()

        val state = viewModel.uiState.value as AuthUiState.ForgotPassword
        assertEquals("Invalid email format", state.validationErrors[AuthField.EMAIL])
        assertFalse(state.isEmailSent)
    }

    @Test
    fun `onSubmitNewPassword with valid email successfully sets isEmailSent`() = runTest {
        val testEmail = "test@example.com"
        whenever(validateEmailUseCase(testEmail)).thenReturn(ValidationResult.Valid)
        whenever(validateResetPasswordEmailUseCase(testEmail)).thenReturn(
            ValidateResetPasswordEmailUseCase.ValidationResultGroup(isValid = true, emailError = null)
        )
        whenever(sendPasswordResetUseCase(testEmail)).thenReturn(Result.success(Unit))

        viewModel.onEmailChanged(testEmail)
        viewModel.onSubmitNewPassword()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as AuthUiState.ForgotPassword
        assertTrue(state.isEmailSent)
        assertNull(state.generalError)
    }

    @Test
    fun `onSubmitNewPassword on backend failure updates generalError`() = runTest {
        val testEmail = "test@example.com"
        whenever(validateEmailUseCase(testEmail)).thenReturn(ValidationResult.Valid)
        whenever(validateResetPasswordEmailUseCase(testEmail)).thenReturn(
            ValidateResetPasswordEmailUseCase.ValidationResultGroup(isValid = true, emailError = null)
        )
        whenever(sendPasswordResetUseCase(testEmail)).thenReturn(Result.failure(Exception("Network error")))

        viewModel.onEmailChanged(testEmail)
        viewModel.onSubmitNewPassword()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as AuthUiState.ForgotPassword
        assertFalse(state.isEmailSent)
        assertEquals("Network error", state.generalError)
    }
}
