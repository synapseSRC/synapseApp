package com.synapse.social.studioasinc.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Spacing
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.shared.domain.model.StorageConfig
import com.synapse.social.studioasinc.shared.domain.model.StorageProvider
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageProviderScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val storageConfig by viewModel.storageConfig.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_storage_providers_title),
                        style = SettingsTypography.screenTitle
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = SettingsColors.screenBackground,
                    scrolledContainerColor = SettingsColors.screenBackground
                )
            )
        },
        containerColor = SettingsColors.screenBackground
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = SettingsColors.screenBackground,
                divider = {}
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text(stringResource(R.string.storage_tab_assign)) }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text(stringResource(R.string.storage_tab_setup)) }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> AssignProvidersTab(storageConfig, viewModel)
                    1 -> ProviderSetupTab(storageConfig, viewModel)
                }
            }
        }
    }
}

@Composable
private fun AssignProvidersTab(
    storageConfig: StorageConfig,
    viewModel: SettingsViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {
        item {
            MediaProviderCard(
                title = stringResource(R.string.storage_provider_photos),
                icon = Icons.Default.Image,
                selectedProvider = storageConfig.photoProvider,
                isConfigured = storageConfig.isProviderConfigured(storageConfig.photoProvider),
                onProviderSelect = { viewModel.updatePhotoProvider(it) }
            )
        }
        item {
            MediaProviderCard(
                title = stringResource(R.string.storage_provider_videos),
                icon = Icons.Default.Videocam,
                selectedProvider = storageConfig.videoProvider,
                isConfigured = storageConfig.isProviderConfigured(storageConfig.videoProvider),
                onProviderSelect = { viewModel.updateVideoProvider(it) }
            )
        }
        item {
            MediaProviderCard(
                title = stringResource(R.string.storage_provider_other_files),
                icon = Icons.Default.CloudUpload,
                selectedProvider = storageConfig.otherProvider,
                isConfigured = storageConfig.isProviderConfigured(storageConfig.otherProvider),
                onProviderSelect = { viewModel.updateOtherProvider(it) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(Spacing.Medium))
            Text(
                text = stringResource(R.string.storage_upload_preferences),
                style = SettingsTypography.sectionHeader,
                color = SettingsColors.sectionTitle,
                modifier = Modifier.padding(horizontal = Spacing.Small)
            )
            Spacer(modifier = Modifier.height(Spacing.Small))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = SettingsShapes.itemShape,
                colors = CardDefaults.cardColors(containerColor = SettingsColors.cardBackground)
            ) {
                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(R.string.storage_high_quality_uploads),
                            style = SettingsTypography.itemTitle
                        )
                    },
                    supportingContent = {
                        Text(
                            stringResource(R.string.storage_high_quality_uploads_desc),
                            style = SettingsTypography.itemSubtitle
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = !storageConfig.compressImages,
                            onCheckedChange = { viewModel.updateCompression(!it) }
                        )
                    },
                    colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaProviderCard(
    title: String,
    icon: ImageVector,
    selectedProvider: StorageProvider,
    isConfigured: Boolean,
    onProviderSelect: (String) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val selectedDisplayName = stringResource(selectedProvider.toDisplayNameRes())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showSheet = true },
        shape = SettingsShapes.cardShape,
        colors = CardDefaults.cardColors(containerColor = SettingsColors.cardBackground)
    ) {
        Row(
            modifier = Modifier
                .padding(Spacing.Medium)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(Sizes.AvatarLarge),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Spacing.Large)
                    )
                }
            }

            Spacer(modifier = Modifier.width(Spacing.Medium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = SettingsTypography.itemTitle,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                AssistChip(
                    onClick = { showSheet = true },
                    label = { Text(selectedDisplayName) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isConfigured) Icons.Default.CheckCircle else Icons.Outlined.Key,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize),
                            tint = if (isConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    border = null,
                    shape = SettingsShapes.chipShape
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = SettingsColors.chevronIcon
            )
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = SettingsColors.cardBackgroundElevated
        ) {
            ProviderSelectionList(
                selectedProvider = selectedProvider,
                onProviderSelect = {
                    onProviderSelect(it)
                    showSheet = false
                }
            )
        }
    }
}

@Composable
private fun ProviderSelectionList(
    selectedProvider: StorageProvider,
    onProviderSelect: (String) -> Unit
) {
    val providers = listOf(
        StorageProvider.DEFAULT,
        StorageProvider.IMGBB,
        StorageProvider.CLOUDINARY,
        StorageProvider.SUPABASE,
        StorageProvider.CLOUDFLARE_R2
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.Huge)
    ) {
        Text(
            text = stringResource(R.string.storage_provider_selection),
            style = SettingsTypography.sectionHeader,
            modifier = Modifier.padding(Spacing.Medium)
        )

        providers.forEach { provider ->
            val displayName = stringResource(provider.toDisplayNameRes())
            ListItem(
                headlineContent = { Text(displayName) },
                trailingContent = {
                    if (provider == selectedProvider) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                modifier = Modifier.clickable { onProviderSelect(displayName) },
                colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
            )
        }
    }
}

@Composable
private fun ProviderSetupTab(
    storageConfig: StorageConfig,
    viewModel: SettingsViewModel
) {
    val providers = listOf(
        StorageProvider.IMGBB,
        StorageProvider.CLOUDINARY,
        StorageProvider.SUPABASE,
        StorageProvider.CLOUDFLARE_R2
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {
        items(providers) { provider ->
            val photosLabel = stringResource(R.string.storage_provider_photos)
            val videosLabel = stringResource(R.string.storage_provider_videos)
            val filesLabel = stringResource(R.string.storage_provider_other_files)

            val usage = remember(storageConfig, photosLabel, videosLabel, filesLabel) {
                val usages = mutableListOf<String>()
                if (storageConfig.photoProvider == provider) usages.add(photosLabel)
                if (storageConfig.videoProvider == provider) usages.add(videosLabel)
                if (storageConfig.otherProvider == provider) usages.add(filesLabel)
                usages.joinToString(", ")
            }

            ProviderDashboardTile(
                provider = provider,
                isConfigured = storageConfig.isProviderConfigured(provider),
                usage = usage,
                storageConfig = storageConfig,
                viewModel = viewModel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderDashboardTile(
    provider: StorageProvider,
    isConfigured: Boolean,
    usage: String,
    storageConfig: StorageConfig,
    viewModel: SettingsViewModel
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val displayName = stringResource(provider.toDisplayNameRes())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showSheet = true },
        shape = SettingsShapes.itemShape,
        colors = CardDefaults.cardColors(containerColor = SettingsColors.cardBackground)
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = displayName,
                    style = SettingsTypography.itemTitle
                )
            },
            supportingContent = {
                Column {
                    AnimatedContent(
                        targetState = isConfigured,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "statusBadge"
                    ) { configured ->
                        Surface(
                            shape = SettingsShapes.chipShape,
                            color = if (configured) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = Spacing.ExtraSmall)
                        ) {
                            Text(
                                text = if (configured) stringResource(R.string.storage_ready_to_use)
                                       else stringResource(R.string.storage_requires_setup),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (configured) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = Spacing.Small, vertical = Spacing.Tiny)
                            )
                        }
                    }
                    if (usage.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.storage_used_for, usage),
                            style = SettingsTypography.itemSubtitle
                        )
                    }
                }
            },
            leadingContent = {
                Surface(
                    shape = SettingsShapes.itemShape,
                    color = if (isConfigured) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(Sizes.AvatarDefault)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isConfigured) Icons.Default.CheckCircle else Icons.Outlined.Key,
                            contentDescription = null,
                            tint = if (isConfigured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = SettingsColors.chevronIcon
                )
            },
            colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
        )
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = SettingsColors.cardBackgroundElevated
        ) {
            Box(modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.Large)) {
                when (provider) {
                    StorageProvider.IMGBB -> ImgBBConfigContent(
                        apiKey = storageConfig.imgBBKey,
                        onApiKeyChange = {
                            viewModel.updateImgBBConfig(it)
                            showSheet = false
                        }
                    )
                    StorageProvider.CLOUDINARY -> CloudinaryConfigContent(
                        cloudName = storageConfig.cloudinaryCloudName,
                        apiKey = storageConfig.cloudinaryApiKey,
                        apiSecret = storageConfig.cloudinaryApiSecret,
                        uploadPreset = storageConfig.cloudinaryUploadPreset,
                        onConfigChange = { n, k, s, p ->
                            viewModel.updateCloudinaryConfig(n, k, s, p)
                            showSheet = false
                        }
                    )
                    StorageProvider.SUPABASE -> SupabaseConfigContent(
                        url = storageConfig.supabaseUrl,
                        apiKey = storageConfig.supabaseKey,
                        bucketName = storageConfig.supabaseBucket,
                        onConfigChange = { u, k, b ->
                            viewModel.updateSupabaseConfig(u, k, b)
                            showSheet = false
                        }
                    )
                    StorageProvider.CLOUDFLARE_R2 -> R2ConfigContent(
                        accountId = storageConfig.r2AccountId,
                        accessKeyId = storageConfig.r2AccessKeyId,
                        secretAccessKey = storageConfig.r2SecretAccessKey,
                        bucketName = storageConfig.r2BucketName,
                        onConfigChange = { a, i, s, b ->
                            viewModel.updateR2Config(a, i, s, b)
                            showSheet = false
                        }
                    )
                    else -> {}
                }
            }
            Spacer(modifier = Modifier.height(Spacing.Huge))
        }
    }
}

private fun StorageProvider.toDisplayNameRes(): Int {
    return when (this) {
        StorageProvider.DEFAULT -> R.string.storage_provider_default
        StorageProvider.IMGBB -> R.string.storage_provider_imgbb
        StorageProvider.CLOUDINARY -> R.string.storage_provider_cloudinary
        StorageProvider.SUPABASE -> R.string.storage_provider_supabase
        StorageProvider.CLOUDFLARE_R2 -> R.string.storage_provider_cloudflare_r2
    }
}
