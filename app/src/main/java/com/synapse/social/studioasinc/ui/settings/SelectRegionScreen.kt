package com.synapse.social.studioasinc.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
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
                            val shape = getShapeForItem(index, filteredRegions.size)
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
                    val shape = getShapeForItem(index, filteredRegions.size)
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

@Composable
fun getShapeForItem(index: Int, size: Int): Shape {
    val largeRadius = Spacing.Large
    val smallRadius = Spacing.ExtraSmall
    return when {
        size == 1 -> RoundedCornerShape(largeRadius)
        index == 0 -> RoundedCornerShape(topStart = largeRadius, topEnd = largeRadius, bottomStart = smallRadius, bottomEnd = smallRadius)
        index == size - 1 -> RoundedCornerShape(topStart = smallRadius, topEnd = smallRadius, bottomStart = largeRadius, bottomEnd = largeRadius)
        else -> RoundedCornerShape(smallRadius)
    }
}

@Composable
fun RegionItem(
    region: String,
    isSelected: Boolean,
    onRegionSelected: (String) -> Unit,
    shape: Shape
) {

    val targetContainerColor = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    val containerColor by animateColorAsState(
        targetValue = targetContainerColor,
        label = "containerColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
        label = "contentColor"
    )

    Surface(
        shape = shape,
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clickable { onRegionSelected(region) }
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = region,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null
                )
            },
            trailingContent = {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected"
                    )
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
                headlineColor = contentColor,
                leadingIconColor = contentColor,
                trailingIconColor = contentColor
            )
        )
    }
}
