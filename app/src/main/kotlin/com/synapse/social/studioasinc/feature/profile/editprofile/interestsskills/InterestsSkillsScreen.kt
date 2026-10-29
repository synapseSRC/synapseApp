package com.synapse.social.studioasinc.feature.profile.editprofile.interestsskills

import com.synapse.social.studioasinc.feature.shared.theme.Spacing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.SynapseTheme
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import com.synapse.social.studioasinc.ui.settings.SettingsCard
import com.synapse.social.studioasinc.ui.components.SectionHeader
import com.synapse.social.studioasinc.feature.shared.theme.AppSpacing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InterestsSkillsScreen(
    viewModel: InterestsSkillsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                InterestsSkillsNavigation.NavigateBack -> onNavigateBack()
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(InterestsSkillsEvent.DismissError)
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onEvent(InterestsSkillsEvent.DismissSuccess)
        }
    }

    SynapseTheme {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                MediumTopAppBar(
                    title = { Text(text = stringResource(R.string.interests_skills_title)) },
                    navigationIcon = {
                        IconButton(
                            onClick = { viewModel.onEvent(InterestsSkillsEvent.BackClicked) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back_content_description)
                            )
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { viewModel.onEvent(InterestsSkillsEvent.SaveClicked) },
                            enabled = uiState.hasChanges && !uiState.isSaving && !uiState.isLoading,
                            modifier = Modifier.padding(end = Spacing.space8)
                        ) {
                            if (uiState.isSaving) {
                                ExpressiveLoadingIndicator(modifier = Modifier.size(18.dp))
                            } else {
                                Text(
                                    text = stringResource(R.string.interests_save),
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ExpressiveLoadingIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = AppSpacing.screenPadding,
                        end = AppSpacing.screenPadding,
                        top = paddingValues.calculateTopPadding() + Spacing.space8,
                        bottom = paddingValues.calculateBottomPadding() + Spacing.space24
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionSpacing)
                ) {
                    // 1. Personal Interests Section
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SectionHeader(title = stringResource(R.string.interests_section_title))

                            Text(
                                text = stringResource(R.string.interests_section_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = Spacing.space12)
                            )

                            SettingsCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Spacing.space16)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = uiState.pendingInterestInput,
                                            onValueChange = { viewModel.onEvent(InterestsSkillsEvent.InterestInputChanged(it)) },
                                            modifier = Modifier.weight(1f),
                                            placeholder = { Text(text = stringResource(R.string.add_interest_placeholder)) },
                                            singleLine = true,
                                            isError = uiState.inputError != null,
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                            keyboardActions = KeyboardActions(
                                                onDone = { viewModel.onEvent(InterestsSkillsEvent.AddInterest) }
                                            )
                                        )

                                        Spacer(modifier = Modifier.width(Spacing.space8))

                                        Button(
                                            onClick = { viewModel.onEvent(InterestsSkillsEvent.AddInterest) },
                                            modifier = Modifier.height(56.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Add,
                                                contentDescription = stringResource(R.string.add_interest_content_description)
                                            )
                                        }
                                    }

                                    AnimatedVisibility(
                                        visible = uiState.inputError != null,
                                        enter = fadeIn(),
                                        exit = fadeOut()
                                    ) {
                                        uiState.inputError?.let { errorMsg ->
                                            Text(
                                                text = errorMsg,
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(top = Spacing.space4, start = Spacing.space4)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(Spacing.space16))

                                    if (uiState.interests.isEmpty()) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = Spacing.space24),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Star,
                                                contentDescription = null,
                                                modifier = Modifier.size(48.dp),
                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                            )
                                            Spacer(modifier = Modifier.height(Spacing.space8))
                                            Text(
                                                text = stringResource(R.string.empty_interests_title),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(Spacing.space4))
                                            Text(
                                                text = stringResource(R.string.empty_interests_subtitle),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        // Display Chips with Remove and Reorder Controls
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(Spacing.space8),
                                            verticalArrangement = Arrangement.spacedBy(Spacing.space8)
                                        ) {
                                            uiState.interests.forEachIndexed { index, interest ->
                                                InputChip(
                                                    selected = false,
                                                    onClick = {},
                                                    label = { Text(text = interest) },
                                                    trailingIcon = {
                                                        IconButton(
                                                            onClick = { viewModel.onEvent(InterestsSkillsEvent.RemoveInterest(index)) },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Filled.Close,
                                                                contentDescription = stringResource(
                                                                    R.string.remove_interest_content_description,
                                                                    interest
                                                                ),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    },
                                                    colors = InputChipDefaults.inputChipColors(
                                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(Spacing.space16))

                                        // Detailed Reorder Control List
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(Spacing.space4)
                                        ) {
                                            uiState.interests.forEachIndexed { index, interest ->
                                                Surface(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = MaterialTheme.shapes.small,
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = Spacing.space12, vertical = Spacing.space4),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = interest,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.weight(1f)
                                                        )

                                                        Row {
                                                            IconButton(
                                                                onClick = {
                                                                    viewModel.onEvent(
                                                                        InterestsSkillsEvent.MoveInterest(index, index - 1)
                                                                    )
                                                                },
                                                                enabled = index > 0,
                                                                modifier = Modifier.size(40.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Filled.KeyboardArrowUp,
                                                                    contentDescription = stringResource(
                                                                        R.string.move_interest_up,
                                                                        interest
                                                                    )
                                                                )
                                                            }

                                                            IconButton(
                                                                onClick = {
                                                                    viewModel.onEvent(
                                                                        InterestsSkillsEvent.MoveInterest(index, index + 1)
                                                                    )
                                                                },
                                                                enabled = index < uiState.interests.lastIndex,
                                                                modifier = Modifier.size(40.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                                                    contentDescription = stringResource(
                                                                        R.string.move_interest_down,
                                                                        interest
                                                                    )
                                                                )
                                                            }

                                                            IconButton(
                                                                onClick = {
                                                                    viewModel.onEvent(InterestsSkillsEvent.RemoveInterest(index))
                                                                },
                                                                modifier = Modifier.size(40.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Filled.Close,
                                                                    contentDescription = stringResource(
                                                                        R.string.remove_interest_content_description,
                                                                        interest
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Skills Section (Pending Backend Support Notice)
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SectionHeader(title = stringResource(R.string.skills_section_title))

                            SettingsCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Spacing.space16),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .padding(top = Spacing.space4, end = Spacing.space12)
                                            .size(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.skills_pending_title),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(Spacing.space4))
                                        Text(
                                            text = stringResource(R.string.skills_pending_description),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Languages Section (Pending Backend Support Notice)
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SectionHeader(title = stringResource(R.string.languages_section_title))

                            SettingsCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(Spacing.space16),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .padding(top = Spacing.space4, end = Spacing.space12)
                                            .size(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.languages_pending_title),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(Spacing.space4))
                                        Text(
                                            text = stringResource(R.string.languages_pending_description),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
