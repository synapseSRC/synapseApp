package com.synapse.social.studioasinc.feature.inbox.inbox.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.call.CallLaunchRequest
import com.synapse.social.studioasinc.feature.call.toCallAgainRequest
import com.synapse.social.studioasinc.feature.inbox.inbox.CallsViewModel
import com.synapse.social.studioasinc.feature.inbox.inbox.components.CallRecordItem
import com.synapse.social.studioasinc.feature.inbox.inbox.components.InboxEmptyState
import com.synapse.social.studioasinc.feature.inbox.inbox.models.EmptyStateType
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

/**
 * The Calls tab.
 *
 * Refreshes whenever it becomes visible rather than on construction: the ViewModel outlives a
 * navigation into a chat or a call, and coming back to this tab is exactly when the history the user
 * is looking at may have changed.
 */
@Composable
fun CallsTabScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    debugToolsEnabled: Boolean = false,
    onStartCall: (CallLaunchRequest) -> Unit = {},
    viewModel: CallsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val loadFailedMessage = stringResource(R.string.call_history_load_failed)

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    when {
        // A read that failed with nothing to fall back on: the retry is the only useful action.
        uiState.history.isEmpty() && uiState.error != null -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                InboxEmptyState(
                    type = EmptyStateType.ERROR,
                    message = loadFailedMessage,
                    onActionClick = viewModel::retry,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Covers both the first read and a retry after a failure: until something has landed, a
        // spinner is honest and "no calls yet" is not.
        uiState.history.isEmpty() && (uiState.isLoading || !uiState.hasLoaded) -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        uiState.history.isEmpty() -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                InboxEmptyState(
                    type = EmptyStateType.CALLS,
                    modifier = Modifier.fillMaxSize()
                )
                if (debugToolsEnabled) {
                    DebugCallLaunchCard(
                        onStartCall = onStartCall,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(Spacing.space16)
                    )
                }
            }
        }

        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = contentPadding
            ) {
                // A failed refresh keeps the list the user already had; the row is the only
                // indication that it may be behind, and the only way to ask again.
                if (uiState.error != null) {
                    item(key = "history-refresh-failed") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.space16, vertical = Spacing.space4),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = loadFailedMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = viewModel::retry) {
                                Text(stringResource(R.string.action_retry))
                            }
                        }
                    }
                }
                items(uiState.history, key = { it.id }) { session ->
                    CallRecordItem(
                        session = session,
                        onCallClick = { onStartCall(it.toCallAgainRequest()) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DebugCallLaunchCard(
    onStartCall: (CallLaunchRequest) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val callerName = stringResource(R.string.call_debug_sample_caller)
    val mockAvatarUrl = remember(context) { context.mockCallerAvatarUrl() }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.padding(Spacing.space16),
            verticalArrangement = Arrangement.spacedBy(Spacing.space12)
        ) {
            Text(
                text = stringResource(R.string.call_debug_launch_title),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(R.string.call_debug_launch_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space8)) {
                OutlinedButton(
                    onClick = {
                        onStartCall(
                            CallLaunchRequest(
                                participantId = MOCK_DEBUG_CALLER_ID,
                                participantName = callerName,
                                participantAvatarUrl = mockAvatarUrl,
                                isVideo = false,
                                isIncoming = true
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        modifier = Modifier.size(Sizes.IconMedium)
                    )
                    Spacer(Modifier.width(Spacing.space8))
                    Text(
                        text = stringResource(R.string.call_debug_launch_incoming_voice),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 2
                    )
                }
                OutlinedButton(
                    onClick = {
                        onStartCall(
                            CallLaunchRequest(
                                participantId = MOCK_DEBUG_CALLER_ID,
                                participantName = callerName,
                                participantAvatarUrl = mockAvatarUrl,
                                isVideo = true,
                                isIncoming = true
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        modifier = Modifier.size(Sizes.IconMedium)
                    )
                    Spacer(Modifier.width(Spacing.space8))
                    Text(
                        text = stringResource(R.string.call_debug_launch_incoming_video),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

// Coil's resource fetcher resolves only the numeric form: android.resource://<package>/<resId>.
private fun Context.mockCallerAvatarUrl(): String =
    "android.resource://$packageName/${R.drawable.call_mock_caller_avatar}"

private const val MOCK_DEBUG_CALLER_ID = "debug-caller-amara"
