package com.synapse.social.studioasinc.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.auth.ui.components.AuthButton
import com.synapse.social.studioasinc.feature.auth.ui.components.AuthTextField
import com.synapse.social.studioasinc.feature.auth.ui.components.ErrorCard
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthUiState

@Composable
fun ForgotPasswordScreen(
    state: AuthUiState.ForgotPassword,
    onEmailChanged: (String) -> Unit,
    onSendResetLinkClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Spacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.forgot_password_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(Spacing.Small))

            Text(
                text = stringResource(R.string.forgot_password_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.ExtraLarge))

            if (state.isEmailSent) {

                Text(
                    text = stringResource(R.string.forgot_password_sent_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.forgot_password_sent_message, state.email),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Spacing.Large))
                AuthButton(
                    text = stringResource(R.string.forgot_password_back),
                    onClick = onBackClick
                )
            } else {

                ErrorCard(
                    error = state.emailError
                )

                AuthTextField(
                    value = state.email,
                    onValueChange = onEmailChanged,
                    label = stringResource(R.string.email),
                    error = state.emailError,
                    isValid = state.isEmailValid,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onSendResetLinkClick()
                        }
                    )
                )

                Spacer(modifier = Modifier.height(Spacing.Large))

                AuthButton(
                    text = stringResource(R.string.forgot_password_continue),
                    onClick = {
                        focusManager.clearFocus()
                        onSendResetLinkClick()
                    }
                )

                Spacer(modifier = Modifier.height(Spacing.Medium))

                OutlinedButton(
                    onClick = onBackClick
                ) {
                    Text(stringResource(R.string.forgot_password_back))
                }
            }
        }
    }
}
