package com.synapse.social.studioasinc.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import com.synapse.social.studioasinc.shared.domain.model.StorageProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageProviderScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val storageConfig by viewModel.storageConfig.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Providers") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {


            StorageSection(title = "Upload Preferences") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "High Quality Uploads",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Upload original quality (uses more data)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = !storageConfig.compressImages,
                        onCheckedChange = { viewModel.updateCompression(!it) }
                    )
                }
            }

            StorageSection(title = "Provider Selection") {
                ProviderSelectionItem(
                    title = "Photos",
                    icon = Icons.Default.Image,
                    selectedProvider = storageConfig.photoProvider.toDisplayName(),
                    options = listOf("Default", "ImgBB", "Cloudinary", "Supabase", "Cloudflare R2"),
                    onSelect = { viewModel.updatePhotoProvider(it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                ProviderSelectionItem(
                    title = "Videos",
                    icon = Icons.Default.Videocam,
                    selectedProvider = storageConfig.videoProvider.toDisplayName(),
                    options = listOf("Default", "Cloudinary", "Supabase", "Cloudflare R2"),
                    onSelect = { viewModel.updateVideoProvider(it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                ProviderSelectionItem(
                    title = "Other Files",
                    icon = Icons.Default.CloudUpload,
                    selectedProvider = storageConfig.otherProvider.toDisplayName(),
                    options = listOf("Default", "Supabase", "Cloudflare R2", "Cloudinary"),
                    onSelect = { viewModel.updateOtherProvider(it) }
                )
            }


            Text(
                text = "Provider Configuration",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )


            ProviderConfigCard(
                title = "ImgBB",
                isConfigured = storageConfig.isProviderConfigured(StorageProvider.IMGBB),
                isExpanded = false
            ) {
                ImgBBConfigContent(
                    apiKey = storageConfig.imgBBKey,
                    onApiKeyChange = { viewModel.updateImgBBConfig(it) }
                )
            }


            ProviderConfigCard(
                title = "Cloudinary",
                isConfigured = storageConfig.isProviderConfigured(StorageProvider.CLOUDINARY),
                isExpanded = false
            ) {
                CloudinaryConfigContent(
                    cloudName = storageConfig.cloudinaryCloudName,
                    apiKey = storageConfig.cloudinaryApiKey,
                    apiSecret = storageConfig.cloudinaryApiSecret,
                    onConfigChange = { name, key, secret ->
                        viewModel.updateCloudinaryConfig(name, key, secret)
                    }
                )
            }


            ProviderConfigCard(
                title = "Supabase Storage",
                isConfigured = storageConfig.isProviderConfigured(StorageProvider.SUPABASE),
                isExpanded = false
            ) {
                SupabaseConfigContent(
                    url = storageConfig.supabaseUrl,
                    apiKey = storageConfig.supabaseKey,
                    bucketName = storageConfig.supabaseBucket,
                    onConfigChange = { url, key, bucket ->
                        viewModel.updateSupabaseConfig(url, key, bucket)
                    }
                )
            }


            ProviderConfigCard(
                title = "Cloudflare R2",
                isConfigured = storageConfig.isProviderConfigured(StorageProvider.CLOUDFLARE_R2),
                isExpanded = false
            ) {
                R2ConfigContent(
                    accountId = storageConfig.r2AccountId,
                    accessKeyId = storageConfig.r2AccessKeyId,
                    secretAccessKey = storageConfig.r2SecretAccessKey,
                    bucketName = storageConfig.r2BucketName,
                    onConfigChange = { accId, accKey, secret, bucket ->
                        viewModel.updateR2Config(accId, accKey, secret, bucket)
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun StorageProvider.toDisplayName(): String {
    return when (this) {
        StorageProvider.DEFAULT -> "Default"
        StorageProvider.IMGBB -> "ImgBB"
        StorageProvider.CLOUDINARY -> "Cloudinary"
        StorageProvider.SUPABASE -> "Supabase"
        StorageProvider.CLOUDFLARE_R2 -> "Cloudflare R2"
    }
}

@Composable
private fun StorageSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        content()
    }
}

@Composable
private fun ProviderSelectionItem(
    title: String,
    icon: ImageVector,
    selectedProvider: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = title, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = selectedProvider,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandMore else Icons.Default.ExpandMore,
                contentDescription = "Select",
                modifier = Modifier.rotate(if (expanded) 180f else 0f)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .padding(start = 40.dp, top = 8.dp)
                    .fillMaxWidth()
            ) {
                options.forEach { option ->
                    val isSelected = option == selectedProvider
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(option)
                                expanded = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderConfigCard(
    title: String,
    isConfigured: Boolean,
    isExpanded: Boolean,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(isExpanded) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp)
            ) {

                Column(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isConfigured) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isConfigured) Icons.Default.CheckCircle else Icons.Outlined.Key,
                        contentDescription = null,
                        tint = if (isConfigured) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (isConfigured) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Configured",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = if (isConfigured) "Ready to use" else "Configuration required",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isConfigured) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }


                val rotationAngle by animateFloatAsState(
                    targetValue = if (expanded) 180f else 0f,
                    animationSpec = tween(300, easing = EaseOutCubic),
                    label = "rotation"
                )

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.rotate(rotationAngle)
                    )
                }
            }


            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(),
                exit = shrinkVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeOut()
            ) {
                Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    content()
                }
            }
        }
    }
}

@Composable
private fun ImgBBConfigContent(
    apiKey: String,
    onApiKeyChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StorageSecureTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            label = "API Key"
        )
        HelpText(text = "Get your free API key from api.imgbb.com")
    }
}

@Composable
private fun CloudinaryConfigContent(
    cloudName: String,
    apiKey: String,
    apiSecret: String,
    onConfigChange: (String, String, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = cloudName,
            onValueChange = { newName -> onConfigChange(newName, apiKey, apiSecret) },
            label = { Text("Cloud Name") },
            trailingIcon = {
                if (cloudName.isNotEmpty()) {
                    IconButton(onClick = { onConfigChange("", apiKey, apiSecret) }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear"
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        StorageSecureTextField(
            value = apiKey,
            onValueChange = { newKey -> onConfigChange(cloudName, newKey, apiSecret) },
            label = "API Key"
        )
        StorageSecureTextField(
            value = apiSecret,
            onValueChange = { newSecret -> onConfigChange(cloudName, apiKey, newSecret) },
            label = "API Secret"
        )
        HelpText(text = "Find these in your Cloudinary dashboard under Settings > Access Keys")
    }
}

@Composable
private fun SupabaseConfigContent(
    url: String,
    apiKey: String,
    bucketName: String,
    onConfigChange: (String, String, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = url,
            onValueChange = { newVal -> onConfigChange(newVal, apiKey, bucketName) },
            label = { Text("Project URL") },
            placeholder = { Text("https://your-project.supabase.co") },
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
            shape = RoundedCornerShape(12.dp),
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
            label = { Text("Bucket Name") },
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
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        HelpText(text = "Create a bucket in Supabase Storage and ensure policies allow read/write operations")
    }
}

@Composable
private fun R2ConfigContent(
    accountId: String,
    accessKeyId: String,
    secretAccessKey: String,
    bucketName: String,
    onConfigChange: (String, String, String, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = accountId,
            onValueChange = { newVal -> onConfigChange(newVal, accessKeyId, secretAccessKey, bucketName) },
            label = { Text("Account ID") },
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
            shape = RoundedCornerShape(12.dp),
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
            label = { Text("Bucket Name") },
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
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        HelpText(text = "Create an R2 bucket in your Cloudflare dashboard and generate API tokens")
    }
}

@Composable
private fun HelpText(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Help,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StorageSecureTextField(
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
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

private val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
