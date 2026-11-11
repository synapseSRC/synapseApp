package com.synapse.social.studioasinc.feature.auth.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.auth.ui.components.AuthButton
import com.synapse.social.studioasinc.feature.auth.ui.components.AuthErrorDialog
import com.synapse.social.studioasinc.feature.auth.ui.components.AuthOutlinedButton
import com.synapse.social.studioasinc.feature.auth.ui.components.AuthScreenLayout
import com.synapse.social.studioasinc.feature.auth.ui.components.AuthTextField
import com.synapse.social.studioasinc.feature.auth.ui.components.OAuthSection
import com.synapse.social.studioasinc.feature.auth.ui.components.PasswordStrengthIndicator
import com.synapse.social.studioasinc.feature.auth.ui.components.SignInHeroHeader
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthField
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthUiState
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.SynapseBrand

/**
 * Sign-in / sign-up surface.
 *
 * Both modes are still served by this one composable and by the same ViewModel contract: every
 * callback parameter, the validation map, the error dialog, the loading flag and the mode switch
 * behave exactly as before.
 *
 * The redesign is presentation only:
 * - the page is painted with the branded Synapse gradient (a deliberate choice for both modes, so
 *   the surface itself matches the welcome screen and does not flip when the user switches mode);
 * - the header switches between the branded [SignInHeroHeader] (sign-in only) and the unchanged
 *   plain header (sign-up), so no hero artwork is forced onto the sign-up form;
 * - the shared form controls - fields, primary action, divider, providers, passkey and the footer
 *   link - are styled once, so the two modes cannot drift apart.
 */
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
    onSignInWithPasskeyClick: () -> Unit,
    onDismissError: () -> Unit
) {
    val palette = SynapseBrand.palette
    val pageBackground = Brush.verticalGradient(
        colors = listOf(palette.pageTop, palette.pageMid, palette.pageBottom)
    )

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
                    SignInHeroHeader()
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
                onSignInWithPasskeyClick = onSignInWithPasskeyClick,
                onDismissError = onDismissError
            )
        },
        background = pageBackground
    )
}

/** Plain header used by sign-up mode; unchanged from the pre-redesign screen. */
@Composable
private fun AuthHeader(title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(Spacing.space8))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Leading icon of a form field; the tint follows the field's own state colours. */
@Composable
private fun AuthFieldIcon(imageVector: ImageVector) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        modifier = Modifier.size(Sizes.IconLarge)
    )
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
    onSignInWithPasskeyClick: () -> Unit,
    onDismissError: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val palette = SynapseBrand.palette

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

    generalError?.let {
        AuthErrorDialog(
            error = it,
            onDismiss = onDismissError
        )
    }

    AnimatedVisibility(visible = isSignUpMode) {
        Column {
            AuthTextField(
                value = username,
                onValueChange = onUsernameChanged,
                label = stringResource(R.string.username_label),
                error = validationErrors[AuthField.USERNAME],
                isValid = username.length >= 3 && validationErrors[AuthField.USERNAME] == null,
                isLoading = (state as? AuthUiState.SignUp)?.isCheckingUsername ?: false,
                leadingIcon = { AuthFieldIcon(Icons.Filled.Person) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                shape = MaterialTheme.shapes.medium,
                minHeight = Sizes.ButtonHeightM3Medium,
                focusedBorderColor = palette.accent
            )
        }
    }

    AuthTextField(
        value = email,
        onValueChange = onEmailChanged,
        label = stringResource(R.string.email_label),
        error = validationErrors[AuthField.EMAIL],
        isValid = isEmailValid,
        leadingIcon = { AuthFieldIcon(Icons.Filled.Email) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
        shape = MaterialTheme.shapes.medium,
        minHeight = Sizes.ButtonHeightM3Medium,
        focusedBorderColor = palette.accent
    )

    AuthTextField(
        value = password,
        onValueChange = onPasswordChanged,
        label = stringResource(R.string.password),
        error = validationErrors[AuthField.PASSWORD],
        isValid = password.isNotEmpty(),
        isPassword = true,
        leadingIcon = { AuthFieldIcon(Icons.Filled.Lock) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            focusManager.clearFocus()
            if (validationErrors.isNotEmpty()) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (isSignUpMode) onSignUpClick() else onSignInClick()
        }),
        shape = MaterialTheme.shapes.medium,
        minHeight = Sizes.ButtonHeightM3Medium,
        focusedBorderColor = palette.accent
    )

    if (isSignUpMode && password.isNotEmpty() && state is AuthUiState.SignUp) {
        PasswordStrengthIndicator(
            strength = state.passwordStrength,
            modifier = Modifier.padding(top = Spacing.space8)
        )
    }

    if (!isSignUpMode) {
        Spacer(modifier = Modifier.height(Spacing.space4))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            val forgotPasswordLabel = stringResource(R.string.forgot_password_link)
            Text(
                text = forgotPasswordLabel,
                style = MaterialTheme.typography.labelLarge,
                color = palette.accent,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .clickable(
                        onClickLabel = forgotPasswordLabel,
                        role = Role.Button
                    ) { onForgotPasswordClick() }
                    .padding(vertical = Spacing.space12)
            )
        }
    }

    Spacer(modifier = Modifier.height(Spacing.space16))

    AuthButton(
        text = if (isSignUpMode) stringResource(R.string.action_sign_up) else stringResource(R.string.action_sign_in),
        onClick = {
            focusManager.clearFocus()
            if (validationErrors.isNotEmpty()) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (isSignUpMode) onSignUpClick() else onSignInClick()
        },
        loading = isLoading,
        containerColor = palette.accent,
        contentColor = palette.onAccent,
        shape = MaterialTheme.shapes.medium,
        height = Sizes.ButtonHeightM3Medium,
        textStyle = MaterialTheme.typography.titleMedium,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = Sizes.ElevationLow,
            pressedElevation = Sizes.ElevationNone,
            hoveredElevation = Sizes.ElevationMedium,
            disabledElevation = Sizes.ElevationNone
        )
    )

    OAuthSection(
        onGoogleClick = { onOAuthClick("Google") },
        onAppleClick = { onOAuthClick("Apple") },
        onGitHubClick = { onOAuthClick("GitHub") }
    )

    if (!isSignUpMode) {
        Spacer(modifier = Modifier.height(Spacing.space12))

        AuthOutlinedButton(
            label = stringResource(R.string.action_sign_in_with_passkey),
            onClick = onSignInWithPasskeyClick,
            enabled = !isLoading,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Key,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(Sizes.IconLarge)
                )
            }
        )
    }

    Spacer(modifier = Modifier.height(Spacing.space24))

    val toggleActionLabel = if (isSignUpMode) {
        stringResource(R.string.action_sign_in)
    } else {
        stringResource(R.string.action_sign_up)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.space48)
            .clickable(
                onClickLabel = toggleActionLabel,
                role = Role.Button
            ) { onToggleModeClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isSignUpMode) {
                stringResource(R.string.already_have_account)
            } else {
                stringResource(R.string.dont_have_account)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // Explicit gap: the label strings carry their own leading/trailing spaces for other
        // locales, so the visual separation must not depend on them.
        Spacer(modifier = Modifier.width(Spacing.space4))
        Text(
            text = toggleActionLabel,
            style = MaterialTheme.typography.labelLarge,
            color = palette.accent
        )
    }
}
