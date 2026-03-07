package com.synapse.social.studioasinc.feature.profile.editprofile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionSelectionScreen(
    onBackClick: () -> Unit,
    onRegionSelected: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(false) }

    val countries = remember {
        Locale.getISOCountries()
            .map { code -> Locale("", code) }
            .map { locale -> locale.displayCountry }
            .filter { it.isNotEmpty() }
            .sorted()
            .distinct()
    }

    val filteredCountries = if (searchQuery.isEmpty()) {
        countries
    } else {
        countries.filter {
            it.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            if (!isActive) {
                TopAppBar(
                    title = { Text(stringResource(R.string.select_region)) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { isActive = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.cd_search)
                            )
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isActive) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { isActive = false },
                    active = isActive,
                    onActiveChange = { isActive = it },
                    placeholder = { Text(stringResource(R.string.search_region)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_all))
                            }
                        } else {
                             IconButton(onClick = { isActive = false }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_back))
                            }
                        }
                    }
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = Spacing.Medium, vertical = Spacing.Small)
                    ) {
                        itemsIndexed(filteredCountries) { index, country ->
                            val shape = getShapeForItem(index, filteredCountries.size)
                            RegionItem(
                                region = country,
                                isSelected = false,
                                onRegionSelected = onRegionSelected,
                                shape = shape
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = Spacing.Medium, vertical = Spacing.Small)
                ) {
                    itemsIndexed(filteredCountries) { index, country ->
                        val shape = getShapeForItem(index, filteredCountries.size)
                        RegionItem(
                            region = country,
                            isSelected = false,
                            onRegionSelected = onRegionSelected,
                            shape = shape
                        )
                    }
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
