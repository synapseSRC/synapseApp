package com.synapse.social.studioasinc.ui.settings

import androidx.compose.animation.AnimatedVisibility
import com.synapse.social.studioasinc.R
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
import androidx.compose.ui.res.stringResource
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
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
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
                title = { Text(stringResource(R.string.settings_storage_providers_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(Spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.Large)
        ) {


            StorageSection(title = "Upload Preferences") {
                val isHighQuality = !storageConfig.compressImages
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = isHighQuality,
                            role = Role.Switch,
                            onValueChange = { viewModel.updateCompression(!it) }
                        )
                        .padding(vertical = Spacing.Small),
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
                        checked = isHighQuality,
                        onCheckedChange = null
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
                modifier = Modifier.padding(top = Spacing.Small)
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
                    uploadPreset = storageConfig.cloudinaryUploadPreset,
                    onConfigChange = { name, key, secret, preset ->
                        viewModel.updateCloudinaryConfig(name, key, secret, preset)
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

            Spacer(modifier = Modifier.height(Spacing.ExtraLarge))
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
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Spacing.Medium))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(Spacing.Medium)
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
                .padding(vertical = Spacing.Small)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(Spacing.Medium))
                Column {
                    Text(text = title, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = selectedProvider,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val rotationAngle by animateFloatAsState(
                targetValue = if (expanded) 180f else 0f,
                animationSpec = tween(300, easing = EaseOutCubic),
                label = "rotation"
            )

            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse options" else "Expand options",
                modifier = Modifier.rotate(rotationAngle)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = ExpandEnterAnimation,
            exit = ExpandExitAnimation
        ) {
            Column(
                modifier = Modifier
                    .padding(start = Spacing.ExtraLarge, top = Spacing.Small)
                    .fillMaxWidth()
            ) {
                options.forEach { option ->
                    val isSelected = option == selectedProvider
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isSelected,
                                onClick = {
                                    onSelect(option)
                                    expanded = false
                                },
                                role = Role.RadioButton
                            )
                            .padding(vertical = Spacing.SmallMedium),
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
                                modifier = Modifier.size(Spacing.Medium)
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
        shape = RoundedCornerShape(Spacing.Medium),
        shadowElevation = Spacing.Tiny,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(Spacing.Medium)
            ) {

                Column(
                    modifier = Modifier
                        .size(Spacing.ExtraLarge)
                        .clip(RoundedCornerShape(Spacing.SmallMedium))
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
                        modifier = Modifier.size(Spacing.Large)
                    )
                }

                Spacer(modifier = Modifier.width(Spacing.Medium))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (isConfigured) {
                            Spacer(modifier = Modifier.width(Spacing.Small))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Configured",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(Spacing.Medium)
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
                        contentDescription = if (expanded) "Collapse $title configuration" else "Expand $title configuration",
                        modifier = Modifier.rotate(rotationAngle)
                    )
                }
            }


            AnimatedVisibility(
                visible = expanded,
                enter = ExpandEnterAnimation,
                exit = ExpandExitAnimation
            ) {
                Column(modifier = Modifier.padding(start = Spacing.Large, end = Spacing.Large, bottom = Spacing.Large)) {
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = Spacing.Medium),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    content()
                }
            }
        }
    }
}

private val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)

private val ExpandEnterAnimation = expandVertically(
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
) + fadeIn()

private val ExpandExitAnimation = shrinkVertically(
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
) + fadeOut()
