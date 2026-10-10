package com.synapse.social.studioasinc.presentation.editprofile.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.SynapseTheme
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import com.synapse.social.studioasinc.ui.settings.SettingsClickableItem
import com.synapse.social.studioasinc.ui.settings.SettingsHeaderItem
import com.synapse.social.studioasinc.ui.settings.SettingsItemPosition
import com.synapse.social.studioasinc.ui.settings.SettingsShapes
import com.synapse.social.studioasinc.ui.settings.SettingsSpacing

enum class EditableSocialField {
    GITHUB,
    DISCORD,
    WEBSITE,
    PUBLIC_EMAIL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialLinksScreen(
    viewModel: SocialLinksViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var activeDialogField by remember { mutableStateOf<EditableSocialField?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                SocialLinksNavigation.NavigateBack -> onNavigateBack()
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(SocialLinksEvent.ErrorDismissed)
        }
    }

    SynapseTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = stringResource(R.string.social_links_title)) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.onEvent(SocialLinksEvent.BackClicked) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { viewModel.onEvent(SocialLinksEvent.SaveClicked) },
                            enabled = uiState.hasChanges &&
                                    !uiState.isSaving &&
                                    uiState.githubError == null &&
                                    uiState.discordError == null &&
                                    uiState.websiteError == null &&
                                    uiState.publicEmailError == null
                        ) {
                            if (uiState.isSaving) {
                                ExpressiveLoadingIndicator(modifier = Modifier.size(18.dp))
                            } else {
                                Text(text = stringResource(R.string.save))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        ExpressiveLoadingIndicator()
                    }
                }
                uiState.error != null && uiState.githubProfile.isEmpty() && uiState.discordTag.isEmpty() && uiState.personalWebsite.isEmpty() && uiState.publicEmail.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.error_loading_social_links),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.onEvent(SocialLinksEvent.RetryClicked) }) {
                                Text(text = stringResource(R.string.retry))
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = SettingsSpacing.screenPadding,
                            end = SettingsSpacing.screenPadding,
                            top = paddingValues.calculateTopPadding() + SettingsSpacing.itemSpacing,
                            bottom = paddingValues.calculateBottomPadding() + SettingsSpacing.itemSpacing
                        ),
                        verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)
                    ) {
                        // Section 1: Social Profiles
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                SettingsHeaderItem(title = stringResource(R.string.social_profiles_section))
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(SettingsSpacing.itemSpacing)
                                ) {
                                    // GitHub - Top
                                    SettingsClickableItem(
                                        title = stringResource(R.string.github_title),
                                        subtitle = uiState.githubProfile.ifBlank { stringResource(R.string.not_set) },
                                        imageVector = Icons.Filled.Code,
                                        onClick = { activeDialogField = EditableSocialField.GITHUB },
                                        position = SettingsItemPosition.Top
                                    )

                                    // Discord - Middle
                                    SettingsClickableItem(
                                        title = stringResource(R.string.discord_title),
                                        subtitle = uiState.discordTag.ifBlank { stringResource(R.string.not_set) },
                                        imageVector = Icons.Filled.Message,
                                        onClick = { activeDialogField = EditableSocialField.DISCORD },
                                        position = SettingsItemPosition.Middle
                                    )

                                    // Personal Website - Bottom
                                    SettingsClickableItem(
                                        title = stringResource(R.string.website_title),
                                        subtitle = uiState.personalWebsite.ifBlank { stringResource(R.string.not_set) },
                                        imageVector = Icons.Filled.Language,
                                        onClick = { activeDialogField = EditableSocialField.WEBSITE },
                                        position = SettingsItemPosition.Bottom
                                    )
                                }
                            }
                        }

                        // Section 2: Public Contact
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                SettingsHeaderItem(title = stringResource(R.string.public_contact_section))
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(SettingsSpacing.itemSpacing)
                                ) {
                                    // Public Email - Single
                                    SettingsClickableItem(
                                        title = stringResource(R.string.public_email_title),
                                        subtitle = uiState.publicEmail.ifBlank { stringResource(R.string.public_email_subtitle) },
                                        imageVector = Icons.Filled.Email,
                                        onClick = { activeDialogField = EditableSocialField.PUBLIC_EMAIL },
                                        position = SettingsItemPosition.Single
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Edit Dialog
        activeDialogField?.let { field ->
            val fieldTitle = when (field) {
                EditableSocialField.GITHUB -> stringResource(R.string.github_title)
                EditableSocialField.DISCORD -> stringResource(R.string.discord_title)
                EditableSocialField.WEBSITE -> stringResource(R.string.website_title)
                EditableSocialField.PUBLIC_EMAIL -> stringResource(R.string.public_email_title)
            }
            val initialFieldValue = remember(field) {
                when (field) {
                    EditableSocialField.GITHUB -> uiState.githubProfile
                    EditableSocialField.DISCORD -> uiState.discordTag
                    EditableSocialField.WEBSITE -> uiState.personalWebsite
                    EditableSocialField.PUBLIC_EMAIL -> uiState.publicEmail
                }
            }
            var tempValue by remember(field) { mutableStateOf(initialFieldValue) }
            val fieldError = when (field) {
                EditableSocialField.GITHUB -> uiState.githubError
                EditableSocialField.DISCORD -> uiState.discordError
                EditableSocialField.WEBSITE -> uiState.websiteError
                EditableSocialField.PUBLIC_EMAIL -> uiState.publicEmailError
            }

            fun dismissAndRevert() {
                when (field) {
                    EditableSocialField.GITHUB -> viewModel.onEvent(SocialLinksEvent.GithubProfileChanged(initialFieldValue))
                    EditableSocialField.DISCORD -> viewModel.onEvent(SocialLinksEvent.DiscordTagChanged(initialFieldValue))
                    EditableSocialField.WEBSITE -> viewModel.onEvent(SocialLinksEvent.PersonalWebsiteChanged(initialFieldValue))
                    EditableSocialField.PUBLIC_EMAIL -> viewModel.onEvent(SocialLinksEvent.PublicEmailChanged(initialFieldValue))
                }
                activeDialogField = null
            }

            AlertDialog(
                onDismissRequest = { dismissAndRevert() },
                title = { Text(text = stringResource(R.string.edit_field_dialog_title, fieldTitle)) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = tempValue,
                            onValueChange = {
                                tempValue = it
                                when (field) {
                                    EditableSocialField.GITHUB -> viewModel.onEvent(SocialLinksEvent.GithubProfileChanged(it))
                                    EditableSocialField.DISCORD -> viewModel.onEvent(SocialLinksEvent.DiscordTagChanged(it))
                                    EditableSocialField.WEBSITE -> viewModel.onEvent(SocialLinksEvent.PersonalWebsiteChanged(it))
                                    EditableSocialField.PUBLIC_EMAIL -> viewModel.onEvent(SocialLinksEvent.PublicEmailChanged(it))
                                }
                            },
                            label = { Text(fieldTitle) },
                            placeholder = { Text(stringResource(R.string.field_value_hint, fieldTitle)) },
                            isError = fieldError != null,
                            supportingText = {
                                if (fieldError != null) {
                                    Text(text = fieldError, color = MaterialTheme.colorScheme.error)
                                } else if (field == EditableSocialField.PUBLIC_EMAIL) {
                                    Text(text = stringResource(R.string.public_email_subtitle))
                                }
                            },
                            singleLine = true,
                            shape = SettingsShapes.inputShape,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = when (field) {
                                EditableSocialField.PUBLIC_EMAIL -> KeyboardOptions(keyboardType = KeyboardType.Email, capitalization = KeyboardCapitalization.None)
                                EditableSocialField.WEBSITE -> KeyboardOptions(keyboardType = KeyboardType.Uri, capitalization = KeyboardCapitalization.None)
                                EditableSocialField.GITHUB -> KeyboardOptions(keyboardType = KeyboardType.Uri, capitalization = KeyboardCapitalization.None)
                                EditableSocialField.DISCORD -> KeyboardOptions(keyboardType = KeyboardType.Text, capitalization = KeyboardCapitalization.None)
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { activeDialogField = null },
                        enabled = fieldError == null
                    ) {
                        Text(text = stringResource(R.string.save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dismissAndRevert() }) {
                        Text(text = stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}
