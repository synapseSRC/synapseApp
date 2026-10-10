package com.synapse.social.studioasinc.feature.profile.editprofile.professional

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.shared.domain.model.business.AccountType
import com.synapse.social.studioasinc.ui.settings.SettingsNavigationItem
import com.synapse.social.studioasinc.ui.settings.SettingsSection
import com.synapse.social.studioasinc.ui.settings.SettingsSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalProfileScreen(
    viewModel: ProfessionalProfileViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onNavigateToBusinessPlatform: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(text = stringResource(R.string.professional_profile_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_description)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = SettingsSpacing.screenPadding),
                verticalArrangement = Arrangement.spacedBy(Spacing.Large)
            ) {
                item {
                    AccountTypeSection(
                        accountType = uiState.accountType,
                        isBusinessAccount = uiState.isBusinessAccount,
                        onSwitchToBusiness = { viewModel.switchToBusinessAccount() },
                        switchError = uiState.switchAccountError
                    )
                }

                item {
                    ProfessionalIdentitySection(
                        occupation = uiState.occupation,
                        workplace = uiState.workplace,
                        onOccupationChange = { viewModel.onOccupationChange(it) },
                        onWorkplaceChange = { viewModel.onWorkplaceChange(it) },
                        hasChanges = uiState.hasChanges,
                        isSaving = uiState.isSaving,
                        onSave = { viewModel.save() }
                    )
                }

                if (uiState.isBusinessAccount) {
                    item {
                        BusinessToolsSection(
                            onNavigateToBusinessPlatform = onNavigateToBusinessPlatform
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(Spacing.Large))
                }
            }
        }
    }
}

@Composable
private fun AccountTypeSection(
    accountType: AccountType,
    isBusinessAccount: Boolean,
    onSwitchToBusiness: () -> Unit,
    switchError: String? = null
) {
    SettingsSection(title = stringResource(R.string.professional_section_account_type)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
            modifier = Modifier.padding(vertical = SettingsSpacing.itemVerticalPadding)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SettingsSpacing.itemHorizontalPadding)
                    .semantics(mergeDescendants = true) {
                        contentDescription = when (accountType) {
                            AccountType.PERSONAL -> "Personal"
                            AccountType.CREATOR -> "Creator"
                            AccountType.BUSINESS -> "Business"
                        }
                    }
            ) {
                Icon(
                    imageVector = when (accountType) {
                        AccountType.BUSINESS -> Icons.Default.Business
                        AccountType.CREATOR -> Icons.Default.Work
                        else -> Icons.Default.Group
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(SettingsSpacing.iconSize)
                )
                Spacer(modifier = Modifier.width(SettingsSpacing.iconTextSpacing))
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    Text(
                        text = when (accountType) {
                            AccountType.PERSONAL -> stringResource(R.string.professional_account_type_personal)
                            AccountType.CREATOR -> stringResource(R.string.professional_account_type_creator)
                            AccountType.BUSINESS -> stringResource(R.string.professional_account_type_business)
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (!isBusinessAccount) {
                        Text(
                            text = stringResource(R.string.professional_account_type_switch_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.professional_account_type_business_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            val switchToBusinessLabel = stringResource(R.string.professional_switch_to_business)
            if (!isBusinessAccount) {
                Button(
                    onClick = onSwitchToBusiness,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SettingsSpacing.itemHorizontalPadding)
                        .semantics { contentDescription = switchToBusinessLabel }
                ) {
                    Text(text = switchToBusinessLabel)
                }
            }

            switchError?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = SettingsSpacing.itemHorizontalPadding)
                )
            }
        }
    }
}

@Composable
private fun ProfessionalIdentitySection(
    occupation: String,
    workplace: String,
    onOccupationChange: (String) -> Unit,
    onWorkplaceChange: (String) -> Unit,
    hasChanges: Boolean,
    isSaving: Boolean,
    onSave: () -> Unit
) {
    SettingsSection(title = stringResource(R.string.professional_section_identity)) {
        Column(
            modifier = Modifier.padding(
                horizontal = SettingsSpacing.itemHorizontalPadding,
                vertical = SettingsSpacing.itemVerticalPadding
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
        ) {
            OutlinedTextField(
                value = occupation,
                onValueChange = onOccupationChange,
                label = { Text(text = stringResource(R.string.professional_occupation_label)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                singleLine = true
            )
            OutlinedTextField(
                value = workplace,
                onValueChange = onWorkplaceChange,
                label = { Text(text = stringResource(R.string.professional_workplace_label)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                singleLine = true
            )

            if (hasChanges) {
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                ) {
                    Text(text = stringResource(R.string.professional_save_changes))
                }
            }
        }
    }
}

@Composable
private fun BusinessToolsSection(
    onNavigateToBusinessPlatform: () -> Unit
) {
    SettingsSection(title = stringResource(R.string.professional_section_business_tools)) {
        Column {
            SettingsNavigationItem(
                title = stringResource(R.string.professional_business_platform_title),
                subtitle = stringResource(R.string.professional_business_platform_subtitle),
                imageVector = Icons.Default.Business,
                onClick = onNavigateToBusinessPlatform,
                position = com.synapse.social.studioasinc.ui.settings.SettingsItemPosition.Single
            )
        }
    }
}
