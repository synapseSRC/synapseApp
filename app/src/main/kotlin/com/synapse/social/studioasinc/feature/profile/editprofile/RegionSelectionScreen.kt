package com.synapse.social.studioasinc.feature.profile.editprofile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.runtime.Composable
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
            .map { code -> Locale.Builder().setRegion(code).build() }
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
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onSearch = { isActive = false },
                            expanded = isActive,
                            onExpandedChange = { isActive = it },
                            placeholder = { Text(stringResource(R.string.search_region)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
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
                        )
                    },
                    expanded = isActive,
                    onExpandedChange = { isActive = it }
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = Spacing.Medium, vertical = Spacing.Small)
                    ) {
                        regionListItems(filteredCountries, onRegionSelected)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = Spacing.Medium, vertical = Spacing.Small)
                ) {
                    regionListItems(filteredCountries, onRegionSelected)
                }
            }
        }
    }
}

private fun LazyListScope.regionListItems(
    countries: List<String>,
    onRegionSelected: (String) -> Unit
) {
    itemsIndexed(countries) { index, country ->
        val shape = getRegionShapeForItem(index, countries.size)
        RegionItem(
            region = country,
            isSelected = false,
            onRegionSelected = onRegionSelected,
            shape = shape
        )
    }
}
