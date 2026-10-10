package com.synapse.social.studioasinc.feature.profile.editprofile.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.SynapseTheme
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import com.synapse.social.studioasinc.ui.settings.SettingsCard
import com.synapse.social.studioasinc.ui.settings.SettingsItemPosition
import com.synapse.social.studioasinc.ui.settings.SettingsNavigationItem
import com.synapse.social.studioasinc.ui.settings.SettingsSection
import com.synapse.social.studioasinc.ui.settings.SettingsShapes
import com.synapse.social.studioasinc.ui.settings.SettingsSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(
    viewModel: LocationViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToRegionSelection: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                LocationNavigation.NavigateBack -> onNavigateBack()
                is LocationNavigation.NavigateToRegionSelection -> onNavigateToRegionSelection(event.currentRegion)
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { errorMsg ->
            snackbarHostState.showSnackbar(errorMsg)
            viewModel.onEvent(LocationEvent.DismissError)
        }
    }

    SynapseTheme {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                MediumTopAppBar(
                    title = { Text(text = stringResource(R.string.nav_location)) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.onEvent(LocationEvent.BackClicked) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    actions = {
                        if (uiState.isSaving) {
                            ExpressiveLoadingIndicator(
                                modifier = Modifier
                                    .padding(end = Spacing.Medium)
                                    .size(Sizes.IconDefault)
                            )
                        } else {
                            TextButton(
                                onClick = { viewModel.onEvent(LocationEvent.SaveClicked) },
                                enabled = uiState.hasChanges && !uiState.isLoading
                            ) {
                                Text(
                                    text = stringResource(R.string.save),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ExpressiveLoadingIndicator()
                }
            } else {
                LocationContent(
                    uiState = uiState,
                    paddingValues = paddingValues,
                    onEvent = viewModel::onEvent
                )
            }
        }
    }
}

@Composable
fun LocationContent(
    uiState: LocationUiState,
    paddingValues: PaddingValues,
    onEvent: (LocationEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = SettingsSpacing.screenPadding,
            end = SettingsSpacing.screenPadding,
            top = paddingValues.calculateTopPadding() + Spacing.Small,
            bottom = paddingValues.calculateBottomPadding() + Spacing.Large
        ),
        verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)
    ) {
        // 1. LOCATION Section
        item {
            SettingsSection(title = stringResource(R.string.section_location)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(SettingsSpacing.itemSpacing)
                ) {
                    // Country / Region Navigation Item
                    SettingsNavigationItem(
                        title = stringResource(R.string.region),
                        subtitle = uiState.selectedRegion.ifBlank { null }
                            ?: stringResource(R.string.profile_select_region),
                        imageVector = Icons.Filled.LocationOn,
                        onClick = { onEvent(LocationEvent.SelectRegionClicked) },
                        position = SettingsItemPosition.Top
                    )

                    // Current City Input inside Grouped Card
                    SettingsCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.Medium)
                        ) {
                            OutlinedTextField(
                                value = uiState.currentCity,
                                onValueChange = { onEvent(LocationEvent.CurrentCityChanged(it)) },
                                label = { Text(stringResource(R.string.current_city)) },
                                placeholder = { Text(stringResource(R.string.location_name_hint)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.LocationCity,
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = SettingsShapes.inputShape,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    keyboardType = KeyboardType.Text
                                )
                            )
                        }
                    }
                }
            }
        }

        // 2. HOMETOWN Section
        item {
            SettingsSection(title = stringResource(R.string.hometown)) {
                SettingsCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.Medium)
                    ) {
                        OutlinedTextField(
                            value = uiState.hometown,
                            onValueChange = { onEvent(LocationEvent.HometownChanged(it)) },
                            label = { Text(stringResource(R.string.hometown)) },
                            placeholder = { Text(stringResource(R.string.location_name_hint)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Home,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = SettingsShapes.inputShape,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                keyboardType = KeyboardType.Text
                            )
                        )
                    }
                }
            }
        }
    }
}
