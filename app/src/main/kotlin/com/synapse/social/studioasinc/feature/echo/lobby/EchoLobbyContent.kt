package com.synapse.social.studioasinc.feature.echo.lobby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.echo.lobby.components.CreateEchoRoomSheet
import com.synapse.social.studioasinc.feature.echo.lobby.components.EchoRoomCard
import com.synapse.social.studioasinc.feature.echo.messageRes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

/**
 * The lobby list itself, without the frame around it.
 *
 * Two hosts render this: the standalone EchoLobby route, which wraps it in a TopAppBar and its own
 * action button, and the Inbox "Echo" tab. Keeping the reads, the four list states, and the create
 * sheet here means the two entry points cannot drift apart.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EchoLobbyContent(
    onOpenRoom: (roomId: String, speakOnEntry: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: EchoLobbyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()

    // Runs on the first composition too, so entering the tab or returning from a room both read
    // afresh — the list may have changed while this content was off screen.
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    // Consume before navigating so a configuration change cannot replay the same room.
    LaunchedEffect(uiState.pendingRoomId) {
        val roomId = uiState.pendingRoomId ?: return@LaunchedEffect
        val speakOnEntry = uiState.pendingSpeakOnEntry
        viewModel.consumePendingRoom()
        onOpenRoom(roomId, speakOnEntry)
    }

    Box(modifier = modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::refresh,
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullToRefreshState,
                    isRefreshing = uiState.isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        ) {
            when {
                uiState.rooms.isEmpty() && (uiState.isLoading || !uiState.hasLoaded) -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                uiState.rooms.isEmpty() && uiState.error != null -> {
                    EchoLobbyMessage(
                        title = stringResource(R.string.echo_lobby_error_title),
                        body = stringResource(uiState.error!!.messageRes()),
                        actionLabel = stringResource(R.string.action_retry),
                        onAction = viewModel::refresh,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                uiState.rooms.isEmpty() -> {
                    EchoLobbyMessage(
                        title = stringResource(R.string.echo_lobby_empty_title),
                        body = stringResource(R.string.echo_lobby_empty_body),
                        actionLabel = stringResource(R.string.echo_lobby_start),
                        onAction = viewModel::openCreateSheet,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = Spacing.space16,
                            end = Spacing.space16,
                            top = Spacing.space8,
                            bottom = contentPadding.calculateBottomPadding() + Spacing.space64
                        ),
                        verticalArrangement = Arrangement.spacedBy(Spacing.space12)
                    ) {
                        if (uiState.error != null) {
                            item(key = "lobby-refresh-failed") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.echo_lobby_refresh_failed),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = viewModel::refresh) {
                                        Text(stringResource(R.string.action_retry))
                                    }
                                }
                            }
                        }
                        items(uiState.rooms, key = { it.room.id }) { summary ->
                            EchoRoomCard(
                                summary = summary,
                                onClick = { onOpenRoom(summary.room.id, false) }
                            )
                        }
                        item(key = "lobby-bottom-space") {
                            Spacer(modifier = Modifier.padding(bottom = Spacing.space48))
                        }
                    }
                }
            }
        }
    }

    if (uiState.create.isOpen) {
        CreateEchoRoomSheet(
            state = uiState.create,
            onTitleChange = viewModel::updateCreateTitle,
            onTopicChange = viewModel::updateCreateTopic,
            onVisibilityChange = viewModel::updateCreateVisibility,
            onMaxSpeakersChange = viewModel::updateCreateMaxSpeakers,
            onDismiss = viewModel::dismissCreateSheet,
            onCreate = viewModel::submitCreate
        )
    }
}

@Composable
private fun EchoLobbyMessage(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = Spacing.space32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.space8)
        )
        TextButton(
            onClick = onAction,
            modifier = Modifier.padding(top = Spacing.space16)
        ) {
            Text(actionLabel)
        }
    }
}
