package com.synapse.social.studioasinc.feature.auth.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.auth.ui.components.AuthButton
import com.synapse.social.studioasinc.feature.auth.ui.components.AuthScreenLayout
import com.synapse.social.studioasinc.feature.auth.ui.components.AuthTextField
import com.synapse.social.studioasinc.feature.auth.ui.components.ErrorCard
import com.synapse.social.studioasinc.feature.auth.ui.components.OAuthSection
import com.synapse.social.studioasinc.feature.auth.ui.components.PasswordStrengthIndicator
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthField
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthUiState
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun SignInSignUpScreenContent(
    isSignUpMode: Boolean,
    state: AuthUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onSignInClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onToggleModeClick: () -> Unit,
    onOAuthClick: (String) -> Unit,
    onDismissError: () -> Unit
) {
    AuthScreenLayout(
        header = {
            AnimatedContent(
                targetState = isSignUpMode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "AuthHeaderAnimation"
            ) { targetIsSignUp ->
                if (targetIsSignUp) {
                    AuthHeader(
                        title = stringResource(R.string.sign_up_title),
                        subtitle = stringResource(R.string.sign_up_subtitle)
                    )
                } else {
                    AuthHeader(
                        title = stringResource(R.string.sign_in_title),
                        subtitle = stringResource(R.string.sign_in_subtitle)
                    )
                }
            }
        },
        form = {
            UnifiedAuthForm(
                isSignUpMode = isSignUpMode,
                state = state,
                onEmailChanged = onEmailChanged,
                onPasswordChanged = onPasswordChanged,
                onUsernameChanged = onUsernameChanged,
                onSignInClick = onSignInClick,
                onSignUpClick = onSignUpClick,
                onForgotPasswordClick = onForgotPasswordClick,
                onToggleModeClick = onToggleModeClick,
                onOAuthClick = onOAuthClick,
                onDismissError = onDismissError
            )
        }
    )
}

@Composable
private fun AuthHeader(title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(Spacing.Small))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun UnifiedAuthForm(
    isSignUpMode: Boolean,
    state: AuthUiState,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onSignInClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onToggleModeClick: () -> Unit,
    onOAuthClick: (String) -> Unit,
    onDismissError: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current

    val generalError = when (state) {
        is AuthUiState.SignIn -> if (state.isErrorDismissed) null else state.generalError
        is AuthUiState.SignUp -> if (state.isErrorDismissed) null else state.generalError
        else -> null
    }

    val email = when (state) {
        is AuthUiState.SignIn -> state.email
        is AuthUiState.SignUp -> state.email
        else -> ""
    }

    val password = when (state) {
        is AuthUiState.SignIn -> state.password
        is AuthUiState.SignUp -> state.password
        else -> ""
    }

    val username = (state as? AuthUiState.SignUp)?.username ?: ""

    val validationErrors = when (state) {
        is AuthUiState.SignIn -> state.validationErrors
        is AuthUiState.SignUp -> state.validationErrors
        else -> emptyMap()
    }

    val isEmailValid = when (state) {
        is AuthUiState.SignIn -> state.isEmailValid
        is AuthUiState.SignUp -> state.isEmailValid
        else -> false
    }

    val isLoading = when (state) {
        is AuthUiState.SignIn -> state.isLoading
        is AuthUiState.SignUp -> state.isLoading
        else -> false
    }

    ErrorCard(
        error = generalError,
        onDismiss = onDismissError
    )

    AnimatedVisibility(visible = isSignUpMode) {
        Column {
            AuthTextField(
                value = username,
                onValueChange = onUsernameChanged,
                label = stringResource(R.string.username_label),
                error = validationErrors[AuthField.USERNAME],
                isValid = username.length >= 3 && validationErrors[AuthField.USERNAME] == null,
                isLoading = (state as? AuthUiState.SignUp)?.isCheckingUsername ?: false,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )
            Spacer(modifier = Modifier.height(Spacing.Small))
        }
    }

    AuthTextField(
        value = email,
        onValueChange = onEmailChanged,
        label = stringResource(R.string.email_label),
        error = validationErrors[AuthField.EMAIL],
        isValid = isEmailValid,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
    )

    AuthTextField(
        value = password,
        onValueChange = onPasswordChanged,
        label = stringResource(R.string.password),
        error = validationErrors[AuthField.PASSWORD],
        isValid = false,
        isPassword = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            focusManager.clearFocus()
            if (validationErrors.isNotEmpty()) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (isSignUpMode) onSignUpClick() else onSignInClick()
        })
    )

    if (isSignUpMode && password.isNotEmpty() && state is AuthUiState.SignUp) {
        PasswordStrengthIndicator(
            strength = state.passwordStrength,
            modifier = Modifier.padding(top = Spacing.Small)
        )
    }

    if (!isSignUpMode) {
        Spacer(modifier = Modifier.height(Spacing.Small))

        Text(
            text = stringResource(R.string.forgot_password_link),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onForgotPasswordClick() }
                .padding(vertical = Spacing.ExtraSmall),
            textAlign = TextAlign.End
        )
    }

    Spacer(modifier = Modifier.height(Spacing.Medium))

    AuthButton(
        text = if (isSignUpMode) stringResource(R.string.action_sign_up) else stringResource(R.string.action_sign_in),
        onClick = {
            focusManager.clearFocus()
            if (validationErrors.isNotEmpty()) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (isSignUpMode) onSignUpClick() else onSignInClick()
        },
        loading = isLoading
    )

    OAuthSection(
        onGoogleClick = { onOAuthClick("Google") },
        onAppleClick = { onOAuthClick("Apple") },
        onGitHubClick = { onOAuthClick("GitHub") }
    )

    Spacer(modifier = Modifier.height(Spacing.Large))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = if (isSignUpMode) stringResource(R.string.already_have_account) else stringResource(R.string.dont_have_account),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = if (isSignUpMode) " ${stringResource(R.string.action_sign_in)}" else " ${stringResource(R.string.action_sign_up)}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onToggleModeClick() }
        )
    }
}
