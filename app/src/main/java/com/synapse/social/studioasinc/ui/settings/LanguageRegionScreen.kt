package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageRegionScreen(
    viewModel: LanguageRegionViewModel,
    onBackClick: () -> Unit
) {
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val availableLanguages by viewModel.availableLanguages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.settings_language_region_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = {

            error?.let { errorMessage ->
                Snackbar(
                    modifier = Modifier.padding(Spacing.Medium),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text(stringResource(R.string.action_dismiss))
                        }
                    }
                ) {
                    Text(errorMessage)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = SettingsSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing),
            contentPadding = PaddingValues(vertical = Spacing.Medium)
        ) {

            item {
                SettingsSection(title = "App Language") {

                    availableLanguages.forEachIndexed { index, languageOption ->
                        LanguageSelectionItem(
                            languageOption = languageOption,
                            isSelected = viewModel.isLanguageSelected(languageOption),
                            onClick = {
                                if (!isLoading) {
                                    viewModel.setLanguage(languageOption)
                                }
                            },
                            enabled = !isLoading
                        )


                        if (index < availableLanguages.size - 1) {
                            SettingsDivider()
                        }
                    }
                }
            }


            item {
                SettingsSection(title = "Region") {
                    SettingsNavigationItem(
                        title = "Region Preferences",
                        subtitle = "Date, time, and number formats",
                        imageVector = Icons.Filled.Language,
                        onClick = { viewModel.navigateToRegionPreferences() },
                        enabled = !isLoading
                    )
                }
            }


            item {
                Spacer(modifier = Modifier.height(Spacing.ExtraLarge))
            }
        }
    }
}



@Composable
private fun LanguageSelectionItem(
    languageOption: LanguageOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SettingsSpacing.itemPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = languageOption.nativeName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )


                if (languageOption.name != languageOption.nativeName) {
                    Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                    Text(
                        text = languageOption.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        }
                    )
                }
            }


            RadioButton(
                selected = isSelected,
                onClick = onClick,
                enabled = enabled,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
