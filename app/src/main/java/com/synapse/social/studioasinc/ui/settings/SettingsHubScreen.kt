package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHubScreen(
    viewModel: SettingsHubViewModel,
    onBackClick: () -> Unit,
    onNavigateToCategory: (SettingsDestination) -> Unit
) {
    val userProfile by viewModel.userProfileSummary.collectAsState()
    val settingsGroups by viewModel.settingsGroups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = SettingsColors.screenBackground,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings_hub_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_description)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToCategory(SettingsDestination.Search) }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SettingsColors.screenBackground,
                    scrolledContainerColor = SettingsColors.cardBackground
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        if (isLoading && userProfile == null) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                ExpressiveLoadingIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = SettingsSpacing.screenPadding),
                verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {

                item {
                    userProfile?.let { profile ->
                        ProfileHeaderCard(
                            displayName = profile.displayName,
                            email = profile.email,
                            avatarUrl = profile.avatarUrl
                        )
                    }
                }


                items(settingsGroups) { group ->
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (group.title != null) {
                             SettingsHeaderItem(title = group.title)
                        } else {


                        }


                        SettingsCard {
                             group.categories.forEachIndexed { index, category ->
                                val position = when {
                                    group.categories.size == 1 -> SettingsItemPosition.Single
                                    index == 0 -> SettingsItemPosition.Top
                                    index == group.categories.lastIndex -> SettingsItemPosition.Bottom
                                    else -> SettingsItemPosition.Middle
                                }

                                SettingsNavigationItem(
                                    title = category.title,
                                    subtitle = category.subtitle,
                                    imageVector = category.icon,
                                    onClick = {
                                        viewModel.onNavigateToCategory(category.destination)
                                        onNavigateToCategory(category.destination)
                                    },
                                    position = position
                                )

                                if (index < group.categories.size - 1) {
                                    SettingsDivider()
                                }
                            }
                        }
                    }
                }


                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
    }
}
