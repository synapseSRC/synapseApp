package com.synapse.social.studioasinc.feature.auth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

/**
 * "Or continue with" divider plus the provider buttons.
 *
 * The provider list is unchanged (Google, Apple, GitHub, in that order, with the same callbacks).
 * The divider is now a hairline pair with a tighter label style and the block sits closer to the
 * primary action, so the alternative methods read as one secondary group.
 */
@Composable
fun OAuthSection(
    onGoogleClick: () -> Unit,
    onAppleClick: () -> Unit,
    onGitHubClick: () -> Unit
) {
    Spacer(modifier = Modifier.height(Spacing.space24))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DividerLine(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(id = R.string.or_continue_with),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.space12)
        )
        DividerLine(modifier = Modifier.weight(1f))
    }

    Spacer(modifier = Modifier.height(Spacing.space16))

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space12)) {
        OAuthButton(
            provider = "Google",
            onClick = onGoogleClick
        )
        OAuthButton(
            provider = "Apple",
            onClick = onAppleClick
        )
        OAuthButton(
            provider = "GitHub",
            onClick = onGitHubClick
        )
    }
}

@Composable
private fun DividerLine(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .height(Sizes.BorderThin)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}
