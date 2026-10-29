package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.AppColors
import com.synapse.social.studioasinc.feature.shared.theme.AppSpacing
import com.synapse.social.studioasinc.feature.shared.theme.AppTypography



@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppearanceScreen(
    viewModel: AppearanceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChatCustomization: () -> Unit = {},
    onNavigateToFont: () -> Unit = {}
) {
    val appearanceSettings by viewModel.appearanceSettings.collectAsState()
    val selectedFontDisplayName by viewModel.selectedFontDisplayName.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        containerColor = AppColors.screenBackground,
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
                    containerColor = AppColors.screenBackground,
                    scrolledContainerColor = AppColors.cardBackground
                )
            )
        },
        snackbarHost = {
            if (error != null) {
                Snackbar(
                    modifier = Modifier.padding(Spacing.space16),
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
                .padding(horizontal = AppSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionSpacing)
        ) {

            item {
                SettingsSection(title = stringResource(R.string.settings_font_title)) {
                    SettingsNavigationItem(
                        title = stringResource(R.string.settings_font_title),
                        subtitle = selectedFontDisplayName,
                        imageVector = null,
                        onClick = onNavigateToFont,
                        enabled = !isLoading
                    )
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.settings_theme_t)) {

                    SettingsSelectionItem(
                        title = stringResource(R.string.appearance_theme_mode),
                        subtitle = stringResource(R.string.appearance_theme_mode_subtitle),
                        icon = Icons.Filled.Palette,
                        options = viewModel.getThemeModeOptions(),
                        selectedOption = viewModel.getThemeModeDisplayName(appearanceSettings.themeMode),
                        onSelect = { selected ->
                            val mode = viewModel.getThemeModeFromDisplayName(selected)
                            viewModel.setThemeMode(mode)
                        },
                        enabled = !isLoading,
                        position = if (viewModel.isDynamicColorSupported) SettingsItemPosition.Top else SettingsItemPosition.Single
                    )


                    if (viewModel.isDynamicColorSupported) {
                        SettingsToggleItem(
                            title = stringResource(R.string.appearance_dynamic_color),
                            subtitle = stringResource(R.string.appearance_dynamic_color_subtitle),
                            imageVector = Icons.Filled.Palette,
                            checked = appearanceSettings.dynamicColorEnabled,
                            onCheckedChange = { viewModel.setDynamicColorEnabled(it) },
                            enabled = !isLoading,
                            position = SettingsItemPosition.Bottom
                        )
                    }
                }
            }


            item {
                SettingsSection(title = stringResource(R.string.appearance_display)) {

                    SettingsSliderItem(
                        title = stringResource(R.string.appearance_font_size),
                        subtitle = stringResource(R.string.appearance_font_size_subtitle),
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
                SettingsSection(title = stringResource(R.string.appearance_media_layout_beta)) {
                    val options = viewModel.getPostViewStyleOptions()
                    val selected = viewModel.getPostViewStyleFromDisplayName(appearanceSettings.postViewStyle.displayName()).displayName()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppSpacing.itemHorizontalPadding, vertical = AppSpacing.itemVerticalPadding)
                    ) {
                        Text(
                            text = stringResource(R.string.appearance_post_media_grid),
                            style = AppTypography.itemTitle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(Spacing.space4))
                        Text(
                            text = stringResource(R.string.appearance_post_media_grid_subtitle),
                            style = AppTypography.itemSubtitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(Spacing.space16))

                        val connectedLeadingShapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                        val connectedMiddleShapes = ButtonGroupDefaults.connectedMiddleButtonShapes()
                        val connectedTrailingShapes = ButtonGroupDefaults.connectedTrailingButtonShapes()

                        ButtonGroup(
                            overflowIndicator = { menuState ->
                                ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            expandedRatio = 0f,
                            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            options.forEachIndexed { index, label ->
                                val isSelected = label == selected
                                val buttonShapes = when (index) {
                                    0 -> connectedLeadingShapes
                                    options.lastIndex -> connectedTrailingShapes
                                    else -> connectedMiddleShapes
                                }
                                val selectStyle = {
                                    val style = viewModel.getPostViewStyleFromDisplayName(label)
                                    viewModel.setPostViewStyle(style)
                                }

                                customItem(
                                    buttonGroupContent = {
                                        ToggleButton(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                if (checked) selectStyle()
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .semantics { role = Role.RadioButton },
                                            shapes = buttonShapes
                                        ) {
                                            Row(
                                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Text(label)
                                                if (label == "Grid") {
                                                    Spacer(modifier = Modifier.width(Spacing.space4))
                                                    Badge(
                                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                    ) {
                                                        Text(
                                                            text = stringResource(R.string.appearance_beta),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            modifier = Modifier.padding(horizontal = Spacing.space4, vertical = Spacing.space0)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    menuContent = { menuState ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            trailingIcon = {
                                                if (isSelected) Icon(Icons.Default.Check, contentDescription = null)
                                            },
                                            onClick = {
                                                menuState.dismiss()
                                                selectStyle()
                                            }
                                        )
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
