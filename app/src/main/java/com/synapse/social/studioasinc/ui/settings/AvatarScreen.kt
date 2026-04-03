package com.synapse.social.studioasinc.ui.settings

import android.widget.Toast
import android.Manifest
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.core.util.ImageLoader
import com.synapse.social.studioasinc.presentation.editprofile.EditProfileEvent
import com.synapse.social.studioasinc.presentation.editprofile.EditProfileViewModel
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@Composable
private fun AvatarPreviewCard(
    avatarUrl: String?,
    isUploading: Boolean
) {
    val context = LocalContext.current
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsShapes.cardShape,
        color = SettingsColors.cardBackgroundElevated,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Sizes.IconGiant),
                        strokeWidth = 4.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    if (avatarUrl != null && avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageLoader.buildImageRequest(context, avatarUrl),
                            contentDescription = stringResource(R.string.current_profile_photo),
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = stringResource(R.string.default_avatar),
                                    modifier = Modifier.size(Sizes.AvatarLarge),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.Medium))
            
            Text(
                text = if (isUploading) stringResource(R.string.uploading) else stringResource(R.string.current_profile_photo_label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (!isUploading) {
                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                Text(
                    text = stringResource(R.string.choose_option_below),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarScreen(
    viewModel: AvatarViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    var showRemoveDialog by remember { mutableStateOf(false) }
    val isRemoving by viewModel.isRemoving.collectAsState()

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessages()
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadPhoto(uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.uploadBitmap(bitmap)
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, context.getString(R.string.camera_permission_required), Toast.LENGTH_SHORT).show()
        }
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text(stringResource(R.string.remove_profile_photo_title)) },
            text = { Text(stringResource(R.string.remove_profile_photo_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeProfilePhoto(
                            onSuccess = {
                                showRemoveDialog = false
                                Toast.makeText(context, context.getString(R.string.profile_photo_removed), Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                showRemoveDialog = false
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    enabled = !isRemoving
                ) {
                    if (isRemoving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Sizes.IconLarge),
                            strokeWidth = Sizes.BorderDefault
                        )
                    } else {
                        Text(stringResource(R.string.action_remove))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.avatar_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = SettingsSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(SettingsSpacing.sectionSpacing)
        ) {
            item {
                Spacer(modifier = Modifier.height(Spacing.Small))
                AvatarPreviewCard(
                    avatarUrl = uiState.currentAvatarUrl,
                    isUploading = uiState.isUploading
                )
            }

            item {
                SettingsSection(title = stringResource(R.string.profile_photo_section)) {
                    SettingsNavigationItem(
                        title = stringResource(R.string.choose_from_gallery),
                        subtitle = stringResource(R.string.choose_from_gallery_subtitle),
                        imageVector = Icons.Filled.Image,
                        position = SettingsItemPosition.Top,
                        onClick = {
                            try {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            } catch (e: android.content.ActivityNotFoundException) {
                                Toast.makeText(context, context.getString(R.string.no_photo_picker), Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        title = stringResource(R.string.take_photo),
                        subtitle = stringResource(R.string.take_photo_subtitle),
                        imageVector = Icons.Filled.CameraAlt,
                        position = SettingsItemPosition.Middle,
                        onClick = {
                            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                try {
                                    cameraLauncher.launch(null)
                                } catch (e: android.content.ActivityNotFoundException) {
                                    Toast.makeText(context, context.getString(R.string.no_camera_available), Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        title = stringResource(R.string.remove_profile_photo_title),
                        subtitle = stringResource(R.string.remove_profile_photo_subtitle),
                        imageVector = Icons.Filled.Delete,
                        position = SettingsItemPosition.Bottom,
                        onClick = { showRemoveDialog = true }
                    )
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.avatar_creation_section)) {
                    SettingsNavigationItem(
                        title = stringResource(R.string.create_avatar),
                        subtitle = stringResource(R.string.create_avatar_subtitle),
                        imageVector = Icons.Filled.Add,
                        position = SettingsItemPosition.Top,
                        onClick = {
                            Toast.makeText(context, context.getString(R.string.avatar_creator_soon), Toast.LENGTH_SHORT).show()
                        }
                    )
                    SettingsDivider()
                    SettingsNavigationItem(
                        title = stringResource(R.string.edit_avatar),
                        subtitle = stringResource(R.string.edit_avatar_subtitle),
                        imageVector = Icons.Filled.Edit,
                        position = SettingsItemPosition.Bottom,
                        onClick = {
                            Toast.makeText(context, context.getString(R.string.avatar_editor_soon), Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}
