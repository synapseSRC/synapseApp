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
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.Sizes

@Composable
fun OAuthSection(
    onGoogleClick: () -> Unit,
    onAppleClick: () -> Unit,
    onGitHubClick: () -> Unit
) {
    Spacer(modifier = Modifier.height(Spacing.Large))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            modifier = Modifier
                .weight(1f)
                .height(Sizes.BorderThin)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        Text(
            text = stringResource(id = R.string.or_continue_with),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = Spacing.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(
            modifier = Modifier
                .weight(1f)
                .height(Sizes.BorderThin)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }

    Spacer(modifier = Modifier.height(Spacing.Large))

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.SmallMedium)) {
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
