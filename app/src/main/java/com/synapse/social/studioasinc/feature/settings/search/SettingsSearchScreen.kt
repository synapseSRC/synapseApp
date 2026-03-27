package com.synapse.social.studioasinc.feature.settings.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.ui.settings.SettingsDataProvider
import com.synapse.social.studioasinc.ui.settings.SettingsColors
import com.synapse.social.studioasinc.ui.settings.SettingsShapes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

data class SettingsSearchItem(
    val title: String,
    val subtitle: String,
    val category: String,
    val route: String,
    val keywords: List<String>
)



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSearchScreen(
    onBackClick: () -> Unit,
    onNavigateToSetting: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val searchItems = remember { getSearchableSettings() }

    val filteredItems = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            searchItems.filter { item ->
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.subtitle.contains(searchQuery, ignoreCase = true) ||
                item.category.contains(searchQuery, ignoreCase = true) ||
                item.keywords.any { it.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    Scaffold(
        containerColor = SettingsColors.screenBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_search_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.Medium),
                placeholder = { Text(stringResource(R.string.settings_search_hint)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                singleLine = true
            )


            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = Spacing.Medium)
            ) {
                items(filteredItems) { item ->
                    SettingsSearchResultItem(
                        item = item,
                        onClick = { onNavigateToSetting(item.route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSearchResultItem(
    item: SettingsSearchItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.ExtraSmall),
        colors = CardDefaults.cardColors(
            containerColor = SettingsColors.cardBackground
        ),
        shape = SettingsShapes.itemShape
    ) {
        Column(
            modifier = Modifier.padding(Spacing.Medium)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = item.category,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun getSearchableSettings(): List<SettingsSearchItem> {
    val groups = SettingsDataProvider.getSettingsGroups()
    return groups.flatMap { group ->
        group.categories.map { category ->
            SettingsSearchItem(
                title = category.title,
                subtitle = category.subtitle,
                category = group.title ?: "General",
                route = category.destination.route,
                keywords = category.keywords
            )
        }
    }
}
