package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.components.RegionItem
import com.synapse.social.studioasinc.feature.shared.components.getRegionShapeForItem
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectRegionScreen(
    currentRegion: String,
    onRegionSelected: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val allRegions = RegionData.allRegions

    val filteredRegions by remember(searchQuery) {
        derivedStateOf {
            if (searchQuery.isEmpty()) {
                allRegions
            } else {
                allRegions.filter { it.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (isSearchActive) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { isSearchActive = false },
                    active = true,
                    onActiveChange = { active ->
                        isSearchActive = active
                        if (!active) searchQuery = ""
                    },
                    placeholder = { Text(stringResource(R.string.search_region)) },
                    leadingIcon = {
                        IconButton(onClick = {
                            isSearchActive = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_all))
                            }
                        }
                    },
                    colors = SearchBarDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    )
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = Spacing.Medium, vertical = Spacing.Small)
                    ) {
                        itemsIndexed(filteredRegions) { index, region ->
                            val shape = getRegionShapeForItem(index, filteredRegions.size)
                            RegionItem(
                                region = region,
                                isSelected = region == currentRegion,
                                onRegionSelected = onRegionSelected,
                                shape = shape
                            )
                        }
                    }
                }
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.select_region)) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.cd_search))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }
        }
    ) { paddingValues ->
        if (!isSearchActive) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = Spacing.Medium, vertical = Spacing.Small)
            ) {
                itemsIndexed(filteredRegions) { index, region ->
                    val shape = getRegionShapeForItem(index, filteredRegions.size)
                    RegionItem(
                        region = region,
                        isSelected = region == currentRegion,
                        onRegionSelected = onRegionSelected,
                        shape = shape
                    )
                }
            }
        }
    }
}
