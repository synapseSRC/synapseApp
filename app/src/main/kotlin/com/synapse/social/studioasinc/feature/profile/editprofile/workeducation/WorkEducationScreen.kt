package com.synapse.social.studioasinc.feature.profile.editprofile.workeducation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Work
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.SynapseTheme
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import com.synapse.social.studioasinc.ui.settings.SettingsCard
import com.synapse.social.studioasinc.ui.settings.SettingsSection
import com.synapse.social.studioasinc.ui.settings.SettingsShapes
import com.synapse.social.studioasinc.ui.settings.SettingsSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkEducationScreen(
    viewModel: WorkEducationViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                WorkEducationNavigation.NavigateBack -> onNavigateBack()
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { errorMsg ->
            snackbarHostState.showSnackbar(errorMsg)
            viewModel.onEvent(WorkEducationEvent.DismissError)
        }
    }

    SynapseTheme {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                MediumTopAppBar(
                    title = { Text(text = stringResource(R.string.nav_work_education)) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.onEvent(WorkEducationEvent.BackClicked) }) {
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
                                onClick = { viewModel.onEvent(WorkEducationEvent.SaveClicked) },
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
                WorkEducationContent(
                    uiState = uiState,
                    paddingValues = paddingValues,
                    onEvent = viewModel::onEvent
                )
            }
        }
    }
}

@Composable
fun WorkEducationContent(
    uiState: WorkEducationUiState,
    paddingValues: PaddingValues,
    onEvent: (WorkEducationEvent) -> Unit,
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
        // 1. Current Work Section
        item {
            SettingsSection(title = stringResource(R.string.section_current_work)) {
                SettingsCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.Medium),
                        verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
                    ) {
                        OutlinedTextField(
                            value = uiState.occupation,
                            onValueChange = { onEvent(WorkEducationEvent.OccupationChanged(it)) },
                            label = { Text(stringResource(R.string.occupation)) },
                            placeholder = { Text(stringResource(R.string.occupation_hint)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Work,
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

                        OutlinedTextField(
                            value = uiState.workplace,
                            onValueChange = { onEvent(WorkEducationEvent.WorkplaceChanged(it)) },
                            label = { Text(stringResource(R.string.workplace)) },
                            placeholder = { Text(stringResource(R.string.workplace_hint)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Business,
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

        // 2. Education Section
        item {
            SettingsSection(title = stringResource(R.string.education)) {
                SettingsCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.Medium)
                    ) {
                        OutlinedTextField(
                            value = uiState.education,
                            onValueChange = { onEvent(WorkEducationEvent.EducationChanged(it)) },
                            label = { Text(stringResource(R.string.education)) },
                            placeholder = { Text(stringResource(R.string.education_hint)) },
                            supportingText = {
                                Text(
                                    text = stringResource(R.string.education_helper_text),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.School,
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