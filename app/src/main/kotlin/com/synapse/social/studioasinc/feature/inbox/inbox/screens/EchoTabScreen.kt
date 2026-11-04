package com.synapse.social.studioasinc.feature.inbox.inbox.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.echo.lobby.EchoLobbyContent
import com.synapse.social.studioasinc.feature.echo.lobby.EchoLobbyViewModel
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

/**
 * The Echo Rooms lobby as an inbox tab.
 *
 * The standalone route carries its title in a TopAppBar; a tab has no app bar of its own, so the
 * title and subtitle are rendered here above the shared [EchoLobbyContent]. Hosting the content
 * rather than navigating keeps the bottom bar reachable while the user browses live rooms.
 */
@Composable
fun EchoTabScreen(
    onOpenRoom: (roomId: String, speakOnEntry: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: EchoLobbyViewModel = hiltViewModel()
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.space16, vertical = Spacing.space8)
            ) {
                Text(
                    text = stringResource(R.string.echo_lobby_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = stringResource(R.string.echo_lobby_subtitle),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            EchoLobbyContent(
                onOpenRoom = onOpenRoom,
                modifier = Modifier.weight(1f),
                contentPadding = contentPadding,
                viewModel = viewModel
            )
        }

        ExtendedFloatingActionButton(
            onClick = viewModel::openCreateSheet,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.echo_lobby_start)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    bottom = contentPadding.calculateBottomPadding() + Spacing.space16,
                    end = Spacing.space16
                )
        )
    }
}
