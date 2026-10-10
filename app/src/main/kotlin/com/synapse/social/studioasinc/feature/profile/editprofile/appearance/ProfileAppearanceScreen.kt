package com.synapse.social.studioasinc.feature.profile.editprofile.appearance

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.SynapseTheme
import com.synapse.social.studioasinc.presentation.editprofile.UploadState
import com.synapse.social.studioasinc.ui.components.ExpressiveLoadingIndicator
import com.synapse.social.studioasinc.ui.settings.SettingsHeaderItem
import com.synapse.social.studioasinc.ui.settings.SettingsItemPosition
import com.synapse.social.studioasinc.ui.settings.SettingsNavigationItem
import com.synapse.social.studioasinc.ui.settings.SettingsSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAppearanceScreen(
    viewModel: ProfileAppearanceViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val coverCropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { viewModel.onEvent(ProfileAppearanceEvent.CoverCropped(it)) }
        }
    }

    val coverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onEvent(ProfileAppearanceEvent.CoverSelected(uri))
            coverCropLauncher.launch(
                CropImageContractOptions(
                    uri = uri,
                    cropImageOptions = CropImageOptions(
                        aspectRatioX = 16,
                        aspectRatioY = 9,
                        fixAspectRatio = true
                    )
                )
            )
        }
    }

    fun launchCoverPicker() {
        try {
            coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (e: android.content.ActivityNotFoundException) {
            Toast.makeText(context, context.getString(R.string.no_photo_picker_found), Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                ProfileAppearanceNavigation.NavigateBack -> onNavigateBack()
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    SynapseTheme {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                MediumTopAppBar(
                    title = {
                        Column {
                            Text(text = stringResource(R.string.profile_appearance_title))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.onEvent(ProfileAppearanceEvent.BackClicked) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    actions = {
                        if (uiState.hasPendingChanges) {
                            TextButton(
                                onClick = { viewModel.onEvent(ProfileAppearanceEvent.ApplyChanges) },
                                enabled = !uiState.isSaving
                            ) {
                                Text(
                                    text = stringResource(R.string.action_apply_changes),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.mediumTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ExpressiveLoadingIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(
                        horizontal = SettingsSpacing.screenPadding,
                        vertical = SettingsSpacing.sectionSpacing
                    ),
                    verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)
                ) {
                    // 1. Live Interactive Profile Preview Header
                    item {
                        ProfileAppearanceLivePreview(
                            avatarUrl = uiState.avatarUrl,
                            coverUrl = uiState.coverUrl,
                            pendingCoverUri = uiState.pendingCoverUri,
                            displayName = uiState.profile?.displayName ?: uiState.profile?.username ?: stringResource(R.string.default_user_name),
                            username = uiState.profile?.username ?: "",
                            bio = uiState.profile?.bio,
                            isUploading = uiState.isSaving || uiState.uploadState is UploadState.Uploading
                        )
                    }

                    // 2. Profile Cover Background Settings Group
                    item {
                        ProfileBackgroundGroup(
                            coverUrl = uiState.coverUrl,
                            pendingCoverUri = uiState.pendingCoverUri,
                            uploadState = uiState.uploadState,
                            hasPendingChanges = uiState.hasPendingChanges,
                            onChangeBackground = { launchCoverPicker() },
                            onResetChanges = { viewModel.onEvent(ProfileAppearanceEvent.ResetChanges) },
                            onRemoveBackground = { viewModel.onEvent(ProfileAppearanceEvent.RequestRemoveCover) },
                            onRetryUpload = { viewModel.onEvent(ProfileAppearanceEvent.RetryCoverUpload) }
                        )
                    }

                    // 3. System Appearance Capabilities Info Section
                    item {
                        AppearanceCapabilitiesInfoGroup()
                    }
                }
            }
        }

        if (uiState.showRemoveCoverDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onEvent(ProfileAppearanceEvent.DismissRemoveCoverDialog) },
                title = { Text(text = stringResource(R.string.dialog_remove_background_title)) },
                text = { Text(text = stringResource(R.string.dialog_remove_background_body)) },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.onEvent(ProfileAppearanceEvent.ConfirmRemoveCover) }
                    ) {
                        Text(text = stringResource(R.string.remove))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.onEvent(ProfileAppearanceEvent.DismissRemoveCoverDialog) }
                    ) {
                        Text(text = stringResource(R.string.cancel))
                    }
                }
            )
        }

        if (uiState.showUnsavedChangesDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onEvent(ProfileAppearanceEvent.DismissUnsavedDialog) },
                title = { Text(text = stringResource(R.string.dialog_unsaved_changes_title)) },
                text = { Text(text = stringResource(R.string.dialog_unsaved_changes_body)) },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.onEvent(ProfileAppearanceEvent.ConfirmDiscardUnsaved) }
                    ) {
                        Text(text = stringResource(R.string.dialog_discard))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.onEvent(ProfileAppearanceEvent.DismissUnsavedDialog) }
                    ) {
                        Text(text = stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun ProfileAppearanceLivePreview(
    avatarUrl: String?,
    coverUrl: String?,
    pendingCoverUri: android.net.Uri?,
    displayName: String,
    username: String,
    bio: String?,
    isUploading: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // Cover Background Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    val coverModel: Any? = pendingCoverUri ?: coverUrl
                    if (coverModel != null) {
                        AsyncImage(
                            model = coverModel,
                            contentDescription = stringResource(R.string.cd_cover_preview),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    if (isUploading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            ExpressiveLoadingIndicator()
                        }
                    }

                    // "Live Preview" Pill Badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        tonalElevation = 2.dp
                    ) {
                        Text(
                            text = stringResource(R.string.live_preview_label),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Profile Avatar Overlap
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp)
                        .offset(y = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = stringResource(R.string.cd_avatar_preview),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                }
            }

            // User Info Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp)
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (username.isNotBlank()) {
                    Text(
                        text = "@$username",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!bio.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileBackgroundGroup(
    coverUrl: String?,
    pendingCoverUri: android.net.Uri?,
    uploadState: UploadState,
    hasPendingChanges: Boolean,
    onChangeBackground: () -> Unit,
    onResetChanges: () -> Unit,
    onRemoveBackground: () -> Unit,
    onRetryUpload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SettingsHeaderItem(title = stringResource(R.string.section_profile_background))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SettingsSpacing.itemSpacing)
        ) {
            if (uploadState is UploadState.Error) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SettingsItemPosition.Top.getShape(),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uploadState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onRetryUpload) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.retry_upload),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Change Cover Background
            SettingsNavigationItem(
                title = stringResource(R.string.action_change_background),
                subtitle = stringResource(R.string.choose_from_gallery_subtitle),
                imageVector = Icons.Filled.AddPhotoAlternate,
                onClick = onChangeBackground,
                position = if (uploadState is UploadState.Error) SettingsItemPosition.Middle else SettingsItemPosition.Top
            )

            // Reset Changes (when pending draft exists)
            if (hasPendingChanges) {
                SettingsNavigationItem(
                    title = stringResource(R.string.action_reset_background),
                    subtitle = stringResource(R.string.dialog_unsaved_changes_body),
                    imageVector = Icons.Filled.RestartAlt,
                    onClick = onResetChanges,
                    position = SettingsItemPosition.Middle
                )
            }

            // Remove Cover Background (when cover or pending draft exists)
            if (!coverUrl.isNullOrBlank() || pendingCoverUri != null) {
                SettingsNavigationItem(
                    title = stringResource(R.string.action_remove_background),
                    subtitle = stringResource(R.string.dialog_remove_background_body),
                    imageVector = Icons.Filled.Delete,
                    onClick = onRemoveBackground,
                    position = SettingsItemPosition.Bottom
                )
            }
        }
    }
}

@Composable
private fun AppearanceCapabilitiesInfoGroup(
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SettingsHeaderItem(title = stringResource(R.string.section_appearance_info))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.appearance_capabilities_info),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
