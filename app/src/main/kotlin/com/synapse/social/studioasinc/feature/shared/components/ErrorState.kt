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
            .padding(Spacing.space32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(Sizes.AvatarExtraLarge),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(Spacing.space16))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.space8))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(Spacing.space24))
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.space8))
                Text(stringResource(R.string.action_retry))
            }
        }
    }
}

