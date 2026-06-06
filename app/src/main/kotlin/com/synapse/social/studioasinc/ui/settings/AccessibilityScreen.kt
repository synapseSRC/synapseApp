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
                            contentDescription = "Back"
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
                SettingsSection(title = "Increase Contrast") {
                    SettingsToggleItem(
                        title = "Increase Contrast",
                        subtitle = "Darken key colors for better visibility",
                        imageVector = Icons.Filled.Contrast,
                        checked = increaseContrast,
                        onCheckedChange = { viewModel.updateIncreaseContrast(it) }
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "High Contrast Text",
                        subtitle = "Use high contrast colors for text",
                        imageVector = Icons.Filled.TextFormat,
                        checked = highContrastText,
                        onCheckedChange = { viewModel.updateHighContrastText(it) }
                    )
                }
            }


            item {
                SettingsSection(title = "Animation Toggles") {
                    SettingsToggleItem(
                        title = "Reduce Animations",
                        subtitle = "Minimize motion and transitions",
                        imageVector = Icons.Filled.Animation,
                        checked = reduceAnimations,
                        onCheckedChange = { viewModel.updateReduceAnimations(it) }
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "Auto-play Animations",
                        subtitle = "Toggle auto-play for stickers and GIFs",
                        imageVector = Icons.Filled.PlayCircle,
                        checked = autoplayAnimations,
                        onCheckedChange = { viewModel.updateAutoplayAnimations(it) }
                    )
                }
            }
        }
    }
}
