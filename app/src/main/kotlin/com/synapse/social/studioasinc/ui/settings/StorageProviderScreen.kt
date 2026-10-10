package com.synapse.social.studioasinc.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back_button)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SettingsColors.screenBackground,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
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
        contentPadding = PaddingValues(
            horizontal = SettingsSpacing.screenPadding,
            vertical = Spacing.Medium
        ),
        verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)
    ) {
        item {
            SettingsSection(title = stringResource(R.string.storage_tab_assign)) {
                SettingsCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        MediaAssignmentRow(
                            title = stringResource(R.string.storage_provider_photos),
                            icon = Icons.Default.Image,
                            selectedProvider = storageConfig.photoProvider,
                            isPhotos = true,
                            onProviderSelect = { viewModel.updatePhotoProvider(it) }
                        )
                        SettingsDivider()
                        MediaAssignmentRow(
                            title = stringResource(R.string.storage_provider_videos),
                            icon = Icons.Default.Videocam,
                            selectedProvider = storageConfig.videoProvider,
                            isPhotos = false,
                            onProviderSelect = { viewModel.updateVideoProvider(it) }
                        )
                        SettingsDivider()
                        MediaAssignmentRow(
                            title = stringResource(R.string.storage_provider_other_files),
                            icon = Icons.Default.CloudUpload,
                            selectedProvider = storageConfig.otherProvider,
                            isPhotos = false,
                            onProviderSelect = { viewModel.updateOtherProvider(it) }
                        )
                    }
                }
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.storage_upload_preferences)) {
                SettingsToggleItem(
                    title = stringResource(R.string.storage_high_quality_uploads),
                    subtitle = stringResource(R.string.storage_high_quality_uploads_desc),
                    checked = !storageConfig.compressImages,
                    onCheckedChange = { viewModel.updateCompression(!it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaAssignmentRow(
    title: String,
    icon: ImageVector,
    selectedProvider: StorageProvider,
    isPhotos: Boolean = true,
    onProviderSelect: (String) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val selectedDisplayName = stringResource(selectedProvider.toDisplayNameRes())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showSheet = true }
            .padding(
                horizontal = SettingsSpacing.itemHorizontalPadding,
                vertical = SettingsSpacing.itemVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconBadge(imageVector = icon)

        Spacer(modifier = Modifier.width(SettingsSpacing.iconTextSpacing))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = SettingsTypography.itemTitle,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
            Text(
                text = selectedDisplayName,
                style = SettingsTypography.itemSubtitle,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.width(Spacing.Medium))

        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = SettingsColors.chevronIcon
        )
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = SettingsColors.cardBackgroundElevated
        ) {
            ProviderSelectionList(
                title = title,
                selectedProvider = selectedProvider,
                isPhotos = isPhotos,
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
    title: String,
    selectedProvider: StorageProvider,
    isPhotos: Boolean = true,
    onProviderSelect: (String) -> Unit
) {
    val providers = if (isPhotos) {
        listOf(
            StorageProvider.DEFAULT,
            StorageProvider.IMGBB,
            StorageProvider.CLOUDINARY,
            StorageProvider.SUPABASE,
            StorageProvider.CLOUDFLARE_R2
        )
    } else {
        listOf(
            StorageProvider.DEFAULT,
            StorageProvider.CLOUDINARY,
            StorageProvider.SUPABASE,
            StorageProvider.CLOUDFLARE_R2
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.Huge)
    ) {
        Text(
            text = stringResource(R.string.storage_provider_selection) + " - $title",
            style = SettingsTypography.sectionHeader,
            modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.SmallMedium)
        )

        providers.forEachIndexed { index, provider ->
            val displayName = stringResource(provider.toDisplayNameRes())
            val isSelected = provider == selectedProvider

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onProviderSelect(displayName) }
                    .padding(horizontal = Spacing.Medium, vertical = Spacing.Medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = displayName,
                    style = SettingsTypography.itemTitle,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )

                Icon(
                    imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (isSelected) "Selected" else "Not selected",
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (index < providers.size - 1) {
                SettingsDivider()
            }
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
        contentPadding = PaddingValues(
            horizontal = SettingsSpacing.screenPadding,
            vertical = Spacing.Medium
        ),
        verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)
    ) {
        item {
            SettingsSection(title = stringResource(R.string.storage_provider_config)) {
                SettingsCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        providers.forEachIndexed { index, provider ->
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

                            ProviderRow(
                                provider = provider,
                                isConfigured = storageConfig.isProviderConfigured(provider),
                                usage = usage,
                                storageConfig = storageConfig,
                                viewModel = viewModel
                            )

                            if (index < providers.size - 1) {
                                SettingsDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderRow(
    provider: StorageProvider,
    isConfigured: Boolean,
    usage: String,
    storageConfig: StorageConfig,
    viewModel: SettingsViewModel
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val displayName = stringResource(provider.toDisplayNameRes())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showSheet = true }
            .padding(
                horizontal = SettingsSpacing.itemHorizontalPadding,
                vertical = SettingsSpacing.itemVerticalPadding
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconBadge(
            imageVector = if (isConfigured) Icons.Default.CheckCircle else Icons.Outlined.Key
        )

        Spacer(modifier = Modifier.width(SettingsSpacing.iconTextSpacing))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayName,
                    style = SettingsTypography.itemTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(Spacing.Small))
                Surface(
                    shape = SettingsShapes.chipShape,
                    color = if (isConfigured) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (isConfigured) stringResource(R.string.storage_ready_to_use)
                               else stringResource(R.string.storage_requires_setup),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isConfigured) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.Small, vertical = Spacing.Tiny)
                    )
                }
            }

            if (usage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                Text(
                    text = stringResource(R.string.storage_used_for, usage),
                    style = SettingsTypography.itemSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(Spacing.Medium))

        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = SettingsColors.chevronIcon
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
