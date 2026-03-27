package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

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
                .padding(horizontal = Spacing.Large, vertical = Spacing.Medium)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + Spacing.Medium)
        ) {
            Text(
                text = stringResource(R.string.api_add_key_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = Spacing.Medium)
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
                    label = { Text(stringResource(R.string.label_provider)) },
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

            Spacer(modifier = Modifier.height(Spacing.Medium))

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text(stringResource(R.string.label_api_key)) },
                supportingText = { Text(stringResource(R.string.api_key_supporting_text)) },
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

            Spacer(modifier = Modifier.height(Spacing.Medium))

            OutlinedTextField(
                value = keyName,
                onValueChange = { keyName = it },
                label = { Text(stringResource(R.string.label_key_name_optional)) },
                supportingText = { Text(stringResource(R.string.key_name_example)) },
                modifier = Modifier.fillMaxWidth(),
                shape = SettingsShapes.inputShape
            )

            Spacer(modifier = Modifier.height(Spacing.Medium))

            OutlinedTextField(
                value = usageLimit,
                onValueChange = { usageLimit = it.filter { char -> char.isDigit() } },
                label = { Text(stringResource(R.string.label_usage_limit_optional)) },
                supportingText = { Text(stringResource(R.string.usage_limit_supporting_text)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = SettingsShapes.inputShape
            )

            Spacer(modifier = Modifier.height(Spacing.ExtraLarge))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.settings_cancel))
                }
                Spacer(modifier = Modifier.width(Spacing.Medium))
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
                    Text(stringResource(R.string.action_save_api_key))
                }
            }
        }
    }
}
