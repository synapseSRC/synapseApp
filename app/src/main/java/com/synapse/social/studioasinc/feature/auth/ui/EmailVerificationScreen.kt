package com.synapse.social.studioasinc.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.auth.ui.components.AuthButton
import com.synapse.social.studioasinc.feature.auth.ui.models.AuthUiState

@Composable
fun EmailVerificationScreen(
    state: AuthUiState.EmailVerification,
    onResendClick: () -> Unit,
    onBackToSignInClick: () -> Unit
) {
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
            Icon(
                imageVector = Icons.Filled.Email,
                contentDescription = null,
                modifier = Modifier.size(Sizes.AvatarSemiLarge),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(Spacing.Large))

            Text(
                text = stringResource(R.string.email_verification_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Spacing.Medium))

            Text(
                text = stringResource(R.string.email_verification_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = state.email,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(Spacing.ExtraLarge))

            if (state.isResent) {
                Text(
                    text = stringResource(R.string.email_verification_resent),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = Spacing.Medium)
                )
            }

            if (state.canResend) {
                AuthButton(
                    text = stringResource(R.string.resend_email),
                    onClick = onResendClick
                )
            } else {

                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    androidx.compose.material3.CircularProgressIndicator(
                        progress = { state.resendCooldownSeconds / 60f },
                        modifier = Modifier.size(Sizes.IconGiant),
                    )
                    Spacer(modifier = Modifier.height(Spacing.Small))
                    Text(
                        text = stringResource(R.string.email_verification_resend_wait, state.resendCooldownSeconds),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.Large))

            OutlinedButton(
                onClick = onBackToSignInClick
            ) {
                Text(stringResource(R.string.forgot_password_back))
            }
        }
    }
}
