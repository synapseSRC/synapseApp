package com.synapse.social.studioasinc.feature.profile.editprofile.media

import android.content.Intent
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.synapse.social.studioasinc.CreatePostActivity
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
fun ProfileMediaScreen(
    viewModel: ProfileMediaViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToPhotoHistory: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val avatarCropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { viewModel.onEvent(ProfileMediaEvent.AvatarCropped(it)) }
        }
    }

    val coverCropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { viewModel.onEvent(ProfileMediaEvent.CoverCropped(it)) }
        }
    }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onEvent(ProfileMediaEvent.AvatarSelected(uri))
            avatarCropLauncher.launch(
                CropImageContractOptions(
                    uri = uri,
                    cropImageOptions = CropImageOptions(
                        aspectRatioX = 1,
                        aspectRatioY = 1,
                        fixAspectRatio = true
                    )
                )
            )
        }
    }

    val coverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onEvent(ProfileMediaEvent.CoverSelected(uri))
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

    fun launchAvatarPicker() {
        try {
            avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (e: android.content.ActivityNotFoundException) {
            Toast.makeText(context, context.getString(R.string.no_photo_picker_found), Toast.LENGTH_LONG).show()
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
                ProfileMediaNavigation.NavigateBack -> onNavigateBack()
                ProfileMediaNavigation.NavigateToAvatarHistory -> onNavigateToPhotoHistory("PROFILE")
                ProfileMediaNavigation.NavigateToCoverHistory -> onNavigateToPhotoHistory("COVER")
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
                            Text(text = stringResource(R.string.profile_media_title))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.onEvent(ProfileMediaEvent.BackClicked) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
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
                    // Profile Media Visual Live Preview Header
                    item {
                        ProfileMediaVisualPreview(
                            avatarUrl = uiState.avatarUrl,
                            coverUrl = uiState.coverUrl,
                            avatarUploadState = uiState.avatarUploadState,
                            coverUploadState = uiState.coverUploadState
                        )
                    }

                    // Profile Photo Section
                    item {
                        ProfilePhotoGroup(
                            avatarUrl = uiState.avatarUrl,
                            uploadState = uiState.avatarUploadState,
                            onChangePhoto = { launchAvatarPicker() },
                            onRemovePhoto = { viewModel.onEvent(ProfileMediaEvent.RequestRemoveAvatar) },
                            onViewHistory = { viewModel.onEvent(ProfileMediaEvent.ViewAvatarHistoryClicked) },
                            onRetryUpload = { viewModel.onEvent(ProfileMediaEvent.RetryAvatarUpload) }
                        )
                    }

                    // Cover Photo Section
                    item {
                        CoverPhotoGroup(
                            coverUrl = uiState.coverUrl,
                            uploadState = uiState.coverUploadState,
                            onChangeCover = { launchCoverPicker() },
                            onRemoveCover = { viewModel.onEvent(ProfileMediaEvent.RequestRemoveCover) },
                            onViewHistory = { viewModel.onEvent(ProfileMediaEvent.ViewCoverHistoryClicked) },
                            onRetryUpload = { viewModel.onEvent(ProfileMediaEvent.RetryCoverUpload) }
                        )
                    }
                }
            }
        }

        if (uiState.showRemoveAvatarDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onEvent(ProfileMediaEvent.DismissRemoveAvatarDialog) },
                title = { Text(text = stringResource(R.string.remove_profile_photo_title)) },
                text = { Text(text = stringResource(R.string.remove_profile_photo_body)) },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.onEvent(ProfileMediaEvent.ConfirmRemoveAvatar) }
                    ) {
                        Text(text = stringResource(R.string.remove))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.onEvent(ProfileMediaEvent.DismissRemoveAvatarDialog) }
                    ) {
                        Text(text = stringResource(R.string.cancel))
                    }
                }
            )
        }

        if (uiState.showRemoveCoverDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onEvent(ProfileMediaEvent.DismissRemoveCoverDialog) },
                title = { Text(text = stringResource(R.string.remove_cover_photo_title)) },
                text = { Text(text = stringResource(R.string.remove_cover_photo_body)) },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.onEvent(ProfileMediaEvent.ConfirmRemoveCover) }
                    ) {
                        Text(text = stringResource(R.string.remove))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.onEvent(ProfileMediaEvent.DismissRemoveCoverDialog) }
                    ) {
                        Text(text = stringResource(R.string.cancel))
                    }
                }
            )
        }

        if (uiState.showShareToFeedDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onEvent(ProfileMediaEvent.DismissShareToFeedDialog) },
                title = { Text(text = stringResource(R.string.dialog_share_profile_pic_title)) },
                text = { Text(text = stringResource(R.string.dialog_share_profile_pic_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val intent = Intent(context, CreatePostActivity::class.java).apply {
                                putExtra(CreatePostActivity.EXTRA_PREFILL_IMAGE_URL, uiState.avatarUrl)
                            }
                            context.startActivity(intent)
                            viewModel.onEvent(ProfileMediaEvent.DismissShareToFeedDialog)
                        }
                    ) {
                        Text(text = stringResource(R.string.okay))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.onEvent(ProfileMediaEvent.DismissShareToFeedDialog) }
                    ) {
                        Text(text = stringResource(R.string.no))
                    }
                }
            )
        }
    }
}

@Composable
private fun ProfileMediaVisualPreview(
    avatarUrl: String?,
    coverUrl: String?,
    avatarUploadState: UploadState,
    coverUploadState: UploadState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Cover Banner Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    if (!coverUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = coverUrl,
                            contentDescription = stringResource(R.string.cd_cover_preview),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    if (coverUploadState is UploadState.Uploading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            ExpressiveLoadingIndicator()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }

            // Overlapping Avatar Preview
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp)
                    .offset(y = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
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
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    if (avatarUploadState is UploadState.Uploading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            ExpressiveLoadingIndicator()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfilePhotoGroup(
    avatarUrl: String?,
    uploadState: UploadState,
    onChangePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onViewHistory: () -> Unit,
    onRetryUpload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SettingsHeaderItem(title = stringResource(R.string.section_profile_photo))

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

            // Change Photo
            SettingsNavigationItem(
                title = stringResource(R.string.action_change_photo),
                subtitle = stringResource(R.string.choose_from_gallery_subtitle),
                imageVector = Icons.Filled.AddPhotoAlternate,
                onClick = onChangePhoto,
                position = if (uploadState is UploadState.Error) SettingsItemPosition.Middle else SettingsItemPosition.Top
            )

            // Remove Photo (only if avatar exists)
            if (!avatarUrl.isNullOrBlank()) {
                SettingsNavigationItem(
                    title = stringResource(R.string.action_remove_photo),
                    subtitle = stringResource(R.string.remove_profile_photo_subtitle),
                    imageVector = Icons.Filled.Delete,
                    onClick = onRemovePhoto,
                    position = SettingsItemPosition.Middle
                )
            }

            // View Photo History
            SettingsNavigationItem(
                title = stringResource(R.string.action_view_photo_history),
                subtitle = stringResource(R.string.nav_profile_media_sub),
                imageVector = Icons.Filled.History,
                onClick = onViewHistory,
                position = SettingsItemPosition.Bottom
            )
        }
    }
}

@Composable
private fun CoverPhotoGroup(
    coverUrl: String?,
    uploadState: UploadState,
    onChangeCover: () -> Unit,
    onRemoveCover: () -> Unit,
    onViewHistory: () -> Unit,
    onRetryUpload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SettingsHeaderItem(title = stringResource(R.string.section_cover_photo))

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

            // Change Cover
            SettingsNavigationItem(
                title = stringResource(R.string.action_change_cover),
                subtitle = stringResource(R.string.choose_from_gallery_subtitle),
                imageVector = Icons.Filled.AddPhotoAlternate,
                onClick = onChangeCover,
                position = if (uploadState is UploadState.Error) SettingsItemPosition.Middle else SettingsItemPosition.Top
            )

            // Remove Cover (only if cover exists)
            if (!coverUrl.isNullOrBlank()) {
                SettingsNavigationItem(
                    title = stringResource(R.string.action_remove_cover),
                    subtitle = stringResource(R.string.remove_cover_photo_body),
                    imageVector = Icons.Filled.Delete,
                    onClick = onRemoveCover,
                    position = SettingsItemPosition.Middle
                )
            }

            // View Cover History
            SettingsNavigationItem(
                title = stringResource(R.string.action_view_cover_history),
                subtitle = stringResource(R.string.nav_cover_photo_history_sub),
                imageVector = Icons.Filled.History,
                onClick = onViewHistory,
                position = SettingsItemPosition.Bottom
            )
        }
    }
}
