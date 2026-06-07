package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
    val chatMessagePaginationLimit by viewModel.chatMessagePaginationLimit.collectAsState()
    var paginationLimitInput by remember(chatMessagePaginationLimit) {
        mutableStateOf(chatMessagePaginationLimit.toString())
    }
    val focusManager = LocalFocusManager.current

    Scaffold(
        containerColor = SettingsColors.screenBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_flags_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_button)
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
                    SettingsHeaderItem(title = stringResource(R.string.settings_experimental_features_title))

                    SettingsCard {
                        SettingsToggleItem(
                            title = stringResource(R.string.settings_message_suggestions_title),
                            subtitle = stringResource(R.string.settings_message_suggestions_subtitle),
                            imageVector = Icons.Filled.Build,
                            checked = messageSuggestionEnabled,
                            onCheckedChange = { viewModel.setMessageSuggestionEnabled(it) },
                            position = SettingsItemPosition.Top
                        )
                        SettingsDivider()
                        SettingsToggleItem(
                            title = stringResource(R.string.settings_disable_chat_avatars_title),
                            subtitle = stringResource(R.string.settings_disable_chat_avatars_subtitle),
                            imageVector = Icons.Filled.Person,
                            checked = chatAvatarDisabled,
                            onCheckedChange = { viewModel.setChatAvatarDisabled(it) },
                            position = SettingsItemPosition.Bottom
                        )
                    }
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsHeaderItem(title = stringResource(R.string.settings_performance_section))

                    SettingsCard {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = SettingsItemPosition.Single.getShape(),
                            color = SettingsColors.cardBackground
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = SettingsSpacing.itemHorizontalPadding,
                                        vertical = SettingsSpacing.itemVerticalPadding
                                    ),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.settings_message_pagination_limit_title),
                                        style = SettingsTypography.itemTitle,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.settings_message_pagination_limit_subtitle),
                                        style = SettingsTypography.itemSubtitle,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedTextField(
                                    value = paginationLimitInput,
                                    onValueChange = { paginationLimitInput = it.filter { c -> c.isDigit() } },
                                    modifier = Modifier.width(80.dp),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = {
                                        val value = paginationLimitInput.toIntOrNull()
                                        if (value != null && value > 0) {
                                            viewModel.setChatMessagePaginationLimit(value)
                                        } else {
                                            paginationLimitInput = chatMessagePaginationLimit.toString()
                                        }
                                        focusManager.clearFocus()
                                    }),
                                    textStyle = SettingsTypography.itemTitle.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
