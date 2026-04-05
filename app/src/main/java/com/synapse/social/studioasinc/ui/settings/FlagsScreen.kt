package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlagsScreen(
    viewModel: FlagsViewModel,
    onBackClick: () -> Unit
) {
    val messageSuggestionEnabled by viewModel.messageSuggestionEnabled.collectAsState()
    val chatAvatarDisabled by viewModel.chatAvatarDisabled.collectAsState()

    Scaffold(
        containerColor = SettingsColors.screenBackground,
        topBar = {
            TopAppBar(
                title = { Text("Flags") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SettingsColors.screenBackground,
                    scrolledContainerColor = SettingsColors.cardBackground
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = SettingsSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsHeaderItem(title = "Experimental Features")

                    SettingsCard {
                        SettingsToggleItem(
                            title = "Message Suggestions",
                            subtitle = "Enable smart replies in chat",
                            imageVector = Icons.Filled.Build,
                            checked = messageSuggestionEnabled,
                            onCheckedChange = { viewModel.setMessageSuggestionEnabled(it) },
                            position = SettingsItemPosition.Top
                        )
                        SettingsToggleItem(
                            title = "Disable Chat Avatars",
                            subtitle = "Hide sender avatars in chat",
                            imageVector = Icons.Filled.Person,
                            checked = chatAvatarDisabled,
                            onCheckedChange = { viewModel.setChatAvatarDisabled(it) },
                            position = SettingsItemPosition.Bottom
                        )
                    }
                }
            }
        }
    }
}
