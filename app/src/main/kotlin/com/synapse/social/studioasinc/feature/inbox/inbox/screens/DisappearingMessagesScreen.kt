package com.synapse.social.studioasinc.feature.inbox.inbox.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.inbox.inbox.ChatInfoViewModel
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.chat.DisappearingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisappearingMessagesScreen(
    chatId: String,
    onNavigateBack: () -> Unit,
    viewModel: ChatInfoViewModel = hiltViewModel()
) {
    LaunchedEffect(chatId) {
        viewModel.loadChatInfo(chatId, "")
    }

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedMode by remember(uiState.disappearingMode) { mutableStateOf(uiState.disappearingMode) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.disappearing_messages_title)) },
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
                .padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier.padding(Spacing.Medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(Spacing.Medium))
                    Text(
                        text = stringResource(R.string.disappearing_message_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    DisappearingModeOptionRow(
                        title = stringResource(R.string.disappearing_mode_off),
                        isSelected = selectedMode == DisappearingMode.OFF,
                        onClick = { selectedMode = DisappearingMode.OFF }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    DisappearingModeOptionRow(
                        title = stringResource(R.string.disappearing_mode_24_hours),
                        isSelected = selectedMode == DisappearingMode.TWENTY_FOUR_HOURS,
                        onClick = { selectedMode = DisappearingMode.TWENTY_FOUR_HOURS }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    DisappearingModeOptionRow(
                        title = stringResource(R.string.disappearing_mode_7_days),
                        isSelected = selectedMode == DisappearingMode.SEVEN_DAYS,
                        onClick = { selectedMode = DisappearingMode.SEVEN_DAYS }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.setDisappearingMode(chatId, selectedMode)
                    Toast.makeText(context, R.string.toast_disappearing_mode_updated, Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_apply))
            }
        }
    }
}

@Composable
private fun DisappearingModeOptionRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
