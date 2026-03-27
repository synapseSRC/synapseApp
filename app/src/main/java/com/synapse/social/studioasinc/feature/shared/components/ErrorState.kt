package com.synapse.social.studioasinc.ui.components

import androidx.compose.foundation.layout.*
import com.synapse.social.studioasinc.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
fun ErrorState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.ExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(Sizes.AvatarExtraLarge),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(Spacing.Medium))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.Small))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(Spacing.Large))
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.Small))
                Text(stringResource(R.string.action_retry))
            }
        }
    }
}

@Composable
fun NetworkErrorState(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit
) {
    ErrorState(
        title = "Connection Error",
        message = "Unable to connect to the server. Please check your internet connection and try again.",
        modifier = modifier,
        onRetry = onRetry
    )
}

@Composable
fun LoadErrorState(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit
) {
    ErrorState(
        title = "Failed to Load",
        message = "Something went wrong while loading the profile. Please try again.",
        modifier = modifier,
        onRetry = onRetry
    )
}
