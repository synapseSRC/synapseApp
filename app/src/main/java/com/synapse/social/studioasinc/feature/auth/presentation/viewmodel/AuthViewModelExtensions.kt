package com.synapse.social.studioasinc.feature.auth.presentation.viewmodel

import com.synapse.social.studioasinc.feature.auth.ui.models.AuthNavigationEvent
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthUiState
import com.synapse.social.studioasinc.shared.domain.usecase.auth.IsEmailVerifiedUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.auth.RefreshSessionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

fun CoroutineScope.startResendCooldownExt(
    uiState: MutableStateFlow<AuthUiState>,
    resendCooldownSeconds: Int,
    cooldownJob: Job?,
    onJobCreated: (Job) -> Unit
) {
    cooldownJob?.cancel()
    val state = uiState.value as? AuthUiState.EmailVerification ?: return

    val job = launch {
        for (seconds in resendCooldownSeconds downTo 1) {
            val current = uiState.value as? AuthUiState.EmailVerification ?: break
            uiState.value = current.copy(canResend = false, resendCooldownSeconds = seconds)
            delay(1000)
        }
        val finalState = uiState.value as? AuthUiState.EmailVerification
        if (finalState != null) {
            uiState.value = finalState.copy(canResend = true, resendCooldownSeconds = 0, isResent = false)
        }
    }
    onJobCreated(job)
}

suspend fun CoroutineScope.checkEmailVerificationExt(
    uiState: MutableStateFlow<AuthUiState>,
    refreshSessionUseCase: RefreshSessionUseCase,
    isEmailVerifiedUseCase: IsEmailVerifiedUseCase,
    navigationEvent: MutableSharedFlow<AuthNavigationEvent>
) {
    val pollInterval = 3000L
    while (uiState.value is AuthUiState.EmailVerification) {
        delay(pollInterval)
        if (uiState.value !is AuthUiState.EmailVerification) break

        refreshSessionUseCase().onSuccess {
            if (isEmailVerifiedUseCase()) {
                uiState.value = AuthUiState.Success("Email verified successfully")
                delay(1000)
                navigationEvent.emit(AuthNavigationEvent.NavigateToMain)
            }
        }
    }
}
