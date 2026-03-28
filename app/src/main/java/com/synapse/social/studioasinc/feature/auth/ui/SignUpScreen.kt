package com.synapse.social.studioasinc.feature.auth.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.auth.ui.components.AuthButton
import com.synapse.social.studioasinc.feature.auth.ui.components.AuthScreenLayout
import com.synapse.social.studioasinc.feature.auth.ui.components.AuthTextField
import com.synapse.social.studioasinc.feature.auth.ui.components.ErrorCard
import com.synapse.social.studioasinc.feature.auth.ui.components.OAuthSection
import com.synapse.social.studioasinc.feature.auth.ui.components.PasswordStrengthIndicator
import com.synapse.social.studioasinc.feature.auth.ui.components.UserCreatedDialog
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthUiState

@Composable
fun SignUpScreen(
    state: AuthUiState.SignUp,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onSignUpClick: () -> Unit,
    onToggleModeClick: () -> Unit,
    onOAuthClick: (String) -> Unit,
    onDismissSuccessDialog: () -> Unit
) {
    if (state.showSuccessDialog) {
        UserCreatedDialog(onDismiss = onDismissSuccessDialog)
    }

    AuthScreenLayout(
        header = { SignUpHeader() },
        form = {
            SignUpForm(
                state = state,
                onEmailChanged = onEmailChanged,
                onPasswordChanged = onPasswordChanged,
                onUsernameChanged = onUsernameChanged,
                onSignUpClick = onSignUpClick,
                onToggleModeClick = onToggleModeClick,
                onOAuthClick = onOAuthClick
            )
        }
    )
}

@Composable
private fun SignUpHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.sign_up_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(Spacing.Small))
        Text(
            text = stringResource(R.string.sign_up_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SignUpForm(
    state: AuthUiState.SignUp,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onSignUpClick: () -> Unit,
    onToggleModeClick: () -> Unit,
    onOAuthClick: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current

    AuthTextField(
        value = state.username,
        onValueChange = onUsernameChanged,
        label = stringResource(R.string.username_label),
        error = state.usernameError,
        isValid = state.username.length >= 3 && state.usernameError == null,
        isLoading = state.isCheckingUsername,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
    )

    AuthTextField(
        value = state.email,
        onValueChange = onEmailChanged,
        label = stringResource(R.string.email_label),
        error = state.emailError,
        isValid = state.isEmailValid,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
    )

    AuthTextField(
        value = state.password,
        onValueChange = onPasswordChanged,
        label = stringResource(R.string.password),
        error = state.passwordError,
        isValid = false,
        isPassword = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            focusManager.clearFocus()
            onSignUpClick()
        })
    )

    if (state.password.isNotEmpty()) {
        PasswordStrengthIndicator(
            strength = state.passwordStrength,
            modifier = Modifier.padding(top = Spacing.Small)
        )
    }

    Spacer(modifier = Modifier.height(Spacing.Medium))

    state.generalError?.let { errorMsg ->
        ErrorCard(error = errorMsg)
        Spacer(modifier = Modifier.height(Spacing.Medium))
    }

    Spacer(modifier = Modifier.height(Spacing.Small))

    AuthButton(
        text = stringResource(R.string.action_sign_up),
        onClick = {
            focusManager.clearFocus()
            onSignUpClick()
        },
        loading = state.isLoading
    )

    OAuthSection(
        onGoogleClick = { onOAuthClick("Google") },
        onAppleClick = { onOAuthClick("Apple") },
        onGitHubClick = { onOAuthClick("GitHub") }
    )

    Spacer(modifier = Modifier.height(Spacing.Large))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = stringResource(R.string.already_have_account), style = MaterialTheme.typography.bodyMedium)
        Text(
            text = stringResource(R.string.action_sign_in),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onToggleModeClick() }
        )
    }
}
