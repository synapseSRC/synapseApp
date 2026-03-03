package com.synapse.social.studioasinc.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.presentation.editprofile.EditProfileEvent
import com.synapse.social.studioasinc.presentation.editprofile.EditProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarScreen(
    viewModel: EditProfileViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    var showRemoveDialog by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            Toast.makeText(context, "Photo selected: $uri", Toast.LENGTH_SHORT).show()
            viewModel.onEvent(EditProfileEvent.AvatarSelected(uri))
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            Toast.makeText(context, "Photo captured", Toast.LENGTH_SHORT).show()
            // TODO: Upload bitmap to profile
        }
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove Profile Photo") },
            text = { Text("Are you sure you want to remove your profile photo?") },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveDialog = false
                    viewModel.removeProfilePhoto(onSuccess = {
                        Toast.makeText(context, "Profile photo removed", Toast.LENGTH_SHORT).show()
                    })
                }) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text("Avatar") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
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
                SettingsSection(title = "Profile Photo") {
                    SettingsClickableItem(
                        title = "Choose from Gallery",
                        subtitle = "Select a photo from your device",
                        position = SettingsItemPosition.Top,
                        onClick = {
                            try {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            } catch (e: android.content.ActivityNotFoundException) {
                                Toast.makeText(context, "No photo picker available", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    SettingsClickableItem(
                        title = "Take Photo",
                        subtitle = "Capture a new photo with camera",
                        position = SettingsItemPosition.Middle,
                        onClick = {
                            try {
                                cameraLauncher.launch(null)
                            } catch (e: android.content.ActivityNotFoundException) {
                                Toast.makeText(context, "No camera available", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    SettingsClickableItem(
                        title = "Remove Profile Photo",
                        subtitle = "Use default avatar",
                        position = SettingsItemPosition.Bottom,
                        onClick = { showRemoveDialog = true }
                    )
                }
            }

            item {
                SettingsSection(title = "Avatar Creation") {
                    SettingsClickableItem(
                        title = "Create Avatar",
                        subtitle = "Design a custom avatar",
                        position = SettingsItemPosition.Top,
                        onClick = {
                            Toast.makeText(context, "Avatar creator coming soon", Toast.LENGTH_SHORT).show()
                        }
                    )
                    SettingsClickableItem(
                        title = "Edit Avatar",
                        subtitle = "Modify existing avatar",
                        position = SettingsItemPosition.Bottom,
                        onClick = {
                            Toast.makeText(context, "Avatar editor coming soon", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}
