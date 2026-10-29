package com.synapse.social.studioasinc.feature.echo.room

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.call.openAppSettings
import com.synapse.social.studioasinc.feature.echo.messageRes
import com.synapse.social.studioasinc.feature.echo.room.components.EchoAudienceRow
import com.synapse.social.studioasinc.feature.echo.room.components.EchoConnectionBanner
import com.synapse.social.studioasinc.feature.echo.room.components.EchoEndedPanel
import com.synapse.social.studioasinc.feature.echo.room.components.EchoErrorBanner
import com.synapse.social.studioasinc.feature.echo.room.components.EchoFailedPanel
import com.synapse.social.studioasinc.feature.echo.room.components.EchoInvitationBanner
import com.synapse.social.studioasinc.feature.echo.room.components.EchoJoiningPanel
import com.synapse.social.studioasinc.feature.echo.room.components.EchoMicBlockedBanner
import com.synapse.social.studioasinc.feature.echo.room.components.EchoParticipantSheet
import com.synapse.social.studioasinc.feature.echo.room.components.EchoReactionOverlay
import com.synapse.social.studioasinc.feature.echo.room.components.EchoRoomControls
import com.synapse.social.studioasinc.feature.echo.room.components.EchoStage
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.echo.EchoErrorKind

/**
 * A live room.
 *
 * The screen owns nothing session-like: joining, the token, the transport and the membership write
 * all live in [EchoRoomViewModel]. What is here is the shape of the room — stage, listeners,
 * banners — and the two platform-only behaviours it needs, the permission launch and the share
 * intent.
 *
 * Leaving is deliberate: the back arrow asks a host to confirm ending the room and lets everyone
 * else leave immediately, because there is no "background" state a lone participant could occupy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EchoRoomScreen(
    onClose: () -> Unit,
    viewModel: EchoRoomViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val reactionEmojis = stringArrayResource(R.array.echo_reaction_emojis).toList()
    var confirmEnd by rememberSaveable { mutableStateOf(false) }

    EchoMicrophonePermissionEffect(
        prompt = state.permissionPrompt,
        onAnswered = viewModel::permissionsAnswered
    )

    LaunchedEffect(state.closeRequested) {
        if (state.closeRequested) onClose()
    }

    // A resume is also a read: realtime can go quiet, and coming back to the room is the moment a
    // stale roster is most likely to be wrong.
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    val requestClose: () -> Unit = {
        when {
            state.phase == EchoRoomPhase.ENDED || state.phase == EchoRoomPhase.FAILED ->
                viewModel.closeEnded()

            state.isHost -> confirmEnd = true
            else -> viewModel.leave()
        }
    }

    BackHandler(onBack = requestClose)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = {
            EchoRoomTopBar(
                state = state,
                onBack = requestClose,
                onShare = { context.shareRoom(state) }
            )
        },
        bottomBar = {
            if (state.phase == EchoRoomPhase.LIVE) {
                EchoRoomControls(
                    state = state,
                    reactionEmojis = reactionEmojis,
                    onToggleMic = viewModel::toggleMicrophone,
                    onToggleHand = viewModel::toggleHand,
                    onReaction = viewModel::sendReaction,
                    onLeave = viewModel::leave,
                    onEnd = { confirmEnd = true }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (state.phase) {
                EchoRoomPhase.JOINING -> EchoJoiningPanel()
                EchoRoomPhase.FAILED -> EchoFailedPanel(
                    error = state.error,
                    onRetry = viewModel::retryJoin,
                    onClose = viewModel::closeEnded
                )

                EchoRoomPhase.ENDED -> EchoEndedPanel(
                    isHost = state.isHost,
                    onClose = viewModel::closeEnded
                )
                EchoRoomPhase.LIVE -> EchoLiveRoomContent(
                    state = state,
                    onTakeStage = viewModel::requestMicrophone,
                    onOpenSettings = { context.openAppSettings() },
                    onReconnect = viewModel::retryAudio,
                    onDismissError = viewModel::dismissError,
                    onParticipantClick = viewModel::openParticipantMenu
                )
            }

            EchoReactionOverlay(
                reactions = state.reactions,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    if (confirmEnd) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            title = { Text(stringResource(R.string.echo_room_end_confirm_title)) },
            text = { Text(stringResource(R.string.echo_room_end_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmEnd = false
                        viewModel.endRoomForEveryone()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.echo_room_end_room),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmEnd = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    val menuParticipant = state.participantMenu?.let(state::participant)
    // A member can leave while the sheet is open; the sheet has nothing left to act on then.
    LaunchedEffect(state.participantMenu, menuParticipant) {
        if (state.participantMenu != null && menuParticipant == null) {
            viewModel.dismissParticipantMenu()
        }
    }
    if (menuParticipant != null) {
        EchoParticipantSheet(
            participant = menuParticipant,
            isBusy = state.busy,
            onDismiss = viewModel::dismissParticipantMenu,
            onSetRole = { role -> viewModel.setParticipantRole(menuParticipant.userId, role) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EchoRoomTopBar(
    state: EchoRoomUiState,
    onBack: () -> Unit,
    onShare: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = state.title ?: stringResource(R.string.echo_room_fallback_title),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val detail = state.detail
                if (detail != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space8)) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.echo_lobby_stage_count,
                                detail.speakerCount,
                                detail.speakerCount
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = pluralStringResource(
                                R.plurals.echo_lobby_listener_count,
                                detail.listenerCount,
                                detail.listenerCount
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back)
                )
            }
        },
        actions = {
            IconButton(onClick = onShare, enabled = state.roomId != null) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = stringResource(R.string.echo_room_share)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    )
}

@Composable
private fun EchoLiveRoomContent(
    state: EchoRoomUiState,
    onTakeStage: () -> Unit,
    onOpenSettings: () -> Unit,
    onReconnect: () -> Unit,
    onDismissError: () -> Unit,
    onParticipantClick: (String) -> Unit
) {
    val error = state.error
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.space16),
        verticalArrangement = Arrangement.spacedBy(Spacing.space12)
    ) {
        state.topic?.takeIf { it.isNotBlank() }?.let { topic ->
            Text(
                text = topic,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.space8)
            )
        }

        when {
            error == EchoErrorKind.AUDIO_FAILED -> EchoConnectionBanner(
                message = stringResource(error.messageRes()),
                onReconnect = onReconnect
            )

            state.connectionTroubled -> EchoConnectionBanner(
                message = stringResource(R.string.echo_room_connection_lost),
                onReconnect = onReconnect
            )

            error != null -> EchoErrorBanner(
                message = stringResource(error.messageRes()),
                onDismiss = onDismissError
            )
        }

        when {
            state.canSpeak && state.micBlocked && !state.micEnabled -> EchoMicBlockedBanner(
                onOpenSettings = onOpenSettings
            )

            state.invitedToSpeak -> EchoInvitationBanner(onTakeStage = onTakeStage)
        }

        state.detail?.let { detail ->
            EchoStage(
                detail = detail,
                activeSpeakerIds = state.activeSpeakerIds,
                onParticipantClick = onParticipantClick,
                modifier = Modifier.padding(top = Spacing.space4)
            )
            EchoAudienceRow(
                detail = detail,
                onParticipantClick = onParticipantClick
            )
            Spacer(modifier = Modifier.height(Spacing.space24))
        }
    }
}

private fun Context.shareRoom(state: EchoRoomUiState) {
    val roomId = state.roomId ?: return
    val title = state.title ?: getString(R.string.echo_room_fallback_title)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(
            Intent.EXTRA_TEXT,
            getString(R.string.echo_room_share_text, title, "$ECHO_LINK_PREFIX$roomId")
        )
    }
    runCatching {
        startActivity(Intent.createChooser(send, getString(R.string.echo_room_share_chooser)))
    }
}

private const val ECHO_LINK_PREFIX = "synapse://echo/"
