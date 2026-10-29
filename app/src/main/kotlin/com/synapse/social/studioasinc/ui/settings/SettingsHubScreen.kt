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
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.AppColors
import com.synapse.social.studioasinc.feature.shared.theme.AppSpacing
import com.synapse.social.studioasinc.ui.components.SectionHeader

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
    val unreadCategoryIds by viewModel.unreadCategoryIds.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = AppColors.screenBackground,
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
                    containerColor = AppColors.screenBackground,
                    scrolledContainerColor = AppColors.cardBackground
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
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = AppSpacing.screenPadding),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionSpacing),
                        contentPadding = PaddingValues(vertical = Spacing.space8)
                    ) {
                        item {
                            SettingsSearchBar(
                                query = searchQuery,
                                onQueryChange = viewModel::onSearchQueryChange
                            )
                        }

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
                                unreadCategoryIds = unreadCategoryIds,
                                onNavigate = {
                                    viewModel.onNavigateToCategory(it)
                                    onNavigateToCategory(it)
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(Spacing.space32))
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
            .padding(vertical = Spacing.space8),
        shape = CircleShape,
        color = AppColors.cardBackground,
        tonalElevation = Sizes.ElevationLow
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = Spacing.space16, vertical = Spacing.space12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(Spacing.space12))
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
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(Sizes.IconLarge)) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.settings_search_clear_content_description))
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
            .background(AppColors.screenBackground),
        color = AppColors.screenBackground
    ) {
        LazyColumn(
            modifier = Modifier.padding(Spacing.space16),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.itemSpacing)
        ) {
            items(results) { node ->
                val action = node.action
                val navigateDestination =
                    (action as? com.synapse.social.studioasinc.shared.domain.model.settings.SettingsAction.Navigate)?.destination
                val toggleAction =
                    action as? com.synapse.social.studioasinc.shared.domain.model.settings.SettingsAction.Toggle

                SettingsListItem(
                    title = node.title,
                    subtitle = node.subtitle,
                    onClick = {
                        when {
                            toggleAction != null -> onActionClick(toggleAction)
                            navigateDestination != null -> onNavigate(navigateDestination)
                            action != null -> onActionClick(action)
                        }
                    },
                    trailingContent = if (toggleAction != null) {
                        {
                            Switch(
                                checked = toggleAction.currentValue,
                                onCheckedChange = { onActionClick(toggleAction) }
                            )
                        }
                    } else {
                        null
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsFlattenedContent(
    settingsGroups: List<SettingsGroup>,
    onNavigate: (SettingsDestination) -> Unit,
    unreadCategoryIds: Set<String> = emptySet()
) {
    val unreadDescription = stringResource(R.string.settings_unread_indicator)
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionSpacing)) {
        settingsGroups.forEach { group ->
            Column {
                if (group.title != null) {
                    SectionHeader(
                        title = group.title.asString()
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.itemSpacing)
                ) {
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
                            position = position,
                            indicator = if (category.id in unreadCategoryIds) {
                                { SettingsUnreadDot(description = unreadDescription) }
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        }
    }
}
