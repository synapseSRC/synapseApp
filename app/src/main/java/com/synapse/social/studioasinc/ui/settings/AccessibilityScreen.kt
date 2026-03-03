package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilityScreen(
    onBackClick: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text("Accessibility") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
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
                        icon = R.drawable.ic_contrast,
                        checked = false,
                        onCheckedChange = { }
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "High Contrast Text",
                        subtitle = "Use high contrast colors for text",
                        icon = R.drawable.ic_text_format,
                        checked = false,
                        onCheckedChange = { }
                    )
                }
            }


            item {
                SettingsSection(title = "Animation Toggles") {
                    SettingsToggleItem(
                        title = "Reduce Animations",
                        subtitle = "Minimize motion and transitions",
                        icon = R.drawable.ic_animation,
                        checked = false,
                        onCheckedChange = { }
                    )
                    SettingsDivider()
                    SettingsToggleItem(
                        title = "Auto-play Animations",
                        subtitle = "Toggle auto-play for stickers and GIFs",
                        icon = R.drawable.ic_play_circle,
                        checked = true,
                        onCheckedChange = { }
                    )
                }
            }
        }
    }
}
