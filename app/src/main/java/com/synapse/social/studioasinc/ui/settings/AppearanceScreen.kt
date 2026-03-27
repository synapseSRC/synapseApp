package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    viewModel: AppearanceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChatCustomization: () -> Unit = {}
) {
    val appearanceSettings by viewModel.appearanceSettings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.settings_appearance_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = {
            if (error != null) {
                Snackbar(
                    modifier = Modifier.padding(Spacing.Medium),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text(stringResource(R.string.action_dismiss))
                        }
                    }
                ) {
                    Text(error ?: "")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(paddingValues)
                .padding(horizontal = SettingsSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)
        ) {

            item {
                SettingsSection(title = "Theme") {

                    SettingsSelectionItem(
                        title = "Theme Mode",
                        subtitle = "Choose your preferred theme",
                        icon = Icons.Filled.Palette,
                        options = viewModel.getThemeModeOptions(),
                        selectedOption = viewModel.getThemeModeDisplayName(appearanceSettings.themeMode),
                        onSelect = { selected ->
                            val mode = viewModel.getThemeModeFromDisplayName(selected)
                            viewModel.setThemeMode(mode)
                        },
                        enabled = !isLoading
                    )


                    if (viewModel.isDynamicColorSupported) {
                        SettingsDivider()
                        SettingsToggleItem(
                            title = "Dynamic Color",
                            subtitle = "Use colors from your wallpaper",
                            imageVector = Icons.Filled.Palette,
                            checked = appearanceSettings.dynamicColorEnabled,
                            onCheckedChange = { viewModel.setDynamicColorEnabled(it) },
                            enabled = !isLoading
                        )
                    }
                }
            }


            item {
                SettingsSection(title = "Display") {

                    SettingsSliderItem(
                        title = "Font Size",
                        subtitle = "Adjust text size for better readability",
                        value = viewModel.getSliderValueFromFontScale(appearanceSettings.fontScale),
                        valueRange = 0f..3f,
                        steps = 2,
                        onValueChange = { value ->
                            val scale = viewModel.getFontScaleFromSliderValue(value)
                            viewModel.setFontScale(scale)
                        },
                        valueLabel = { value ->
                            val scale = viewModel.getFontScaleFromSliderValue(value)
                            viewModel.getFontScalePreviewText(scale)
                        },
                        enabled = !isLoading
                    )
                }
            }



            item {
                SettingsSection(title = "Media Layout (Beta)") {
                    val options = viewModel.getPostViewStyleOptions()
                    val selected = viewModel.getPostViewStyleFromDisplayName(appearanceSettings.postViewStyle.displayName()).displayName()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = SettingsSpacing.itemHorizontalPadding, vertical = SettingsSpacing.itemVerticalPadding)
                    ) {
                        Text(
                            text = "Post Media Grid",
                            style = SettingsTypography.itemTitle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                        Text(
                            text = "Choose how multiple images/videos are displayed",
                            style = SettingsTypography.itemSubtitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(Spacing.Medium))

                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            options.forEachIndexed { index, label ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                                    onClick = {
                                        val style = viewModel.getPostViewStyleFromDisplayName(label)
                                        viewModel.setPostViewStyle(style)
                                    },
                                    selected = label == selected,
                                    label = {
                                        Row(
                                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(label)
                                            if (label == "Grid") {
                                                Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                                                Badge(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                ) {
                                                    Text(
                                                        text = "Beta",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        modifier = Modifier.padding(horizontal = Spacing.ExtraSmall, vertical = 0.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(Sizes.HeightSmall))
            }
        }
    }
}
