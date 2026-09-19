package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilityScreen(
    viewModel: AccessibilityViewModel,
    onBackClick: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val increaseContrast by viewModel.increaseContrastEnabled.collectAsState()
    val highContrastText by viewModel.highContrastTextEnabled.collectAsState()
    val reduceAnimations by viewModel.reduceAnimationsEnabled.collectAsState()
    val autoplayAnimations by viewModel.autoplayAnimationsEnabled.collectAsState()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.settings_accessibility_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_button)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = SettingsSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)
        ) {

            item {
                SettingsSection(title = stringResource(R.string.settings_increase_contrast_section)) {
                    SettingsToggleItem(
                        title = stringResource(R.string.settings_increase_contrast_title),
                        subtitle = stringResource(R.string.settings_increase_contrast_subtitle),
                        imageVector = Icons.Filled.Contrast,
                        checked = increaseContrast,
                        onCheckedChange = { viewModel.updateIncreaseContrast(it) }
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = stringResource(R.string.settings_high_contrast_text_title),
                        subtitle = stringResource(R.string.settings_high_contrast_text_subtitle),
                        imageVector = Icons.Filled.TextFormat,
                        checked = highContrastText,
                        onCheckedChange = { viewModel.updateHighContrastText(it) }
                    )
                }
            }


            item {
                SettingsSection(title = stringResource(R.string.settings_animation_toggles_title)) {
                    SettingsToggleItem(
                        title = stringResource(R.string.settings_reduce_animations_title),
                        subtitle = stringResource(R.string.settings_reduce_animations_subtitle),
                        imageVector = Icons.Filled.Animation,
                        checked = reduceAnimations,
                        onCheckedChange = { viewModel.updateReduceAnimations(it) }
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = stringResource(R.string.settings_autoplay_animations_title),
                        subtitle = stringResource(R.string.settings_autoplay_animations_subtitle),
                        imageVector = Icons.Filled.PlayCircle,
                        checked = autoplayAnimations,
                        onCheckedChange = { viewModel.updateAutoplayAnimations(it) }
                    )
                }
            }
        }
    }
}
