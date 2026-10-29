package com.synapse.social.studioasinc.feature.inbox.inbox.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.inbox.inbox.ChatInfoViewModel
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPrivacyScreen(
    chatId: String,
    onNavigateBack: () -> Unit,
    viewModel: ChatInfoViewModel = hiltViewModel()
) {
    LaunchedEffect(chatId) {
        viewModel.loadChatInfo(chatId, "")
    }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.chat_privacy_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Spacing.space16),
            verticalArrangement = Arrangement.spacedBy(Spacing.space16)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.space16),
                    verticalArrangement = Arrangement.spacedBy(Spacing.space8)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(Spacing.space8))
                        Text(
                            text = stringResource(R.string.encryption_status_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = stringResource(R.string.encryption_status_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier.padding(Spacing.space16),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(Spacing.space16))
                        Column {
                            Text(
                                text = stringResource(R.string.chat_info_chat_lock),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.chat_info_chat_lock_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = uiState.isLocked,
                        onCheckedChange = { viewModel.toggleChatLock(chatId) }
                    )
                }
            }
        }
    }
}
