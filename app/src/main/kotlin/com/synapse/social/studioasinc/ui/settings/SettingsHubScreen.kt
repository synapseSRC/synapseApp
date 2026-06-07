package com.synapse.social.studioasinc.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.shared.domain.model.settings.HeroCard
import com.synapse.social.studioasinc.shared.domain.model.settings.SettingsNode

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
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val heroCards by viewModel.heroCards.collectAsState()

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
                            contentDescription = stringResource(R.string.settings_settings_hub_back)
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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading && userProfile == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator()
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.padding(horizontal = SettingsSpacing.screenPadding)) {
                        SettingsSearchBar(
                            query = searchQuery,
                            onQueryChange = viewModel::onSearchQueryChange
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
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

                    item {
                        SettingsFlattenedContent(
                            settingsGroups = settingsGroups,
                            onNavigate = {
                                viewModel.onNavigateToCategory(it)
                                onNavigateToCategory(it)
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

                        }

                        // Command Palette Result Overlay
                        AnimatedVisibility(
                            visible = searchQuery.isNotEmpty(),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            SettingsCommandPaletteResults(
                                results = searchResults,
                                onActionClick = viewModel::onActionClick,
                                onNavigate = { route ->
                                    val dest = SettingsDestination.fromRoute(route)
                                    if (dest != null) onNavigateToCategory(dest)
                                }
                            )
                        }
                }
            }
        }
    }
}

@Composable
fun SettingsSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = CircleShape,
        color = SettingsColors.cardBackground,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(stringResource(R.string.settings_search_settings_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    singleLine = true
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.settings_search_clear_content_description))
                }
            }
        }
    }
}

@Composable
fun SettingsHeroCardsSection(
    heroCards: List<HeroCard>,
    onCardClick: (HeroCard) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(heroCards) { card ->
            Card(
                onClick = { onCardClick(card) },
                modifier = Modifier.width(280.dp),
                shape = SettingsShapes.cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = if (card.backgroundColor != null) Color(android.graphics.Color.parseColor(card.backgroundColor))
                                   else MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        imageVector = if (card.id == "storage_cleanup") Icons.Filled.Storage else Icons.Filled.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(card.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(card.description, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun SettingsCommandPaletteResults(
    results: List<SettingsNode>,
    onActionClick: (com.synapse.social.studioasinc.shared.domain.model.settings.SettingsAction) -> Unit,
    onNavigate: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(results) { node ->
                val action = node.action
                Card(
                    onClick = {
                        if (action is com.synapse.social.studioasinc.shared.domain.model.settings.SettingsAction.Navigate) {
                            onNavigate(action.destination)
                        } else if (action != null) {
                            onActionClick(action)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = SettingsShapes.itemShape,
                    colors = CardDefaults.cardColors(containerColor = SettingsColors.cardBackground)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(node.title, style = MaterialTheme.typography.titleSmall)
                            node.subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                        if (action is com.synapse.social.studioasinc.shared.domain.model.settings.SettingsAction.Toggle) {
                            Switch(checked = action.currentValue, onCheckedChange = { onActionClick(action) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsFlattenedContent(
    settingsGroups: List<SettingsGroup>,
    onNavigate: (SettingsDestination) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)) {
        settingsGroups.forEach { group ->
            Column {
                if (group.title != null) {
                    SettingsHeaderItem(
                        title = group.title.asString()
                    )
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
                            title = category.title.asString(),
                            subtitle = category.subtitle.asString(),
                            imageVector = category.icon,
                            onClick = { onNavigate(category.destination) },
                            position = position
                        )

                        if (index < group.categories.size - 1) {
                            SettingsDivider()
                        }
                    }
                }
            }
        }
    }
}
