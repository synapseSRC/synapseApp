package com.synapse.social.studioasinc.feature.inbox.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.shared.domain.model.chat.GroupJoinOutcome

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInviteScreen(
    inviteCode: String,
    onNavigateBack: () -> Unit,
    onGroupOpened: (String) -> Unit,
    viewModel: GroupInviteViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(inviteCode) { viewModel.loadInvite(inviteCode) }
    LaunchedEffect(state.outcome, state.preview) {
        if (state.outcome == GroupJoinOutcome.JOINED || state.outcome == GroupJoinOutcome.ALREADY_MEMBER) {
            state.preview?.chatId?.let(onGroupOpened)
        }
    }

    GroupCreationTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.group_invite_preview_title)) },
                    navigationIcon = {
                        androidx.compose.material3.IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).padding(Spacing.space16), contentAlignment = Alignment.Center) {
                when {
                    state.isLoading -> CircularProgressIndicator()
                    state.preview == null -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.space12)) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(Spacing.space48))
                        Text(stringResource(R.string.group_invite_invalid), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                        Text(stringResource(R.string.group_invite_invalid_support), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                    else -> {
                        val preview = state.preview!!
                        Card(shape = MaterialTheme.shapes.largeIncreased, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(Spacing.space24), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.space12)) {
                                Icon(Icons.Filled.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(Spacing.space48))
                                Text(preview.name, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                                preview.description?.takeIf { it.isNotBlank() }?.let {
                                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                }
                                Text(pluralStringResource(R.plurals.group_invite_member_count, preview.memberCount, preview.memberCount), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (state.outcome == GroupJoinOutcome.REQUESTED) {
                                    Icon(Icons.Filled.GroupAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text(stringResource(R.string.group_join_request_sent), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                                } else {
                                    Button(
                                        onClick = { viewModel.requestToJoin(inviteCode) },
                                        enabled = !state.isJoining,
                                        modifier = Modifier.fillMaxWidth().height(Sizes.ButtonHeightM3Medium)
                                    ) {
                                        if (state.isJoining) CircularProgressIndicator(modifier = Modifier.size(Spacing.space20), strokeWidth = 2.dp)
                                        else Icon(Icons.Filled.GroupAdd, contentDescription = null)
                                        Spacer(Modifier.size(Spacing.space8))
                                        Text(stringResource(if (preview.requireJoinApproval) R.string.group_request_to_join else R.string.group_join_now))
                                    }
                                }
                                if (state.error) Text(stringResource(R.string.group_invite_join_error), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }
}
