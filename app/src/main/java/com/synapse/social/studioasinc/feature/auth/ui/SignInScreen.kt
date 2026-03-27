package com.synapse.social.studioasinc.feature.auth.ui

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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.auth.ui.components.AuthButton
import com.synapse.social.studioasinc.feature.auth.ui.components.AuthScreenLayout
import com.synapse.social.studioasinc.feature.auth.ui.components.AuthTextField
import com.synapse.social.studioasinc.feature.auth.ui.components.ErrorCard
import com.synapse.social.studioasinc.feature.auth.ui.components.OAuthSection
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthUiState

@Composable
fun SignInScreen(
    state: AuthUiState.SignIn,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSignInClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onToggleModeClick: () -> Unit,
    onOAuthClick: (String) -> Unit
) {
    AuthScreenLayout(
        header = { SignInHeader() },
        form = {
            SignInForm(
                state = state,
                onEmailChanged = onEmailChanged,
                onPasswordChanged = onPasswordChanged,
                onSignInClick = onSignInClick,
                onForgotPasswordClick = onForgotPasswordClick,
                onToggleModeClick = onToggleModeClick,
                onOAuthClick = onOAuthClick
            )
        }
    )
}

@Composable
private fun SignInHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Welcome Back",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(Spacing.Small))
        Text(
            text = "Sign in to continue",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SignInForm(
    state: AuthUiState.SignIn,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSignInClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onToggleModeClick: () -> Unit,
    onOAuthClick: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current

    AuthTextField(
        value = state.email,
        onValueChange = onEmailChanged,
        label = "Email",
        error = state.emailError,
        isValid = state.isEmailValid,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
    )

    AuthTextField(
        value = state.password,
        onValueChange = onPasswordChanged,
        label = "Password",
        error = state.passwordError,
        isValid = false,
        isPassword = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            focusManager.clearFocus()
            onSignInClick()
        })
    )

    Spacer(modifier = Modifier.height(Spacing.Small))

    Text(
        text = "Forgot Password?",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onForgotPasswordClick() }
            .padding(vertical = Spacing.ExtraSmall),
        textAlign = TextAlign.End
    )

    Spacer(modifier = Modifier.height(Spacing.Medium))

    state.generalError?.let { errorMsg ->
        ErrorCard(error = errorMsg)
        Spacer(modifier = Modifier.height(Spacing.Medium))
    }

    Spacer(modifier = Modifier.height(Spacing.Small))

    AuthButton(
        text = "Sign In",
        onClick = {
            focusManager.clearFocus()
            onSignInClick()
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
        Text(text = "Don't have an account? ", style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "Sign Up",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onToggleModeClick() }
        )
    }
}
