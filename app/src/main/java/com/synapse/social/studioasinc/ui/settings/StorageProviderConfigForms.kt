package com.synapse.social.studioasinc.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
internal fun ImgBBConfigContent(
    apiKey: String,
    onApiKeyChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        StorageSecureTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            label = "API Key"
        )
        HelpText(text = "Get your free API key from api.imgbb.com")
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
            shape = RoundedCornerShape(Spacing.SmallMedium),
            singleLine = true
        )
        StorageSecureTextField(
            value = localUploadPreset,
            onValueChange = { localUploadPreset = it },
            label = "Upload Preset (unsigned)"
        )
        StorageSecureTextField(
            value = localApiKey,
            onValueChange = { localApiKey = it },
            label = "API Key (signed, optional)"
        )
        StorageSecureTextField(
            value = localApiSecret,
            onValueChange = { localApiSecret = it },
            label = "API Secret (signed, optional)"
        )
        HelpText(text = "Use an Upload Preset for unsigned uploads, or API Key + Secret for signed uploads. Find these in your Cloudinary dashboard.")
        Button(
            onClick = { onConfigChange(localCloudName, localApiKey, localApiSecret, localUploadPreset) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.action_save))
        }
    }
}

@Composable
internal fun SupabaseConfigContent(
    url: String,
    apiKey: String,
    bucketName: String,
    onConfigChange: (String, String, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        OutlinedTextField(
            value = url,
            onValueChange = { newVal -> onConfigChange(newVal, apiKey, bucketName) },
            label = { Text(stringResource(R.string.label_project_url)) },
            placeholder = { Text(stringResource(R.string.placeholder_project_url)) },
            trailingIcon = {
                if (url.isNotEmpty()) {
                    IconButton(onClick = { onConfigChange("", apiKey, bucketName) }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear"
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Spacing.SmallMedium),
            singleLine = true
        )
        StorageSecureTextField(
            value = apiKey,
            onValueChange = { newVal -> onConfigChange(url, newVal, bucketName) },
            label = "Service Role / API Key"
        )
        OutlinedTextField(
            value = bucketName,
            onValueChange = { newVal -> onConfigChange(url, apiKey, newVal) },
            label = { Text(stringResource(R.string.label_bucket_name)) },
            trailingIcon = {
                if (bucketName.isNotEmpty()) {
                    IconButton(onClick = { onConfigChange(url, apiKey, "") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear"
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Spacing.SmallMedium),
            singleLine = true
        )
        HelpText(text = "Create a bucket in Supabase Storage and ensure policies allow read/write operations")
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
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        OutlinedTextField(
            value = accountId,
            onValueChange = { newVal -> onConfigChange(newVal, accessKeyId, secretAccessKey, bucketName) },
            label = { Text(stringResource(R.string.label_account_id)) },
            trailingIcon = {
                if (accountId.isNotEmpty()) {
                    IconButton(onClick = { onConfigChange("", accessKeyId, secretAccessKey, bucketName) }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear"
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Spacing.SmallMedium),
            singleLine = true
        )
        StorageSecureTextField(
            value = accessKeyId,
            onValueChange = { newVal -> onConfigChange(accountId, newVal, secretAccessKey, bucketName) },
            label = "Access Key ID"
        )
        StorageSecureTextField(
            value = secretAccessKey,
            onValueChange = { newVal -> onConfigChange(accountId, accessKeyId, newVal, bucketName) },
            label = "Secret Access Key"
        )
        OutlinedTextField(
            value = bucketName,
            onValueChange = { newVal -> onConfigChange(accountId, accessKeyId, secretAccessKey, newVal) },
            label = { Text(stringResource(R.string.label_bucket_name)) },
            trailingIcon = {
                if (bucketName.isNotEmpty()) {
                    IconButton(onClick = { onConfigChange(accountId, accessKeyId, secretAccessKey, "") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear"
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Spacing.SmallMedium),
            singleLine = true
        )
        HelpText(text = "Create an R2 bucket in your Cloudflare dashboard and generate API tokens")
    }
}

@Composable
internal fun HelpText(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Spacing.Small))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(Spacing.SmallMedium)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Help,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Spacing.Medium)
        )
        Spacer(modifier = Modifier.width(Spacing.Small))
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
        shape = RoundedCornerShape(Spacing.SmallMedium),
        singleLine = true
    )
}
