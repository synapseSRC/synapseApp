package com.synapse.social.studioasinc.feature.shared.components.picker

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.feature.shared.theme.Sizes
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SynapseFilePicker(
    onDismissRequest: () -> Unit,
    onFilesSelected: (List<PickedFile>) -> Unit,
    maxSelection: Int = 1,
    viewModel: FilePickerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val mediaPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    var permissionGranted by remember { mutableStateOf(false) }
    var contactPermissionGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it } ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                permissions[Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == true)
        permissionGranted = granted
        if (granted) viewModel.loadMedia()
    }

    val contactPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        contactPermissionGranted = granted
        if (granted) viewModel.loadContacts()
    }

    LaunchedEffect(Unit) {
        val granted = mediaPermissions.all {
            context.checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            context.checkSelfPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == android.content.pm.PackageManager.PERMISSION_GRANTED)

        permissionGranted = granted
        contactPermissionGranted = context.checkSelfPermission(Manifest.permission.READ_CONTACTS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!granted) permissionLauncher.launch(mediaPermissions) else viewModel.loadMedia()
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.selectedCategory != FilePickerCategory.CONTACT && !permissionGranted -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(R.string.permission_media_required))
                        Button(onClick = { permissionLauncher.launch(mediaPermissions) }) {
                            Text(stringResource(R.string.action_grant_permission))
                        }
                    }
                }
                uiState.selectedCategory == FilePickerCategory.CONTACT && !contactPermissionGranted -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(R.string.permission_contacts_required))
                        Button(onClick = { contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) }) {
                            Text(stringResource(R.string.action_grant_permission))
                        }
                    }
                }
                else -> {
                    when (uiState.selectedCategory) {
                        FilePickerCategory.MEDIA -> {
                            MediaGridContent(
                                mediaItems = viewModel.filteredMediaItems,
                                selectedUris = uiState.selectedUris,
                                isLoading = uiState.isLoading,
                                maxSelection = maxSelection,
                                mediaFilter = uiState.mediaFilter,
                                onFilterChanged = viewModel::setFilter,
                                onFileClicked = { file ->
                                    if (maxSelection == 1) {
                                        onFilesSelected(listOf(file))
                                        onDismissRequest()
                                    } else {
                                        viewModel.toggleSelection(file.uri, maxSelection)
                                    }
                                }
                            )
                        }
                        FilePickerCategory.DOCS, FilePickerCategory.AUDIO, FilePickerCategory.FILE -> {
                            FileListContent(
                                files = uiState.fileItems,
                                isLoading = uiState.isLoading,
                                category = uiState.selectedCategory,
                                onFileClicked = { file ->
                                    onFilesSelected(listOf(file))
                                    onDismissRequest()
                                },
                                modifier = Modifier.padding(bottom = Spacing.Huge)
                            )
                        }
                        FilePickerCategory.CONTACT -> {
                            FileListContent(
                                files = uiState.contactItems,
                                isLoading = uiState.isLoading,
                                category = uiState.selectedCategory,
                                onFileClicked = { file ->
                                    onFilesSelected(listOf(file))
                                    onDismissRequest()
                                },
                                modifier = Modifier.padding(bottom = Spacing.Huge)
                            )
                        }
                    }
                }
            }

            // Floating bottom dock
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Spacing.Large),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(visible = uiState.selectedUris.isNotEmpty() && maxSelection > 1) {
                    Button(
                        onClick = {
                            val selectedFiles = uiState.mediaItems.filter { it.uri in uiState.selectedUris }
                            onFilesSelected(selectedFiles)
                            onDismissRequest()
                        },
                        modifier = Modifier.padding(bottom = Spacing.Medium)
                    ) {
                        Text(stringResource(R.string.picker_send_button, uiState.selectedUris.size))
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = Spacing.Small, vertical = Spacing.ExtraSmall)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                    ) {
                        FilePickerCategory.values().forEach { category ->
                            IconButton(
                                onClick = {
                                    viewModel.setCategory(category)
                                    when (category) {
                                        FilePickerCategory.MEDIA -> viewModel.loadMedia()
                                        FilePickerCategory.DOCS, FilePickerCategory.AUDIO, FilePickerCategory.FILE ->
                                            viewModel.loadFiles(category)
                                        FilePickerCategory.CONTACT -> {
                                            if (contactPermissionGranted) viewModel.loadContacts()
                                            else contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(Sizes.AvatarDefault)
                                    .background(
                                        color = if (uiState.selectedCategory == category)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = stringResource(category.labelResId),
                                    tint = if (uiState.selectedCategory == category)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(Sizes.IconLarge)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
