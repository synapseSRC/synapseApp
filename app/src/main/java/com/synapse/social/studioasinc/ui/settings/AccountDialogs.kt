package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing






@Composable
fun ChangeEmailDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    isLoading: Boolean = false,
    error: String? = null
) {
    var newEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = "Change Email",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {
                Text(
                    text = "Enter your new email address and current password to verify the change.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )


                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = { Text(stringResource(R.string.label_new_email)) },
                    placeholder = { Text(stringResource(R.string.placeholder_email_example)) },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = SettingsShapes.inputShape,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    isError = error != null && error.contains("email", ignoreCase = true)
                )


                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.label_current_password)) },
                    placeholder = { Text(stringResource(R.string.placeholder_enter_password)) },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = SettingsShapes.inputShape,
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                                         else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (newEmail.isNotBlank() && password.isNotBlank()) {
                                onConfirm(newEmail, password)
                            }
                        }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible) "Hide password"
                                                   else "Show password"
                            )
                        }
                    },
                    isError = error != null && error.contains("password", ignoreCase = true)
                )


                if (error != null) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = { onConfirm(newEmail, password) },
                enabled = !isLoading && newEmail.isNotBlank() && password.isNotBlank(),
                shape = SettingsShapes.itemShape
            ) {
                if (isLoading) {
                    ExpressiveLoadingIndicator(
                        modifier = Modifier.size(Sizes.IconSemiMedium)
                    )
                    Spacer(modifier = Modifier.width(Spacing.Small))
                }
                Text(stringResource(R.string.action_change_email))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
        shape = SettingsShapes.cardShape
    )
}



@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit,
    isLoading: Boolean = false,
    error: String? = null,
    calculatePasswordStrength: (String) -> Int = { 0 }
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val passwordStrength = remember(newPassword) { calculatePasswordStrength(newPassword) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = "Change Password",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {
                Text(
                    text = "Enter your current password and choose a new password. Your new password must be at least 8 characters long.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )


                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text(stringResource(R.string.label_current_password)) },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = SettingsShapes.inputShape,
                    visualTransformation = if (currentPasswordVisible) VisualTransformation.None
                                         else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                            Icon(
                                imageVector = if (currentPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (currentPasswordVisible) "Hide password"
                                                   else "Show password"
                            )
                        }
                    }
                )


                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.label_new_password)) },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = SettingsShapes.inputShape,
                    visualTransformation = if (newPasswordVisible) VisualTransformation.None
                                         else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                            Icon(
                                imageVector = if (newPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (newPasswordVisible) "Hide password"
                                                   else "Show password"
                            )
                        }
                    },
                    isError = newPassword.isNotEmpty() && newPassword.length < 8
                )


                if (newPassword.isNotEmpty()) {
                    PasswordStrengthIndicator(strength = passwordStrength)
                }


                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(R.string.label_confirm_new_password)) },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = SettingsShapes.inputShape,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None
                                         else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (currentPassword.isNotBlank() && newPassword.isNotBlank() &&
                                confirmPassword.isNotBlank()) {
                                onConfirm(currentPassword, newPassword, confirmPassword)
                            }
                        }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (confirmPasswordVisible) "Hide password"
                                                   else "Show password"
                            )
                        }
                    },
                    isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword
                )


                if (error != null) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = { onConfirm(currentPassword, newPassword, confirmPassword) },
                enabled = !isLoading && currentPassword.isNotBlank() &&
                         newPassword.length >= 8 && confirmPassword == newPassword,
                shape = SettingsShapes.itemShape
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Sizes.IconSemiMedium),
                        strokeWidth = Sizes.BorderDefault
                    )
                    Spacer(modifier = Modifier.width(Spacing.Small))
                }
                Text(stringResource(R.string.action_change_password))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.settings_cancel))
            }
        },
        shape = SettingsShapes.cardShape
    )
}



@Composable
private fun PasswordStrengthIndicator(strength: Int) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.Small)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(Spacing.ExtraSmall)
                        .then(
                            if (index < strength) {
                                Modifier.background(
                                    color = when (strength) {
                                        1 -> MaterialTheme.colorScheme.error
                                        2 -> MaterialTheme.colorScheme.tertiary
                                        3 -> MaterialTheme.colorScheme.primary
                                        4 -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    shape = SettingsShapes.chipShape
                                )
                            } else {
                                Modifier.background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = SettingsShapes.chipShape
                                )
                            }
                        )
                )
            }
        }

        Text(
            text = when (strength) {
                0 -> "Enter a password"
                1 -> "Weak password"
                2 -> "Fair password"
                3 -> "Good password"
                4 -> "Strong password"
                else -> ""
            },
            style = MaterialTheme.typography.bodySmall,
            color = when (strength) {
                0 -> MaterialTheme.colorScheme.onSurfaceVariant
                1 -> MaterialTheme.colorScheme.error
                2 -> MaterialTheme.colorScheme.tertiary
                3, 4 -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}



@Composable
fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    isLoading: Boolean = false,
    error: String? = null
) {
    var confirmationText by remember { mutableStateOf("") }
    val requiredText = AccountSettingsViewModel.DELETE_ACCOUNT_CONFIRMATION

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(Sizes.IconGiant)
            )
        },
        title = {
            Text(
                text = "Delete Account",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {
                Text(
                    text = "This action cannot be undone. All your data, including posts, messages, and profile information will be permanently deleted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "To confirm, please type:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    shape = SettingsShapes.inputShape
                ) {
                    Text(
                        text = requiredText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(Spacing.SmallMedium)
                    )
                }

                OutlinedTextField(
                    value = confirmationText,
                    onValueChange = { confirmationText = it },
                    label = { Text(stringResource(R.string.label_confirmation)) },
                    placeholder = { Text(stringResource(R.string.placeholder_type_phrase)) },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = SettingsShapes.inputShape,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (confirmationText == requiredText) {
                                onConfirm(confirmationText)
                            }
                        }
                    ),
                    isError = confirmationText.isNotEmpty() && confirmationText != requiredText,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.error,
                        errorBorderColor = MaterialTheme.colorScheme.error
                    )
                )


                if (error != null) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(confirmationText) },
                enabled = !isLoading && confirmationText == requiredText,
                shape = SettingsShapes.itemShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                if (isLoading) {
                    ExpressiveLoadingIndicator(
                        modifier = Modifier.size(Sizes.IconSemiMedium),
                        color = MaterialTheme.colorScheme.onError
                    )
                    Spacer(modifier = Modifier.width(Spacing.Small))
                }
                Text(stringResource(R.string.action_delete_account))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.settings_cancel))
            }
        },
        shape = SettingsShapes.cardShape
    )
}
