package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.settings.ApiKeyInfo

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

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(Unit) {
        viewModel.loadSettings()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text("API Key Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = {
            error?.let { errorMessage ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = viewModel::clearError) {
                            Text("Dismiss")
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
                .padding(horizontal = SettingsSpacing.screenPadding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)
        ) {
            item {
                SettingsSection(title = "Provider Settings") {
                    var expanded by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.padding(SettingsSpacing.itemPadding)) {
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = viewModel.getProviderDisplayName(providerSettings.preferredProvider),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Preferred Provider") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                                shape = SettingsShapes.inputShape
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

                    SettingsDivider()

                    SettingsToggleItem(
                        imageVector = Icons.Default.CloudSync,
                        title = "Fallback to platform AI",
                        subtitle = "Use default models if your custom API key fails",
                        checked = providerSettings.fallbackToPlatform,
                        onCheckedChange = { viewModel.updateFallbackSetting(it) }
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Your API Keys",
                            style = SettingsTypography.sectionHeader,
                            color = SettingsColors.sectionTitle
                        )

                        TextButton(
                            onClick = { showAddKeyDialog = true }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add API Key")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Key")
                        }
                    }

                    if (apiKeys.isEmpty()) {
                        SettingsCard {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = SettingsColors.itemIcon.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No API Keys Configured",
                                    style = SettingsTypography.itemTitle,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Add your own API keys for unlimited AI usage and priority processing.",
                                    style = SettingsTypography.itemSubtitle,
                                    color = SettingsColors.itemIcon,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { showAddKeyDialog = true }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add API Key")
                                }
                            }
                        }
                    } else {
                        SettingsCard {
                            apiKeys.forEachIndexed { index, apiKey ->
                                ApiKeyItem(
                                    apiKey = apiKey,
                                    onDelete = { viewModel.deleteApiKey(apiKey.id) }
                                )
                                if (index < apiKeys.size - 1) {
                                    SettingsDivider()
                                }
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
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

@Composable
fun ApiKeyItem(
    apiKey: ApiKeyInfo,
    onDelete: (String) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(SettingsSpacing.itemPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = apiKey.keyName,
                style = SettingsTypography.itemTitle,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${apiKey.provider.uppercase()} • ${apiKey.usageCount}/${apiKey.usageLimit ?: "∞"} used",
                style = SettingsTypography.itemSubtitle,
                color = SettingsColors.itemIcon
            )
        }

        IconButton(
            onClick = { showDeleteConfirm = true }
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = SettingsColors.destructiveButton
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete API Key") },
            text = { Text("Are you sure you want to delete the API key '${apiKey.keyName}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(apiKey.id)
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = SettingsColors.destructiveButton)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddApiKeyDialog(
    availableProviders: List<String>,
    onDismiss: () -> Unit,
    onAdd: (String, String, String?, Int?) -> Unit
) {
    var selectedProvider by remember { mutableStateOf(availableProviders.firstOrNull() ?: "openai") }
    var apiKey by remember { mutableStateOf("") }
    var keyName by remember { mutableStateOf("") }
    var usageLimit by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
        ) {
            Text(
                text = "Add API Key",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedProvider.uppercase(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Provider") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    shape = SettingsShapes.inputShape
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    availableProviders.forEach { provider ->
                        DropdownMenuItem(
                            text = { Text(provider.uppercase()) },
                            onClick = {
                                selectedProvider = provider
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                supportingText = { Text("Your key is stored securely and never shared.") },
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showKey) "Hide" else "Show"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = SettingsShapes.inputShape
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = keyName,
                onValueChange = { keyName = it },
                label = { Text("Key Name (Optional)") },
                supportingText = { Text("e.g. 'Work Project'") },
                modifier = Modifier.fillMaxWidth(),
                shape = SettingsShapes.inputShape
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = usageLimit,
                onValueChange = { usageLimit = it.filter { char -> char.isDigit() } },
                label = { Text("Usage Limit (Optional)") },
                supportingText = { Text("Maximum number of tokens or requests allowed.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = SettingsShapes.inputShape
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = {
                        onAdd(
                            selectedProvider,
                            apiKey,
                            keyName.takeIf { it.isNotBlank() },
                            usageLimit.toIntOrNull()
                        )
                    },
                    enabled = apiKey.isNotBlank()
                ) {
                    Text("Save API Key")
                }
            }
        }
    }
}
