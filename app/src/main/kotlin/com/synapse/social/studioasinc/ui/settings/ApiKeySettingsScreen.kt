package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import com.synapse.social.studioasinc.R
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.settings.ApiKeyInfo
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.AppColors
import com.synapse.social.studioasinc.feature.shared.theme.AppShapes
import com.synapse.social.studioasinc.feature.shared.theme.AppSpacing
import com.synapse.social.studioasinc.feature.shared.theme.AppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeySettingsScreen(
    viewModel: ApiKeySettingsViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val apiKeys by viewModel.apiKeys.collectAsState()
    val providerSettings by viewModel.providerSettings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var showAddKeyDialog by remember { mutableStateOf(false) }

    var customModelText by remember { mutableStateOf(providerSettings.customModel ?: "") }

    LaunchedEffect(providerSettings.customModel) {
        customModelText = providerSettings.customModel ?: ""
    }

    LaunchedEffect(customModelText) {
        kotlinx.coroutines.delay(500)
        viewModel.updateCustomModel(customModelText.takeIf { it.isNotBlank() })
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(Unit) {
        viewModel.loadSettings()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = AppColors.screenBackground,
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.settings_api_key_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.screenBackground,
                    scrolledContainerColor = AppColors.cardBackground
                ),
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = {
            error?.let { errorMessage ->
                Snackbar(
                    modifier = Modifier.padding(Spacing.space16),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text(stringResource(R.string.action_dismiss))
                        }
                    }
                ) {
                    Text(errorMessage)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = AppSpacing.screenPadding),
            contentPadding = PaddingValues(bottom = Spacing.space32),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sectionSpacing)
        ) {
            item {
                SettingsSection(title = stringResource(R.string.api_provider_settings)) {
                    var expanded by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.padding(AppSpacing.itemPadding)) {
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = viewModel.getProviderDisplayName(providerSettings.preferredProvider),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.label_preferred_provider)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                                shape = AppShapes.inputShape
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                viewModel.getAvailableProviders().forEach { provider ->
                                    DropdownMenuItem(
                                        text = { Text(viewModel.getProviderDisplayName(provider)) },
                                        onClick = {
                                            viewModel.updatePreferredProvider(provider)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.padding(AppSpacing.itemPadding)) {
                        OutlinedTextField(
                            value = customModelText,
                            onValueChange = { customModelText = it },
                            label = { Text(stringResource(R.string.api_custom_model)) },
                            placeholder = { Text(stringResource(R.string.api_custom_model_hint)) },
                            supportingText = { Text(stringResource(R.string.api_custom_model_support)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = AppShapes.inputShape,
                            singleLine = true,
                            trailingIcon = {
                                if (customModelText.isNotBlank()) {
                                    IconButton(onClick = {
                                        customModelText = ""
                                        viewModel.updateCustomModel(null)
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            }
                        )
                    }

                    SettingsToggleItem(
                        imageVector = Icons.Default.CloudSync,
                        title = stringResource(R.string.api_fallback_platform),
                        subtitle = stringResource(R.string.api_fallback_support),
                        checked = providerSettings.fallbackToPlatform,
                        onCheckedChange = { viewModel.updateFallbackSetting(it) },
                        position = SettingsItemPosition.Bottom
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.space12)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = Spacing.space8),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.api_your_keys),
                            style = AppTypography.sectionHeader,
                            color = AppColors.sectionTitle
                        )

                        TextButton(
                            onClick = { showAddKeyDialog = true }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add API Key")
                            Spacer(modifier = Modifier.width(Spacing.space8))
                            Text(stringResource(R.string.action_add_key))
                        }
                    }

                    if (apiKeys.isEmpty()) {
                        SettingsCard {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = Spacing.space32, horizontal = Spacing.space16),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    modifier = Modifier.size(Sizes.IconGiant),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(Spacing.space16))
                                Text(
                                    text = stringResource(R.string.api_no_keys),
                                    style = AppTypography.itemTitle,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(Spacing.space8))
                                Text(
                                    text = stringResource(R.string.api_add_own_keys),
                                    style = AppTypography.itemSubtitle,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(Spacing.space16))
                                Button(
                                    onClick = { showAddKeyDialog = true }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(Sizes.IconMedium))
                                    Spacer(modifier = Modifier.width(Spacing.space8))
                                    Text(stringResource(R.string.action_add_api_key))
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.itemSpacing)) {
                            apiKeys.forEachIndexed { index, apiKey ->
                                val position = when {
                                    apiKeys.size == 1 -> SettingsItemPosition.Single
                                    index == 0 -> SettingsItemPosition.Top
                                    index == apiKeys.lastIndex -> SettingsItemPosition.Bottom
                                    else -> SettingsItemPosition.Middle
                                }
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = position.getShape(),
                                    color = AppColors.cardBackground
                                ) {
                                    ApiKeyItem(
                                        apiKey = apiKey,
                                        onDelete = { viewModel.deleteApiKey(apiKey.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(Spacing.space16),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    if (showAddKeyDialog) {
        AddApiKeyDialog(
            availableProviders = viewModel.getAvailableProviders(),
            onDismiss = { showAddKeyDialog = false },
            onAdd = { provider, key, name, limit ->
                viewModel.addApiKey(provider, name ?: "", key, limit)
                showAddKeyDialog = false
            }
        )
    }
}
