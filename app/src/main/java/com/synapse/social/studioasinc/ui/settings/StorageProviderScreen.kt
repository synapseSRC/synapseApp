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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Snackbar
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.launch
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


@Composable

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
fun StorageProviderScreen(
    navController: NavController,
    viewModel: StorageProviderViewModel = hiltViewModel()
) {
    val storageConfig by viewModel.storageConfig.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.error, uiState.successMessage) {
        val message = uiState.error ?: uiState.successMessage
        if (message != null) {
            keyboardController?.hide()
            focusManager.clearFocus()
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { data.dismiss() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(data.visuals.message)
                }
            }
        },
        topBar = {
            MediumTopAppBar(
                title = { Text("Storage Providers") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = SettingsSpacing.screenPadding,
                end = SettingsSpacing.screenPadding,
                top = SettingsSpacing.screenPadding,
                bottom = SettingsSpacing.screenPadding * 2
            ),
            verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)
        ) {
            item {


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

            }
            item {
                StorageSection(title = "Provider Selection") {
                ProviderSelectionItem(
                    title = "Photos",
                    icon = Icons.Default.Image,
                    selectedProvider = storageConfig.photoProvider.toDisplayName(),
                    options = listOf("Default", "ImgBB", "Cloudinary", "Supabase", "Cloudflare R2"),
                    onSelect = { viewModel.updatePhotoProvider(it) }
                )

                SettingsDivider()

                ProviderSelectionItem(
                    title = "Videos",
                    icon = Icons.Default.Videocam,
                    selectedProvider = storageConfig.videoProvider.toDisplayName(),
                    options = listOf("Default", "Cloudinary", "Supabase", "Cloudflare R2"),
                    onSelect = { viewModel.updateVideoProvider(it) }
                )

                SettingsDivider()

                ProviderSelectionItem(
                    title = "Other Files",
                    icon = Icons.Default.CloudUpload,
                    selectedProvider = storageConfig.otherProvider.toDisplayName(),
                    options = listOf("Default", "Supabase", "Cloudflare R2", "Cloudinary"),
                    onSelect = { viewModel.updateOtherProvider(it) }
                )
            }


            }
            item {
                Text(
                    text = "Provider Configuration",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )


            }
            item {
                ProviderConfigCard(
                    title = "ImgBB",
                isConfigured = storageConfig.isProviderConfigured(StorageProvider.IMGBB),
                isExpanded = false
            ) {
                ImgBBConfigContent(
                    apiKey = storageConfig.imgBBKey,
                    onApiKeyChange = { viewModel.updateImgBBConfig(it) },
                    onTestConnection = { viewModel.testImgBBConnection(it) },
                    isLoading = uiState.isLoading
                )
            }


            }
            item {
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
                    },
                    onTestConnection = { name, key, secret ->
                        viewModel.testCloudinaryConnection(name, key, secret)
                    },
                    isLoading = uiState.isLoading
                )
            }


            }
            item {
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
                    },
                    onTestConnection = { url, key, bucket ->
                        viewModel.testSupabaseConnection(url, key, bucket)
                    },
                    isLoading = uiState.isLoading
                )
            }


            }
            item {
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
                    },
                    onTestConnection = { accId, accKey, secret, bucket ->
                        viewModel.testR2Connection(accId, accKey, secret, bucket)
                    },
                    isLoading = uiState.isLoading
                )
            }

            }
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
    SettingsSection(title = title) {
        Column(
            verticalArrangement = Arrangement.spacedBy(SettingsSpacing.itemSpacing),
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
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

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SettingsColors.cardBackground
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(
                        horizontal = SettingsSpacing.itemHorizontalPadding,
                        vertical = SettingsSpacing.itemVerticalPadding
                    )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = SettingsColors.itemIcon,
                        modifier = Modifier.size(SettingsSpacing.iconSize)
                    )
                    Spacer(modifier = Modifier.width(SettingsSpacing.iconTextSpacing))
                    Column {
                        Text(text = title, style = SettingsTypography.itemTitle, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedProvider,
                            style = SettingsTypography.itemSubtitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = "Select",
                    tint = SettingsColors.chevronIcon,
                    modifier = Modifier.rotate(if (expanded) 180f else 0f)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .padding(start = 56.dp, end = SettingsSpacing.itemHorizontalPadding)
                        .fillMaxWidth()
                ) {
                    options.forEach { option ->
                        val isSelected = option == selectedProvider
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(SettingsShapes.itemShape)
                                .clickable {
                                    onSelect(option)
                                    expanded = false
                                }
                                .padding(vertical = SettingsSpacing.itemVerticalPadding),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option,
                                style = SettingsTypography.itemSubtitle,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
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
        color = SettingsColors.cardBackground,
        shape = SettingsShapes.cardShape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(
                        horizontal = SettingsSpacing.itemHorizontalPadding,
                        vertical = SettingsSpacing.itemVerticalPadding
                    )
            ) {

                Column(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(SettingsShapes.itemShape)
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
                            SettingsColors.itemIcon
                        },
                        modifier = Modifier.size(SettingsSpacing.iconSize)
                    )
                }

                Spacer(modifier = Modifier.width(SettingsSpacing.iconTextSpacing))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = SettingsTypography.itemTitle
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
                        style = SettingsTypography.itemSubtitle,
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
                        tint = SettingsColors.chevronIcon,
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
                Column(modifier = Modifier.padding(
                    start = SettingsSpacing.itemHorizontalPadding,
                    end = SettingsSpacing.itemHorizontalPadding,
                    bottom = SettingsSpacing.itemVerticalPadding * 2
                )) {
                    SettingsDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    content()
                }
            }
        }
    }
}

@Composable
private fun ImgBBConfigContent(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    onTestConnection: (String) -> Unit,
    isLoading: Boolean
) {
    var key by remember { mutableStateOf(apiKey) }

    Column(verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)) {
        StorageSecureTextField(
            value = key,
            onValueChange = { key = it },
            label = "API Key",
            enabled = !isLoading
        )
        HelpText(text = "Get your free API key from api.imgbb.com")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)
        ) {
            OutlinedButton(
                onClick = { onTestConnection(key) },
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("Test Connection")
            }
            Button(
                onClick = { onApiKeyChange(key) },
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun CloudinaryConfigContent(
    cloudName: String,
    apiKey: String,
    apiSecret: String,
    onConfigChange: (String, String, String) -> Unit,
    onTestConnection: (String, String, String) -> Unit,
    isLoading: Boolean
) {
    var name by remember { mutableStateOf(cloudName) }
    var key by remember { mutableStateOf(apiKey) }
    var secret by remember { mutableStateOf(apiSecret) }

    Column(verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Cloud Name") },
            enabled = !isLoading,
            trailingIcon = {
                if (name.isNotEmpty() && !isLoading) {
                    IconButton(onClick = { name = "" }) {
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
            value = key,
            onValueChange = { key = it },
            label = "API Key",
            enabled = !isLoading
        )
        StorageSecureTextField(
            value = secret,
            onValueChange = { secret = it },
            label = "API Secret",
            enabled = !isLoading
        )
        HelpText(text = "Find these in your Cloudinary dashboard under Settings > Access Keys")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)
        ) {
            OutlinedButton(
                onClick = { onTestConnection(name, key, secret) },
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("Test Connection")
            }
            Button(
                onClick = { onConfigChange(name, key, secret) },
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun SupabaseConfigContent(
    url: String,
    apiKey: String,
    bucketName: String,
    onConfigChange: (String, String, String) -> Unit,
    onTestConnection: (String, String, String) -> Unit,
    isLoading: Boolean
) {
    var currentUrl by remember { mutableStateOf(url) }
    var key by remember { mutableStateOf(apiKey) }
    var bucket by remember { mutableStateOf(bucketName) }

    Column(verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)) {
        OutlinedTextField(
            value = currentUrl,
            onValueChange = { currentUrl = it },
            label = { Text("Project URL") },
            placeholder = { Text("https://your-project.supabase.co") },
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            trailingIcon = {
                if (currentUrl.isNotEmpty() && !isLoading) {
                    IconButton(onClick = { currentUrl = "" }) {
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
            value = key,
            onValueChange = { key = it },
            label = "Service Role / API Key",
            enabled = !isLoading
        )
        OutlinedTextField(
            value = bucket,
            onValueChange = { bucket = it },
            label = { Text("Bucket Name") },
            enabled = !isLoading,
            trailingIcon = {
                if (bucket.isNotEmpty() && !isLoading) {
                    IconButton(onClick = { bucket = "" }) {
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
        HelpText(text = "Create a bucket in Supabase Storage and ensure policies allow read/write operations")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)
        ) {
            OutlinedButton(
                onClick = { onTestConnection(currentUrl, key, bucket) },
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("Test Connection")
            }
            Button(
                onClick = { onConfigChange(currentUrl, key, bucket) },
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun R2ConfigContent(
    accountId: String,
    accessKeyId: String,
    secretAccessKey: String,
    bucketName: String,
    onConfigChange: (String, String, String, String) -> Unit,
    onTestConnection: (String, String, String, String) -> Unit,
    isLoading: Boolean
) {
    var accId by remember { mutableStateOf(accountId) }
    var accKey by remember { mutableStateOf(accessKeyId) }
    var secret by remember { mutableStateOf(secretAccessKey) }
    var bucket by remember { mutableStateOf(bucketName) }

    Column(verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)) {
        OutlinedTextField(
            value = accId,
            onValueChange = { accId = it },
            label = { Text("Account ID") },
            enabled = !isLoading,
            trailingIcon = {
                if (accId.isNotEmpty() && !isLoading) {
                    IconButton(onClick = { accId = "" }) {
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
            value = accKey,
            onValueChange = { accKey = it },
            label = "Access Key ID",
            enabled = !isLoading
        )
        StorageSecureTextField(
            value = secret,
            onValueChange = { secret = it },
            label = "Secret Access Key",
            enabled = !isLoading
        )
        OutlinedTextField(
            value = bucket,
            onValueChange = { bucket = it },
            label = { Text("Bucket Name") },
            enabled = !isLoading,
            trailingIcon = {
                if (bucket.isNotEmpty() && !isLoading) {
                    IconButton(onClick = { bucket = "" }) {
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
        HelpText(text = "Create an R2 bucket in your Cloudflare dashboard and generate API tokens")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)
        ) {
            OutlinedButton(
                onClick = { onTestConnection(accId, accKey, secret, bucket) },
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("Test Connection")
            }
            Button(
                onClick = { onConfigChange(accId, accKey, secret, bucket) },
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun HelpText(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clip(SettingsShapes.chipShape)
            .background(SettingsColors.cardBackground)
            .padding(SettingsSpacing.itemPadding)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Help,
            contentDescription = null,
            tint = SettingsColors.itemIcon,
            modifier = Modifier.size(SettingsSpacing.iconSize * 0.75f)
        )
        Spacer(modifier = Modifier.width(SettingsSpacing.iconTextSpacing / 2))
        Text(
            text = text,
            style = SettingsTypography.itemSubtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StorageSecureTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean = true
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
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
