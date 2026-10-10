package com.synapse.social.studioasinc.feature.profile.lockprofile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
internal fun FollowRequestsSection(
    uiState: LockProfileUiState,
    onRefresh: () -> Unit,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Spacer(modifier = Modifier.height(Spacing.Large))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.follow_requests_title), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onRefresh, enabled = !uiState.isLoadingRequests) {
                Text(stringResource(R.string.follow_requests_refresh))
            }
        }
        if (uiState.isLoadingRequests) {
            CircularProgressIndicator()
        }
        uiState.requestsError?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        if (!uiState.isLoadingRequests && uiState.requestsError == null && uiState.followRequests.isEmpty()) {
            Text(stringResource(R.string.follow_requests_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        uiState.followRequests.forEach { request ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.ExtraSmall)) {
                Column(modifier = Modifier.fillMaxWidth().padding(Spacing.Medium)) {
                    Text(request.displayName?.takeIf { it.isNotBlank() } ?: request.username, style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.follow_request_requested_label, request.username), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { onReject(request.requesterId) }, enabled = uiState.respondingToRequestId == null) {
                            Text(stringResource(R.string.follow_request_reject))
                        }
                        TextButton(onClick = { onAccept(request.requesterId) }, enabled = uiState.respondingToRequestId == null) {
                            Text(stringResource(R.string.follow_request_accept))
                        }
                    }
                }
            }
        }
    }
}
