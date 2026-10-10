package com.synapse.social.studioasinc.feature.profile.personalinfo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.SynapseTheme
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalInformationScreen(
    viewModel: PersonalInformationViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }

    var showPronounsDialog by remember { mutableStateOf(false) }
    var showBirthdayPicker by remember { mutableStateOf(false) }
    var showGenderDialog by remember { mutableStateOf(false) }
    var showRelationshipStatusDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                PersonalInformationNavigation.NavigateBack -> onNavigateBack()
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(PersonalInformationEvent.ErrorDismissed)
        }
    }

    SynapseTheme {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                MediumTopAppBar(
                    title = { Text(text = stringResource(R.string.nav_personal_info)) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.onEvent(PersonalInformationEvent.BackClicked) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    actions = {
                        if (uiState.isSaving) {
                            ExpressiveLoadingIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            TextButton(
                                onClick = { viewModel.onEvent(PersonalInformationEvent.SaveClicked) },
                                enabled = uiState.hasChanges
                            ) {
                                Text(
                                    text = stringResource(R.string.save),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.mediumTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ExpressiveLoadingIndicator()
                }
            } else {
                PersonalInformationContent(
                    uiState = uiState,
                    paddingValues = paddingValues,
                    onPronounsClick = { showPronounsDialog = true },
                    onBirthdayClick = { showBirthdayPicker = true },
                    onGenderClick = { showGenderDialog = true },
                    onRelationshipStatusClick = { showRelationshipStatusDialog = true }
                )
            }
        }

        if (showPronounsDialog) {
            PronounsDialog(
                currentPronouns = uiState.pronouns,
                onDismissRequest = { showPronounsDialog = false },
                onPronounsSelected = { pronouns ->
                    viewModel.onEvent(PersonalInformationEvent.PronounsChanged(pronouns))
                }
            )
        }

        if (showBirthdayPicker) {
            BirthdayDatePickerDialog(
                currentBirthdayString = uiState.birthday,
                onDismissRequest = { showBirthdayPicker = false },
                onBirthdaySelected = { dateString ->
                    viewModel.onEvent(PersonalInformationEvent.BirthdayChanged(dateString))
                }
            )
        }

        if (showGenderDialog) {
            GenderDialog(
                currentGender = uiState.gender,
                onDismissRequest = { showGenderDialog = false },
                onGenderSelected = { gender ->
                    viewModel.onEvent(PersonalInformationEvent.GenderSelected(gender))
                }
            )
        }

        if (showRelationshipStatusDialog) {
            RelationshipStatusDialog(
                currentStatus = uiState.relationshipStatus,
                onDismissRequest = { showRelationshipStatusDialog = false },
                onStatusSelected = { status ->
                    viewModel.onEvent(PersonalInformationEvent.RelationshipStatusSelected(status))
                }
            )
        }
    }
}
