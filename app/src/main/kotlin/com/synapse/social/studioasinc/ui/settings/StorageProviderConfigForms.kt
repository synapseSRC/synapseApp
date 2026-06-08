package com.synapse.social.studioasinc.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import kotlinx.coroutines.delay

@Composable
internal fun ImgBBConfigContent(
    apiKey: String,
    onApiKeyChange: (String) -> Unit
) {
    var localApiKey by remember(apiKey) { mutableStateOf(apiKey) }
    var isSaving by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        StorageSecureTextField(
            value = localApiKey,
            onValueChange = { localApiKey = it },
            label = stringResource(R.string.label_api_key_simple)
        )
        HelpText(text = stringResource(R.string.settings_help_imgbb))

        SaveConnectButton(
            isLoading = isSaving,
            onClick = {
                isSaving = true
            },
            onComplete = {
                onApiKeyChange(localApiKey)
                isSaving = false
            }
        )
    }
}

@Composable
internal fun CloudinaryConfigContent(
    cloudName: String,
    apiKey: String,
    apiSecret: String,
    uploadPreset: String = "",
    onConfigChange: (String, String, String, String) -> Unit
) {
    var localCloudName by remember(cloudName) { mutableStateOf(cloudName) }
    var localApiKey by remember(apiKey) { mutableStateOf(apiKey) }
    var localApiSecret by remember(apiSecret) { mutableStateOf(apiSecret) }
    var localUploadPreset by remember(uploadPreset) { mutableStateOf(uploadPreset) }
    var isSaving by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        OutlinedTextField(
            value = localCloudName,
            onValueChange = { localCloudName = it },
            label = { Text(stringResource(R.string.label_cloud_name)) },
            trailingIcon = {
                if (localCloudName.isNotEmpty()) {
                    IconButton(onClick = { localCloudName = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = SettingsShapes.inputShape,
            singleLine = true
        )
        StorageSecureTextField(
            value = localUploadPreset,
            onValueChange = { localUploadPreset = it },
            label = stringResource(R.string.label_upload_preset)
        )
        StorageSecureTextField(
            value = localApiKey,
            onValueChange = { localApiKey = it },
            label = stringResource(R.string.label_api_key_optional)
        )
        StorageSecureTextField(
            value = localApiSecret,
            onValueChange = { localApiSecret = it },
            label = stringResource(R.string.label_api_secret_optional)
        )
        HelpText(text = stringResource(R.string.settings_help_cloudinary))

        SaveConnectButton(
            isLoading = isSaving,
            onClick = {
                isSaving = true
            },
            onComplete = {
                onConfigChange(localCloudName, localApiKey, localApiSecret, localUploadPreset)
                isSaving = false
            }
        )
    }
}

@Composable
internal fun SupabaseConfigContent(
    url: String,
    apiKey: String,
    bucketName: String,
    onConfigChange: (String, String, String) -> Unit
) {
    var localUrl by remember(url) { mutableStateOf(url) }
    var localApiKey by remember(apiKey) { mutableStateOf(apiKey) }
    var localBucketName by remember(bucketName) { mutableStateOf(bucketName) }
    var isSaving by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        OutlinedTextField(
            value = localUrl,
            onValueChange = { localUrl = it },
            label = { Text(stringResource(R.string.label_project_url)) },
            placeholder = { Text(stringResource(R.string.placeholder_project_url)) },
            trailingIcon = {
                if (localUrl.isNotEmpty()) {
                    IconButton(onClick = { localUrl = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear"
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = SettingsShapes.inputShape,
            singleLine = true
        )
        StorageSecureTextField(
            value = localApiKey,
            onValueChange = { localApiKey = it },
            label = stringResource(R.string.label_supabase_api_key)
        )
        OutlinedTextField(
            value = localBucketName,
            onValueChange = { localBucketName = it },
            label = { Text(stringResource(R.string.label_bucket_name)) },
            trailingIcon = {
                if (localBucketName.isNotEmpty()) {
                    IconButton(onClick = { localBucketName = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear"
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = SettingsShapes.inputShape,
            singleLine = true
        )
        HelpText(text = stringResource(R.string.settings_help_supabase))

        SaveConnectButton(
            isLoading = isSaving,
            onClick = {
                isSaving = true
            },
            onComplete = {
                onConfigChange(localUrl, localApiKey, localBucketName)
                isSaving = false
            }
        )
    }
}

@Composable
internal fun R2ConfigContent(
    accountId: String,
    accessKeyId: String,
    secretAccessKey: String,
    bucketName: String,
    onConfigChange: (String, String, String, String) -> Unit
) {
    var localAccountId by remember(accountId) { mutableStateOf(accountId) }
    var localAccessKeyId by remember(accessKeyId) { mutableStateOf(accessKeyId) }
    var localSecretAccessKey by remember(secretAccessKey) { mutableStateOf(secretAccessKey) }
    var localBucketName by remember(bucketName) { mutableStateOf(bucketName) }
    var isSaving by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        OutlinedTextField(
            value = localAccountId,
            onValueChange = { localAccountId = it },
            label = { Text(stringResource(R.string.label_account_id)) },
            trailingIcon = {
                if (localAccountId.isNotEmpty()) {
                    IconButton(onClick = { localAccountId = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear"
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = SettingsShapes.inputShape,
            singleLine = true
        )
        StorageSecureTextField(
            value = localAccessKeyId,
            onValueChange = { localAccessKeyId = it },
            label = stringResource(R.string.label_access_key_id)
        )
        StorageSecureTextField(
            value = localSecretAccessKey,
            onValueChange = { localSecretAccessKey = it },
            label = stringResource(R.string.label_secret_access_key)
        )
        OutlinedTextField(
            value = localBucketName,
            onValueChange = { localBucketName = it },
            label = { Text(stringResource(R.string.label_bucket_name)) },
            trailingIcon = {
                if (localBucketName.isNotEmpty()) {
                    IconButton(onClick = { localBucketName = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear"
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = SettingsShapes.inputShape,
            singleLine = true
        )
        HelpText(text = stringResource(R.string.settings_help_cloudflare))

        SaveConnectButton(
            isLoading = isSaving,
            onClick = {
                isSaving = true
            },
            onComplete = {
                onConfigChange(localAccountId, localAccessKeyId, localSecretAccessKey, localBucketName)
                isSaving = false
            }
        )
    }
}

@Composable
internal fun SaveConnectButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    onComplete: () -> Unit
) {
    var showSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            delay(800) // Mock validation/connection time
            showSuccess = true
            delay(1000)
            showSuccess = false
            onComplete()
        }
    }

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(Sizes.AvatarLarge),
        enabled = !isLoading,
        shape = SettingsShapes.itemShape
    ) {
        AnimatedContent(
            targetState = if (isLoading) (if (showSuccess) "success" else "loading") else "idle",
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "buttonContent"
        ) { state ->
            when (state) {
                "loading" -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Sizes.IconDefault),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = Sizes.BorderDefault
                        )
                        Spacer(modifier = Modifier.width(Spacing.Medium))
                        Text(stringResource(R.string.storage_connecting))
                    }
                }
                "success" -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(Sizes.IconDefault)
                        )
                        Spacer(modifier = Modifier.width(Spacing.Medium))
                        Text(stringResource(R.string.upload_complete))
                    }
                }
                else -> {
                    Text(
                        text = stringResource(R.string.storage_action_save_connect),
                        style = SettingsTypography.buttonText
                    )
                }
            }
        }
    }
}

@Composable
internal fun HelpText(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Spacing.SmallMedium))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(Spacing.Medium)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Help,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Spacing.MediumLarge)
        )
        Spacer(modifier = Modifier.width(Spacing.Medium))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun StorageSecureTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            Row {
                if (value.isNotEmpty()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear"
                        )
                    }
                }
                val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = icon,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            }
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Key,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsShapes.inputShape,
        singleLine = true
    )
}
